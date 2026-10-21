package org.infospray.peonsimulator.supervisor.application;

import org.infospray.peonsimulator.supervisor.application.port.secondary.AutoCorrectionReportRepository;
import org.infospray.peonsimulator.supervisor.application.port.secondary.CodexCorrectionAgent;
import org.infospray.peonsimulator.supervisor.application.port.secondary.CorrectionWorkspace;
import org.infospray.peonsimulator.supervisor.application.port.secondary.SimulationCycleLauncher;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RunAutoCorrectionCycleCommandTest {
    @Test
    void shouldApplyValidateDeployAndRelaunch() {
        AutoCorrectionReportRepository reports = mock(AutoCorrectionReportRepository.class);
        CodexCorrectionAgent codex = mock(CodexCorrectionAgent.class);
        CorrectionWorkspace workspace = mock(CorrectionWorkspace.class);
        SimulationCycleLauncher launcher = mock(SimulationCycleLauncher.class);
        Path report = Path.of("reports/world.md");
        when(reports.nextPending()).thenReturn(Optional.of(report));
        when(codex.apply(report, 1)).thenReturn(new CodexCorrectionResult("Correction appliquée"));
        when(workspace.snapshot()).thenReturn(new WorkspaceSnapshot(Map.of(".idea/dataSources.xml", "initial")), new WorkspaceSnapshot(Map.of(".idea/dataSources.xml", "initial", "src/main/java/org/infospray/peonsimulator/domain/Peon.java", "modified")));
        RunAutoCorrectionCycleCommand command = new RunAutoCorrectionCycleCommand(reports, codex, workspace, launcher, 3);

        AutoCorrectionResult result = command.execute();

        assertEquals(AutoCorrectionResult.Status.COMPLETED, result.status());
        InOrder order = inOrder(workspace, codex, launcher, reports);
        order.verify(workspace).snapshot();
        order.verify(codex).apply(report, 1);
        order.verify(workspace).snapshot();
        order.verify(workspace).runTests();
        order.verify(workspace).deploy();
        order.verify(launcher).launch(1);
        order.verify(reports).markCompleted(report);
    }

    @Test
    void shouldStopWithoutCodexWhenMaximumCyclesIsReached() {
        AutoCorrectionReportRepository reports = mock(AutoCorrectionReportRepository.class);
        CodexCorrectionAgent codex = mock(CodexCorrectionAgent.class);
        CorrectionWorkspace workspace = mock(CorrectionWorkspace.class);
        SimulationCycleLauncher launcher = mock(SimulationCycleLauncher.class);
        when(reports.completedCycles()).thenReturn(3);
        RunAutoCorrectionCycleCommand command = new RunAutoCorrectionCycleCommand(reports, codex, workspace, launcher, 3);

        AutoCorrectionResult result = command.execute();

        assertEquals(AutoCorrectionResult.Status.STOPPED, result.status());
        verify(codex, never()).apply(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void shouldMarkReportFailedAndStopWhenValidationFails() {
        AutoCorrectionReportRepository reports = mock(AutoCorrectionReportRepository.class);
        CodexCorrectionAgent codex = mock(CodexCorrectionAgent.class);
        CorrectionWorkspace workspace = mock(CorrectionWorkspace.class);
        SimulationCycleLauncher launcher = mock(SimulationCycleLauncher.class);
        Path report = Path.of("reports/world.md");
        when(reports.nextPending()).thenReturn(Optional.of(report));
        when(codex.apply(report, 1)).thenReturn(new CodexCorrectionResult("Correction appliquée"));
        when(workspace.snapshot()).thenReturn(new WorkspaceSnapshot(Map.of()), new WorkspaceSnapshot(Map.of("src/test/java/org/infospray/peonsimulator/PeonTest.java", "modified")));
        org.mockito.Mockito.doThrow(new IllegalStateException("tests failed")).when(workspace).runTests();
        RunAutoCorrectionCycleCommand command = new RunAutoCorrectionCycleCommand(reports, codex, workspace, launcher, 3);

        AutoCorrectionResult result = command.execute();

        assertEquals(AutoCorrectionResult.Status.STOPPED, result.status());
        verify(reports).markFailed(report, "tests failed");
        verify(workspace, never()).deploy();
        verify(launcher, never()).launch(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void shouldStopBeforeTestsWhenCodexDoesNotModifyJavaCode() {
        AutoCorrectionReportRepository reports = mock(AutoCorrectionReportRepository.class);
        CodexCorrectionAgent codex = mock(CodexCorrectionAgent.class);
        CorrectionWorkspace workspace = mock(CorrectionWorkspace.class);
        SimulationCycleLauncher launcher = mock(SimulationCycleLauncher.class);
        Path report = Path.of("reports/world.md");
        when(reports.nextPending()).thenReturn(Optional.of(report));
        when(codex.apply(report, 1)).thenReturn(new CodexCorrectionResult("Je n'ai rien modifié"));
        WorkspaceSnapshot unchanged = new WorkspaceSnapshot(Map.of(".idea/dataSources.xml", "initial"));
        when(workspace.snapshot()).thenReturn(unchanged, unchanged);
        RunAutoCorrectionCycleCommand command = new RunAutoCorrectionCycleCommand(reports, codex, workspace, launcher, 3);

        AutoCorrectionResult result = command.execute();

        assertEquals(AutoCorrectionResult.Status.STOPPED, result.status());
        verify(reports).markFailed(report, "Codex n'a produit aucune modification");
        verify(workspace, never()).runTests();
        verify(workspace, never()).deploy();
        verify(launcher, never()).launch(org.mockito.ArgumentMatchers.anyInt());
    }
}
