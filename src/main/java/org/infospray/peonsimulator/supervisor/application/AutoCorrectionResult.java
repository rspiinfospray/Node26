package org.infospray.peonsimulator.supervisor.application;

public record AutoCorrectionResult(Status status, String message) {
    public enum Status { NO_REPORT, COMPLETED, STOPPED }
}
