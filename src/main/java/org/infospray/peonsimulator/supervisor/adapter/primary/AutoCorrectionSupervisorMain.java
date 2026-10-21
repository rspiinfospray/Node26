package org.infospray.peonsimulator.supervisor.adapter.primary;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.infospray.peonsimulator.supervisor.adapter.secondary.http.WorldSimulationCycleLauncher;
import org.infospray.peonsimulator.supervisor.adapter.secondary.process.CodexCliCorrectionAgent;
import org.infospray.peonsimulator.supervisor.adapter.secondary.process.LocalCorrectionWorkspace;
import org.infospray.peonsimulator.supervisor.adapter.secondary.report.FileAutoCorrectionReportRepository;
import org.infospray.peonsimulator.supervisor.application.AutoCorrectionResult;
import org.infospray.peonsimulator.supervisor.application.RunAutoCorrectionCycleCommand;
import org.infospray.peonsimulator.supervisor.configuration.AutoCorrectionProperties;

import java.nio.file.Path;

public final class AutoCorrectionSupervisorMain {
    private AutoCorrectionSupervisorMain() {
    }

    public static void main(String[] arguments) {
        AutoCorrectionProperties properties = AutoCorrectionProperties.fromEnvironment();
        FileAutoCorrectionReportRepository reports = new FileAutoCorrectionReportRepository(properties.reportDirectory());
        LocalCorrectionWorkspace workspace = new LocalCorrectionWorkspace(properties.projectDirectory());
        Path prompt = properties.projectDirectory().resolve("src/main/resources/supervisor/codex-fix-prompt.md");
        CodexCliCorrectionAgent codex = new CodexCliCorrectionAgent(properties.projectDirectory(), prompt, properties.reportDirectory().resolve("codex-results"));
        WorldSimulationCycleLauncher launcher = new WorldSimulationCycleLauncher(properties.apiBaseUrl(), new ObjectMapper());
        RunAutoCorrectionCycleCommand command = new RunAutoCorrectionCycleCommand(reports, codex, workspace, launcher, properties.maximumCycles());
        boolean runOnce = Boolean.parseBoolean(System.getenv().getOrDefault("AUTOFIX_RUN_ONCE", "false"));
        System.out.println("Superviseur actif, maximum " + properties.maximumCycles() + " cycles");
        while (true) {
            AutoCorrectionResult result = command.execute();
            if (result.status() != AutoCorrectionResult.Status.NO_REPORT) { System.out.println(result.message()); }
            if (runOnce || result.status() == AutoCorrectionResult.Status.STOPPED) { return; }
            try { Thread.sleep(properties.pollingInterval()); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); return; }
        }
    }
}
