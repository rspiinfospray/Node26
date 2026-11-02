package org.infospray.peonsimulator.application.command;

import org.infospray.peonsimulator.application.dto.WorldSummaryResponse;
import org.infospray.peonsimulator.application.port.secondary.WorldRepository;
import org.infospray.peonsimulator.domain.model.World;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GetWorldSummariesCommand {
    private final WorldRepository worldRepository;

    public GetWorldSummariesCommand(WorldRepository worldRepository) { this.worldRepository = worldRepository; }

    public List<WorldSummaryResponse> execute() { return this.worldRepository.findAll().stream().map(this::toSummary).toList(); }

    private WorldSummaryResponse toSummary(World world) { return new WorldSummaryResponse(world.getId(), world.getName(), world.getStatus(), world.getCurrentRound(), world.getMaxRounds(), world.getSequenceNumber(), world.getPeons().size(), world.alivePeons().size()); }
}
