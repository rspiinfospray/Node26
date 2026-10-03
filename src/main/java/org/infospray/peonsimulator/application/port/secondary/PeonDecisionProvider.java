package org.infospray.peonsimulator.application.port.secondary;

import org.infospray.peonsimulator.application.dto.PeonDecisionContext;
import org.infospray.peonsimulator.domain.action.PeonAction;

public interface PeonDecisionProvider {
    PeonAction decide(PeonDecisionContext context);
}
