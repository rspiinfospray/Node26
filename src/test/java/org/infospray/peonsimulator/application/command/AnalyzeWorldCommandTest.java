package org.infospray.peonsimulator.application.command;

import org.infospray.peonsimulator.application.port.secondary.EventRepository;
import org.infospray.peonsimulator.application.port.secondary.WorldRepository;
import org.infospray.peonsimulator.domain.event.SimulationEvent;
import org.infospray.peonsimulator.domain.model.World;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalyzeWorldCommandTest {
    @Test
    void shouldAlwaysDescribeTheMostFrequentActionWhenDecisionsExist() {
        UUID worldId = UUID.randomUUID();
        World world = new World();
        world.setId(worldId);
        WorldRepository worldRepository = mock(WorldRepository.class);
        EventRepository eventRepository = mock(EventRepository.class);
        List<SimulationEvent> events = List.of(
                this.decision(worldId, "SE_DEPLACER"),
                this.decision(worldId, "MANGER"),
                this.decision(worldId, "ATTAQUER"),
                this.decision(worldId, "COMMUNIQUER"),
                this.decision(worldId, "NE_RIEN_FAIRE")
        );
        when(worldRepository.findById(worldId)).thenReturn(Optional.of(world));
        when(eventRepository.findByWorldId(worldId)).thenReturn(events);

        List<String> trends = new AnalyzeWorldCommand(worldRepository, eventRepository).execute(worldId).behavioralTrends();

        assertThat(trends).isNotEmpty().anyMatch(trend -> trend.contains("décisions") && trend.contains("20 %"));
    }

    private SimulationEvent decision(UUID worldId, String actionType) {
        return SimulationEvent.of(UUID.randomUUID(), "PEON_DECISION_MADE", worldId, UUID.randomUUID(), UUID.randomUUID(), 1, 1, 1, Map.of("actionType", actionType, "reason", "test"));
    }
}
