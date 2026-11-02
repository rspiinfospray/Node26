package org.infospray.peonsimulator.application.command;

import org.infospray.peonsimulator.application.dto.PeonDeathContextResponse;
import org.infospray.peonsimulator.application.port.secondary.EventRepository;
import org.infospray.peonsimulator.application.port.secondary.WorldRepository;
import org.infospray.peonsimulator.configuration.GameDefaultsProperties;
import org.infospray.peonsimulator.domain.event.SimulationEvent;
import org.infospray.peonsimulator.domain.model.Grave;
import org.infospray.peonsimulator.domain.model.HexCoordinate;
import org.infospray.peonsimulator.domain.model.Peon;
import org.infospray.peonsimulator.domain.model.World;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetPeonDeathContextCommandTest {
    @Test
    void shouldRejectSequenceLimitOutsideAllowedRange() {
        GetPeonDeathContextCommand command = new GetPeonDeathContextCommand(mock(WorldRepository.class), mock(EventRepository.class), new GameDefaultsProperties());

        assertThatIllegalArgumentException().isThrownBy(() -> command.execute(UUID.randomUUID(), UUID.randomUUID(), 0)).withMessage("sequenceLimit doit être compris entre 1 et 100");
        assertThatIllegalArgumentException().isThrownBy(() -> command.execute(UUID.randomUUID(), UUID.randomUUID(), 101)).withMessage("sequenceLimit doit être compris entre 1 et 100");
    }

    @Test
    void shouldBuildCompactContextFromTwentySequencesAndCorrelatedAttack() {
        WorldRepository worldRepository = mock(WorldRepository.class);
        EventRepository eventRepository = mock(EventRepository.class);
        GameDefaultsProperties defaults = new GameDefaultsProperties();
        UUID worldId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        Peon victim = new Peon(UUID.randomUUID(), "Sylmar", teamId, new HexCoordinate(1, 1));
        Peon attacker = new Peon(UUID.randomUUID(), "Azog", UUID.randomUUID(), new HexCoordinate(1, 1));
        victim.setHealthPoints(0);
        World world = new World();
        world.setId(worldId);
        world.setHungerHealthLossPerTurn(5);
        world.setPeons(Map.of(victim.getId(), victim, attacker.getId(), attacker));
        world.setGraves(Map.of(victim.getId(), new Grave(victim.getId(), victim.getPosition(), 4, 30)));
        UUID decisionActionId = UUID.randomUUID();
        UUID attackActionId = UUID.randomUUID();
        SimulationEvent oldDecision = this.event(worldId, teamId, victim.getId(), decisionActionId, "PEON_DECISION_MADE", 9, 3, 0, Map.of("actionType", "SE_DEPLACER"));
        SimulationEvent decision = this.event(worldId, teamId, victim.getId(), decisionActionId, "PEON_DECISION_MADE", 15, 4, 0, Map.of("actionType", "SE_DEPLACER", "purpose", "SURVIVRE", "reason", "chercher de la nourriture", "selectedScore", 12.5, "candidates", List.of(Map.of("large", "payload"))));
        SimulationEvent hunger = this.event(worldId, teamId, victim.getId(), decisionActionId, "PEON_HUNGER_APPLIED", 15, 4, 1, Map.of("healthBefore", 25, "healthAfter", 20, "loss", 5));
        SimulationEvent attack = this.event(worldId, attacker.getTeamId(), attacker.getId(), attackActionId, "PEON_ATTACKED", 30, 4, 0, Map.of("targetPeonId", victim.getId(), "healthBefore", 20, "healthAfter", 0, "damage", 20));
        SimulationEvent death = this.event(worldId, teamId, victim.getId(), attackActionId, "PEON_DIED", 30, 4, 1, Map.of("reason", "ATTACK", "attackerPeonId", attacker.getId(), "position", Map.of("q", 1, "r", 1)));
        when(worldRepository.findById(worldId)).thenReturn(Optional.of(world));
        when(eventRepository.findByWorldId(worldId)).thenReturn(List.of(oldDecision, decision, hunger, attack, death));

        PeonDeathContextResponse response = new GetPeonDeathContextCommand(worldRepository, eventRepository, defaults).execute(worldId, victim.getId(), 20);

        assertThat(response.window()).isEqualTo(new PeonDeathContextResponse.Window(9, 30, 30, 4, 3, 2, List.of(9L, 15L, 30L)));
        assertThat(response.death().attackerName()).isEqualTo("Azog");
        assertThat(response.healthEvolution()).extracting(PeonDeathContextResponse.HealthChange::cause).containsExactly("HUNGER", "ATTACK");
        assertThat(response.decisions()).extracting(PeonDeathContextResponse.Decision::action).containsExactly("SE_DEPLACER", "SE_DEPLACER");
        assertThat(response.decisions().getLast().stateBefore()).asString().contains("health=25");
        assertThat(response.decisions().getLast().result()).asString().contains("healthAfter=20");
        assertThat(response.lifetimeSummary()).isNotNull();
        assertThat(response.fatalAttackContext()).asString().contains("attackerName=Azog");
    }

    private SimulationEvent event(UUID worldId, UUID teamId, UUID peonId, UUID actionId, String type, long sequence, int round, int index, Map<String, Object> payload) {
        return new SimulationEvent(UUID.randomUUID(), actionId, type, 1, Instant.now(), worldId, teamId, peonId, round, sequence, index, actionId, actionId, payload);
    }
}
