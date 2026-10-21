package org.infospray.peonsimulator.application;

import org.infospray.peonsimulator.configuration.GameDefaultsProperties;
import org.infospray.peonsimulator.domain.model.Cell;
import org.infospray.peonsimulator.domain.model.Granary;
import org.infospray.peonsimulator.domain.model.HexCoordinate;
import org.infospray.peonsimulator.domain.model.House;
import org.infospray.peonsimulator.domain.model.LongTermStrategy;
import org.infospray.peonsimulator.domain.model.Peon;
import org.infospray.peonsimulator.domain.model.PeonPersonality;
import org.infospray.peonsimulator.domain.model.PeonRelation;
import org.infospray.peonsimulator.domain.model.RememberedCell;
import org.infospray.peonsimulator.domain.model.RememberedPeonObservation;
import org.infospray.peonsimulator.domain.model.StrategicPhase;
import org.infospray.peonsimulator.domain.model.StrategyType;
import org.infospray.peonsimulator.domain.model.TerrainType;
import org.infospray.peonsimulator.domain.model.WheatField;
import org.infospray.peonsimulator.domain.model.World;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PeonStrategyPlannerTest {
    private final PeonStrategyPlanner planner = new PeonStrategyPlanner();
    private final GameDefaultsProperties defaults = new GameDefaultsProperties();

    @Test
    void shouldTargetTheClosestKnownEnemyDuringProtection() {
        UUID teamId = UUID.randomUUID();
        Peon peon = new Peon(UUID.randomUUID(), "Eldamir", teamId, new HexCoordinate(0, 0));
        peon.setPersonality(new PeonPersonality(0, 100, 0, 0, 0));
        peon.setLongTermStrategy(new LongTermStrategy(StrategyType.PROTEGER_L_EQUIPE, 1, teamId));
        HexCoordinate enemyPosition = new HexCoordinate(4, 0);
        RememberedPeonObservation enemy = new RememberedPeonObservation(UUID.randomUUID(), UUID.randomUUID(), PeonRelation.ENEMY, enemyPosition, 100, 2, 10);
        peon.getMentalMap().put("4:0", new RememberedCell(new Cell(enemyPosition, TerrainType.PLAIN, 0), false, 10, java.util.List.of(enemy)));
        World world = new World();
        world.setCurrentRound(1);
        world.setPeons(new LinkedHashMap<>(Map.of(peon.getId(), peon)));
        world.addCell(new Cell(new HexCoordinate(0, 0), TerrainType.PLAIN, 0));

        this.planner.update(world, peon, this.defaults);

        assertThat(peon.getLongTermStrategy().getType()).isEqualTo(StrategyType.PROTEGER_L_EQUIPE);
        assertThat(peon.getLongTermStrategy().getTargetArea()).isEqualTo(enemyPosition);
        assertThat(peon.getLongTermStrategy().getPhase()).isEqualTo(StrategicPhase.REJOINDRE_LA_MENACE);
    }

    @Test
    void shouldCreatePersistentAgriculturalStrategyWithTacticalPhase() {
        Fixture fixture = this.agriculturalFixture();

        this.planner.update(fixture.world(), fixture.peon(), this.defaults);

        assertThat(fixture.peon().getLongTermStrategy().getType()).isEqualTo(StrategyType.AUTONOMIE_ALIMENTAIRE);
        assertThat(fixture.peon().getLongTermStrategy().getPhase()).isEqualTo(StrategicPhase.ETENDRE_L_EXPLOITATION);
        assertThat(fixture.peon().getLongTermStrategy().getObjectives()).containsEntry("champs", 1).containsEntry("champsCible", 3);
        assertThat(fixture.peon().getLongTermStrategy().getNextPriority()).contains("2 champ");
    }

    @Test
    void shouldKeepCommittedStrategyForAtLeastTenRounds() {
        Fixture fixture = this.agriculturalFixture();
        this.planner.update(fixture.world(), fixture.peon(), this.defaults);
        fixture.peon().setPersonality(new PeonPersonality(0, 0, 100, 0, 0));
        fixture.world().setCurrentRound(6);

        this.planner.update(fixture.world(), fixture.peon(), this.defaults);

        assertThat(fixture.peon().getLongTermStrategy().getType()).isEqualTo(StrategyType.AUTONOMIE_ALIMENTAIRE);
        assertThat(fixture.peon().getLongTermStrategy().getCommitmentUntilRound()).isEqualTo(11);
    }

    @Test
    void shouldRenewCommitmentWhenAnExpiredStrategyIsSelectedAgain() {
        Fixture fixture = this.agriculturalFixture();
        this.planner.update(fixture.world(), fixture.peon(), this.defaults);
        fixture.world().setCurrentRound(11);

        this.planner.update(fixture.world(), fixture.peon(), this.defaults);

        assertThat(fixture.peon().getLongTermStrategy().getCommitmentUntilRound()).isEqualTo(21);
        assertThat(fixture.peon().getLongTermStrategy().getLastEvaluatedAtRound()).isEqualTo(11);
    }

    @Test
    void shouldAbandonAStagnantStrategyAfterTwoReevaluations() {
        Fixture fixture = this.agriculturalFixture();
        this.planner.update(fixture.world(), fixture.peon(), this.defaults);
        StrategyType initialType = fixture.peon().getLongTermStrategy().getType();
        fixture.world().setCurrentRound(6);
        this.planner.update(fixture.world(), fixture.peon(), this.defaults);
        fixture.world().setCurrentRound(11);
        this.planner.update(fixture.world(), fixture.peon(), this.defaults);
        fixture.world().setCurrentRound(16);

        this.planner.update(fixture.world(), fixture.peon(), this.defaults);

        assertThat(fixture.peon().getLongTermStrategy().getType()).isNotEqualTo(initialType);
    }

    @Test
    void shouldDiversifyStrategyWhenAgricultureIsAlreadyCovered() {
        Fixture fixture = this.agriculturalFixture();
        Peon agriculturalAlly = new Peon(UUID.randomUUID(), "Borin", fixture.peon().getTeamId(), new HexCoordinate(3, 0));
        agriculturalAlly.setLongTermStrategy(new org.infospray.peonsimulator.domain.model.LongTermStrategy(StrategyType.AUTONOMIE_ALIMENTAIRE, 1, fixture.peon().getTeamId()));
        fixture.world().getPeons().put(agriculturalAlly.getId(), agriculturalAlly);
        fixture.peon().setPersonality(new PeonPersonality(80, 90, 20, 20, 20));

        this.planner.update(fixture.world(), fixture.peon(), this.defaults);

        assertThat(fixture.peon().getLongTermStrategy().getType()).isNotEqualTo(StrategyType.AUTONOMIE_ALIMENTAIRE);
    }

    @Test
    void shouldNotAdvancePersonalFarmWithRemoteAlliedBuildings() {
        UUID teamId = UUID.randomUUID();
        Peon peon = new Peon(UUID.randomUUID(), "Doriak", teamId, new HexCoordinate(0, 0));
        peon.setPersonality(new PeonPersonality(50, 20, 50, 80, 80));
        Peon ally = new Peon(UUID.randomUUID(), "Aldric", teamId, new HexCoordinate(10, 10));
        World world = new World();
        world.setCurrentRound(1);
        world.setPeons(new LinkedHashMap<>(Map.of(peon.getId(), peon, ally.getId(), ally)));
        House remoteHouse = new House(UUID.randomUUID(), new HexCoordinate(10, 10), ally.getId());
        world.getHouses().put(remoteHouse.getId(), remoteHouse);
        Granary remoteGranary = new Granary(UUID.randomUUID(), new HexCoordinate(11, 10), teamId);
        world.getGranaries().put(remoteGranary.getId(), remoteGranary);
        world.getWheatFields().put(UUID.randomUUID(), new WheatField(UUID.randomUUID(), new HexCoordinate(12, 10), teamId, remoteGranary.getId(), 8));

        this.planner.update(world, peon, this.defaults);

        assertThat(peon.getLongTermStrategy().getPhase()).isEqualTo(StrategicPhase.ETABLIR_UN_FOYER);
        assertThat(peon.getLongTermStrategy().getObjectives()).containsEntry("maisons", 0).containsEntry("greniers", 0).containsEntry("champs", 0);
    }

    private Fixture agriculturalFixture() {
        UUID teamId = UUID.randomUUID();
        Peon peon = new Peon(UUID.randomUUID(), "Aldric", teamId, new HexCoordinate(0, 0));
        peon.setPersonality(new PeonPersonality(20, 10, 10, 100, 100));
        World world = new World();
        world.setCurrentRound(1);
        world.setPeons(new LinkedHashMap<>(Map.of(peon.getId(), peon)));
        for (int index = 0; index < 20; index++) { world.addCell(new Cell(new HexCoordinate(index, 0), TerrainType.PLAIN, 0)); }
        House house = new House(UUID.randomUUID(), new HexCoordinate(0, 1), peon.getId());
        world.getHouses().put(house.getId(), house);
        Granary granary = new Granary(UUID.randomUUID(), new HexCoordinate(1, 0), teamId);
        world.getGranaries().put(granary.getId(), granary);
        WheatField field = new WheatField(UUID.randomUUID(), new HexCoordinate(2, 0), teamId, granary.getId(), 8);
        world.getWheatFields().put(field.getId(), field);
        return new Fixture(world, peon);
    }

    private record Fixture(World world, Peon peon) {
    }
}
