package org.infospray.peonsimulator.supervisor.adapter.secondary.process;

import org.infospray.peonsimulator.supervisor.application.CodexCorrectionResult;
import org.infospray.peonsimulator.supervisor.application.port.secondary.CodexCorrectionAgent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public class CodexCliCorrectionAgent implements CodexCorrectionAgent {
    private final Path projectDirectory;
    private final Path promptTemplate;
    private final Path outputDirectory;

    public CodexCliCorrectionAgent(Path projectDirectory, Path promptTemplate, Path outputDirectory) {
        this.projectDirectory = projectDirectory;
        this.promptTemplate = promptTemplate;
        this.outputDirectory = outputDirectory;
    }

    @Override
    public CodexCorrectionResult apply(Path report, int cycleNumber) {
        String prompt = this.buildPrompt(report, cycleNumber);
        Path output = this.outputDirectory.resolve("codex-cycle-" + cycleNumber + ".md");
        try { Files.createDirectories(this.outputDirectory); Files.deleteIfExists(output); } catch (IOException exception) { throw new IllegalStateException("Impossible de préparer le dossier de sortie Codex", exception); }
        String escapedOutput = output.toString().replace("'", "''");
        String script = "$prompt=[Console]::In.ReadToEnd(); $prompt | & codex.cmd exec --sandbox danger-full-access --ephemeral --output-last-message '" + escapedOutput + "' -; exit $LASTEXITCODE";
        ProcessExecution.run(this.projectDirectory, Duration.ofMinutes(45), prompt, List.of("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", script));
        try {
            if (!Files.isRegularFile(output)) { throw new IllegalStateException("Codex n'a créé aucun fichier de résultat"); }
            return new CodexCorrectionResult(Files.readString(output, StandardCharsets.UTF_8));
        } catch (IOException exception) { throw new IllegalStateException("Impossible de lire le résultat Codex", exception); }
    }

    private String buildPrompt(Path report, int cycleNumber) {
        try {
            String template = Files.readString(this.promptTemplate, StandardCharsets.UTF_8);
            return template.replace("{{CYCLE_NUMBER}}", String.valueOf(cycleNumber)).replace("{{REPORT_PATH}}", report.toAbsolutePath().normalize().toString()).replace("{{PROJECT_PATH}}", this.projectDirectory.toString());
        } catch (IOException exception) { throw new IllegalStateException("Impossible de lire le prompt Codex", exception); }
    }
}
