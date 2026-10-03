package org.infospray.peonsimulator.application.port.secondary;

import org.infospray.peonsimulator.application.dto.PeonDecisionContext;
import org.infospray.peonsimulator.application.dto.PeonDecision;

public interface PeonDecisionProvider {
    PeonDecision decide(PeonDecisionContext context);
}
