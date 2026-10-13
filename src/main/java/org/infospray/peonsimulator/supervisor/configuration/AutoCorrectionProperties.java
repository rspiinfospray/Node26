package org.infospray.peonsimulator.supervisor.configuration;

import java.nio.file.Path;
import java.time.Duration;

public record AutoCorrectionProperties(Path projectDirectory, Path reportDirectory, String requiredBranch, int maximumCycles, Duration pollingInterval, String apiBaseUrl) {
    public static AutoCorrectionProperties fromEnvironment() {
        Path projectDirectory = Path.of(value("AUTOFIX_PROJECT_DIRECTORY", ".")).toAbsolutePath().normalize();
        Path reportDirectory = projectDirectory.resolve(value("AUTOFIX_REPORT_DIRECTORY", "reports")).normalize();
        return new AutoCorrectionProperties(projectDirectory, reportDirectory, value("AUTOFIX_REQUIRED_BRANCH", "analyse"), integer("AUTOFIX_MAXIMUM_CYCLES", 3), Duration.ofSeconds(integer("AUTOFIX_POLL_SECONDS", 15)), value("AUTOFIX_API_BASE_URL", "http://localhost:8080"));
    }

    private static String value(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int integer(String name, int fallback) {
        try { return Math.max(1, Integer.parseInt(value(name, String.valueOf(fallback)))); } catch (NumberFormatException exception) { throw new IllegalArgumentException(name + " doit être un entier positif", exception); }
    }
}
