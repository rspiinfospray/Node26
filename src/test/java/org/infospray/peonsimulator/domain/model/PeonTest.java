package org.infospray.peonsimulator.domain.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PeonTest {
    @Test
    void shouldPreserveHealthAboveOneHundredWhenSerialized() throws Exception {
        Peon peon = new Peon(UUID.randomUUID(), "Gandalf", UUID.randomUUID(), new HexCoordinate(1, 1));
        peon.setMaxHealthPoints(150);
        peon.setHealthPoints(150);
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(peon);
        Peon restored = objectMapper.readValue(json, Peon.class);
        assertThat(json.indexOf("maxHealthPoints")).isLessThan(json.indexOf("healthPoints"));
        assertThat(restored.getMaxHealthPoints()).isEqualTo(150);
        assertThat(restored.getHealthPoints()).isEqualTo(150);
    }

    @Test
    void shouldIncreaseCombatStatisticsForEveryLevelGained() {
        Peon peon = new Peon(UUID.randomUUID(), "Aragorn", UUID.randomUUID(), new HexCoordinate(1, 1));
        peon.hurt(70);
        int previousLevel = peon.gainExperience(200, 10, 10);
        assertThat(previousLevel).isEqualTo(1);
        assertThat(peon.getLevel()).isEqualTo(3);
        assertThat(peon.getMaxHealthPoints()).isEqualTo(120);
        assertThat(peon.getHealthPoints()).isEqualTo(120);
        assertThat(peon.getAttackDamage()).isEqualTo(40);
    }

    @Test
    void shouldLearnFromDecisionAndLimitPersonalHistory() {
        Peon peon = new Peon(UUID.randomUUID(), "Frodon", UUID.randomUUID(), new HexCoordinate(1, 1));
        peon.beginDecision("PV_FAIBLES:SEUL:NOURRITURE_CONNUE:DECOUVERT", ActionType.SE_DEPLACER);
        peon.gainExperience(25, 10, 10);
        ActionExperience experience = peon.learnFromDecision(1, 20, false, 0.20, 1);
        assertThat(experience.experienceDelta()).isEqualTo(25);
        assertThat(peon.getActionHistory()).hasSize(1);
        assertThat(peon.getLearnedActions().get("PV_FAIBLES:SEUL:NOURRITURE_CONNUE:DECOUVERT|SE_DEPLACER").getExpectedReward()).isEqualTo(4);
    }
}
