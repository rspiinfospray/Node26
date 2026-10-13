package org.infospray.peonsimulator.supervisor.adapter.secondary.process;

import org.infospray.peonsimulator.supervisor.application.port.secondary.CorrectionWorkspace;
import org.infospray.peonsimulator.supervisor.application.WorkspaceSnapshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public WorkspaceSnapshot snapshot() {
        String status = ProcessExecution.run(this.projectDirectory, Duration.ofSeconds(20), null, List.of("git", "status", "--porcelain", "--untracked-files=all"));
        Map<String, String> files = new LinkedHashMap<>();
        status.lines().filter(line -> line.length() > 3).forEach(line -> {
            String path = this.normalizedPath(line.substring(3));
            files.put(path, this.fingerprint(path));
        });
        return new WorkspaceSnapshot(files);
    }

    @Override
    public void runTests() { ProcessExecution.run(this.projectDirectory, Duration.ofMinutes(15), null, List.of("docker", "compose", "build", "backend")); }

    @Override
    public void deploy() { ProcessExecution.run(this.projectDirectory, Duration.ofMinutes(10), null, List.of("docker", "compose", "up", "-d", "backend")); }

    private String normalizedPath(String statusPath) {
        int renameSeparator = statusPath.indexOf(" -> ");
        String path = renameSeparator >= 0 ? statusPath.substring(renameSeparator + 4) : statusPath;
        return path.replace('\\', '/');
    }

    private String fingerprint(String relativePath) {
        Path path = this.projectDirectory.resolve(relativePath).normalize();
        if (!path.startsWith(this.projectDirectory) || !Files.isRegularFile(path)) { return "<absent>"; }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (IOException exception) { throw new IllegalStateException("Impossible de lire " + relativePath, exception); } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 indisponible", exception); }
    }
}
