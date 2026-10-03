package org.infospray.peonsimulator.adapter.secondary.decision;

import org.infospray.peonsimulator.application.dto.PeonDecisionContext;
import org.infospray.peonsimulator.application.port.secondary.PeonDecisionProvider;
import org.infospray.peonsimulator.configuration.GameDefaultsProperties;
import org.infospray.peonsimulator.domain.action.PeonAction;
import org.infospray.peonsimulator.domain.model.ActionType;
import org.infospray.peonsimulator.domain.model.HexCoordinate;
import org.infospray.peonsimulator.domain.model.PeonPurpose;
import org.infospray.peonsimulator.domain.model.PeonRelation;
import org.infospray.peonsimulator.domain.model.RememberedCell;
import org.infospray.peonsimulator.domain.model.RememberedPeonObservation;
import org.springframework.stereotype.Component;

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
    public PeonAction decide(PeonDecisionContext context) {
        if (context.foodOnCurrentCell() > 0 && context.healthPoints() < this.defaults.getEatHealthThreshold()) {
            return new PeonAction(ActionType.MANGER, null, null, PeonPurpose.SURVIVRE, "Nourriture disponible et PV incomplets");
        }
        List<PeonDecisionContext.ObservedPeon> enemies = context.peonsOnCurrentCell().stream().filter(peon -> !peon.teamId().equals(context.teamId())).toList();
        if (!enemies.isEmpty()) {
            PeonDecisionContext.ObservedPeon target = enemies.stream().min(Comparator.comparingInt(PeonDecisionContext.ObservedPeon::healthPoints)).orElseThrow();
            if (this.isDangerous(context, target.healthPoints(), target.level())) {
                HexCoordinate escape = this.fleeFrom(context, context.position());
                if (escape != null) { return new PeonAction(ActionType.SE_DEPLACER, escape, null, PeonPurpose.SURVIVRE, "Fuite d'un ennemi sur la même case : risque trop élevé pour les PV"); }
            }
            if (context.healthPoints() >= this.defaults.getAttackMinimumHealth()) {
                return new PeonAction(ActionType.ATTAQUER, null, target.id(), PeonPurpose.GAGNER_DES_NIVEAUX, "Attaque jugée supportable pour obtenir " + this.defaults.getAttackExperienceGain() + " XP");
            }
        }
        HexCoordinate rememberedFood = context.mentalMap().values().stream().filter(cell -> cell.getRememberedFoodQuantity() > 0).min(Comparator.comparingInt(cell -> cell.getCoordinate().distanceTo(context.position()))).map(RememberedCell::getCoordinate).orElse(null);
        if (rememberedFood != null && context.healthPoints() < this.defaults.getSeekFoodHealthThreshold()) {
            HexCoordinate step = context.traversableNeighbors().stream().min(Comparator.comparingInt(cell -> cell.distanceTo(rememberedFood))).orElse(null);
            if (step != null) { return new PeonAction(ActionType.SE_DEPLACER, step, null, PeonPurpose.SURVIVRE, "Recherche d'une nourriture mémorisée"); }
        }
        RememberedPeonObservation knownEnemy = this.closestKnownEnemy(context);
        if (knownEnemy != null && context.concealedByTree() && this.isDangerous(context, knownEnemy.healthPoints(), knownEnemy.level()) && this.isRecent(context, knownEnemy)) {
            return new PeonAction(ActionType.VOIR, null, null, PeonPurpose.SURVIVRE, "Maintien à couvert dans les arbres : l'ennemi dangereux ne peut pas voir le peon depuis une autre case");
        }
        if (knownEnemy != null && this.isDangerous(context, knownEnemy.healthPoints(), knownEnemy.level())) {
            HexCoordinate escape = this.fleeFrom(context, knownEnemy.position());
            if (escape != null) { return new PeonAction(ActionType.SE_DEPLACER, escape, null, PeonPurpose.SURVIVRE, "Éloignement d'un ennemi mémorisé dangereux pour les PV"); }
        }
        if (knownEnemy != null && context.healthPoints() >= this.defaults.getEnemyPursuitHealthThreshold() && knownEnemy.level() <= context.level() + 1) {
            HexCoordinate pursuit = this.moveToward(context, knownEnemy.position());
            if (pursuit != null) { return new PeonAction(ActionType.SE_DEPLACER, pursuit, null, PeonPurpose.GAGNER_DES_NIVEAUX, "Poursuite d'un ennemi mémorisé pour gagner " + this.defaults.getAttackExperienceGain() + " XP"); }
        }
        boolean allyPresent = context.peonsOnCurrentCell().stream().anyMatch(peon -> peon.teamId().equals(context.teamId()));
        if (allyPresent && context.sequenceNumber() % this.defaults.getCommunicationFrequency() == 0) {
            return new PeonAction(ActionType.COMMUNIQUER, null, null, PeonPurpose.AIDER_LES_ALLIES, "Partage périodique de la carte mentale avec un allié présent");
        }
        List<HexCoordinate> exploration = context.traversableNeighbors().stream().filter(cell -> !context.mentalMap().containsKey(cell.q() + ":" + cell.r())).toList();
        List<HexCoordinate> candidates = exploration.isEmpty() ? context.traversableNeighbors() : exploration;
        if (!candidates.isEmpty()) {
            Random random = new Random(context.decisionSeed() ^ context.peonId().getMostSignificantBits() ^ context.sequenceNumber());
            HexCoordinate destination = candidates.get(random.nextInt(candidates.size()));
            return new PeonAction(ActionType.SE_DEPLACER, destination, null, PeonPurpose.GAGNER_DES_NIVEAUX, exploration.isEmpty() ? "Déplacement pour progresser" : "Exploration d'une case inconnue");
        }
        long newestObservation = context.mentalMap().values().stream().mapToLong(RememberedCell::getLastObservedAtSequence).max().orElse(-1);
        if (newestObservation < context.sequenceNumber()) {
            return new PeonAction(ActionType.VOIR, null, null, PeonPurpose.SURVIVRE, "Actualisation d'une perception devenue ancienne");
        }
        return PeonAction.idle(PeonPurpose.SURVIVRE, "Aucune action praticable");
    }

    private boolean isDangerous(PeonDecisionContext context, int enemyHealth, int enemyLevel) {
        return context.healthPoints() <= this.defaults.getEnemyFleeHealthThreshold() || enemyLevel >= context.level() + 2 || enemyHealth > context.healthPoints() && context.healthPoints() < this.defaults.getEnemyPursuitHealthThreshold();
    }

    private RememberedPeonObservation closestKnownEnemy(PeonDecisionContext context) {
        Map<UUID, RememberedPeonObservation> newest = new LinkedHashMap<>();
        context.mentalMap().values().stream().flatMap(cell -> cell.getRememberedPeons().stream()).filter(observation -> observation.relation() == PeonRelation.ENEMY).forEach(observation -> {
            RememberedPeonObservation previous = newest.get(observation.peonId());
            if (previous == null || observation.observedAtSequence() > previous.observedAtSequence()) { newest.put(observation.peonId(), observation); }
        });
        return newest.values().stream().min(Comparator.comparingInt(observation -> observation.position().distanceTo(context.position()))).orElse(null);
    }

    private boolean isRecent(PeonDecisionContext context, RememberedPeonObservation observation) {
        return context.sequenceNumber() - observation.observedAtSequence() <= Math.max(2, context.level());
    }

    private HexCoordinate fleeFrom(PeonDecisionContext context, HexCoordinate enemyPosition) {
        int currentDistance = context.position().distanceTo(enemyPosition);
        return context.traversableNeighbors().stream().filter(candidate -> candidate.distanceTo(enemyPosition) > currentDistance).max(Comparator.comparingInt(candidate -> candidate.distanceTo(enemyPosition))).orElse(null);
    }

    private HexCoordinate moveToward(PeonDecisionContext context, HexCoordinate enemyPosition) {
        int currentDistance = context.position().distanceTo(enemyPosition);
        return context.traversableNeighbors().stream().filter(candidate -> candidate.distanceTo(enemyPosition) < currentDistance).min(Comparator.comparingInt(candidate -> candidate.distanceTo(enemyPosition))).orElse(null);
    }
}
