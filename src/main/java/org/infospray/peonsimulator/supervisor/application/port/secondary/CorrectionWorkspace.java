package org.infospray.peonsimulator.supervisor.application.port.secondary;

public interface CorrectionWorkspace {
    void verifyBranch();
    void runTests();
    void deploy();
}
