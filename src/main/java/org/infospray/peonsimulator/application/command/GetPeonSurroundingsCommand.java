package org.infospray.peonsimulator.application.command;

import org.infospray.peonsimulator.application.port.secondary.WorldRepository;
import org.infospray.peonsimulator.domain.model.Peon;
import org.infospray.peonsimulator.domain.model.World;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class GetPeonSurroundingsCommand {
    private final WorldRepository worldRepository;
    public GetPeonSurroundingsCommand(WorldRepository worldRepository) { this.worldRepository = worldRepository; }

    public Map<String, Object> execute(UUID worldId, UUID peonId, long sequenceNumber, int radius) {
        World world = this.worldRepository.findAtSequence(worldId, sequenceNumber).orElseThrow(() -> new IllegalArgumentException("État historique introuvable: " + sequenceNumber));
        Peon peon = world.getPeons().get(peonId);
        if (peon == null) { throw new IllegalArgumentException("Peon introuvable: " + peonId); }
        int safeRadius = Math.max(1, Math.min(radius, 12));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("worldId", worldId); result.put("sequence", sequenceNumber); result.put("peon", peon);
        result.put("cells", world.getCells().entrySet().stream().filter(entry -> entry.getValue().getCoordinate().distanceTo(peon.getPosition()) <= safeRadius).collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        result.put("houses", world.getHouses().values().stream().filter(value -> value.getPosition().distanceTo(peon.getPosition()) <= safeRadius).toList());
        result.put("granaries", world.getGranaries().values().stream().filter(value -> value.getPosition().distanceTo(peon.getPosition()) <= safeRadius).toList());
        result.put("fields", world.getWheatFields().values().stream().filter(value -> value.getPosition().distanceTo(peon.getPosition()) <= safeRadius).toList());
        result.put("sheep", world.getSheep().values().stream().filter(value -> value.getPosition().distanceTo(peon.getPosition()) <= safeRadius).toList());
        return result;
    }
}
