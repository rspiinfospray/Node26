package org.infospray.peonsimulator.application.command;

import org.infospray.peonsimulator.application.dto.EventQuery;
import org.infospray.peonsimulator.application.port.secondary.EventRepository;
import org.infospray.peonsimulator.application.port.secondary.WorldRepository;
import org.infospray.peonsimulator.domain.event.SimulationEvent;
import org.infospray.peonsimulator.domain.model.Peon;
import org.infospray.peonsimulator.domain.model.World;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PeonDeathDiagnosticTest {
    @Test
    void shouldCountADeathOnceWhenItSharesTheVictimsDecisionSequence() {
        UUID worldId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID peonId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        EventRepository events = mock(EventRepository.class);
        SimulationEvent decision = SimulationEvent.of(null, "PEON_DECISION_MADE", worldId, null, peonId, 7, 42, 1, Map.of("actionType", "SE_DEPLACER"));
        SimulationEvent death = SimulationEvent.of(null, "PEON_DIED", worldId, null, peonId, 7, 42, 3, Map.of("reason", "MOVEMENT"));
        when(events.find(new EventQuery(worldId, 0, 2000, peonId, 7, 7, Set.of()))).thenReturn(List.of(decision, death));
        var summary = new SummarizePeonDiagnosticCommand(events).execute(worldId, peonId, 7, 7);
        assertThat(summary.anomalies()).hasSize(1);
        assertThat(summary.anomalies().getFirst().rounds()).containsExactly(7);
        assertThat(summary.anomalies().getFirst().details()).containsEntry("sequence", 42L);
    }

    @Test
    void shouldIncludeDeathDuringAnotherPeonsTurnInBothDiagnostics() {
        UUID worldId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID peonId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        EventRepository events = mock(EventRepository.class);
        WorldRepository worlds = mock(WorldRepository.class);
        World world = new World();
        Peon peon = new Peon();
        world.getPeons().put(peonId, peon);
        when(worlds.findById(worldId)).thenReturn(Optional.of(world));
        SimulationEvent death = SimulationEvent.of(null, "PEON_DIED", worldId, null, peonId, 7, 42, 3, Map.of("reason", "ATTACK"));
        when(events.find(new EventQuery(worldId, 0, 2000, peonId, null, null, Set.of()))).thenReturn(List.of(death));

        var summary = new SummarizePeonDiagnosticCommand(events).execute(worldId, peonId, null, null);
        var diagnostic = new GetPeonDiagnosticCommand(worlds, events).execute(worldId, peonId, null, null);

        assertThat(summary.anomalies()).hasSize(1);
        assertThat(summary.anomalies().getFirst().type()).isEqualTo("DEATH");
        assertThat(diagnostic.turns()).hasSize(1);
        assertThat(diagnostic.turns().getFirst().decision()).isEmpty();
        assertThat(diagnostic.turns().getFirst().outcomes().getFirst().get("eventType")).isEqualTo("PEON_DIED");
    }
}
