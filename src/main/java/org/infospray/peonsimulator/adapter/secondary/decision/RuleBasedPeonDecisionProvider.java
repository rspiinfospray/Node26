package org.infospray.peonsimulator.adapter.secondary.decision;

import org.infospray.peonsimulator.application.dto.PeonDecision;
import org.infospray.peonsimulator.application.dto.PeonDecisionContext;
import org.infospray.peonsimulator.application.port.secondary.PeonDecisionProvider;
import org.infospray.peonsimulator.configuration.GameDefaultsProperties;
import org.infospray.peonsimulator.domain.action.PeonAction;
import org.infospray.peonsimulator.domain.model.ActionKnowledge;
import org.infospray.peonsimulator.domain.model.ActionType;
import org.infospray.peonsimulator.domain.model.HexCoordinate;
import org.infospray.peonsimulator.domain.model.PeonPurpose;
import org.infospray.peonsimulator.domain.model.PeonRelation;
import org.infospray.peonsimulator.domain.model.RememberedCell;
import org.infospray.peonsimulator.domain.model.RememberedGraveObservation;
import org.infospray.peonsimulator.domain.model.RememberedPeonObservation;
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
        if (context.foodOnCurrentCell() > 0) { candidates.add(this.scoreEat(context, situationKey)); }
        if (context.currentTerrain() == TerrainType.TREE && context.treeHealthPoints() > 0) { candidates.add(this.scoreChopWood(context, situationKey)); }
        if (context.buildable() && context.woodQuantity() >= this.defaults.getWoodRequiredForHouse()) { candidates.add(this.scoreBuildHouse(context, situationKey)); }
        context.peonsOnCurrentCell().stream().filter(peon -> !peon.teamId().equals(context.teamId())).forEach(enemy -> candidates.add(this.scoreAttack(context, enemy, situationKey)));
        if (context.peonsOnCurrentCell().stream().anyMatch(peon -> peon.teamId().equals(context.teamId())) || context.house() != null && context.house().hostTeamId() != null && context.house().hostTeamId().equals(context.teamId())) { candidates.add(this.scoreCommunicate(context, situationKey)); }
        RememberedPeonObservation knownEnemy = this.closestKnownEnemy(context);
        HexCoordinate knownFood = this.closestKnownFood(context);
        HexCoordinate knownTree = this.closestKnownTree(context);
        RememberedGraveObservation knownGrave = this.closestKnownGrave(context);
        context.traversableNeighbors().forEach(destination -> candidates.add(this.scoreMove(context, destination, knownEnemy, knownFood, knownTree, knownGrave, situationKey)));
        candidates.add(this.scoreSee(context, knownEnemy, knownGrave, situationKey));
        candidates.add(this.scoreIdle(context, knownEnemy, knownGrave, situationKey));
        candidates.sort(Comparator.comparingDouble(WeightedAction::score).reversed());
        Random random = new Random(context.decisionSeed() ^ context.peonId().getMostSignificantBits() ^ context.sequenceNumber());
        boolean exploration = candidates.size() > 1 && random.nextInt(100) < this.defaults.getExplorationPercentage();
        WeightedAction selected = exploration ? candidates.get(1 + random.nextInt(candidates.size() - 1)) : candidates.get(0);
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
        return this.weighted(new PeonAction(ActionType.MANGER, null, null, PeonPurpose.SURVIVRE, ""), factors);
    }

    private WeightedAction scoreAttack(PeonDecisionContext context, PeonDecisionContext.ObservedPeon enemy, String situationKey) {
        Map<String, Double> factors = new LinkedHashMap<>();
        factors.put("expérience", this.defaults.getAttackExperienceGain() * 0.5);
        factors.put("agressivité", context.personality().getAggressiveness() * 0.35);
        factors.put("avantage de niveau", (context.level() - enemy.level()) * 14.0);
        factors.put("avantage de PV", (context.healthPoints() - enemy.healthPoints()) * 0.25);
        factors.put("risque de dégâts", -Math.max(0, enemy.level() * 10 + 10) * context.personality().getPrudence() / 100.0);
        if (this.healthPercentage(context) < this.defaults.getAttackMinimumHealth()) { factors.put("PV critiques", -55.0); }
        this.addLearning(context, situationKey, ActionType.ATTAQUER, factors);
        return this.weighted(new PeonAction(ActionType.ATTAQUER, null, enemy.id(), PeonPurpose.GAGNER_DES_NIVEAUX, ""), factors);
    }

    private WeightedAction scoreCommunicate(PeonDecisionContext context, String situationKey) {
        Map<String, Double> factors = new LinkedHashMap<>();
        factors.put("solidarité", context.personality().getSolidarity() * 0.55);
        factors.put("connaissances partageables", Math.min(25, context.mentalMap().size() * 0.15));
        this.addLearning(context, situationKey, ActionType.COMMUNIQUER, factors);
        return this.weighted(new PeonAction(ActionType.COMMUNIQUER, null, null, PeonPurpose.AIDER_LES_ALLIES, ""), factors);
    }

    private WeightedAction scoreChopWood(PeonDecisionContext context, String situationKey) {
        Map<String, Double> factors = new LinkedHashMap<>();
        factors.put("ingéniosité", context.personality().getIngenuity() * 0.55);
        factors.put("expérience", this.defaults.getChopWoodExperienceGain() * 0.45);
        factors.put("besoin de bois", Math.max(0, this.defaults.getWoodRequiredForHouse() - context.woodQuantity()) * 0.18);
        this.addLearning(context, situationKey, ActionType.COUPER_DU_BOIS, factors);
        return this.weighted(new PeonAction(ActionType.COUPER_DU_BOIS, null, null, PeonPurpose.SURVIVRE, ""), factors);
    }

    private WeightedAction scoreBuildHouse(PeonDecisionContext context, String situationKey) {
        Map<String, Double> factors = new LinkedHashMap<>();
        factors.put("ingéniosité", context.personality().getIngenuity() * 0.75);
        factors.put("abri", (100.0 - this.healthPercentage(context)) * 0.35 + context.personality().getPrudence() * 0.35);
        factors.put("expérience", this.defaults.getBuildHouseExperienceGain() * 0.45);
        this.addLearning(context, situationKey, ActionType.CONSTRUIRE_MAISON, factors);
        return this.weighted(new PeonAction(ActionType.CONSTRUIRE_MAISON, null, null, PeonPurpose.SURVIVRE, ""), factors);
    }

    private WeightedAction scoreMove(PeonDecisionContext context, HexCoordinate destination, RememberedPeonObservation enemy, HexCoordinate food, HexCoordinate tree, RememberedGraveObservation grave, String situationKey) {
        Map<String, Double> factors = new LinkedHashMap<>();
        RememberedCell destinationMemory = context.mentalMap().get(destination.q() + ":" + destination.r());
        factors.put("expérience", this.defaults.getMoveExperienceGain() * 0.35);
        if (destinationMemory != null && !destinationMemory.isVisited()) { factors.put("exploration", 18.0 + context.personality().getCuriosity() * 0.35); }
        if (food != null) {
            int progress = context.position().distanceTo(food) - destination.distanceTo(food);
            double need = (double) (context.maxHealthPoints() - context.healthPoints()) / context.maxHealthPoints();
            factors.put("recherche de nourriture", progress * need * 55);
        }
        if (tree != null && context.woodQuantity() < this.defaults.getWoodRequiredForHouse()) {
            int progress = context.position().distanceTo(tree) - destination.distanceTo(tree);
            factors.put("recherche de bois", progress * context.personality().getIngenuity() * 0.32);
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
        factors.put("coût en PV", -this.defaults.getMoveHealthCost() * (1 + context.personality().getPrudence() / 100.0));
        this.addLearning(context, situationKey, ActionType.SE_DEPLACER, factors);
        boolean graveWarningDominates = grave != null && context.personality().getPrudence() > context.personality().getCuriosity() + context.personality().getAggressiveness();
        PeonPurpose purpose = food != null && this.healthPercentage(context) < this.defaults.getSeekFoodHealthThreshold() || enemy != null && this.isDangerous(context, enemy.healthPoints(), enemy.level()) || graveWarningDominates ? PeonPurpose.SURVIVRE : PeonPurpose.GAGNER_DES_NIVEAUX;
        return this.weighted(new PeonAction(ActionType.SE_DEPLACER, destination, null, purpose, ""), factors);
    }

    private WeightedAction scoreSee(PeonDecisionContext context, RememberedPeonObservation enemy, RememberedGraveObservation grave, String situationKey) {
        Map<String, Double> factors = new LinkedHashMap<>();
        int roundsSinceObservation = Math.max(0, context.currentRound() - context.lastObservationRound());
        int staleRounds = Math.max(0, roundsSinceObservation - 1);
        factors.put(staleRounds == 0 ? "perception encore fraîche" : "perception à actualiser", staleRounds == 0 ? -12.0 : Math.min(35, staleRounds * 6.0));
        if (staleRounds > 0) { factors.put("curiosité", context.personality().getCuriosity() * 0.12); }
        if (grave != null) { factors.put("surveillance autour de la tombe", context.personality().getCuriosity() * 0.10 + context.personality().getAggressiveness() * 0.06); }
        if (context.concealedByTree() && enemy != null && this.isDangerous(context, enemy.healthPoints(), enemy.level())) { factors.put("maintien à couvert", context.personality().getPrudence() * 0.5); }
        this.addLearning(context, situationKey, ActionType.VOIR, factors);
        return this.weighted(new PeonAction(ActionType.VOIR, null, null, PeonPurpose.SURVIVRE, ""), factors);
    }

    private WeightedAction scoreIdle(PeonDecisionContext context, RememberedPeonObservation enemy, RememberedGraveObservation grave, String situationKey) {
        Map<String, Double> factors = new LinkedHashMap<>();
        factors.put("inaction", -8.0);
        if (context.concealedByTree() && enemy != null && this.isDangerous(context, enemy.healthPoints(), enemy.level())) { factors.put("camouflage", context.personality().getPrudence() * 0.35); }
        if (context.concealedByTree() && grave != null) { factors.put("vigilance près d'une tombe", context.personality().getPrudence() * 0.20); }
        this.addLearning(context, situationKey, ActionType.NE_RIEN_FAIRE, factors);
        return this.weighted(PeonAction.idle(PeonPurpose.SURVIVRE, ""), factors);
    }

    private void addLearning(PeonDecisionContext context, String situationKey, ActionType actionType, Map<String, Double> factors) {
        ActionKnowledge knowledge = context.learnedActions().get(situationKey + "|" + actionType.name());
        if (knowledge == null) {
            String legacySituationKey = situationKey.replace(":AUCUNE_TOMBE_CONNUE", "").replace(":TOMBE_CONNUE", "").replace(":BOIS_INCOMPLET", "").replace(":BOIS_COMPLET", "").replace(":SANS_ABRI", "").replace(":ABRITE", "");
            knowledge = context.learnedActions().get(legacySituationKey + "|" + actionType.name());
        }
        if (knowledge != null) { factors.put("expérience passée", knowledge.getExpectedReward()); }
    }

    private WeightedAction weighted(PeonAction action, Map<String, Double> factors) {
        return new WeightedAction(action, factors.values().stream().mapToDouble(Double::doubleValue).sum(), factors);
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
        String grave = this.closestKnownGrave(context) == null ? "AUCUNE_TOMBE_CONNUE" : "TOMBE_CONNUE";
        String shelter = context.house() != null && context.house().actorInside() ? "ABRITE" : "SANS_ABRI";
        String wood = context.woodQuantity() >= this.defaults.getWoodRequiredForHouse() ? "BOIS_COMPLET" : "BOIS_INCOMPLET";
        return health + ":" + company + ":" + food + ":" + grave + ":" + wood + ":" + shelter + ":" + (context.concealedByTree() ? "ARBRE" : "DECOUVERT");
    }

    private HexCoordinate closestKnownFood(PeonDecisionContext context) {
        return context.mentalMap().values().stream().filter(cell -> cell.getRememberedFoodQuantity() > 0).min(Comparator.comparingInt(cell -> cell.getCoordinate().distanceTo(context.position()))).map(RememberedCell::getCoordinate).orElse(null);
    }

    private HexCoordinate closestKnownTree(PeonDecisionContext context) {
        return context.mentalMap().values().stream().filter(cell -> cell.getTerrain() == TerrainType.TREE).min(Comparator.comparingInt(cell -> cell.getCoordinate().distanceTo(context.position()))).map(RememberedCell::getCoordinate).orElse(null);
    }

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

    private boolean isDangerous(PeonDecisionContext context, int enemyHealth, int enemyLevel) {
        double healthPercentage = this.healthPercentage(context);
        return healthPercentage <= this.defaults.getEnemyFleeHealthThreshold() || enemyLevel >= context.level() + 2 || enemyHealth > context.healthPoints() && healthPercentage < this.defaults.getEnemyPursuitHealthThreshold();
    }

    private double healthPercentage(PeonDecisionContext context) { return context.healthPoints() * 100.0 / context.maxHealthPoints(); }

    private double round(double value) { return Math.round(value * 10.0) / 10.0; }
    private String signed(double value) { return (value >= 0 ? "+" : "") + this.round(value); }

    private record WeightedAction(PeonAction action, double score, Map<String, Double> factors) {
    }
}
