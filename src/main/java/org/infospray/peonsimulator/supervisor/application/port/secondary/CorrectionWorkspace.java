package org.infospray.peonsimulator.supervisor.application.port.secondary;

import org.infospray.peonsimulator.supervisor.application.WorkspaceSnapshot;

public interface CorrectionWorkspace {
    void verifyBranch();
    WorkspaceSnapshot snapshot();
    void runTests();
    void deploy();
}
