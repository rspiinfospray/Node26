package org.infospray.peonsimulator.supervisor.application.port.secondary;

import java.nio.file.Path;

public interface CodexCorrectionAgent {
    void apply(Path report, int cycleNumber);
}
