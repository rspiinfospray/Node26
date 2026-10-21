package org.infospray.peonsimulator.supervisor.application;

import org.infospray.peonsimulator.supervisor.application.port.secondary.AutoCorrectionReportRepository;
import org.infospray.peonsimulator.supervisor.application.port.secondary.CodexCorrectionAgent;
import org.infospray.peonsimulator.supervisor.application.port.secondary.CorrectionWorkspace;
import org.infospray.peonsimulator.supervisor.application.port.secondary.SimulationCycleLauncher;

import java.nio.file.Path;

public class RunAutoCorrectionCycleCommand {
    private final AutoCorrectionReportRepository reportRepository;
    private final CodexCorrectionAgent codexAgent;
    private final CorrectionWorkspace workspace;
    private final SimulationCycleLauncher simulationLauncher;
    private final int maximumCycles;

    public RunAutoCorrectionCycleCommand(AutoCorrectionReportRepository reportRepository, CodexCorrectionAgent codexAgent, CorrectionWorkspace workspace, SimulationCycleLauncher simulationLauncher, int maximumCycles) {
        this.reportRepository = reportRepository;
        this.codexAgent = codexAgent;
        this.workspace = workspace;
        this.simulationLauncher = simulationLauncher;
        this.maximumCycles = maximumCycles;
    }

    public AutoCorrectionResult execute() {
        int completedCycles = this.reportRepository.completedCycles();
        if (completedCycles >= this.maximumCycles) { return new AutoCorrectionResult(AutoCorrectionResult.Status.STOPPED, "Limite de " + this.maximumCycles + " cycles atteinte"); }
        Path report = this.reportRepository.nextPending().orElse(null);
        if (report == null) { return new AutoCorrectionResult(AutoCorrectionResult.Status.NO_REPORT, "Aucun nouveau rapport"); }
        int cycleNumber = completedCycles + 1;
        try {
            WorkspaceSnapshot beforeCodex = this.workspace.snapshot();
            CodexCorrectionResult correction = this.codexAgent.apply(report, cycleNumber);
            WorkspaceSnapshot afterCodex = this.workspace.snapshot();
            this.validateCorrection(beforeCodex, afterCodex, correction);
            this.workspace.runTests();
            this.workspace.deploy();
            this.simulationLauncher.launch(cycleNumber);
            this.reportRepository.markCompleted(report);
            return new AutoCorrectionResult(AutoCorrectionResult.Status.COMPLETED, "Cycle " + cycleNumber + " terminé pour " + report.getFileName());
        } catch (RuntimeException exception) {
            this.reportRepository.markFailed(report, exception.getMessage());
            return new AutoCorrectionResult(AutoCorrectionResult.Status.STOPPED, "Cycle " + cycleNumber + " arrêté: " + exception.getMessage());
        }
    }

    private void validateCorrection(WorkspaceSnapshot beforeCodex, WorkspaceSnapshot afterCodex, CodexCorrectionResult correction) {
        if (correction.response().isBlank()) { throw new IllegalStateException("Codex n'a produit aucun compte rendu"); }
        java.util.Set<String> changedFiles = afterCodex.changedFilesSince(beforeCodex);
        if (changedFiles.isEmpty()) { throw new IllegalStateException("Codex n'a produit aucune modification"); }
        boolean containsCode = changedFiles.stream().map(path -> path.replace('\\', '/')).anyMatch(path -> path.startsWith("src/main/java/") || path.startsWith("src/test/java/"));
        if (!containsCode) { throw new IllegalStateException("Codex n'a modifié aucun fichier Java de production ou de test: " + changedFiles); }
    }
}
