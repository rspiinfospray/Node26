package org.infospray.peonsimulator.application.command;

import org.infospray.peonsimulator.application.Illuvatar;
import org.infospray.peonsimulator.domain.event.SimulationEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class GetWorldTimelineCommand {
    private final Illuvatar illuvatar;
    public GetWorldTimelineCommand(Illuvatar illuvatar) { this.illuvatar = illuvatar; }
    public List<SimulationEvent> execute(UUID worldId, long after, int limit) { return this.illuvatar.getEvents(worldId).stream().filter(event -> event.sequenceNumber() >= after).limit(Math.min(limit, 2000)).toList(); }
}
