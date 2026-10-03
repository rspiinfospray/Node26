package org.infospray.peonsimulator.application.command;

import org.infospray.peonsimulator.application.Illuvatar;
import org.springframework.stereotype.Component;

@Component
public class DeleteAllWorldsCommand {
    private final Illuvatar illuvatar;

    public DeleteAllWorldsCommand(Illuvatar illuvatar) { this.illuvatar = illuvatar; }

    public void execute() { this.illuvatar.deleteAllWorlds(); }
}
