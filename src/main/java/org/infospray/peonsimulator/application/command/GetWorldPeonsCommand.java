package org.infospray.peonsimulator.application.command;

import org.infospray.peonsimulator.application.dto.PeonSummaryResponse;
import org.infospray.peonsimulator.application.port.secondary.WorldRepository;
import org.infospray.peonsimulator.domain.model.Grave;
import org.infospray.peonsimulator.domain.model.Peon;
import org.infospray.peonsimulator.domain.model.World;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class GetWorldPeonsCommand {
    private final WorldRepository worldRepository;

    public GetWorldPeonsCommand(WorldRepository worldRepository) { this.worldRepository = worldRepository; }

    public List<PeonSummaryResponse> execute(UUID worldId) {
        World world = this.worldRepository.findById(worldId).orElseThrow(() -> new IllegalArgumentException("Monde introuvable: " + worldId));
        return world.getPeons().values().stream().map(peon -> this.toSummary(world, peon)).toList();
    }

    private PeonSummaryResponse toSummary(World world, Peon peon) {
        Grave grave = world.getGraves().get(peon.getId());
        Long deathSequence = peon.isAlive() || grave == null ? null : grave.deathSequence();
        PeonSummaryResponse.Status status = peon.isAlive() ? PeonSummaryResponse.Status.VIVANT : PeonSummaryResponse.Status.MORT;
        return new PeonSummaryResponse(peon.getId(), peon.getFirstName(), status, deathSequence);
    }
}
