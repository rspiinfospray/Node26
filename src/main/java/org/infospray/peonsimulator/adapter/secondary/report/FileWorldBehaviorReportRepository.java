package org.infospray.peonsimulator.adapter.secondary.report;

import org.infospray.peonsimulator.application.dto.WorldBehaviorReportStatus;
import org.infospray.peonsimulator.application.port.secondary.WorldBehaviorReportRepository;
import org.infospray.peonsimulator.configuration.OpenAiReportProperties;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class FileWorldBehaviorReportRepository implements WorldBehaviorReportRepository {
    private final Path directory;
    private final ConcurrentHashMap<UUID, WorldBehaviorReportStatus> transientStatuses = new ConcurrentHashMap<>();

    public FileWorldBehaviorReportRepository(OpenAiReportProperties properties) { this.directory = Path.of(properties.getDirectory()).toAbsolutePath().normalize(); }

    @Override
    public WorldBehaviorReportStatus status(UUID worldId) {
        Path report = this.findReport(worldId).orElse(null);
        if (report != null) {
            try { return new WorldBehaviorReportStatus(worldId, WorldBehaviorReportStatus.Status.GENERATED, report.getFileName().toString(), Files.getLastModifiedTime(report).toInstant(), null); } catch (IOException exception) { throw new IllegalStateException("Impossible de lire l'état du rapport", exception); }
        }
        return this.transientStatuses.getOrDefault(worldId, new WorldBehaviorReportStatus(worldId, WorldBehaviorReportStatus.Status.NOT_GENERATED, null, null, null));
    }

    @Override
    public void markGenerating(UUID worldId) { this.transientStatuses.put(worldId, new WorldBehaviorReportStatus(worldId, WorldBehaviorReportStatus.Status.GENERATING, null, null, null)); }

    @Override
    public void save(UUID worldId, String worldName, String markdown) {
        try {
            Files.createDirectories(this.directory);
            String safeName = worldName.replaceAll("[^a-zA-Z0-9._-]", "-");
            Path target = this.directory.resolve("world-" + safeName + "-" + worldId + ".md");
            Files.writeString(target, markdown, StandardCharsets.UTF_8);
            this.transientStatuses.remove(worldId);
        } catch (IOException exception) { throw new IllegalStateException("Impossible d'écrire le rapport Markdown", exception); }
    }

    @Override
    public void markFailed(UUID worldId, String error) { this.transientStatuses.put(worldId, new WorldBehaviorReportStatus(worldId, WorldBehaviorReportStatus.Status.FAILED, null, null, error)); }

    @Override
    public void queueForSupervision(UUID worldId) {
        Path report = this.findReport(worldId).orElseThrow(() -> new IllegalStateException("Aucun rapport n'est disponible pour ce monde"));
        Path stateDirectory = this.directory.resolve("autofix-state");
        try {
            Files.deleteIfExists(stateDirectory.resolve(report.getFileName().toString() + ".completed"));
            Files.deleteIfExists(stateDirectory.resolve(report.getFileName().toString() + ".failed"));
            Files.setLastModifiedTime(report, FileTime.from(Instant.now()));
        } catch (IOException exception) { throw new IllegalStateException("Impossible de placer le rapport dans la file du superviseur", exception); }
    }

    @Override
    public Optional<String> read(UUID worldId) {
        return this.findReport(worldId).map(path -> {
            try { return Files.readString(path, StandardCharsets.UTF_8); } catch (IOException exception) { throw new IllegalStateException("Impossible de lire le rapport Markdown", exception); }
        });
    }

    private Optional<Path> findReport(UUID worldId) {
        if (!Files.isDirectory(this.directory)) { return Optional.empty(); }
        try (var paths = Files.list(this.directory)) { return paths.filter(path -> path.getFileName().toString().endsWith("-" + worldId + ".md")).findFirst(); } catch (IOException exception) { throw new IllegalStateException("Impossible de parcourir les rapports", exception); }
    }
}
