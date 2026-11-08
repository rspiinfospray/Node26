package org.infospray.peonsimulator.adapter.secondary.decision;

import org.infospray.peonsimulator.application.port.secondary.PeonDecisionCandidate;
import org.infospray.peonsimulator.application.port.secondary.PeonDecisionProvider;
import org.infospray.peonsimulator.application.port.secondary.PeonDecisionResolver;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
public final class PeonDecisionCandidateRegistry implements PeonDecisionResolver {
    private final Map<String, PeonDecisionProvider> candidates;

    public PeonDecisionCandidateRegistry(List<PeonDecisionCandidate> candidates) {
        Map<String, PeonDecisionProvider> registered = new TreeMap<>();
        for (PeonDecisionCandidate candidate : candidates) {
            String id = candidate.candidateId();
            if (id == null || id.isBlank() || !id.equals(id.trim())) { throw new IllegalStateException("Invalid decision candidate identifier: " + id); }
            if (registered.putIfAbsent(id, candidate) != null) { throw new IllegalStateException("Duplicate decision candidate identifier: " + id); }
        }
        if (!registered.containsKey("baseline")) { throw new IllegalStateException("Missing baseline decision candidate"); }
        this.candidates = Collections.unmodifiableMap(registered);
    }

    @Override
    public PeonDecisionProvider resolve(String candidateId) {
        PeonDecisionProvider candidate = this.candidates.get(candidateId);
        if (candidate == null) { throw new IllegalArgumentException("Unknown decisionCandidateId: " + candidateId + "; available: " + this.candidates.keySet()); }
        return candidate;
    }
}
