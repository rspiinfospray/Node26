package org.infospray.peonsimulator.supervisor.application.port.secondary;

import org.infospray.peonsimulator.supervisor.application.CodexCorrectionResult;

import java.nio.file.Path;

public interface CodexCorrectionAgent {
    CodexCorrectionResult apply(Path report, int cycleNumber);
}
