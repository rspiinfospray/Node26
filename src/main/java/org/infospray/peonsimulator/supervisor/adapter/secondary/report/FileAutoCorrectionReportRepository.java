package org.infospray.peonsimulator.supervisor.adapter.secondary.report;

import org.infospray.peonsimulator.supervisor.application.port.secondary.AutoCorrectionReportRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;

public class FileAutoCorrectionReportRepository implements AutoCorrectionReportRepository {
    private final Path reportDirectory;
    private final Path stateDirectory;

    public FileAutoCorrectionReportRepository(Path reportDirectory) {
        this.reportDirectory = reportDirectory;
        this.stateDirectory = reportDirectory.resolve("autofix-state");
    }

    @Override
    public Optional<Path> nextPending() {
        if (!Files.isDirectory(this.reportDirectory)) { return Optional.empty(); }
        try (var paths = Files.list(this.reportDirectory)) {
            return paths.filter(path -> path.getFileName().toString().endsWith(".md")).filter(path -> !this.hasMarker(path, ".completed") && !this.hasMarker(path, ".failed")).max(Comparator.comparingLong(this::lastModified));
        } catch (IOException exception) { throw new IllegalStateException("Impossible de parcourir les rapports", exception); }
    }

    @Override
    public int completedCycles() {
        if (!Files.isDirectory(this.stateDirectory)) { return 0; }
        try (var paths = Files.list(this.stateDirectory)) { return (int) paths.filter(path -> path.getFileName().toString().endsWith(".completed")).count(); } catch (IOException exception) { throw new IllegalStateException("Impossible de compter les cycles terminés", exception); }
    }

    @Override
    public void markCompleted(Path report) { this.writeMarker(report, ".completed", "Terminé le " + Instant.now()); }

    @Override
    public void markFailed(Path report, String error) { this.writeMarker(report, ".failed", "Échec le " + Instant.now() + System.lineSeparator() + String.valueOf(error)); }

    private boolean hasMarker(Path report, String suffix) { return Files.exists(this.marker(report, suffix)); }

    private Path marker(Path report, String suffix) { return this.stateDirectory.resolve(report.getFileName().toString() + suffix); }

    private void writeMarker(Path report, String suffix, String content) {
        try { Files.createDirectories(this.stateDirectory); Files.writeString(this.marker(report, suffix), content, StandardCharsets.UTF_8); } catch (IOException exception) { throw new IllegalStateException("Impossible d'enregistrer l'état du cycle", exception); }
    }

    private long lastModified(Path path) {
        try { return Files.getLastModifiedTime(path).toMillis(); } catch (IOException exception) { throw new IllegalStateException("Impossible de lire la date du rapport", exception); }
    }
}
