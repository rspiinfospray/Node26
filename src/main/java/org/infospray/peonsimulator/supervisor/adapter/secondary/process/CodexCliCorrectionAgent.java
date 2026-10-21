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
    private static final int MAXIMUM_ATTEMPTS = 2;
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
        String escapedCodexCommand = this.codexCommand().replace("'", "''");
        String script = "$prompt=[Console]::In.ReadToEnd(); $prompt | & '" + escapedCodexCommand + "' exec --sandbox danger-full-access --ephemeral --output-last-message '" + escapedOutput + "' -; exit $LASTEXITCODE";
        for (int attempt = 1; attempt <= MAXIMUM_ATTEMPTS; attempt++) {
            Path transcript = this.outputDirectory.resolve("codex-cycle-" + cycleNumber + "-attempt-" + attempt + ".log");
            String processOutput;
            try {
                processOutput = ProcessExecution.run(this.projectDirectory, Duration.ofMinutes(45), prompt, List.of("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", script));
                this.writeTranscript(transcript, processOutput);
            } catch (RuntimeException exception) {
                this.writeTranscript(transcript, exception.getMessage());
                throw new IllegalStateException("Codex a échoué. Sortie complète: " + transcript, exception);
            }
            if (this.hasResult(output)) { return this.readResult(output); }
            if (attempt == MAXIMUM_ATTEMPTS) { throw new IllegalStateException("Codex n'a créé aucun fichier de résultat après " + MAXIMUM_ATTEMPTS + " tentatives. Dernier journal: " + transcript + this.outputSummary(processOutput)); }
        }
        throw new IllegalStateException("Codex n'a produit aucun résultat");
    }

    private CodexCorrectionResult readResult(Path output) {
        try {
            return new CodexCorrectionResult(Files.readString(output, StandardCharsets.UTF_8));
        } catch (IOException exception) { throw new IllegalStateException("Impossible de lire le résultat Codex", exception); }
    }

    private boolean hasResult(Path output) {
        try { return Files.isRegularFile(output) && Files.size(output) > 0; } catch (IOException exception) { throw new IllegalStateException("Impossible de vérifier le résultat Codex", exception); }
    }

    private void writeTranscript(Path transcript, String content) {
        try { Files.writeString(transcript, String.valueOf(content), StandardCharsets.UTF_8); } catch (IOException exception) { throw new IllegalStateException("Impossible d'enregistrer la sortie Codex dans " + transcript, exception); }
    }

    private String outputSummary(String output) {
        if (output == null || output.isBlank()) { return ". La commande Codex n'a produit aucune sortie standard."; }
        String normalized = output.strip();
        int start = Math.max(0, normalized.length() - 1000);
        return ". Fin de la sortie Codex: " + normalized.substring(start);
    }

    private String codexCommand() {
        String configuredCommand = System.getenv("AUTOFIX_CODEX_COMMAND");
        return configuredCommand == null || configuredCommand.isBlank() ? "codex.cmd" : configuredCommand;
    }

    private String buildPrompt(Path report, int cycleNumber) {
        try {
            String template = Files.readString(this.promptTemplate, StandardCharsets.UTF_8);
            return template.replace("{{CYCLE_NUMBER}}", String.valueOf(cycleNumber)).replace("{{REPORT_PATH}}", report.toAbsolutePath().normalize().toString()).replace("{{PROJECT_PATH}}", this.projectDirectory.toString());
        } catch (IOException exception) { throw new IllegalStateException("Impossible de lire le prompt Codex", exception); }
    }
}
