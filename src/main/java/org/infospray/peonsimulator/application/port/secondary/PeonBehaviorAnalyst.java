package org.infospray.peonsimulator.application.port.secondary;

import org.infospray.peonsimulator.application.dto.PeonBehaviorAnalysisRequest;

public interface PeonBehaviorAnalyst {
    String analyze(PeonBehaviorAnalysisRequest request);
}
