package org.infospray.peonsimulator.supervisor.adapter.secondary.process;

import org.infospray.peonsimulator.supervisor.application.port.secondary.CorrectionWorkspace;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public class LocalCorrectionWorkspace implements CorrectionWorkspace {
    private final Path projectDirectory;
    private final String requiredBranch;

    public LocalCorrectionWorkspace(Path projectDirectory, String requiredBranch) {
        this.projectDirectory = projectDirectory;
        this.requiredBranch = requiredBranch;
    }

    @Override
    public void verifyBranch() {
        String branch = ProcessExecution.run(this.projectDirectory, Duration.ofSeconds(20), null, List.of("git", "branch", "--show-current"));
        if (!this.requiredBranch.equals(branch)) { throw new IllegalStateException("Branche interdite: " + branch + ". La branche exigée est " + this.requiredBranch); }
    }

    @Override
    public void runTests() { ProcessExecution.run(this.projectDirectory, Duration.ofMinutes(15), null, List.of("docker", "compose", "build", "backend")); }

    @Override
    public void deploy() { ProcessExecution.run(this.projectDirectory, Duration.ofMinutes(10), null, List.of("docker", "compose", "up", "-d", "backend")); }
}
