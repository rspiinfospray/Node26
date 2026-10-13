package org.infospray.peonsimulator.supervisor.application.port.secondary;

import java.nio.file.Path;
import java.util.Optional;

public interface AutoCorrectionReportRepository {
    Optional<Path> nextPending();
    int completedCycles();
    void markCompleted(Path report);
    void markFailed(Path report, String error);
}
