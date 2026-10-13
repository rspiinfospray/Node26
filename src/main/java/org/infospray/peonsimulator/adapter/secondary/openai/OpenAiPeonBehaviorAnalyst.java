package org.infospray.peonsimulator.adapter.secondary.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.infospray.peonsimulator.application.dto.PeonBehaviorAnalysisRequest;
import org.infospray.peonsimulator.application.port.secondary.PeonBehaviorAnalyst;
import org.infospray.peonsimulator.configuration.OpenAiReportProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiPeonBehaviorAnalyst implements PeonBehaviorAnalyst {
    private final ObjectMapper objectMapper;
    private final OpenAiReportProperties properties;
    private final RestClient restClient;

    public OpenAiPeonBehaviorAnalyst(ObjectMapper objectMapper, OpenAiReportProperties properties, RestClient.Builder restClientBuilder) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public String analyze(PeonBehaviorAnalysisRequest request) {
        if (this.properties.getApiKey() == null || this.properties.getApiKey().isBlank()) { throw new IllegalStateException("OPENAI_API_KEY n'est pas configurée"); }
        Map<String, Object> body = Map.of("model", this.properties.getModel(), "instructions", this.instructions(), "input", this.serialize(request));
        JsonNode response = this.restClient.post().uri(this.properties.getEndpoint()).header(HttpHeaders.AUTHORIZATION, "Bearer " + this.properties.getApiKey()).contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);
        if (response == null) { throw new IllegalStateException("L'API OpenAI a renvoyé une réponse vide"); }
        for (JsonNode output : response.path("output")) {
            if (!"message".equals(output.path("type").asText())) { continue; }
            for (JsonNode content : output.path("content")) {
                if ("output_text".equals(content.path("type").asText()) && !content.path("text").asText().isBlank()) { return content.path("text").asText(); }
            }
        }
        throw new IllegalStateException("La réponse OpenAI ne contient aucune analyse textuelle");
    }

    private String instructions() {
        return "Tu es un analyste expert du moteur de simulation Node26. Rédige en français uniquement la section Markdown consacrée au péon fourni. Appuie chaque constat sur les données du diagnostic, distingue les faits des hypothèses, analyse la cohérence entre sa personnalité, sa stratégie à long terme, ses décisions et leurs résultats. Fais ressortir les comportements irrationnels, les occasions manquées, les boucles, les décisions ayant menacé sa survie et les défauts probables du moteur. Propose ensuite des préconisations concrètes, priorisées et testables pour améliorer les règles ou le moteur de décision. N'invente aucun événement. Utilise les titres: Résumé, Chronologie comportementale, Comportements rationnels, Comportements irrationnels, Causes probables, Préconisations. Ne répète pas un titre de niveau 1 et ne place pas le résultat dans un bloc de code.";
    }

    private String serialize(PeonBehaviorAnalysisRequest request) {
        try { return this.objectMapper.writeValueAsString(request); } catch (Exception exception) { throw new IllegalStateException("Impossible de préparer les données de diagnostic", exception); }
    }
}
