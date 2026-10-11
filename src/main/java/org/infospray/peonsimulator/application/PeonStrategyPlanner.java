package org.infospray.peonsimulator.application;

import org.infospray.peonsimulator.configuration.GameDefaultsProperties;
import org.infospray.peonsimulator.domain.model.ActionExperience;
import org.infospray.peonsimulator.domain.model.ActionType;
import org.infospray.peonsimulator.domain.model.Granary;
import org.infospray.peonsimulator.domain.model.House;
import org.infospray.peonsimulator.domain.model.LongTermStrategy;
import org.infospray.peonsimulator.domain.model.Peon;
import org.infospray.peonsimulator.domain.model.RememberedPeonObservation;
import org.infospray.peonsimulator.domain.model.StrategicPhase;
import org.infospray.peonsimulator.domain.model.StrategyStatus;
import org.infospray.peonsimulator.domain.model.StrategyType;
import org.infospray.peonsimulator.domain.model.World;
import org.infospray.peonsimulator.domain.model.WheatField;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PeonStrategyPlanner {
    private static final int REEVALUATION_ROUNDS = 5;
    private static final int COMMITMENT_ROUNDS = 10;

    public boolean update(World world, Peon peon, GameDefaultsProperties defaults) {
        StrategicSituation situation = this.situation(world, peon, defaults);
        LongTermStrategy current = peon.getLongTermStrategy();
        boolean fullEvaluation = current == null || world.getCurrentRound() - current.getLastEvaluatedAtRound() >= REEVALUATION_ROUNDS;
        if (fullEvaluation) {
            StrategyType selected = current != null && world.getCurrentRound() < current.getCommitmentUntilRound() ? current.getType() : this.selectStrategy(peon, situation, world.getCurrentRound());
            if (current == null || selected != current.getType()) {
                current = new LongTermStrategy(selected, world.getCurrentRound(), peon.getTeamId());
                current.setCommitmentUntilRound(world.getCurrentRound() + COMMITMENT_ROUNDS);
                peon.setLongTermStrategy(current);
            }
            current.setExpectedValue(this.strategyScores(peon, situation, world.getCurrentRound()).get(selected));
            current.setLastEvaluatedAtRound(world.getCurrentRound());
        }
        StrategicPhase previousPhase = current.getPhase();
        this.updateProgress(current, situation, peon, defaults);
        if (current.getPhase() != previousPhase) { current.setTacticalUntilRound(world.getCurrentRound() + 5); }
        return fullEvaluation;
    }

    private StrategicSituation situation(World world, Peon peon, GameDefaultsProperties defaults) {
        List<Peon> allies = world.alivePeons().stream().filter(ally -> ally.getTeamId().equals(peon.getTeamId())).toList();
        List<House> houses = world.getHouses().values().stream().filter(house -> house.getOwnerPeonIds().stream().map(world.getPeons()::get).anyMatch(owner -> owner != null && owner.isAlive() && owner.getTeamId().equals(peon.getTeamId()))).toList();
        List<Granary> granaries = world.getGranaries().values().stream().filter(granary -> granary.getTeamId().equals(peon.getTeamId())).toList();
        List<WheatField> fields = world.getWheatFields().values().stream().filter(field -> field.getTeamId().equals(peon.getTeamId())).toList();
        int storedWheat = granaries.stream().mapToInt(Granary::getWheatQuantity).sum();
        int targetFields = Math.max(3, (int) Math.ceil(allies.size() / 2.0) + 1);
        int safetyStock = Math.max(allies.size(), (int) Math.ceil(allies.size() * defaults.getHungerHealthLossPerTurn() * 12.0 / defaults.getWheatHealthGain()));
        long knownEnemies = peon.getMentalMap().values().stream().flatMap(memory -> memory.getRememberedPeons().stream()).filter(observation -> !observation.teamId().equals(peon.getTeamId())).map(RememberedPeonObservation::peonId).distinct().count();
        double exploredPercentage = world.getCells().isEmpty() ? 100 : peon.getMentalMap().size() * 100.0 / world.getCells().size();
        Map<StrategyType, Long> assignments = new EnumMap<>(StrategyType.class);
        allies.stream().filter(ally -> !ally.getId().equals(peon.getId())).map(Peon::getLongTermStrategy).filter(java.util.Objects::nonNull).filter(strategy -> strategy.getStatus() == StrategyStatus.EN_COURS).map(LongTermStrategy::getType).forEach(type -> assignments.merge(type, 1L, Long::sum));
        boolean ownsHouse = houses.stream().anyMatch(house -> house.isOwner(peon.getId()));
        return new StrategicSituation(allies.size(), houses, granaries, fields, storedWheat, targetFields, safetyStock, knownEnemies, exploredPercentage, assignments, ownsHouse);
    }

    private StrategyType selectStrategy(Peon peon, StrategicSituation situation, int currentRound) {
        return this.strategyScores(peon, situation, currentRound).entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(StrategyType.EXPLORER_LE_MONDE);
    }

    private Map<StrategyType, Double> strategyScores(Peon peon, StrategicSituation situation, int currentRound) {
        Map<StrategyType, Double> scores = new EnumMap<>(StrategyType.class);
        double reserveDeficit = Math.max(0, situation.safetyStock() - situation.storedWheat()) * 60.0 / Math.max(1, situation.safetyStock());
        double foodNeed = (situation.granaries().isEmpty() ? 35 : 0) + Math.max(0, situation.targetFields() - situation.fields().size()) * 12.0 + reserveDeficit;
        scores.put(StrategyType.AUTONOMIE_ALIMENTAIRE, foodNeed + peon.getPersonality().getIngenuity() * 0.45 + peon.getPersonality().getSolidarity() * 0.4 + this.pastValue(peon, StrategyType.AUTONOMIE_ALIMENTAIRE));
        double homeNeed = (situation.ownsHouse() ? 5 : 65) + (situation.houses().isEmpty() ? 20 : 0) + (situation.granaries().isEmpty() ? 10 : 0);
        scores.put(StrategyType.CONSTRUIRE_UN_FOYER, homeNeed + peon.getPersonality().getIngenuity() * 0.45 + peon.getPersonality().getPrudence() * 0.35 + this.pastValue(peon, StrategyType.CONSTRUIRE_UN_FOYER));
        scores.put(StrategyType.EXPLORER_LE_MONDE, Math.max(0, 100 - situation.exploredPercentage()) * 0.65 + peon.getPersonality().getCuriosity() * 0.75 + this.pastValue(peon, StrategyType.EXPLORER_LE_MONDE));
        scores.put(StrategyType.PROTEGER_L_EQUIPE, 15 + situation.knownEnemies() * 22.0 + peon.getPersonality().getAggressiveness() * 0.75 + peon.getPersonality().getPrudence() * 0.3 + this.pastValue(peon, StrategyType.PROTEGER_L_EQUIPE));
        scores.replaceAll((type, score) -> score - this.assignmentPenalty(type, situation));
        return scores;
    }

    private double assignmentPenalty(StrategyType type, StrategicSituation situation) {
        int desired = switch (type) {
            case AUTONOMIE_ALIMENTAIRE -> Math.min(3, Math.max(1, (int) Math.ceil(situation.aliveAllies() / 3.0)));
            case CONSTRUIRE_UN_FOYER -> situation.houses().isEmpty() ? 2 : 1;
            case EXPLORER_LE_MONDE -> 1;
            case PROTEGER_L_EQUIPE -> situation.knownEnemies() > 0 ? 2 : 1;
        };
        return Math.max(0, situation.assignments().getOrDefault(type, 0L) - desired + 1) * 70.0;
    }

    private double pastValue(Peon peon, StrategyType type) {
        List<ActionType> relevant = switch (type) {
            case AUTONOMIE_ALIMENTAIRE -> List.of(ActionType.CONSTRUIRE_CHAMP, ActionType.CONSTRUIRE_GRENIER, ActionType.RECOLTER, ActionType.MANGER_BLE);
            case CONSTRUIRE_UN_FOYER -> List.of(ActionType.COUPER_DU_BOIS, ActionType.CONSTRUIRE_MAISON, ActionType.CONSTRUIRE_GRENIER);
            case EXPLORER_LE_MONDE -> List.of(ActionType.VOIR, ActionType.SE_DEPLACER);
            case PROTEGER_L_EQUIPE -> List.of(ActionType.ATTAQUER, ActionType.COMMUNIQUER);
        };
        return peon.getActionHistory().stream().filter(experience -> relevant.contains(experience.actionType())).mapToDouble(ActionExperience::reward).average().orElse(0) * 0.25;
    }

    private void updateProgress(LongTermStrategy strategy, StrategicSituation situation, Peon peon, GameDefaultsProperties defaults) {
        strategy.setStatus(StrategyStatus.EN_COURS);
        switch (strategy.getType()) {
            case AUTONOMIE_ALIMENTAIRE -> this.updateFoodStrategy(strategy, situation, peon, defaults);
            case CONSTRUIRE_UN_FOYER -> this.updateHomeStrategy(strategy, situation, peon, defaults);
            case EXPLORER_LE_MONDE -> this.updateExploreStrategy(strategy, situation, peon);
            case PROTEGER_L_EQUIPE -> this.updateProtectStrategy(strategy, situation, peon);
        }
    }

    private void updateFoodStrategy(LongTermStrategy strategy, StrategicSituation situation, Peon peon, GameDefaultsProperties defaults) {
        House house = situation.houses().stream().filter(candidate -> candidate.isOwner(peon.getId())).min(Comparator.comparingInt(candidate -> candidate.getPosition().distanceTo(peon.getPosition()))).orElse(null);
        Granary granary = house == null ? null : situation.granaries().stream().filter(candidate -> candidate.getPosition().distanceTo(house.getPosition()) >= 1 && candidate.getPosition().distanceTo(house.getPosition()) <= 2).min(Comparator.comparingInt(candidate -> candidate.getPosition().distanceTo(peon.getPosition()))).orElse(null);
        List<WheatField> fields = granary == null ? List.of() : situation.fields().stream().filter(field -> granary.getId().equals(field.getGranaryId())).toList();
        int storedWheat = granary == null ? 0 : granary.getWheatQuantity();
        strategy.setTargetHouseId(house == null ? null : house.getId());
        strategy.setTargetGranaryId(granary == null ? null : granary.getId());
        strategy.setTargetArea(granary != null ? granary.getPosition() : house != null ? house.getPosition() : peon.getPosition());
        Map<String, Integer> objectives = new LinkedHashMap<>();
        objectives.put("maisons", house == null ? 0 : 1); objectives.put("maisonsCible", 1);
        objectives.put("greniers", granary == null ? 0 : 1); objectives.put("greniersCible", 1);
        objectives.put("champs", fields.size()); objectives.put("champsCible", situation.targetFields());
        objectives.put("bleStocke", storedWheat); objectives.put("reserveCible", situation.safetyStock());
        strategy.setObjectives(objectives);
        if (house == null) { strategy.setPhase(StrategicPhase.ETABLIR_UN_FOYER); strategy.setTacticalObjective("Établir une maison alliée"); strategy.setNextPriority(peon.itemCount(org.infospray.peonsimulator.domain.model.ItemType.WOOD) >= defaults.getWoodRequiredForHouse() ? "Trouver une plaine pour construire la maison" : "Récolter du bois pour construire une maison"); strategy.setProgress(5); return; }
        if (granary == null) { strategy.setPhase(StrategicPhase.CONSTRUIRE_UN_GRENIER); strategy.setTacticalObjective("Construire un grenier près de la maison"); strategy.setNextPriority(peon.itemCount(org.infospray.peonsimulator.domain.model.ItemType.WOOD) >= defaults.getWoodRequiredForGranary() ? "Trouver une plaine à 1 ou 2 cases de la maison" : "Récolter du bois pour le grenier"); strategy.setProgress(20); return; }
        if (fields.isEmpty()) { strategy.setPhase(StrategicPhase.CREER_UN_PREMIER_CHAMP); strategy.setTacticalObjective("Créer le premier champ de l'exploitation"); strategy.setNextPriority("Construire un champ à 4 cases maximum du grenier"); strategy.setProgress(35); return; }
        if (fields.size() < situation.targetFields()) { strategy.setPhase(StrategicPhase.ETENDRE_L_EXPLOITATION); strategy.setTacticalObjective("Atteindre " + situation.targetFields() + " champs autour du grenier"); strategy.setNextPriority("Construire encore " + (situation.targetFields() - fields.size()) + " champ(s)"); strategy.setProgress(40 + 30.0 * fields.size() / situation.targetFields()); return; }
        if (storedWheat < situation.safetyStock()) { strategy.setPhase(StrategicPhase.REMPLIR_LA_RESERVE); strategy.setTacticalObjective("Constituer une réserve de " + situation.safetyStock() + " blés"); strategy.setNextPriority("Récolter les champs mûrs"); strategy.setProgress(70 + 25.0 * storedWheat / Math.max(1, situation.safetyStock())); return; }
        strategy.setPhase(StrategicPhase.MAINTENIR_L_AUTONOMIE); strategy.setTacticalObjective("Maintenir la réserve alimentaire de l'équipe"); strategy.setNextPriority("Surveiller le stock et la croissance de l'équipe"); strategy.setProgress(100); strategy.setStatus(StrategyStatus.ACCOMPLIE);
    }

    private void updateHomeStrategy(LongTermStrategy strategy, StrategicSituation situation, Peon peon, GameDefaultsProperties defaults) {
        boolean ownsHouse = situation.houses().stream().anyMatch(house -> house.isOwner(peon.getId()));
        strategy.setObjectives(Map.of("maisons", situation.houses().size(), "maisonsCible", 1, "bois", peon.itemCount(org.infospray.peonsimulator.domain.model.ItemType.WOOD), "boisMaison", defaults.getWoodRequiredForHouse()));
        if (ownsHouse) { strategy.setPhase(StrategicPhase.DEVELOPPER_LE_VILLAGE); strategy.setTacticalObjective("Développer et partager le foyer"); strategy.setNextPriority("Inviter des alliés et soutenir les constructions collectives"); strategy.setProgress(100); strategy.setStatus(StrategyStatus.ACCOMPLIE); return; }
        if (peon.itemCount(org.infospray.peonsimulator.domain.model.ItemType.WOOD) < defaults.getWoodRequiredForHouse()) { strategy.setPhase(StrategicPhase.RASSEMBLER_DU_BOIS); strategy.setTacticalObjective("Rassembler " + defaults.getWoodRequiredForHouse() + " bois"); strategy.setNextPriority("Rejoindre un arbre ou récupérer un fagot"); strategy.setProgress(peon.itemCount(org.infospray.peonsimulator.domain.model.ItemType.WOOD) * 70.0 / defaults.getWoodRequiredForHouse()); return; }
        strategy.setPhase(StrategicPhase.CONSTRUIRE_L_ABRI); strategy.setTacticalObjective("Construire une maison"); strategy.setNextPriority("Trouver une plaine libre"); strategy.setProgress(75);
    }

    private void updateExploreStrategy(LongTermStrategy strategy, StrategicSituation situation, Peon peon) {
        strategy.setObjectives(Map.of("exploration", (int) Math.round(situation.exploredPercentage()), "explorationCible", 80));
        strategy.setPhase(situation.exploredPercentage() < 40 ? StrategicPhase.REJOINDRE_LA_FRONTIERE : StrategicPhase.CARTOGRAPHIER_LA_REGION);
        strategy.setTacticalObjective("Cartographier les zones inconnues");
        strategy.setNextPriority("Se déplacer vers une case connue non visitée puis observer");
        strategy.setTargetArea(peon.getPosition());
        strategy.setProgress(Math.min(100, situation.exploredPercentage() / 80.0 * 100));
        if (situation.exploredPercentage() >= 80) { strategy.setStatus(StrategyStatus.ACCOMPLIE); }
    }

    private void updateProtectStrategy(LongTermStrategy strategy, StrategicSituation situation, Peon peon) {
        strategy.setObjectives(Map.of("menacesConnues", (int) situation.knownEnemies(), "alliesVivants", situation.aliveAllies()));
        strategy.setPhase(situation.knownEnemies() > 0 ? StrategicPhase.REJOINDRE_LA_MENACE : StrategicPhase.PATROUILLER);
        strategy.setTacticalObjective(situation.knownEnemies() > 0 ? "Neutraliser ou éloigner les menaces connues" : "Patrouiller autour du territoire allié");
        strategy.setNextPriority(situation.knownEnemies() > 0 ? "Rejoindre prudemment l'ennemi le plus proche" : "Observer et rester proche des alliés");
        strategy.setTargetArea(peon.getPosition());
        strategy.setProgress(situation.knownEnemies() == 0 ? 100 : Math.max(10, 70 - situation.knownEnemies() * 10));
        if (situation.knownEnemies() == 0) { strategy.setStatus(StrategyStatus.ACCOMPLIE); }
    }

    private record StrategicSituation(int aliveAllies, List<House> houses, List<Granary> granaries, List<WheatField> fields, int storedWheat, int targetFields, int safetyStock, long knownEnemies, double exploredPercentage, Map<StrategyType, Long> assignments, boolean ownsHouse) {
    }
}
