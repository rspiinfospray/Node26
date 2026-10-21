package org.infospray.peonsimulator.supervisor.application.port.secondary;

import org.infospray.peonsimulator.supervisor.application.WorkspaceSnapshot;

public interface CorrectionWorkspace {
    WorkspaceSnapshot snapshot();
    void runTests();
    void deploy();
}
