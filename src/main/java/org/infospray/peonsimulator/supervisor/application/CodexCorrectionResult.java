package org.infospray.peonsimulator.supervisor.application;

public record CodexCorrectionResult(String response) {
    public CodexCorrectionResult {
        if (response == null || response.isBlank()) { throw new IllegalArgumentException("La réponse Codex est vide"); }
    }
}
