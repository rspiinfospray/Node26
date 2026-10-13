package org.infospray.peonsimulator.supervisor.adapter.secondary.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.infospray.peonsimulator.supervisor.application.port.secondary.SimulationCycleLauncher;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WorldSimulationCycleLauncher implements SimulationCycleLauncher {
    private final URI apiBaseUri;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public WorldSimulationCycleLauncher(String apiBaseUrl, ObjectMapper objectMapper) {
        this.apiBaseUri = URI.create(apiBaseUrl);
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Override
    public void launch(int cycleNumber) {
        this.awaitBackend();
        JsonNode template = this.finishedWorldTemplate();
        Map<String, Object> request = this.creationRequest(template, cycleNumber);
        JsonNode created = this.send("POST", "/api/worlds", request);
        this.send("POST", "/api/worlds/" + created.path("id").asText() + "/start", null);
    }

    private void awaitBackend() {
        Instant deadline = Instant.now().plus(Duration.ofMinutes(2));
        while (Instant.now().isBefore(deadline)) {
            try { this.send("GET", "/api/worlds", null); return; } catch (RuntimeException exception) {
                try { Thread.sleep(Duration.ofSeconds(2)); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException("Attente du backend interrompue", interrupted); }
            }
        }
        throw new IllegalStateException("Le backend n'est pas disponible après le déploiement");
    }

    private JsonNode finishedWorldTemplate() {
        JsonNode worlds = this.send("GET", "/api/worlds", null);
        List<JsonNode> finished = new ArrayList<>();
        worlds.forEach(world -> { if ("FINISHED".equals(world.path("status").asText())) { finished.add(world); } });
        return finished.stream().max(Comparator.comparingLong(world -> world.path("sequenceNumber").asLong())).orElseThrow(() -> new IllegalStateException("Aucun monde terminé ne peut servir de modèle"));
    }

    private Map<String, Object> creationRequest(JsonNode world, int cycleNumber) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("name", world.path("name").asText() + "-autofix-" + cycleNumber);
        request.put("width", world.path("width").asInt());
        request.put("height", world.path("height").asInt());
        request.put("rockPercentage", world.path("rockPercentage").asInt());
        request.put("treePercentage", world.path("treePercentage").asInt());
        request.put("foodCellPercentage", world.path("foodCellPercentage").asInt());
        request.put("minFoodPerCell", world.path("minFoodPerCell").asInt());
        request.put("maxFoodPerCell", world.path("maxFoodPerCell").asInt());
        request.put("sheepCount", world.path("sheepCount").asInt());
        request.put("hungerHealthLossPerTurn", world.path("hungerHealthLossPerTurn").asInt());
        request.put("maxRounds", world.path("maxRounds").asInt());
        request.put("seed", world.path("seed").asLong());
        List<Map<String, Object>> teams = new ArrayList<>();
        world.path("teams").elements().forEachRemaining(team -> teams.add(Map.of("name", team.path("name").asText(), "peonCount", this.teamPeonCount(world, team.path("id").asText()))));
        request.put("teams", teams);
        return request;
    }

    private int teamPeonCount(JsonNode world, String teamId) {
        int count = 0;
        var peons = world.path("peons").elements();
        while (peons.hasNext()) { if (teamId.equals(peons.next().path("teamId").asText())) { count++; } }
        return count;
    }

    private JsonNode send(String method, String path, Object body) {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(this.apiBaseUri.resolve(path)).timeout(Duration.ofMinutes(2)).header("Content-Type", "application/json");
            if (body == null) { request.method(method, HttpRequest.BodyPublishers.noBody()); } else { request.method(method, HttpRequest.BodyPublishers.ofString(this.objectMapper.writeValueAsString(body))); }
            HttpResponse<String> response = this.httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) { throw new IllegalStateException("API Node26 en échec (" + response.statusCode() + "): " + response.body()); }
            return response.body().isBlank() ? this.objectMapper.createObjectNode() : this.objectMapper.readTree(response.body());
        } catch (IOException exception) { throw new IllegalStateException("Erreur d'appel à l'API Node26", exception); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException("Appel à l'API Node26 interrompu", exception); }
    }
}
