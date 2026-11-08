package org.infospray.peonsimulator.application.port.secondary;

/**
 * Compiled candidate with an exact, stable identifier.
 * Implementations must be deterministic, thread-safe and must not share mutable world state.
 */
public interface PeonDecisionCandidate extends PeonDecisionProvider {
    String candidateId();
}
