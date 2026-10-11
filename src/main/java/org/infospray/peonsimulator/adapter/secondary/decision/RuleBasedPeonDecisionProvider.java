package org.infospray.peonsimulator.adapter.secondary.decision;

import org.infospray.peonsimulator.application.dto.PeonDecision;
import org.infospray.peonsimulator.application.dto.PeonDecisionContext;
import org.infospray.peonsimulator.application.port.secondary.PeonDecisionProvider;
import org.infospray.peonsimulator.configuration.GameDefaultsProperties;
import org.infospray.peonsimulator.domain.action.PeonAction;
import org.infospray.peonsimulator.domain.model.ActionKnowledge;
import org.infospray.peonsimulator.domain.model.ActionType;
import org.infospray.peonsimulator.domain.model.HexCoordinate;
import org.infospray.peonsimulator.domain.model.LongTermStrategy;
import org.infospray.peonsimulator.domain.model.PeonPurpose;
import org.infospray.peonsimulator.domain.model.PeonRelation;
import org.infospray.peonsimulator.domain.model.RememberedCell;
import org.infospray.peonsimulator.domain.model.RememberedGraveObservation;
import org.infospray.peonsimulator.domain.model.RememberedPeonObservation;
import org.infospray.peonsimulator.domain.model.StrategicPhase;
import org.infospray.peonsimulator.domain.model.StrategyType;
import org.infospray.peonsimulator.domain.model.TerrainType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Component
public class RuleBasedPeonDecisionProvider implements PeonDecisionProvider {
    private final GameDefaultsProperties defaults;

    public RuleBasedPeonDecisionProvider(GameDefaultsProperties defaults) { this.defaults = defaults; }

    @Override
    public PeonDecision decide(PeonDecisionContext context) {
        String situationKey = this.situationKey(context);
        List<WeightedAction> candidates = new ArrayList<>();
        if (context.healthPoints() < context.maxHealthPoints() && context.foodOnCurrentCell() > 0) { candidates.add(this.scoreEat(context, situationKey)); }
        if (context.healthPoints() < context.maxHealthPoints() && context.sheepOnCurrentCell() > 0) { candidates.add(this.scoreEatSheep(context, situationKey)); }
        if (context.healthPoints() < context.maxHealthPoints() && context.granary() != null && context.teamId().equals(context.granary().teamId()) && context.granary().wheatQuantity() > 0) { candidates.add(this.scoreEatWheat(context, situationKey)); }
        if (context.field() != null && context.teamId().equals(context.field().teamId()) && context.field().wheatQuantity() > 0 && context.knownGranaries().stream().anyMatch(granary -> context.teamId().equals(granary.teamId()))) { candidates.add(this.scoreHarvest(context, situationKey)); }
        if (context.currentTerrain() == TerrainType.TREE && context.treeHealthPoints() > 0) { candidates.add(this.scoreChopWood(context, situationKey)); }
        if (context.buildable() && context.woodQuantity() >= this.defaults.getWoodRequiredForHouse()) { candidates.add(this.scoreBuildHouse(context, situationKey)); }
        if (context.fieldBuildable() && context.woodQuantity() >= this.defaults.getWoodRequiredForField() && this.shouldExpandAgriculture(context)) { candidates.add(this.scoreBuildField(context, situationKey)); }
        if (context.granaryBuildable() && context.woodQuantity() >= this.defaults.getWoodRequiredForGranary()) { candidates.add(this.scoreBuildGranary(context, situationKey)); }
        if (context.woodBundleQuantity() > 0) { candidates.add(this.scoreLoot(context, situationKey)); }
        context.peonsOnCurrentCell().stream().filter(peon -> !peon.teamId().equals(context.teamId())).forEach(enemy -> candidates.add(this.scoreAttack(context, enemy, situationKey)));
        if (context.peonsOnCurrentCell().stream().anyMatch(peon -> peon.teamId().equals(context.teamId())) || context.house() != null && context.house().hostTeamId() != null && context.house().hostTeamId().equals(context.teamId())) { candidates.add(this.scoreCommunicate(context, situationKey)); }
        RememberedPeonObservation knownEnemy = this.closestKnownEnemy(context);
        HexCoordinate knownFood = this.closestKnownFood(context);
        HexCoordinate knownSheep = this.closestKnownSheep(context);
        HexCoordinate knownTree = this.closestKnownTree(context);
        HexCoordinate knownLoot = this.closestKnownLoot(context);
        HexCoordinate knownField = this.closestUsefulField(context);
        HexCoordinate knownGranary = this.closestUsefulGranary(context);
        FoodTarget survivalTarget = this.bestSurvivalTarget(context, knownFood, knownSheep, knownGranary);
        RememberedGraveObservation knownGrave = this.closestKnownGrave(context);
        PeonDecisionContext.ObservedHouse safeHouse = this.closestKnownSafeHouse(context);
        context.traversableNeighbors().forEach(destination -> candidates.add(this.scoreMove(context, destination, knownEnemy, survivalTarget, knownTree, knownLoot, knownField, knownGrave, safeHouse, situationKey)));
        candidates.add(this.scoreSee(context, knownEnemy, knownGrave, situationKey));
        candidates.add(this.scoreIdle(context, knownEnemy, knownGrave, situationKey));
        candidates.sort(Comparator.comparingDouble(WeightedAction::score).reversed());
        List<WeightedAction> safeCandidates = candidates.stream().filter(candidate -> !this.isPredictablyLethal(context, candidate.action())).toList();
        List<WeightedAction> selectionCandidates = safeCandidates.isEmpty() ? candidates : safeCandidates;
        if (survivalTarget != null && this.healthPercentage(context) <= this.defaults.getSeekFoodHealthThreshold()) {
            List<WeightedAction> survivalCandidates = selectionCandidates.stream().filter(candidate -> this.movesToward(candidate.action(), context.position(), survivalTarget.position()) || this.isEating(candidate.action())).toList();
            if (!survivalCandidates.isEmpty()) { selectionCandidates = survivalCandidates; }
        }
        WeightedAction best = selectionCandidates.get(0);
        List<WeightedAction> explorableAlternatives = selectionCandidates.stream().skip(1).filter(candidate -> candidate.score() >= best.score() - this.defaults.getExplorationMaximumScoreGap()).toList();
        Random random = new Random(context.decisionSeed() ^ context.peonId().getMostSignificantBits() ^ context.sequenceNumber());
        boolean criticalHealth = this.healthPercentage(context) <= this.defaults.getCriticalHealthExplorationThreshold();
        boolean exploration = !criticalHealth && !explorableAlternatives.isEmpty() && random.nextInt(100) < this.defaults.getExplorationPercentage();
        WeightedAction selected = exploration ? explorableAlternatives.get(random.nextInt(explorableAlternatives.size())) : best;
        PeonAction action = new PeonAction(selected.action().type(), selected.action().destination(), selected.action().targetPeonId(), selected.action().purpose(), this.explanation(selected, exploration));
        List<PeonDecision.CandidateScore> scores = candidates.stream().map(candidate -> new PeonDecision.CandidateScore(candidate.action().type(), candidate.action().destination(), candidate.action().targetPeonId(), this.round(candidate.score()), candidate.factors())).toList();
        return new PeonDecision(action, situationKey, this.round(selected.score()), exploration, scores);
    }

    private WeightedAction scoreEat(PeonDecisionContext context, String situationKey) {
        Map<String, Double> factors = new LinkedHashMap<>();
        double missingHealthPercentage = 100.0 - this.healthPercentage(context);
        double expectedHealingPercentage = Math.min(this.defaults.getEatHealthGain(), context.maxHealthPoints() - context.healthPoints()) * 100.0 / context.maxHealthPoints();
        factors.put("survie", missingHealthPercentage * 0.8);
        factors.put("soin attendu", expectedHealingPercentage * 0.7);
        factors.put("expérience", this.defaults.getEatExperienceGain() * 0.4);
        factors.put("prudence", context.personality().getPrudence() * 0.12);
        this.addLearning(context, situationKey, ActionType.MANGER, factors);
        return this.weighted(context, new PeonAction(ActionType.MANGER, null, null, PeonPurpose.SURVIVRE, ""), factors);
    }

    private WeightedAction scoreEatSheep(PeonDecisionContext context, String situationKey) {
        Map<String, Double> factors = new LinkedHashMap<>();
        double missingHealthPercentage = 100.0 - this.healthPercentage(context);
        double expectedHealingPercentage = Math.min(this.defaults.getSheepHealthGain(), context.maxHealthPoints() - context.healthPoints()) * 100.0 / context.maxHealthPoints();
        factors.put("survie", missingHealthPercentage * 0.8);
        factors.put("soin supérieur du mouton", expectedHealingPercentage * 0.7);
        factors.put("expérience supérieure", this.defaults.getSheepExperienceGain() * 0.4);
        factors.put("proie enfin rattrapée", 8.0);
        this.addLearning(context, situationKey, ActionType.MANGER_MOUTON, factors);
        return this.weighted(context, new PeonAction(ActionType.MANGER_MOUTON, null, null, PeonPurpose.SURVIVRE, ""), factors);
    }

    private WeightedAction scoreEatWheat(PeonDecisionContext context, String situationKey) {
        Map<String, Double> factors = new LinkedHashMap<>();
        factors.put("réserve renouvelable", 35.0);
        factors.put("survie", (100.0 - this.healthPercentage(context)) * 0.9);
        factors.put("soin", this.defaults.getWheatHealthGain() * 0.6);
        this.addLearning(context, situationKey, ActionType.MANGER_BLE, factors);
        return this.weighted(context, new PeonAction(ActionType.MANGER_BLE, null, null, PeonPurpose.SURVIVRE, ""), factors);
    }

    private WeightedAction scoreHarvest(PeonDecisionContext context, String situationKey) {
        Map<String, Double> factors = new LinkedHashMap<>();
        factors.put("nourriture durable pour l'équipe", 75.0 + context.personality().getSolidarity() * 0.3);
        factors.put("expérience", this.defaults.getHarvestExperienceGain() * 0.4);
        this.addLearning(context, situationKey, ActionType.RECOLTER, factors);
        return this.weighted(context, new PeonAction(ActionType.RECOLTER, null, null, PeonPurpose.AIDER_LES_ALLIES, ""), factors);
    }

    private WeightedAction scoreBuildField(PeonDecisionContext context, String situationKey) {
        Map<String, Double> factors = new LinkedHashMap<>();
        PeonDecisionContext.ObservedGranary granary = this.closestAlliedGranaryForFieldSite(context);
        int nearbyFieldCount = this.fieldsAround(context, granary).size();
        int targetFieldCount = this.targetFieldCount(context);
        int missingFields = Math.max(0, targetFieldCount - nearbyFieldCount);
        int freeCapacity = this.defaults.getGranaryCapacity() - granary.wheatQuantity();
        factors.put("production renouvelable", 38.0);
        factors.put("multiplication des champs", missingFields * 18.0);
        factors.put("besoins de l'équipe", context.livingTeamPeonCount() * 4.0);
        factors.put("capacité disponible du grenier", freeCapacity * 0.45);
        if (nearbyFieldCount > 0) { factors.put("exploitation agricole déjà établie", nearbyFieldCount * 5.0); }
        factors.put("ingéniosité", context.personality().getIngenuity() * 0.35);
        this.addLearning(context, situationKey, ActionType.CONSTRUIRE_CHAMP, factors);
        return this.weighted(context, new PeonAction(ActionType.CONSTRUIRE_CHAMP, null, null, PeonPurpose.SURVIVRE, ""), factors);
    }

    private boolean shouldExpandAgriculture(PeonDecisionContext context) {
        PeonDecisionContext.ObservedGranary granary = this.closestAlliedGranaryForFieldSite(context);
        if (granary == null || granary.wheatQuantity() >= this.defaults.getGranaryCapacity()) { return false; }
        return this.fieldsAround(context, granary).size() < this.targetFieldCount(context);
    }

    private PeonDecisionContext.ObservedGranary closestAlliedGranaryForFieldSite(PeonDecisionContext context) { return context.knownGranaries().stream().filter(granary -> context.teamId().equals(granary.teamId()) && granary.position().distanceTo(context.position()) <= this.defaults.getFieldGranaryRadius()).min(Comparator.comparingInt(granary -> granary.position().distanceTo(context.position()))).orElse(null); }

    private List<PeonDecisionContext.ObservedField> fieldsAround(PeonDecisionContext context, PeonDecisionContext.ObservedGranary granary) { return granary == null ? List.of() : context.knownFields().stream().filter(field -> context.teamId().equals(field.teamId()) && field.position().distanceTo(granary.position()) <= this.defaults.getFieldGranaryRadius()).toList(); }

    private int targetFieldCount(PeonDecisionContext context) { return Math.max(3, (int) Math.ceil(context.livingTeamPeonCount() / 2.0) + 1); }

    private WeightedAction scoreBuildGranary(PeonDecisionContext context, String situationKey) {
        Map<String, Double> factors = new LinkedHashMap<>();
        factors.put("stockage collectif", 50.0 + context.personality().getSolidarity() * 0.25);
        factors.put("champs connus", context.knownFields().size() * 10.0);
        this.addLearning(context, situationKey, ActionType.CONSTRUIRE_GRENIER, factors);
        return this.weighted(context, new PeonAction(ActionType.CONSTRUIRE_GRENIER, null, null, PeonPurpose.AIDER_LES_ALLIES, ""), factors);
    }

    private WeightedAction scoreAttack(PeonDecisionContext context, PeonDecisionContext.ObservedPeon enemy, String situationKey) {
        Map<String, Double> factors = new LinkedHashMap<>();
        factors.put("expérience", this.defaults.getAttackExperienceGain() * 0.5);
        factors.put("agressivité", context.personality().getAggressiveness() * 0.35);
        factors.put("avantage de niveau", (context.level() - enemy.level()) * 14.0);
        factors.put("avantage de PV", (context.healthPoints() - enemy.healthPoints()) * 0.25);
        factors.put("risque de dégâts", -Math.max(0, enemy.level() * 10 + 10) * context.personality().getPrudence() / 100.0);
        int estimatedDamage = this.defaults.getAttackDamage() + Math.max(0, context.level() - 1) * this.defaults.getAttackDamageGainPerLevel();
        if (estimatedDamage >= enemy.healthPoints()) { factors.put("pillage après victoire", this.defaults.getLootExperienceGain() * 0.8 + context.personality().getIngenuity() * 0.18); }
        if (this.healthPercentage(context) < this.defaults.getAttackMinimumHealth()) { factors.put("PV critiques", -55.0); }
        this.addLearning(context, situationKey, ActionType.ATTAQUER, factors);
        return this.weighted(context, new PeonAction(ActionType.ATTAQUER, null, enemy.id(), PeonPurpose.GAGNER_DES_NIVEAUX, ""), factors);
    }

    private WeightedAction scoreCommunicate(PeonDecisionContext context, String situationKey) {
        Map<String, Double> factors = new LinkedHashMap<>();
        factors.put("solidarité", context.personality().getSolidarity() * 0.55);
        factors.put("connaissances réellement partageables", Math.min(25, context.usefulCommunicationCount() * 0.8));
        if (context.usefulCommunicationCount() == 0) { factors.put("aucune information nouvelle", -30.0); }
        if (context.consecutiveCommunicationCount() > 0) { factors.put("communications répétées", -context.consecutiveCommunicationCount() * 25.0); }
        double foodUrgency = this.foodSurvivalUrgency(context);
        if (foodUrgency > 0) { factors.put("urgence alimentaire", -foodUrgency); }
        if (context.house() != null && !context.house().actorInside() && (context.house().hostTeamId() == null || context.house().hostTeamId().equals(context.teamId()))) {
            factors.put("demande de protection", (100.0 - this.healthPercentage(context)) * 0.55 + context.personality().getPrudence() * 0.35);
            factors.put("économie de faim", this.defaults.getHungerHealthLossPerTurn() * (100 - this.defaults.getHouseHungerPercentage()) / 100.0 * 3.0);
        }
        this.addLearning(context, situationKey, ActionType.COMMUNIQUER, factors);
        return this.weighted(context, new PeonAction(ActionType.COMMUNIQUER, null, null, PeonPurpose.AIDER_LES_ALLIES, ""), factors);
    }

    private WeightedAction scoreChopWood(PeonDecisionContext context, String situationKey) {
        Map<String, Double> factors = new LinkedHashMap<>();
        factors.put("ingéniosité", context.personality().getIngenuity() * 0.55);
        factors.put("expérience", this.defaults.getChopWoodExperienceGain() * 0.45);
        int requiredWood = this.strategicWoodTarget(context);
        factors.put("besoin de bois", Math.max(0, requiredWood - context.woodQuantity()) * 0.18);
        if (context.woodQuantity() >= requiredWood) { factors.put("objectif de bois atteint", -45.0); }
        this.addLearning(context, situationKey, ActionType.COUPER_DU_BOIS, factors);
        return this.weighted(context, new PeonAction(ActionType.COUPER_DU_BOIS, null, null, PeonPurpose.SURVIVRE, ""), factors);
    }

    private WeightedAction scoreBuildHouse(PeonDecisionContext context, String situationKey) {
        Map<String, Double> factors = new LinkedHashMap<>();
        factors.put("ingéniosité", context.personality().getIngenuity() * 0.75);
        factors.put("abri", (100.0 - this.healthPercentage(context)) * 0.35 + context.personality().getPrudence() * 0.35);
        factors.put("expérience", this.defaults.getBuildHouseExperienceGain() * 0.45);
        this.addLearning(context, situationKey, ActionType.CONSTRUIRE_MAISON, factors);
        return this.weighted(context, new PeonAction(ActionType.CONSTRUIRE_MAISON, null, null, PeonPurpose.SURVIVRE, ""), factors);
    }

    private WeightedAction scoreLoot(PeonDecisionContext context, String situationKey) {
        Map<String, Double> factors = new LinkedHashMap<>();
        factors.put("expérience", this.defaults.getLootExperienceGain() * 0.8);
        factors.put("bois récupérable", context.woodBundleQuantity() * 0.28);
        factors.put("ingéniosité", context.personality().getIngenuity() * 0.22);
        if (context.woodQuantity() < this.defaults.getWoodRequiredForHouse()) { factors.put("projet de maison", 18.0); }
        this.addLearning(context, situationKey, ActionType.PILLER, factors);
        return this.weighted(context, new PeonAction(ActionType.PILLER, null, null, PeonPurpose.GAGNER_DES_NIVEAUX, ""), factors);
    }

    private WeightedAction scoreMove(PeonDecisionContext context, HexCoordinate destination, RememberedPeonObservation enemy, FoodTarget survivalTarget, HexCoordinate tree, HexCoordinate loot, HexCoordinate field, RememberedGraveObservation grave, PeonDecisionContext.ObservedHouse safeHouse, String situationKey) {
        Map<String, Double> factors = new LinkedHashMap<>();
        RememberedCell destinationMemory = context.mentalMap().get(destination.q() + ":" + destination.r());
        factors.put("expérience", this.defaults.getMoveExperienceGain() * 0.35);
        if (destinationMemory != null && !destinationMemory.isVisited()) { factors.put("exploration", 18.0 + context.personality().getCuriosity() * 0.35); }
        if (survivalTarget != null) {
            int progress = context.position().distanceTo(survivalTarget.position()) - destination.distanceTo(survivalTarget.position());
            double need = (double) (context.maxHealthPoints() - context.healthPoints()) / context.maxHealthPoints();
            double urgency = this.healthPercentage(context) <= this.defaults.getSeekFoodHealthThreshold() ? 180.0 + (100.0 - this.healthPercentage(context)) : 55.0;
            factors.put("route alimentaire prioritaire · " + survivalTarget.label(), progress * (urgency + need * survivalTarget.healthGain()));
            if (survivalTarget.mobile()) { factors.put("incertitude d’une proie mobile", -6.0); }
        }
        if (tree != null && context.woodQuantity() < this.defaults.getWoodRequiredForHouse()) {
            int progress = context.position().distanceTo(tree) - destination.distanceTo(tree);
            factors.put("recherche de bois", progress * context.personality().getIngenuity() * 0.32);
        }
        if (loot != null) {
            int progress = context.position().distanceTo(loot) - destination.distanceTo(loot);
            factors.put("recherche de butin", progress * (this.defaults.getLootExperienceGain() * 0.8 + context.personality().getIngenuity() * 0.22));
        }
        if (field != null) {
            int knownWheat = context.knownGranaries().stream().filter(candidate -> context.teamId().equals(candidate.teamId())).mapToInt(PeonDecisionContext.ObservedGranary::wheatQuantity).sum();
            double reserveUrgency = knownWheat == 0 ? 90.0 : knownWheat < context.livingTeamPeonCount() * 2 ? 45.0 : 0.0;
            factors.put("récolte du blé", (context.position().distanceTo(field) - destination.distanceTo(field)) * (60.0 + reserveUrgency));
        }
        if (enemy != null) {
            int distanceChange = destination.distanceTo(enemy.position()) - context.position().distanceTo(enemy.position());
            boolean dangerous = this.isDangerous(context, enemy.healthPoints(), enemy.level());
            double motivation = dangerous ? context.personality().getPrudence() * 0.5 : context.personality().getAggressiveness() * -0.28;
            factors.put(dangerous ? "éloignement du danger" : "rapprochement tactique", distanceChange * motivation);
            if (dangerous && destinationMemory != null && destinationMemory.getTerrain() == TerrainType.TREE) { factors.put("couvert des arbres", context.personality().getPrudence() * 0.4); }
        }
        if (grave != null) {
            int distanceChange = destination.distanceTo(grave.position()) - context.position().distanceTo(grave.position());
            factors.put("éloignement de la tombe", distanceChange * context.personality().getPrudence() * 0.30);
            factors.put("enquête autour de la tombe", -distanceChange * (context.personality().getCuriosity() * 0.22 + context.personality().getAggressiveness() * 0.18));
        }
        if (safeHouse != null && !this.isInsideHouse(context)) {
            int progress = context.position().distanceTo(safeHouse.position()) - destination.distanceTo(safeHouse.position());
            double danger = enemy != null && this.isDangerous(context, enemy.healthPoints(), enemy.level()) ? 35.0 : 0.0;
            factors.put("recherche d'un abri", progress * (danger + (100.0 - this.healthPercentage(context)) * 0.45 + context.personality().getPrudence() * 0.35));
        }
        if (this.isInsideHouse(context)) {
            double danger = enemy == null ? 0.0 : context.personality().getPrudence() * (this.isDangerous(context, enemy.healthPoints(), enemy.level()) ? 0.8 : 0.35);
            double hungerProtection = this.defaults.getHungerHealthLossPerTurn() * (100 - this.defaults.getHouseHungerPercentage()) / 100.0 * 4.0;
            factors.put("perte de la protection", -(danger + hungerProtection + (100.0 - this.healthPercentage(context)) * 0.45));
        }
        double tacticalProgress = this.tacticalAgricultureProgress(context, destination);
        if (tacticalProgress != 0) { factors.put("progression vers l'exploitation", tacticalProgress); }
        factors.put("coût en PV", -this.defaults.getMoveHealthCost() * (1 + context.personality().getPrudence() / 100.0));
        this.addLearning(context, situationKey, ActionType.SE_DEPLACER, factors);
        boolean graveWarningDominates = grave != null && context.personality().getPrudence() > context.personality().getCuriosity() + context.personality().getAggressiveness();
        PeonPurpose purpose = survivalTarget != null && this.healthPercentage(context) < this.defaults.getSeekFoodHealthThreshold() || enemy != null && this.isDangerous(context, enemy.healthPoints(), enemy.level()) || graveWarningDominates ? PeonPurpose.SURVIVRE : PeonPurpose.GAGNER_DES_NIVEAUX;
        return this.weighted(context, new PeonAction(ActionType.SE_DEPLACER, destination, null, purpose, ""), factors);
    }

    private WeightedAction scoreSee(PeonDecisionContext context, RememberedPeonObservation enemy, RememberedGraveObservation grave, String situationKey) {
        Map<String, Double> factors = new LinkedHashMap<>();
        int roundsSinceObservation = Math.max(0, context.currentRound() - context.lastObservationRound());
        int staleRounds = Math.max(0, roundsSinceObservation - 1);
        factors.put(staleRounds == 0 ? "perception encore fraîche" : "perception à actualiser", staleRounds == 0 ? -12.0 : Math.min(35, staleRounds * 6.0));
        if (staleRounds > 0) { factors.put("curiosité", context.personality().getCuriosity() * 0.12); }
        if (grave != null) { factors.put("surveillance autour de la tombe", context.personality().getCuriosity() * 0.10 + context.personality().getAggressiveness() * 0.06); }
        if (context.concealedByTree() && enemy != null && this.isDangerous(context, enemy.healthPoints(), enemy.level())) { factors.put("maintien à couvert", context.personality().getPrudence() * 0.5); }
        if (this.isInsideHouse(context)) { factors.put("observation depuis l'abri", 12.0 + context.personality().getPrudence() * 0.25); }
        double foodUrgency = this.foodSurvivalUrgency(context);
        if (foodUrgency > 0) { factors.put("retard avant de manger", -foodUrgency); }
        this.addLearning(context, situationKey, ActionType.VOIR, factors);
        return this.weighted(context, new PeonAction(ActionType.VOIR, null, null, PeonPurpose.SURVIVRE, ""), factors);
    }

    private WeightedAction scoreIdle(PeonDecisionContext context, RememberedPeonObservation enemy, RememberedGraveObservation grave, String situationKey) {
        Map<String, Double> factors = new LinkedHashMap<>();
        factors.put("inaction", -8.0);
        if (context.concealedByTree() && enemy != null && this.isDangerous(context, enemy.healthPoints(), enemy.level())) { factors.put("camouflage", context.personality().getPrudence() * 0.35); }
        if (context.concealedByTree() && grave != null) { factors.put("vigilance près d'une tombe", context.personality().getPrudence() * 0.20); }
        if (this.isInsideHouse(context)) {
            factors.put("protection contre les ennemis", enemy == null ? context.personality().getPrudence() * 0.18 : context.personality().getPrudence() * 0.65);
            factors.put("économie de faim", this.defaults.getHungerHealthLossPerTurn() * (100 - this.defaults.getHouseHungerPercentage()) / 100.0 * 3.0);
        }
        double foodUrgency = this.foodSurvivalUrgency(context);
        if (foodUrgency > 0) { factors.put("attente dangereuse malgré l'abri", -foodUrgency); }
        this.addLearning(context, situationKey, ActionType.NE_RIEN_FAIRE, factors);
        return this.weighted(context, PeonAction.idle(PeonPurpose.SURVIVRE, ""), factors);
    }

    private void addLearning(PeonDecisionContext context, String situationKey, ActionType actionType, Map<String, Double> factors) {
        ActionKnowledge knowledge = context.learnedActions().get(situationKey + "|" + actionType.name());
        if (knowledge == null) {
            String legacySituationKey = situationKey.replace(":AUCUNE_TOMBE_CONNUE", "").replace(":TOMBE_CONNUE", "").replace(":BOIS_INCOMPLET", "").replace(":BOIS_COMPLET", "").replace(":BUTIN_PRESENT", "").replace(":BUTIN_INCONNU", "").replace(":BUTIN_CONNU", "").replace(":SANS_ABRI", "").replace(":ABRITE", "").replace(":MOUTON_PRESENT", "").replace(":MOUTON_CONNU", "").replace(":MOUTON_INCONNU", "");
            knowledge = context.learnedActions().get(legacySituationKey + "|" + actionType.name());
        }
        if (knowledge != null) { factors.put("expérience passée", knowledge.getExpectedReward()); }
    }

    private WeightedAction weighted(PeonDecisionContext context, PeonAction action, Map<String, Double> factors) {
        if (action.type() == ActionType.CONSTRUIRE_MAISON && this.contradictsCurrentProject(context)) { factors.put("construction contraire au projet", -140.0); }
        double strategyBonus = this.strategyBonus(context, action.type());
        if (strategyBonus != 0) { factors.put("projet à long terme · " + context.longTermStrategy().getPhase().name().toLowerCase().replace('_', ' '), strategyBonus); }
        return new WeightedAction(action, factors.values().stream().mapToDouble(Double::doubleValue).sum(), factors);
    }

    private int strategicWoodTarget(PeonDecisionContext context) {
        LongTermStrategy strategy = context.longTermStrategy();
        if (strategy == null || strategy.getType() != StrategyType.AUTONOMIE_ALIMENTAIRE) { return this.defaults.getWoodRequiredForHouse(); }
        return switch (strategy.getPhase()) {
            case CONSTRUIRE_UN_GRENIER -> this.defaults.getWoodRequiredForGranary();
            case CREER_UN_PREMIER_CHAMP, ETENDRE_L_EXPLOITATION -> this.defaults.getWoodRequiredForField();
            default -> this.defaults.getWoodRequiredForHouse();
        };
    }

    private boolean contradictsCurrentProject(PeonDecisionContext context) {
        LongTermStrategy strategy = context.longTermStrategy();
        if (strategy == null) { return false; }
        return strategy.getType() != StrategyType.CONSTRUIRE_UN_FOYER && (strategy.getType() != StrategyType.AUTONOMIE_ALIMENTAIRE || strategy.getPhase() != StrategicPhase.ETABLIR_UN_FOYER);
    }

    private double tacticalAgricultureProgress(PeonDecisionContext context, HexCoordinate destination) {
        LongTermStrategy strategy = context.longTermStrategy();
        if (strategy == null || strategy.getType() != StrategyType.AUTONOMIE_ALIMENTAIRE || strategy.getTargetArea() == null) { return 0; }
        int currentDistance = context.position().distanceTo(strategy.getTargetArea());
        int destinationDistance = destination.distanceTo(strategy.getTargetArea());
        int maximumDistance = switch (strategy.getPhase()) {
            case CONSTRUIRE_UN_GRENIER -> 2;
            case CREER_UN_PREMIER_CHAMP, ETENDRE_L_EXPLOITATION -> this.defaults.getFieldGranaryRadius();
            default -> 0;
        };
        if (maximumDistance == 0) { return 0; }
        if (currentDistance == 0 && destinationDistance == 1) { return 90; }
        if (currentDistance > maximumDistance) { return (currentDistance - destinationDistance) * 90.0; }
        boolean destinationInBuildRing = destinationDistance >= 1 && destinationDistance <= maximumDistance;
        return destinationInBuildRing ? 20 : -60;
    }

    private double strategyBonus(PeonDecisionContext context, ActionType actionType) {
        LongTermStrategy strategy = context.longTermStrategy();
        if (strategy == null) { return 0; }
        return switch (strategy.getType()) {
            case AUTONOMIE_ALIMENTAIRE -> switch (actionType) {
                case CONSTRUIRE_MAISON -> strategy.getPhase() == org.infospray.peonsimulator.domain.model.StrategicPhase.ETABLIR_UN_FOYER ? 45 : 0;
                case CONSTRUIRE_GRENIER -> strategy.getPhase() == org.infospray.peonsimulator.domain.model.StrategicPhase.CONSTRUIRE_UN_GRENIER ? 50 : 0;
                case CONSTRUIRE_CHAMP -> strategy.getPhase() == org.infospray.peonsimulator.domain.model.StrategicPhase.CREER_UN_PREMIER_CHAMP || strategy.getPhase() == org.infospray.peonsimulator.domain.model.StrategicPhase.ETENDRE_L_EXPLOITATION ? 45 : 0;
                case RECOLTER -> strategy.getPhase() == org.infospray.peonsimulator.domain.model.StrategicPhase.REMPLIR_LA_RESERVE || strategy.getPhase() == org.infospray.peonsimulator.domain.model.StrategicPhase.MAINTENIR_L_AUTONOMIE ? 50 : 20;
                case COUPER_DU_BOIS, PILLER -> 25;
                case SE_DEPLACER -> 12;
                default -> 0;
            };
            case CONSTRUIRE_UN_FOYER -> switch (actionType) { case COUPER_DU_BOIS, PILLER -> 35; case CONSTRUIRE_MAISON -> 50; case COMMUNIQUER -> 12; case SE_DEPLACER -> 10; default -> 0; };
            case EXPLORER_LE_MONDE -> switch (actionType) { case VOIR -> 45; case SE_DEPLACER -> 35; case COMMUNIQUER -> 15; default -> 0; };
            case PROTEGER_L_EQUIPE -> switch (actionType) { case ATTAQUER -> 40; case VOIR -> 25; case SE_DEPLACER -> 22; case COMMUNIQUER -> 20; default -> 0; };
        };
    }

    private String explanation(WeightedAction candidate, boolean exploration) {
        String principalFactors = candidate.factors().entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed()).limit(3).map(entry -> entry.getKey() + " " + this.signed(entry.getValue())).reduce((left, right) -> left + ", " + right).orElse("aucun facteur décisif");
        return (exploration ? "Choix d'exploration" : "Meilleur score utilitaire") + " : " + principalFactors;
    }

    private String situationKey(PeonDecisionContext context) {
        double healthPercentage = this.healthPercentage(context);
        String health = healthPercentage <= 20 ? "PV_CRITIQUES" : healthPercentage <= 45 ? "PV_FAIBLES" : healthPercentage < 80 ? "PV_CORRECTS" : healthPercentage < 100 ? "PV_ELEVES" : "PV_MAX";
        List<PeonDecisionContext.ObservedPeon> enemies = context.peonsOnCurrentCell().stream().filter(peon -> !peon.teamId().equals(context.teamId())).toList();
        String company = !enemies.isEmpty() ? "AVEC_ENNEMI" : context.peonsOnCurrentCell().stream().anyMatch(peon -> peon.teamId().equals(context.teamId())) ? "AVEC_ALLIE" : "SEUL";
        String food = context.foodOnCurrentCell() > 0 ? "NOURRITURE_PRESENTE" : this.closestKnownFood(context) != null ? "NOURRITURE_CONNUE" : "NOURRITURE_INCONNUE";
        String sheep = context.sheepOnCurrentCell() > 0 ? "MOUTON_PRESENT" : this.closestKnownSheep(context) != null ? "MOUTON_CONNU" : "MOUTON_INCONNU";
        String grave = this.closestKnownGrave(context) == null ? "AUCUNE_TOMBE_CONNUE" : "TOMBE_CONNUE";
        String shelter = context.house() != null && context.house().actorInside() ? "ABRITE" : "SANS_ABRI";
        String wood = context.woodQuantity() >= this.defaults.getWoodRequiredForHouse() ? "BOIS_COMPLET" : "BOIS_INCOMPLET";
        String loot = context.woodBundleQuantity() > 0 ? "BUTIN_PRESENT" : this.closestKnownLoot(context) == null ? "BUTIN_INCONNU" : "BUTIN_CONNU";
        return health + ":" + company + ":" + food + ":" + sheep + ":" + grave + ":" + wood + ":" + loot + ":" + shelter + ":" + (context.concealedByTree() ? "ARBRE" : "DECOUVERT");
    }

    private HexCoordinate closestKnownFood(PeonDecisionContext context) {
        return context.mentalMap().values().stream().filter(cell -> cell.getRememberedFoodQuantity() > 0).min(Comparator.comparingInt(cell -> cell.getCoordinate().distanceTo(context.position()))).map(RememberedCell::getCoordinate).orElse(null);
    }

    private HexCoordinate closestKnownSheep(PeonDecisionContext context) {
        return context.knownSheepPositions().stream().min(Comparator.comparingInt(position -> position.distanceTo(context.position()))).orElse(null);
    }

    private double foodSurvivalUrgency(PeonDecisionContext context) {
        FoodTarget target = this.bestSurvivalTarget(context, this.closestKnownFood(context), this.closestKnownSheep(context), this.closestUsefulGranary(context));
        if (target == null || this.healthPercentage(context) > this.defaults.getSeekFoodHealthThreshold()) { return 0; }
        return (100.0 - this.healthPercentage(context)) * 0.9 + Math.max(0, 30 - context.healthPoints()) * 3.0;
    }

    private FoodTarget bestSurvivalTarget(PeonDecisionContext context, HexCoordinate food, HexCoordinate sheep, HexCoordinate granary) {
        List<FoodTarget> targets = new ArrayList<>();
        if (context.foodOnCurrentCell() > 0) { targets.add(new FoodTarget(context.position(), this.defaults.getEatHealthGain(), "baies", false, 1)); }
        if (food != null) { targets.add(new FoodTarget(food, this.defaults.getEatHealthGain(), "baies", false, 1)); }
        if (granary != null) { targets.add(new FoodTarget(granary, this.defaults.getWheatHealthGain(), "grenier", false, 0)); }
        if (sheep != null) { targets.add(new FoodTarget(sheep, this.defaults.getSheepHealthGain(), "mouton", true, 2)); }
        int movementTurnCost = this.defaults.getMoveHealthCost() + this.defaults.getHungerHealthLossPerTurn();
        return targets.stream().filter(target -> target.position().equals(context.position()) || context.healthPoints() - context.position().distanceTo(target.position()) * movementTurnCost > 0).min(Comparator.comparingInt((FoodTarget target) -> context.position().distanceTo(target.position()) * movementTurnCost - target.healthGain()).thenComparingInt(FoodTarget::priority)).orElse(null);
    }

    private boolean movesToward(PeonAction action, HexCoordinate origin, HexCoordinate target) { return action.type() == ActionType.SE_DEPLACER && action.destination() != null && action.destination().distanceTo(target) < origin.distanceTo(target); }

    private boolean isEating(PeonAction action) { return action.type() == ActionType.MANGER || action.type() == ActionType.MANGER_BLE || action.type() == ActionType.MANGER_MOUTON; }

    private HexCoordinate closestKnownTree(PeonDecisionContext context) {
        return context.mentalMap().values().stream().filter(cell -> cell.getTerrain() == TerrainType.TREE).min(Comparator.comparingInt(cell -> cell.getCoordinate().distanceTo(context.position()))).map(RememberedCell::getCoordinate).orElse(null);
    }

    private HexCoordinate closestKnownLoot(PeonDecisionContext context) {
        return context.mentalMap().values().stream().filter(cell -> cell.getRememberedWoodBundleQuantity() > 0).min(Comparator.comparingInt(cell -> cell.getCoordinate().distanceTo(context.position()))).map(RememberedCell::getCoordinate).orElse(null);
    }

    private HexCoordinate closestUsefulField(PeonDecisionContext context) { return context.knownFields().stream().filter(field -> context.teamId().equals(field.teamId()) && field.wheatQuantity() > 0).min(Comparator.comparingInt(field -> field.position().distanceTo(context.position()))).map(PeonDecisionContext.ObservedField::position).orElse(null); }

    private HexCoordinate closestUsefulGranary(PeonDecisionContext context) { return context.knownGranaries().stream().filter(granary -> context.teamId().equals(granary.teamId()) && granary.wheatQuantity() > 0).min(Comparator.comparingInt(granary -> granary.position().distanceTo(context.position()))).map(PeonDecisionContext.ObservedGranary::position).orElse(null); }

    private RememberedPeonObservation closestKnownEnemy(PeonDecisionContext context) {
        Map<UUID, RememberedPeonObservation> newest = new LinkedHashMap<>();
        context.mentalMap().values().stream().flatMap(cell -> cell.getRememberedPeons().stream()).filter(observation -> observation.relation() == PeonRelation.ENEMY).forEach(observation -> {
            RememberedPeonObservation previous = newest.get(observation.peonId());
            if (previous == null || observation.observedAtSequence() > previous.observedAtSequence()) { newest.put(observation.peonId(), observation); }
        });
        return newest.values().stream().min(Comparator.comparingInt(observation -> observation.position().distanceTo(context.position()))).orElse(null);
    }

    private RememberedGraveObservation closestKnownGrave(PeonDecisionContext context) {
        Map<UUID, RememberedGraveObservation> newest = new LinkedHashMap<>();
        context.mentalMap().values().stream().flatMap(cell -> cell.getRememberedGraves().stream()).forEach(observation -> {
            RememberedGraveObservation previous = newest.get(observation.peonId());
            if (previous == null || observation.observedAtSequence() > previous.observedAtSequence()) { newest.put(observation.peonId(), observation); }
        });
        return newest.values().stream().min(Comparator.comparingInt(observation -> observation.position().distanceTo(context.position()))).orElse(null);
    }

    private PeonDecisionContext.ObservedHouse closestKnownSafeHouse(PeonDecisionContext context) {
        return context.knownHouses().stream().filter(house -> !house.occupied() || context.teamId().equals(house.hostTeamId())).min(Comparator.comparingInt(house -> house.position().distanceTo(context.position()))).orElse(null);
    }

    private boolean isInsideHouse(PeonDecisionContext context) { return context.house() != null && context.house().actorInside(); }

    private boolean isPredictablyLethal(PeonDecisionContext context, PeonAction action) {
        int experienceGain = switch (action.type()) {
            case MANGER -> this.defaults.getEatExperienceGain();
            case MANGER_MOUTON -> this.defaults.getSheepExperienceGain();
            case MANGER_BLE -> this.defaults.getWheatExperienceGain();
            case SE_DEPLACER -> this.defaults.getMoveExperienceGain();
            case ATTAQUER -> this.defaults.getAttackExperienceGain();
            case COUPER_DU_BOIS -> this.defaults.getChopWoodExperienceGain();
            case CONSTRUIRE_MAISON -> this.defaults.getBuildHouseExperienceGain();
            case CONSTRUIRE_CHAMP -> this.defaults.getBuildFieldExperienceGain();
            case CONSTRUIRE_GRENIER -> this.defaults.getBuildGranaryExperienceGain();
            case RECOLTER -> this.defaults.getHarvestExperienceGain();
            case PILLER -> this.defaults.getLootExperienceGain();
            default -> 0;
        };
        int levelAfter = 1 + (context.experiencePoints() + experienceGain) / this.defaults.getExperiencePerLevel();
        int levelsGained = Math.max(0, levelAfter - context.level());
        int health = levelsGained > 0 ? context.maxHealthPoints() + levelsGained * this.defaults.getMaxHealthGainPerLevel() : context.healthPoints();
        if (action.type() == ActionType.MANGER && levelsGained == 0) { health = Math.min(context.maxHealthPoints(), health + this.defaults.getEatHealthGain()); }
        if (action.type() == ActionType.MANGER_MOUTON && levelsGained == 0) { health = Math.min(context.maxHealthPoints(), health + this.defaults.getSheepHealthGain()); }
        if (action.type() == ActionType.MANGER_BLE && levelsGained == 0) { health = Math.min(context.maxHealthPoints(), health + this.defaults.getWheatHealthGain()); }
        if (action.type() == ActionType.SE_DEPLACER) { health -= this.defaults.getMoveHealthCost(); }
        boolean shelteredAfterAction = action.type() == ActionType.CONSTRUIRE_MAISON || action.type() != ActionType.SE_DEPLACER && this.isInsideHouse(context);
        int hungerLoss = shelteredAfterAction ? (int) Math.ceil(this.defaults.getHungerHealthLossPerTurn() * this.defaults.getHouseHungerPercentage() / 100.0) : this.defaults.getHungerHealthLossPerTurn();
        return health - hungerLoss <= 0;
    }

    private boolean isDangerous(PeonDecisionContext context, int enemyHealth, int enemyLevel) {
        double healthPercentage = this.healthPercentage(context);
        return healthPercentage <= this.defaults.getEnemyFleeHealthThreshold() || enemyLevel >= context.level() + 2 || enemyHealth > context.healthPoints() && healthPercentage < this.defaults.getEnemyPursuitHealthThreshold();
    }

    private double healthPercentage(PeonDecisionContext context) { return context.healthPoints() * 100.0 / context.maxHealthPoints(); }

    private double round(double value) { return Math.round(value * 10.0) / 10.0; }
    private String signed(double value) { return (value >= 0 ? "+" : "") + this.round(value); }

    private record WeightedAction(PeonAction action, double score, Map<String, Double> factors) {
    }

    private record FoodTarget(HexCoordinate position, int healthGain, String label, boolean mobile, int priority) {
    }
}
