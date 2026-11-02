package org.infospray.peonsimulator.application.dto;

import java.util.List;
import java.util.UUID;

public record PeonDeathContextResponse(UUID worldId, PeonIdentity peon, Window window, Death death, List<HealthChange> healthEvolution, List<Decision> decisions, Object lifetimeSummary, Object fatalAttackContext, RelevantRules relevantRules) {
    public record PeonIdentity(UUID id, String name, UUID teamId) {
    }

    public record Window(long fromSequence, long toSequence, long deathSequence, int deathRound, int sequenceCount, int peonActionCount, List<Long> sequences) {
    }

    public record Death(String reason, UUID attackerPeonId, String attackerName, Object position) {
    }

    public record HealthChange(long sequence, int round, int before, int after, int amount, String cause) {
    }

    public record Decision(long sequence, int round, String action, String purpose, String reason, Double selectedScore, Object stateBefore, Object result, Object bestRejectedAlternative, Object longTermStrategy) {
    }

    public record RelevantRules(int movementHealthCost, int hungerHealthLossPerTurn, int baseAttackDamage) {
    }
}
