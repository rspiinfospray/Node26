package org.infospray.peonsimulator.application.command;

import org.infospray.peonsimulator.application.dto.PeonDeathContextResponse;
import org.infospray.peonsimulator.application.port.secondary.EventRepository;
import org.infospray.peonsimulator.application.port.secondary.WorldRepository;
import org.infospray.peonsimulator.configuration.GameDefaultsProperties;
import org.infospray.peonsimulator.domain.event.SimulationEvent;
import org.infospray.peonsimulator.domain.model.Grave;
import org.infospray.peonsimulator.domain.model.Peon;
import org.infospray.peonsimulator.domain.model.World;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.UUID;

@Component
public class GetPeonDeathContextCommand {
    private static final int MAXIMUM_SEQUENCE_LIMIT = 100;
    private final WorldRepository worldRepository;
    private final EventRepository eventRepository;
    private final GameDefaultsProperties defaults;

    public GetPeonDeathContextCommand(WorldRepository worldRepository, EventRepository eventRepository, GameDefaultsProperties defaults) {
        this.worldRepository = worldRepository;
        this.eventRepository = eventRepository;
        this.defaults = defaults;
    }

    public PeonDeathContextResponse execute(UUID worldId, UUID peonId, int sequenceLimit) {
        if (sequenceLimit < 1 || sequenceLimit > MAXIMUM_SEQUENCE_LIMIT) { throw new IllegalArgumentException("sequenceLimit doit être compris entre 1 et " + MAXIMUM_SEQUENCE_LIMIT); }
        World world = this.worldRepository.findById(worldId).orElseThrow(() -> new IllegalArgumentException("Monde introuvable: " + worldId));
        Peon peon = world.getPeons().get(peonId);
        if (peon == null) { throw new IllegalArgumentException("Peon introuvable: " + peonId); }
        if (peon.isAlive()) { throw new IllegalArgumentException("Le peon " + peonId + " est vivant"); }
        Grave grave = world.getGraves().get(peonId);
        if (grave == null) { throw new IllegalArgumentException("Séquence de décès introuvable pour le peon " + peonId); }
        List<SimulationEvent> worldEvents = this.eventRepository.findByWorldId(worldId);
        List<Long> selectedSequences = worldEvents.stream().filter(event -> event.sequenceNumber() <= grave.deathSequence() && this.isRelatedToPeon(event, peonId)).map(SimulationEvent::sequenceNumber).distinct().sorted(Comparator.reverseOrder()).limit(sequenceLimit).sorted().toList();
        if (selectedSequences.isEmpty()) { throw new IllegalArgumentException("Aucun événement trouvé pour le peon " + peonId); }
        long fromSequence = selectedSequences.getFirst();
        List<SimulationEvent> windowEvents = worldEvents.stream().filter(event -> selectedSequences.contains(event.sequenceNumber())).sorted(Comparator.comparingLong(SimulationEvent::sequenceNumber).thenComparingInt(SimulationEvent::eventIndex)).toList();
        SimulationEvent deathEvent = windowEvents.stream().filter(event -> "PEON_DIED".equals(event.eventType()) && peonId.equals(event.peonId()) && event.sequenceNumber() == grave.deathSequence()).findFirst().orElseThrow(() -> new IllegalArgumentException("Événement de décès introuvable pour le peon " + peonId));
        List<PeonDeathContextResponse.HealthChange> healthChanges = windowEvents.stream().filter(event -> this.affectsHealth(event, peonId)).map(this::toHealthChange).toList();
        List<PeonDeathContextResponse.Decision> decisions = windowEvents.stream().filter(event -> "PEON_DECISION_MADE".equals(event.eventType()) && peonId.equals(event.peonId())).map(event -> this.toDecision(event, worldEvents, healthChanges)).toList();
        UUID attackerId = this.uuidValue(deathEvent.payload().get("attackerPeonId"));
        String attackerName = attackerId == null || world.getPeons().get(attackerId) == null ? null : world.getPeons().get(attackerId).getFirstName();
        PeonDeathContextResponse.PeonIdentity identity = new PeonDeathContextResponse.PeonIdentity(peon.getId(), peon.getFirstName(), peon.getTeamId());
        PeonDeathContextResponse.Window window = new PeonDeathContextResponse.Window(fromSequence, grave.deathSequence(), grave.deathSequence(), grave.deathRound(), selectedSequences.size(), decisions.size(), selectedSequences);
        PeonDeathContextResponse.Death death = new PeonDeathContextResponse.Death(this.stringValue(deathEvent.payload().get("reason")), attackerId, attackerName, deathEvent.payload().get("position"));
        PeonDeathContextResponse.RelevantRules rules = new PeonDeathContextResponse.RelevantRules(this.defaults.getMoveHealthCost(), world.getHungerHealthLossPerTurn(), this.defaults.getAttackDamage());
        Object lifetimeSummary = this.lifetimeSummary(worldEvents, peonId);
        World deathWorld = this.worldRepository.findAtSequence(worldId, grave.deathSequence()).orElse(world);
        Object fatalAttackContext = this.fatalAttackContext(deathWorld, windowEvents, deathEvent, peonId, attackerId);
        return new PeonDeathContextResponse(worldId, identity, window, death, healthChanges, decisions, lifetimeSummary, fatalAttackContext, rules);
    }

    private boolean affectsHealth(SimulationEvent event, UUID peonId) {
        if (!(event.payload().get("healthBefore") instanceof Number) || !(event.payload().get("healthAfter") instanceof Number)) { return false; }
        if (peonId.equals(event.peonId())) { return true; }
        return "PEON_ATTACKED".equals(event.eventType()) && peonId.equals(this.uuidValue(event.payload().get("targetPeonId")));
    }

    private boolean isRelatedToPeon(SimulationEvent event, UUID peonId) {
        if (peonId.equals(event.peonId())) { return true; }
        return peonId.equals(this.uuidValue(event.payload().get("targetPeonId")));
    }

    private PeonDeathContextResponse.HealthChange toHealthChange(SimulationEvent event) {
        int before = ((Number) event.payload().get("healthBefore")).intValue();
        int after = ((Number) event.payload().get("healthAfter")).intValue();
        return new PeonDeathContextResponse.HealthChange(event.sequenceNumber(), event.roundNumber(), before, after, Math.abs(after - before), this.healthCause(event.eventType()));
    }

    private PeonDeathContextResponse.Decision toDecision(SimulationEvent event, List<SimulationEvent> windowEvents, List<PeonDeathContextResponse.HealthChange> healthChanges) {
        List<PeonDeathContextResponse.HealthChange> changes = healthChanges.stream().filter(change -> change.sequence() == event.sequenceNumber()).toList();
        Integer healthBefore = changes.isEmpty() ? null : changes.getFirst().before();
        Integer healthAfter = changes.isEmpty() ? null : changes.getLast().after();
        SimulationEvent movement = windowEvents.stream().filter(candidate -> candidate.sequenceNumber() == event.sequenceNumber() && "PEON_MOVED".equals(candidate.eventType()) && event.peonId().equals(candidate.peonId())).findFirst().orElse(null);
        SimulationEvent previousMovement = windowEvents.stream().filter(candidate -> candidate.sequenceNumber() < event.sequenceNumber() && "PEON_MOVED".equals(candidate.eventType()) && event.peonId().equals(candidate.peonId())).max(Comparator.comparingLong(SimulationEvent::sequenceNumber)).orElse(null);
        Object positionBefore = movement != null ? movement.payload().get("from") : previousMovement == null ? null : previousMovement.payload().get("to");
        Map<String, Object> state = this.decisionState(event, healthBefore, positionBefore);
        Map<String, Object> result = new LinkedHashMap<>(); result.put("healthAfter", healthAfter); result.put("position", movement == null ? state.get("position") : movement.payload().get("to"));
        return new PeonDeathContextResponse.Decision(event.sequenceNumber(), event.roundNumber(), this.stringValue(event.payload().get("actionType")), this.stringValue(event.payload().get("purpose")), this.stringValue(event.payload().get("reason")), this.doubleValue(event.payload().get("selectedScore")), state, result, this.bestRejectedAlternative(event), event.payload().get("longTermStrategy"));
    }

    private Map<String, Object> decisionState(SimulationEvent event, Integer healthBefore, Object position) {
        Map<String, Object> state = new LinkedHashMap<>();
        String key = this.stringValue(event.payload().get("situationKey"));
        state.put("health", healthBefore == null && key != null ? this.numberFromKey(key, "PV_EXACTS_") : healthBefore);
        state.put("position", position);
        if (key != null) {
            state.put("foodInventory", this.numberFromKey(key, "NOURRITURE_")); state.put("woodInventory", this.numberFromKey(key, "BOIS_"));
            state.put("distanceToKnownFood", this.numberFromKey(key, "DIST_NOURRITURE_")); state.put("distanceToKnownWood", this.numberFromKey(key, "DIST_BOIS_"));
            state.put("sheltered", key.contains(":ABRITE")); state.put("foodKnown", !key.contains("NOURRITURE_INCONNUE")); state.put("sheepKnown", !key.contains("MOUTON_INCONNU")); state.put("enemyContext", key.contains("ENNEMI") ? "KNOWN_OR_PRESENT" : "NONE_IN_SITUATION");
        }
        return state;
    }

    private Object bestRejectedAlternative(SimulationEvent event) {
        Object candidates = event.payload().get("candidates");
        if (!(candidates instanceof List<?> list)) { return null; }
        Map<?, ?> selected = list.stream().filter(Map.class::isInstance).map(item -> (Map<?, ?>) item).filter(item -> !Boolean.TRUE.equals(item.get("admissible"))).max(Comparator.comparingDouble(item -> this.doubleValue(item.get("score")) == null ? -Double.MAX_VALUE : this.doubleValue(item.get("score")))).orElse(null);
        if (selected == null) { return null; }
        Map<String, Object> result = new LinkedHashMap<>(); result.put("action", selected.get("actionType")); result.put("destination", selected.get("destination")); result.put("targetPeonId", selected.get("targetPeonId")); result.put("score", selected.get("score")); result.put("exclusionReason", selected.get("exclusionReason")); return result;
    }

    private Object lifetimeSummary(List<SimulationEvent> events, UUID peonId) {
        List<SimulationEvent> related = events.stream().filter(event -> this.isRelatedToPeon(event, peonId)).toList();
        Map<String, Long> actions = related.stream().filter(event -> "PEON_DECISION_MADE".equals(event.eventType()) && peonId.equals(event.peonId())).map(event -> this.stringValue(event.payload().get("actionType"))).filter(java.util.Objects::nonNull).collect(Collectors.groupingBy(value -> value, LinkedHashMap::new, Collectors.counting()));
        Map<String, Integer> losses = new LinkedHashMap<>(); related.stream().filter(event -> this.affectsHealth(event, peonId)).forEach(event -> { int before = ((Number) event.payload().get("healthBefore")).intValue(); int after = ((Number) event.payload().get("healthAfter")).intValue(); if (after < before) losses.merge(this.healthCause(event.eventType()), before - after, Integer::sum); });
        int lowestHealth = related.stream().filter(event -> this.affectsHealth(event, peonId)).mapToInt(event -> ((Number) event.payload().get("healthAfter")).intValue()).min().orElse(0);
        return Map.of("turnsPlayed", actions.values().stream().mapToLong(Long::longValue).sum(), "actionsByType", actions, "healthLossByCause", losses, "failedActions", related.stream().filter(event -> "ACTION_REJECTED".equals(event.eventType())).count(), "meals", related.stream().filter(event -> List.of("PEON_ATE", "SHEEP_EATEN", "WHEAT_EATEN", "STORED_FOOD_EATEN").contains(event.eventType())).count(), "attacks", actions.getOrDefault("ATTAQUER", 0L), "communications", actions.getOrDefault("COMMUNIQUER", 0L), "lowestHealth", lowestHealth);
    }

    private Object fatalAttackContext(World world, List<SimulationEvent> events, SimulationEvent deathEvent, UUID victimId, UUID attackerId) {
        if (attackerId == null) { return null; }
        SimulationEvent attack = events.stream().filter(event -> "PEON_ATTACKED".equals(event.eventType()) && victimId.equals(this.uuidValue(event.payload().get("targetPeonId"))) && event.actionId().equals(deathEvent.actionId())).findFirst().orElse(null);
        Peon attacker = world.getPeons().get(attackerId); Peon victim = world.getPeons().get(victimId);
        boolean attackerKnown = victim != null && victim.getMentalMap().values().stream().flatMap(cell -> cell.getRememberedPeons().stream()).anyMatch(observation -> attackerId.equals(observation.peonId()));
        Map<String, Object> result = new LinkedHashMap<>(); result.put("attackerId", attackerId); result.put("attackerName", attacker == null ? null : attacker.getFirstName()); result.put("attackerHealth", attacker == null ? null : attacker.getHealthPoints()); result.put("attackerLevel", attacker == null ? null : attacker.getLevel()); result.put("attackerDamage", attack == null ? null : attack.payload().get("damage")); result.put("victimHealthBefore", attack == null ? null : attack.payload().get("healthBefore")); result.put("victimHealthAfter", attack == null ? null : attack.payload().get("healthAfter")); result.put("position", deathEvent.payload().get("position")); result.put("attackerKnownByVictim", attackerKnown); return result;
    }

    private Integer numberFromKey(String key, String prefix) { java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?:^|:)" + prefix + "(\\d+)").matcher(key); return matcher.find() ? Integer.valueOf(matcher.group(1)) : null; }

    private String healthCause(String eventType) {
        return switch (eventType) {
            case "PEON_MOVEMENT_COST_APPLIED" -> "MOVEMENT";
            case "PEON_HUNGER_APPLIED" -> "HUNGER";
            case "PEON_ATTACKED" -> "ATTACK";
            case "PEON_ATE", "SHEEP_EATEN", "WHEAT_EATEN", "STORED_FOOD_EATEN" -> "HEALING";
            case "PEON_LEVELED_UP", "PEON_LEVEL_UP_HEAL_APPLIED" -> "LEVEL_UP";
            default -> eventType;
        };
    }

    private String stringValue(Object value) { return value == null ? null : String.valueOf(value); }
    private Double doubleValue(Object value) { return value instanceof Number number ? number.doubleValue() : null; }
    private UUID uuidValue(Object value) {
        if (value instanceof UUID uuid) { return uuid; }
        if (value instanceof String text) { return UUID.fromString(text); }
        return null;
    }
}
