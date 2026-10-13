package org.infospray.peonsimulator.supervisor.application;

import java.util.Map;
import java.util.Set;

public record WorkspaceSnapshot(Map<String, String> files) {
    public WorkspaceSnapshot {
        files = Map.copyOf(files);
    }

    public Set<String> changedFilesSince(WorkspaceSnapshot previous) {
        java.util.Set<String> changes = new java.util.LinkedHashSet<>();
        this.files.forEach((path, fingerprint) -> { if (!fingerprint.equals(previous.files().get(path))) { changes.add(path); } });
        previous.files().keySet().stream().filter(path -> !this.files.containsKey(path)).forEach(changes::add);
        return Set.copyOf(changes);
    }
}
