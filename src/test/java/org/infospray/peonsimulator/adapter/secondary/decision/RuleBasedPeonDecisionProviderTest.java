package org.infospray.peonsimulator.adapter.secondary.decision;

import org.infospray.peonsimulator.application.dto.PeonDecision;
import org.infospray.peonsimulator.application.dto.PeonDecisionContext;
import org.infospray.peonsimulator.configuration.GameDefaultsProperties;
import org.infospray.peonsimulator.domain.model.ActionKnowledge;
import org.infospray.peonsimulator.domain.model.ActionType;
import org.infospray.peonsimulator.domain.model.Cell;
import org.infospray.peonsimulator.domain.model.HexCoordinate;
import org.infospray.peonsimulator.domain.model.LongTermStrategy;
import org.infospray.peonsimulator.domain.model.PeonPersonality;
import org.infospray.peonsimulator.domain.model.PeonRelation;
import org.infospray.peonsimulator.domain.model.RememberedCell;
import org.infospray.peonsimulator.domain.model.RememberedGraveObservation;
import org.infospray.peonsimulator.domain.model.RememberedPeonObservation;
import org.infospray.peonsimulator.domain.model.TerrainType;
import org.infospray.peonsimulator.domain.model.StrategicPhase;
import org.infospray.peonsimulator.domain.model.StrategyStatus;
import org.infospray.peonsimulator.domain.model.StrategyType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedPeonDecisionProviderTest {
    @Test
    void shouldKeepAHealthReserveWhenNoSurvivalDestinationExists() {
        ActionKnowledge movement = new ActionKnowledge();
        movement.setExpectedReward(200);
        String situation = "PV_CORRECTS:SEUL:NOURRITURE_INCONNUE:MOUTON_INCONNU:AUCUNE_TOMBE_CONNUE:BOIS_INCOMPLET:BUTIN_INCONNU:SANS_ABRI:DECOUVERT";
        PeonDecision decision = this.provider.decide(this.context(50, false, Map.of(situation + "|SE_DEPLACER", movement), Map.of(), List.of(), 0, List.of(new HexCoordinate(3, 2))));

        assertThat(decision.action().type()).isEqualTo(ActionType.NE_RIEN_FAIRE);
    }

    @Test
    void shouldAlwaysSelectTheHighestAdmissibleScore() {
        GameDefaultsProperties properties = new GameDefaultsProperties();
        properties.setExplorationPercentage(100);
        RuleBasedPeonDecisionProvider provider = new RuleBasedPeonDecisionProvider(properties);
        PeonDecision decision = provider.decide(this.context(100, false, Map.of(), Map.of(), List.of(), 0, List.of(new HexCoordinate(3, 2))));

        assertThat(decision.explorationChoice()).isFalse();
        assertThat(decision.selectedScore()).isEqualTo(decision.candidates().getFirst().score());
    }

    @Test
    void shouldPreferMovementTowardTheClosestUnvisitedFrontier() {
        RememberedCell frontier = new RememberedCell(new Cell(new HexCoordinate(4, 2), TerrainType.PLAIN, 0), false, 5);
        PeonDecision decision = this.provider.decide(this.context(100, false, Map.of(), Map.of("4:2", frontier), List.of(), 0, List.of(new HexCoordinate(3, 2), new HexCoordinate(1, 2))));

        assertThat(this.movementScore(decision, new HexCoordinate(3, 2))).isGreaterThan(this.movementScore(decision, new HexCoordinate(1, 2)));
        assertThat(decision.action().destination()).isEqualTo(new HexCoordinate(3, 2));
    }

    @Test
    void shouldEscapeInsteadOfAttackingWhenEnemyRetaliationWouldBeLethal() {
        UUID teamId = UUID.randomUUID();
        PeonDecisionContext.ObservedPeon enemy = new PeonDecisionContext.ObservedPeon(UUID.randomUUID(), UUID.randomUUID(), 100, 3);
        PeonDecisionContext context = new PeonDecisionContext(UUID.randomUUID(), teamId, 45, 100, 0, 1, new HexCoordinate(2, 2), false, 10, 42, new PeonPersonality(10, 100, 10, 10), Map.of(), Map.of(), List.of(enemy), 0, List.of(new HexCoordinate(1, 2)), 10, 9);

        PeonDecision decision = this.provider.decide(context);

        assertThat(decision.action().type()).isEqualTo(ActionType.SE_DEPLACER);
    }

    @Test
    void shouldNotEnterACellWhereRememberedEnemyDamageWouldBeLethal() {
        UUID teamId = UUID.randomUUID();
        HexCoordinate dangerousPosition = new HexCoordinate(3, 2);
        RememberedPeonObservation enemy = new RememberedPeonObservation(UUID.randomUUID(), UUID.randomUUID(), PeonRelation.ENEMY, dangerousPosition, 100, 3, 9);
        RememberedCell dangerousCell = new RememberedCell(new Cell(dangerousPosition, TerrainType.PLAIN, 0), false, 9, List.of(enemy));
        PeonDecisionContext context = new PeonDecisionContext(UUID.randomUUID(), teamId, 45, 100, 0, 1, new HexCoordinate(2, 2), false, 10, 42, new PeonPersonality(10, 100, 100, 10), Map.of(), Map.of("3:2", dangerousCell), List.of(), 0, List.of(dangerousPosition), 10, 9);

        PeonDecision decision = this.provider.decide(context);

        assertThat(decision.action().type()).isNotEqualTo(ActionType.SE_DEPLACER);
    }

    @Test
    void shouldEatAccessibleSheepInsteadOfFollowingAnUncertainTrailAtLowHealth() {
        PeonDecisionContext context = new PeonDecisionContext(UUID.randomUUID(), UUID.randomUUID(), 30, 100, 0, 1, new HexCoordinate(2, 2), false, 10, 42, new PeonPersonality(10, 10, 100, 10), Map.of(), Map.of(), List.of(), 0, List.of(new HexCoordinate(3, 2)), 10, 9, TerrainType.PLAIN, 0, 0, 0, false, null, List.of(), 0, 0, 1, List.of());

        PeonDecision decision = this.provider.decide(context);

        assertThat(decision.action().type()).isEqualTo(ActionType.MANGER_MOUTON);
    }

    @Test
    void shouldNotOfferRepeatedActionsWithoutNewInformation() {
        UUID teamId = UUID.randomUUID();
        PeonDecisionContext.ObservedPeon ally = new PeonDecisionContext.ObservedPeon(UUID.randomUUID(), teamId, 100, 1);
        PeonDecisionContext context = new PeonDecisionContext(UUID.randomUUID(), teamId, 100, 100, 0, 1, new HexCoordinate(2, 2), false, 10, 42, new PeonPersonality(10, 10, 100, 100), Map.of(), Map.of(), List.of(ally), 0, List.of(), 10, 9, TerrainType.PLAIN, 0, 0, 0, false, null, List.of(), 0, 3);

        PeonDecision decision = this.provider.decide(context);

        assertThat(decision.candidates()).extracting(PeonDecision.CandidateScore::actionType).doesNotContain(ActionType.COMMUNIQUER, ActionType.VOIR);
    }

    @Test
    void shouldMultiplyFieldsAroundGranaryUntilTeamProductionTargetIsReached() {
        PeonDecisionContext expandable = this.agricultureContext(1, 5);
        PeonDecision expandableDecision = this.provider.decide(expandable);
        PeonDecision.CandidateScore fieldCandidate = expandableDecision.candidates().stream().filter(candidate -> candidate.actionType() == ActionType.CONSTRUIRE_CHAMP).findFirst().orElseThrow();
        assertThat(fieldCandidate.factors()).containsKeys("multiplication des champs", "besoins de l'équipe", "capacité disponible du grenier", "exploitation agricole déjà établie");

        PeonDecisionContext sufficientProduction = this.agricultureContext(4, 5);
        PeonDecision sufficientDecision = this.provider.decide(sufficientProduction);
        assertThat(sufficientDecision.candidates()).extracting(PeonDecision.CandidateScore::actionType).doesNotContain(ActionType.CONSTRUIRE_CHAMP);
    }

    @Test
    void shouldExposeLongTermStrategyBonusInActionScore() {
        PeonDecisionContext base = this.agricultureContext(1, 5);
        LongTermStrategy strategy = new LongTermStrategy(StrategyType.AUTONOMIE_ALIMENTAIRE, 1, base.teamId());
        strategy.setPhase(StrategicPhase.ETENDRE_L_EXPLOITATION);
        PeonDecisionContext context = new PeonDecisionContext(base.peonId(), base.teamId(), base.healthPoints(), base.maxHealthPoints(), base.experiencePoints(), base.level(), base.position(), base.concealedByTree(), base.sequenceNumber(), base.decisionSeed(), base.personality(), base.learnedActions(), base.mentalMap(), base.peonsOnCurrentCell(), base.foodOnCurrentCell(), base.traversableNeighbors(), base.currentRound(), base.lastObservationRound(), base.currentTerrain(), base.treeHealthPoints(), base.woodQuantity(), base.woodBundleQuantity(), base.buildable(), base.house(), base.knownHouses(), base.usefulCommunicationCount(), base.consecutiveCommunicationCount(), base.sheepOnCurrentCell(), base.knownSheepPositions(), base.field(), base.granary(), base.knownFields(), base.knownGranaries(), base.fieldBuildable(), base.granaryBuildable(), base.livingTeamPeonCount(), strategy);

        PeonDecision decision = this.provider.decide(context);

        assertThat(decision.candidates().stream().filter(candidate -> candidate.actionType() == ActionType.CONSTRUIRE_CHAMP).findFirst().orElseThrow().factors().keySet()).anyMatch(key -> key.startsWith("projet à long terme"));
    }

    @Test
    void shouldPreferMoreNutritiousSheepToBerriesWhenBothAreAvailable() {
        PeonDecisionContext context = new PeonDecisionContext(UUID.randomUUID(), UUID.randomUUID(), 20, 100, 0, 1, new HexCoordinate(2, 2), false, 10, 42, new PeonPersonality(50, 50, 50, 50), Map.of(), Map.of(), List.of(), 1, List.of(), 10, 9, TerrainType.PLAIN, 0, 0, 0, false, null, List.of(), 0, 0, 1, List.of(new HexCoordinate(2, 2)));

        PeonDecision decision = new RuleBasedPeonDecisionProvider(new GameDefaultsProperties()).decide(context);

        assertThat(decision.action().type()).isEqualTo(ActionType.MANGER_MOUTON);
        assertThat(decision.action().reason()).contains("soin supérieur du mouton");
    }

    @Test
    void shouldNotWasteFoodAtMaximumHealth() {
        UUID teamId = UUID.randomUUID();
        PeonDecisionContext.ObservedGranary granary = new PeonDecisionContext.ObservedGranary(UUID.randomUUID(), teamId, new HexCoordinate(2, 2), 10);
        PeonDecisionContext context = new PeonDecisionContext(UUID.randomUUID(), teamId, 100, 100, 0, 1, new HexCoordinate(2, 2), false, 10, 42, new PeonPersonality(50, 50, 50, 50), Map.of(), Map.of(), List.of(), 2, List.of(), 10, 9, TerrainType.PLAIN, 0, 0, 0, false, null, List.of(), 0, 0, 1, List.of(new HexCoordinate(2, 2)), null, granary, List.of(), List.of(granary), false, false, 4, null);

        PeonDecision decision = this.provider.decide(context);

        assertThat(decision.candidates()).extracting(PeonDecision.CandidateScore::actionType).doesNotContain(ActionType.MANGER, ActionType.MANGER_MOUTON, ActionType.MANGER_BLE);
    }

    @Test
    void shouldBuildGranaryInsteadOfSecondHouseDuringAgriculturalProject() {
        PeonDecisionContext base = this.agricultureContext(4, 5);
        LongTermStrategy strategy = new LongTermStrategy(StrategyType.AUTONOMIE_ALIMENTAIRE, 1, base.teamId());
        strategy.setPhase(StrategicPhase.ETENDRE_L_EXPLOITATION);
        strategy.setTargetArea(new HexCoordinate(3, 2));
        PeonDecisionContext context = new PeonDecisionContext(base.peonId(), base.teamId(), base.healthPoints(), base.maxHealthPoints(), base.experiencePoints(), base.level(), base.position(), base.concealedByTree(), base.sequenceNumber(), base.decisionSeed(), base.personality(), base.learnedActions(), base.mentalMap(), base.peonsOnCurrentCell(), base.foodOnCurrentCell(), base.traversableNeighbors(), base.currentRound(), base.lastObservationRound(), base.currentTerrain(), base.treeHealthPoints(), 200, base.woodBundleQuantity(), true, base.house(), base.knownHouses(), base.usefulCommunicationCount(), base.consecutiveCommunicationCount(), base.sheepOnCurrentCell(), base.knownSheepPositions(), base.field(), base.granary(), base.knownFields(), base.knownGranaries(), false, true, base.livingTeamPeonCount(), strategy);

        PeonDecision decision = this.provider.decide(context);

        assertThat(decision.action().type()).isEqualTo(ActionType.CONSTRUIRE_GRENIER);
        assertThat(decision.candidates().stream().filter(candidate -> candidate.actionType() == ActionType.CONSTRUIRE_MAISON).findFirst().orElseThrow().factors()).containsEntry("construction contraire au projet", -140.0);
    }

    @Test
    void shouldMoveTowardPersonalFarmTarget() {
        UUID teamId = UUID.randomUUID();
        LongTermStrategy strategy = new LongTermStrategy(StrategyType.AUTONOMIE_ALIMENTAIRE, 1, teamId);
        strategy.setPhase(StrategicPhase.CONSTRUIRE_UN_GRENIER);
        strategy.setTargetArea(new HexCoordinate(4, 2));
        PeonDecisionContext context = new PeonDecisionContext(UUID.randomUUID(), teamId, 100, 100, 0, 1, new HexCoordinate(8, 2), false, 10, 42, new PeonPersonality(50, 50, 50, 50), Map.of(), Map.of(), List.of(), 0, List.of(new HexCoordinate(7, 2), new HexCoordinate(9, 2)), 10, 9, TerrainType.PLAIN, 0, 100, 0, false, null, List.of(), 0, 0, 0, List.of(), null, null, List.of(), List.of(), false, false, 4, strategy);

        PeonDecision decision = this.provider.decide(context);

        assertThat(decision.action().type()).isEqualTo(ActionType.SE_DEPLACER);
        assertThat(decision.action().destination()).isEqualTo(new HexCoordinate(7, 2));
    }

    @Test
    void shouldChooseAdjacentGranaryInsteadOfOscillatingTowardBerriesAtCriticalHealth() {
        UUID teamId = UUID.randomUUID();
        HexCoordinate position = new HexCoordinate(2, 24);
        HexCoordinate granaryPosition = new HexCoordinate(1, 25);
        HexCoordinate berryPosition = new HexCoordinate(3, 23);
        RememberedCell berryMemory = new RememberedCell(new Cell(berryPosition, TerrainType.PLAIN, 2), true, 100);
        PeonDecisionContext.ObservedHouse house = new PeonDecisionContext.ObservedHouse(UUID.randomUUID(), UUID.randomUUID(), teamId, position, true, true);
        PeonDecisionContext.ObservedGranary granary = new PeonDecisionContext.ObservedGranary(UUID.randomUUID(), teamId, granaryPosition, 20);
        LongTermStrategy strategy = new LongTermStrategy(StrategyType.EXPLORER_LE_MONDE, 1, teamId);
        strategy.setPhase(StrategicPhase.REJOINDRE_LA_FRONTIERE);
        PeonDecisionContext context = new PeonDecisionContext(UUID.randomUUID(), teamId, 13, 170, 0, 1, position, false, 100, 42, new PeonPersonality(50, 20, 80, 50), Map.of(), Map.of("3:23", berryMemory), List.of(), 0, List.of(granaryPosition, berryPosition), 100, 99, TerrainType.PLAIN, 0, 0, 0, false, house, List.of(house), 0, 0, 0, List.of(), null, null, List.of(), List.of(granary), false, false, 4, strategy);

        PeonDecision decision = this.provider.decide(context);

        assertThat(decision.action().type()).isEqualTo(ActionType.SE_DEPLACER);
        assertThat(decision.action().destination()).isEqualTo(granaryPosition);
        assertThat(decision.action().reason()).contains("route alimentaire prioritaire");
    }

    private final GameDefaultsProperties defaults = this.defaults();
    private final RuleBasedPeonDecisionProvider provider = new RuleBasedPeonDecisionProvider(this.defaults);

    @Test
    void shouldEatWhenSurvivalUtilityIsHighest() {
        PeonDecision decision = this.provider.decide(this.context(35, false, Map.of(), Map.of(), List.of(), 2, List.of(new HexCoordinate(3, 2))));
        assertThat(decision.action().type()).isEqualTo(ActionType.MANGER);
        assertThat(decision.candidates()).extracting(PeonDecision.CandidateScore::actionType).contains(ActionType.MANGER, ActionType.SE_DEPLACER, ActionType.NE_RIEN_FAIRE).doesNotContain(ActionType.VOIR);
    }

    @Test
    void shouldAttackWeakerEnemyWhenUtilityIsFavorable() {
        UUID teamId = UUID.randomUUID();
        PeonDecisionContext.ObservedPeon enemy = new PeonDecisionContext.ObservedPeon(UUID.randomUUID(), UUID.randomUUID(), 35, 1);
        PeonDecisionContext context = new PeonDecisionContext(UUID.randomUUID(), teamId, 95, 100, 0, 2, new HexCoordinate(2, 2), false, 5, 42, new PeonPersonality(35, 85, 50, 50), Map.of(), Map.of(), List.of(enemy), 0, List.of(), 5, 4);
        assertThat(this.provider.decide(context).action().type()).isEqualTo(ActionType.ATTAQUER);
    }

    @Test
    void shouldValueTreeCoverAgainstDangerousRememberedEnemy() {
        HexCoordinate enemyPosition = new HexCoordinate(3, 2);
        Cell enemyCell = new Cell(enemyPosition, TerrainType.PLAIN, 0);
        RememberedPeonObservation enemy = new RememberedPeonObservation(UUID.randomUUID(), UUID.randomUUID(), PeonRelation.ENEMY, enemyPosition, 90, 3, 9);
        RememberedCell memory = new RememberedCell(enemyCell, false, 9, List.of(enemy));
        PeonDecision decision = this.provider.decide(this.context(40, true, Map.of(), Map.of("3:2", memory), List.of(), 0, List.of()));
        assertThat(decision.action().type()).isIn(ActionType.VOIR, ActionType.NE_RIEN_FAIRE);
        assertThat(decision.action().reason()).containsAnyOf("couvert", "camouflage");
    }

    @Test
    void shouldUseLearnedRewardInFutureDecision() {
        ActionKnowledge knowledge = new ActionKnowledge();
        knowledge.setExpectedReward(120);
        String situation = "PV_ELEVES:SEUL:NOURRITURE_INCONNUE:DECOUVERT";
        PeonDecision decision = this.provider.decide(this.context(80, false, Map.of(situation + "|NE_RIEN_FAIRE", knowledge), Map.of(), List.of(), 0, List.of(new HexCoordinate(3, 2))));
        assertThat(decision.action().type()).isEqualTo(ActionType.NE_RIEN_FAIRE);
        assertThat(decision.candidates().getFirst().factors()).containsKey("expérience passée");
    }

    @Test
    void shouldExposeScoresAndSituationForExplanation() {
        PeonDecision decision = this.provider.decide(this.context(100, false, Map.of(), Map.of(), List.of(), 0, List.of(new HexCoordinate(3, 2))));
        assertThat(decision.situationKey()).contains("PV_MAX", "SEUL", "PV_EXACTS_100", "NOURRITURE_0", "BOIS_0");
        assertThat(decision.candidates()).isNotEmpty();
        assertThat(decision.selectedScore()).isEqualTo(decision.candidates().stream().filter(candidate -> candidate.actionType() == decision.action().type()).findFirst().orElseThrow().score());
    }

    @Test
    void shouldIncludeMovementAndHungerInMovementCost() {
        PeonDecision decision = this.provider.decide(this.context(100, false, Map.of(), Map.of(), List.of(), 0, List.of(new HexCoordinate(3, 2))));
        PeonDecision.CandidateScore movement = decision.candidates().stream().filter(candidate -> candidate.actionType() == ActionType.SE_DEPLACER).findFirst().orElseThrow();

        assertThat(movement.factors()).containsEntry("coût complet du tour en PV", -15.0);
    }

    @Test
    void shouldNotOfferAnotherBridgeWhenOneIsAlreadyKnown() {
        HexCoordinate river = new HexCoordinate(3, 2);
        HexCoordinate bridge = new HexCoordinate(2, 3);
        Map<String, RememberedCell> map = Map.of("3:2", new RememberedCell(new Cell(river, TerrainType.RIVER, 0), false, 9), "2:3", new RememberedCell(new Cell(bridge, TerrainType.BRIDGE, 0), false, 9));
        PeonDecisionContext base = this.context(100, false, Map.of(), map, List.of(), 0, List.of());
        PeonDecisionContext context = new PeonDecisionContext(base.peonId(), base.teamId(), base.healthPoints(), base.maxHealthPoints(), base.experiencePoints(), base.level(), base.position(), base.concealedByTree(), base.sequenceNumber(), base.decisionSeed(), base.personality(), base.learnedActions(), base.mentalMap(), base.peonsOnCurrentCell(), base.foodOnCurrentCell(), base.traversableNeighbors(), base.currentRound(), base.lastObservationRound(), TerrainType.PLAIN, 0, 50, 0, false, null, List.of(), 0, 0);

        assertThat(this.provider.decide(context).candidates()).extracting(PeonDecision.CandidateScore::actionType).doesNotContain(ActionType.CONSTRUIRE_PONT);
    }

    @Test
    void shouldNotOfferBridgeWhenOppositeBankIsUnknown() {
        HexCoordinate river = new HexCoordinate(3, 2);
        Map<String, RememberedCell> map = Map.of("3:2", new RememberedCell(new Cell(river, TerrainType.RIVER, 0), false, 9));
        PeonDecisionContext base = this.context(100, false, Map.of(), map, List.of(), 0, List.of());
        PeonDecisionContext context = new PeonDecisionContext(base.peonId(), base.teamId(), base.healthPoints(), base.maxHealthPoints(), base.experiencePoints(), base.level(), base.position(), base.concealedByTree(), base.sequenceNumber(), base.decisionSeed(), base.personality(), base.learnedActions(), base.mentalMap(), base.peonsOnCurrentCell(), base.foodOnCurrentCell(), base.traversableNeighbors(), base.currentRound(), base.lastObservationRound(), TerrainType.PLAIN, 0, 50, 0, false, null, List.of(), 0, 0);

        assertThat(this.provider.decide(context).candidates()).extracting(PeonDecision.CandidateScore::actionType).doesNotContain(ActionType.CONSTRUIRE_PONT);
    }

    @Test
    void shouldClassifyHealthFromMaximumHealthPercentage() {
        PeonDecisionContext context = new PeonDecisionContext(UUID.randomUUID(), UUID.randomUUID(), 60, 200, 0, 1, new HexCoordinate(2, 2), false, 10, 42, new PeonPersonality(50, 50, 50, 50), Map.of(), Map.of(), List.of(), 0, List.of(), 10, 9);
        assertThat(this.provider.decide(context).situationKey()).startsWith("PV_FAIBLES");
    }

    @Test
    void shouldNotObserveAgainWhenPerceptionWasRefreshedOnPreviousRound() {
        PeonDecisionContext context = new PeonDecisionContext(UUID.randomUUID(), UUID.randomUUID(), 80, 100, 0, 1, new HexCoordinate(2, 2), false, 10, 42, new PeonPersonality(50, 50, 90, 50), Map.of(), Map.of(), List.of(), 0, List.of(), 10, 9);
        PeonDecision decision = this.provider.decide(context);
        assertThat(decision.action().type()).isEqualTo(ActionType.NE_RIEN_FAIRE);
        assertThat(decision.candidates()).extracting(PeonDecision.CandidateScore::actionType).doesNotContain(ActionType.VOIR);
    }

    @Test
    void shouldPenalizeImmediateReturnToRecentlyVisitedCell() {
        HexCoordinate recentPosition = new HexCoordinate(3, 2);
        RememberedCell recent = new RememberedCell(new Cell(recentPosition, TerrainType.PLAIN, 0), true, 9);
        PeonDecision contextDecision = this.provider.decide(this.context(100, false, Map.of(), Map.of("3:2", recent), List.of(), 0, List.of(recentPosition, new HexCoordinate(1, 2))));

        assertThat(contextDecision.candidates().stream().filter(candidate -> recentPosition.equals(candidate.destination())).findFirst().orElseThrow().factors()).containsKey("retour immédiat");
        assertThat(contextDecision.action().destination()).isNotEqualTo(recentPosition);
    }

    @Test
    void shouldValueWheatFromUsefulHealingInsteadOfNominalHealing() {
        UUID teamId = UUID.randomUUID();
        PeonDecisionContext.ObservedGranary granary = new PeonDecisionContext.ObservedGranary(UUID.randomUUID(), teamId, new HexCoordinate(2, 2), 10);
        PeonDecisionContext context = new PeonDecisionContext(UUID.randomUUID(), teamId, 99, 100, 0, 1, new HexCoordinate(2, 2), false, 10, 42, new PeonPersonality(50, 50, 50, 50), Map.of(), Map.of(), List.of(), 0, List.of(), 10, 9, TerrainType.PLAIN, 0, 0, 0, false, null, List.of(), 0, 0, 0, List.of(), null, granary, List.of(), List.of(granary), false, false, 4, null);

        PeonDecision.CandidateScore wheat = this.provider.decide(context).candidates().stream().filter(candidate -> candidate.actionType() == ActionType.MANGER_BLE).findFirst().orElseThrow();

        assertThat(wheat.factors()).containsEntry("soin utile", 0.6);
        assertThat(wheat.factors().get("réserve renouvelable")).isLessThan(1.0);
    }

    @Test
    void shouldNotApplyBonusFromCompletedStrategy() {
        PeonDecisionContext base = this.agricultureContext(0, 0);
        LongTermStrategy strategy = new LongTermStrategy(StrategyType.AUTONOMIE_ALIMENTAIRE, 1, base.teamId());
        strategy.setPhase(StrategicPhase.CREER_UN_PREMIER_CHAMP);
        strategy.setStatus(StrategyStatus.ACCOMPLIE);
        PeonDecisionContext context = new PeonDecisionContext(base.peonId(), base.teamId(), base.healthPoints(), base.maxHealthPoints(), base.experiencePoints(), base.level(), base.position(), base.concealedByTree(), base.sequenceNumber(), base.decisionSeed(), base.personality(), base.learnedActions(), base.mentalMap(), base.peonsOnCurrentCell(), base.foodOnCurrentCell(), base.traversableNeighbors(), base.currentRound(), base.lastObservationRound(), base.currentTerrain(), base.treeHealthPoints(), base.woodQuantity(), base.woodBundleQuantity(), base.buildable(), base.house(), base.knownHouses(), base.usefulCommunicationCount(), base.consecutiveCommunicationCount(), base.sheepOnCurrentCell(), base.knownSheepPositions(), base.field(), base.granary(), base.knownFields(), base.knownGranaries(), base.fieldBuildable(), base.granaryBuildable(), base.livingTeamPeonCount(), strategy);

        PeonDecision decision = this.provider.decide(context);

        assertThat(decision.candidates()).allSatisfy(candidate -> assertThat(candidate.factors().keySet()).noneMatch(key -> key.startsWith("projet à long terme")));
    }

    @Test
    void shouldNotOfferCommunicationToAnAbsentHost() {
        UUID peonId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        PeonDecisionContext.ObservedHouse ownHouse = new PeonDecisionContext.ObservedHouse(UUID.randomUUID(), peonId, teamId, new HexCoordinate(2, 2), true, true);
        PeonDecisionContext context = new PeonDecisionContext(peonId, teamId, 100, 100, 0, 1, new HexCoordinate(2, 2), false, 10, 42, new PeonPersonality(50, 50, 50, 90), Map.of(), Map.of(), List.of(), 0, List.of(), 10, 9, TerrainType.PLAIN, 0, 0, 0, false, ownHouse, List.of(ownHouse), 0, 0);

        assertThat(this.provider.decide(context).candidates()).extracting(PeonDecision.CandidateScore::actionType).doesNotContain(ActionType.COMMUNIQUER);
    }

    @Test
    void shouldMoveAwayFromKnownGraveWhenPrudent() {
        PeonDecision decision = this.decisionNearGrave(new PeonPersonality(80, 10, 10, 50));

        double toward = this.movementScore(decision, new HexCoordinate(3, 2));
        double away = this.movementScore(decision, new HexCoordinate(1, 2));
        assertThat(away).isGreaterThan(toward);
        assertThat(decision.situationKey()).contains("TOMBE_CONNUE");
    }

    @Test
    void shouldInvestigateKnownGraveWhenCuriousAndAggressive() {
        PeonDecision decision = this.decisionNearGrave(new PeonPersonality(10, 70, 90, 50));

        double toward = this.movementScore(decision, new HexCoordinate(3, 2));
        double away = this.movementScore(decision, new HexCoordinate(1, 2));
        assertThat(toward).isGreaterThan(away);
    }

    @Test
    void shouldNeverExploreAImmediatelyLethalMovement() {
        GameDefaultsProperties properties = new GameDefaultsProperties();
        properties.setExplorationPercentage(100);
        properties.setCriticalHealthExplorationThreshold(0);
        RuleBasedPeonDecisionProvider exploringProvider = new RuleBasedPeonDecisionProvider(properties);
        PeonDecisionContext context = this.context(6, false, Map.of(), Map.of(), List.of(), 0, List.of(new HexCoordinate(3, 2)));

        PeonDecision decision = exploringProvider.decide(context);
        assertThat(decision.action().type()).isNotEqualTo(ActionType.SE_DEPLACER);
        assertThat(decision.candidates().stream().filter(candidate -> candidate.actionType() == ActionType.SE_DEPLACER).findFirst().orElseThrow().admissible()).isFalse();
        assertThat(decision.candidates().stream().filter(candidate -> candidate.actionType() == ActionType.SE_DEPLACER).findFirst().orElseThrow().exclusionReason()).contains("létale");
    }

    @Test
    void shouldPreserveFoodReserveInsteadOfOfferingTrade() {
        UUID teamId = UUID.randomUUID();
        PeonDecisionContext.ObservedPeon ally = new PeonDecisionContext.ObservedPeon(UUID.randomUUID(), teamId, 100, 100, 1, 100, 0);
        PeonDecisionContext context = new PeonDecisionContext(UUID.randomUUID(), teamId, 30, 100, 0, 1, new HexCoordinate(2, 2), false, 10, 42, new PeonPersonality(50, 50, 50, 90), Map.of(), Map.of(), List.of(ally), 0, List.of(), 10, 9, TerrainType.PLAIN, 0, 0, 20, 0, false, null, List.of(), 0, 0, 0, List.of(), null, null, List.of(), List.of(), false, false, 2, null);

        assertThat(this.provider.decide(context).candidates()).extracting(PeonDecision.CandidateScore::actionType).doesNotContain(ActionType.TROQUER_NOURRITURE_CONTRE_BOIS);
    }

    @Test
    void shouldPenalizeAnActionAfterRepeatedFailures() {
        ActionKnowledge knowledge = new ActionKnowledge();
        knowledge.learn(-5, 1);
        knowledge.learn(-5, 1);
        String situation = "PV_ELEVES:SEUL:NOURRITURE_INCONNUE:MOUTON_INCONNU:AUCUNE_TOMBE_CONNUE:BOIS_INCOMPLET:BUTIN_INCONNU:SANS_ABRI:DECOUVERT";
        PeonDecision decision = this.provider.decide(this.context(80, false, Map.of(situation + "|NE_RIEN_FAIRE", knowledge), Map.of(), List.of(), 0, List.of()));

        assertThat(decision.candidates().stream().filter(candidate -> candidate.actionType() == ActionType.NE_RIEN_FAIRE).findFirst().orElseThrow().factors()).containsKey("échecs répétés");
    }

    @Test
    void shouldMoveIntoUnoccupiedHouseWhenReducedHungerMakesMovementSurvivable() {
        UUID teamId = UUID.randomUUID();
        HexCoordinate position = new HexCoordinate(2, 2);
        HexCoordinate housePosition = new HexCoordinate(3, 2);
        PeonDecisionContext.ObservedHouse house = new PeonDecisionContext.ObservedHouse(UUID.randomUUID(), null, null, housePosition, false, false);
        PeonDecisionContext context = new PeonDecisionContext(UUID.randomUUID(), teamId, 9, 100, 0, 1, position, false, 10, 42, new PeonPersonality(90, 10, 10, 50), Map.of(), Map.of(), List.of(), 0, List.of(housePosition), 10, 9, TerrainType.PLAIN, 0, 0, 0, false, null, List.of(house), 0, 0);

        PeonDecision decision = this.provider.decide(context);

        assertThat(decision.action().type()).isEqualTo(ActionType.SE_DEPLACER);
        assertThat(decision.action().destination()).isEqualTo(housePosition);
    }

    @Test
    void shouldNotExploreAnAlternativeTooFarBelowTheBestScore() {
        GameDefaultsProperties properties = new GameDefaultsProperties();
        properties.setExplorationPercentage(100);
        properties.setExplorationMaximumScoreGap(20);
        RuleBasedPeonDecisionProvider exploringProvider = new RuleBasedPeonDecisionProvider(properties);
        ActionKnowledge knowledge = new ActionKnowledge();
        knowledge.setExpectedReward(120);
        String situation = "PV_ELEVES:SEUL:NOURRITURE_INCONNUE:DECOUVERT";
        PeonDecisionContext context = this.context(80, false, Map.of(situation + "|NE_RIEN_FAIRE", knowledge), Map.of(), List.of(), 0, List.of(new HexCoordinate(3, 2)));

        assertThat(exploringProvider.decide(context).action().type()).isEqualTo(ActionType.NE_RIEN_FAIRE);
    }

    @Test
    void shouldLootWoodBundleOnCurrentCell() {
        PeonDecisionContext context = new PeonDecisionContext(UUID.randomUUID(), UUID.randomUUID(), 80, 100, 0, 1, new HexCoordinate(2, 2), false, 10, 42, new PeonPersonality(50, 50, 50, 50, 70), Map.of(), Map.of(), List.of(), 0, List.of(), 10, 9, TerrainType.PLAIN, 0, 0, 75, false, null, List.of(), 0, 0);

        PeonDecision decision = this.provider.decide(context);

        assertThat(decision.action().type()).isEqualTo(ActionType.PILLER);
        assertThat(decision.candidates().stream().filter(candidate -> candidate.actionType() == ActionType.PILLER).findFirst().orElseThrow().factors()).containsKeys("expérience", "bois récupérable", "projet de maison");
    }

    @Test
    void shouldValueLootOpportunityWhenAttackCanKillEnemy() {
        UUID teamId = UUID.randomUUID();
        PeonDecisionContext.ObservedPeon enemy = new PeonDecisionContext.ObservedPeon(UUID.randomUUID(), UUID.randomUUID(), 10, 1);
        PeonDecisionContext context = new PeonDecisionContext(UUID.randomUUID(), teamId, 100, 100, 0, 1, new HexCoordinate(2, 2), false, 10, 42, new PeonPersonality(20, 80, 50, 50, 70), Map.of(), Map.of(), List.of(enemy), 0, List.of(), 10, 9, TerrainType.PLAIN, 0, 0, 0, false, null, List.of(), 0, 0);

        PeonDecision decision = this.provider.decide(context);

        assertThat(decision.candidates().stream().filter(candidate -> candidate.actionType() == ActionType.ATTAQUER).findFirst().orElseThrow().factors()).containsKey("pillage après victoire");
    }

    @Test
    void shouldLeaveHouseForReachableFoodBeforeItIsTooLate() {
        UUID teamId = UUID.randomUUID();
        HexCoordinate position = new HexCoordinate(6, 8);
        HexCoordinate foodPosition = new HexCoordinate(5, 9);
        RememberedCell foodMemory = new RememberedCell(new Cell(foodPosition, TerrainType.PLAIN, 2), false, 20);
        PeonDecisionContext.ObservedPeon ally = new PeonDecisionContext.ObservedPeon(UUID.randomUUID(), teamId, 80, 2);
        PeonDecisionContext.ObservedHouse house = new PeonDecisionContext.ObservedHouse(UUID.randomUUID(), UUID.randomUUID(), teamId, position, true, true);
        PeonDecisionContext context = new PeonDecisionContext(UUID.randomUUID(), teamId, 15, 120, 100, 2, position, false, 30, 42, new PeonPersonality(69, 18, 72, 83, 23), Map.of(), Map.of("5:9", foodMemory), List.of(ally), 0, List.of(foodPosition), 30, 29, TerrainType.PLAIN, 0, 0, 0, false, house, List.of(house), 18, 0);

        PeonDecision decision = this.provider.decide(context);

        assertThat(decision.action().type()).isEqualTo(ActionType.SE_DEPLACER);
        assertThat(decision.action().destination()).isEqualTo(foodPosition);
    }

    private PeonDecision decisionNearGrave(PeonPersonality personality) {
        UUID peonId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        HexCoordinate gravePosition = new HexCoordinate(3, 2);
        Cell graveCell = new Cell(gravePosition, TerrainType.PLAIN, 0);
        RememberedGraveObservation grave = new RememberedGraveObservation(UUID.randomUUID(), UUID.randomUUID(), PeonRelation.ENEMY, gravePosition, 4, 8, 9);
        RememberedCell memory = new RememberedCell(graveCell, false, 9, List.of(), List.of(grave));
        PeonDecisionContext context = new PeonDecisionContext(peonId, teamId, 100, 100, 0, 1, new HexCoordinate(2, 2), false, 10, 42, personality, Map.of(), Map.of("3:2", memory), List.of(), 0, List.of(new HexCoordinate(3, 2), new HexCoordinate(1, 2)), 10, 9);
        return this.provider.decide(context);
    }

    private double movementScore(PeonDecision decision, HexCoordinate destination) {
        return decision.candidates().stream().filter(candidate -> candidate.actionType() == ActionType.SE_DEPLACER && destination.equals(candidate.destination())).findFirst().orElseThrow().score();
    }

    private PeonDecisionContext context(int health, boolean concealed, Map<String, ActionKnowledge> learning, Map<String, RememberedCell> mentalMap, List<PeonDecisionContext.ObservedPeon> occupants, int food, List<HexCoordinate> neighbors) {
        return new PeonDecisionContext(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.randomUUID(), health, 100, 0, 1, new HexCoordinate(2, 2), concealed, 10, 42, new PeonPersonality(50, 50, 50, 50), learning, mentalMap, occupants, food, neighbors, 10, 9);
    }

    private PeonDecisionContext agricultureContext(int fieldCount, int storedWheat) {
        UUID teamId = UUID.randomUUID();
        HexCoordinate position = new HexCoordinate(2, 2);
        PeonDecisionContext.ObservedGranary granary = new PeonDecisionContext.ObservedGranary(UUID.randomUUID(), teamId, new HexCoordinate(3, 2), storedWheat);
        List<HexCoordinate> fieldPositions = List.of(new HexCoordinate(3, 3), new HexCoordinate(4, 2), new HexCoordinate(4, 1), new HexCoordinate(3, 1));
        List<PeonDecisionContext.ObservedField> fields = java.util.stream.IntStream.range(0, fieldCount).mapToObj(index -> new PeonDecisionContext.ObservedField(UUID.randomUUID(), teamId, fieldPositions.get(index), 0)).toList();
        return new PeonDecisionContext(UUID.randomUUID(), teamId, 100, 100, 0, 1, position, false, 10, 42, new PeonPersonality(50, 50, 50, 70), Map.of(), Map.of(), List.of(), 0, List.of(), 10, 9, TerrainType.PLAIN, 0, 200, 0, true, null, List.of(), 0, 0, 0, List.of(), null, null, fields, List.of(granary), true, false, 6, null);
    }

    private GameDefaultsProperties defaults() {
        GameDefaultsProperties properties = new GameDefaultsProperties();
        properties.setExplorationPercentage(0);
        return properties;
    }
}
