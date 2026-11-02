package org.infospray.peonsimulator.application.command;

import org.infospray.peonsimulator.application.dto.WorldAnalysisContextResponse;
import org.infospray.peonsimulator.application.port.secondary.WorldRepository;
import org.infospray.peonsimulator.domain.model.World;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class GetWorldAnalysisContextCommand {
    private final WorldRepository worldRepository;

    public GetWorldAnalysisContextCommand(WorldRepository worldRepository) { this.worldRepository = worldRepository; }

    public WorldAnalysisContextResponse execute(UUID worldId) {
        World world = this.worldRepository.findById(worldId).orElseThrow(() -> new IllegalArgumentException("Monde introuvable: " + worldId));
        int alivePeons = world.alivePeons().size();
        WorldAnalysisContextResponse.Dimensions dimensions = new WorldAnalysisContextResponse.Dimensions(world.getWidth(), world.getHeight());
        WorldAnalysisContextResponse.Generation generation = new WorldAnalysisContextResponse.Generation(world.getSeed(), world.getRockPercentage(), world.getTreePercentage(), world.getFoodCellPercentage(), world.getSheepCount(), world.isRiverGenerationEnabled());
        WorldAnalysisContextResponse.Simulation simulation = new WorldAnalysisContextResponse.Simulation(world.getCurrentRound(), world.getMaxRounds(), world.getSequenceNumber());
        WorldAnalysisContextResponse.Population population = new WorldAnalysisContextResponse.Population(world.getPeons().size(), alivePeons, world.getPeons().size() - alivePeons);
        WorldAnalysisContextResponse.Constructions constructions = new WorldAnalysisContextResponse.Constructions(world.getHouses().size(), world.getGranaries().size(), world.getWheatFields().size());
        return new WorldAnalysisContextResponse(world.getId(), world.getName(), world.getStatus(), dimensions, generation, simulation, population, constructions, world.getSheep().size());
    }
}
