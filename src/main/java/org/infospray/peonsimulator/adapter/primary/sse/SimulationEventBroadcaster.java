package org.infospray.peonsimulator.adapter.primary.sse;

import org.infospray.peonsimulator.domain.event.SimulationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SimulationEventBroadcaster {
    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID worldId) {
        SseEmitter emitter = new SseEmitter(0L);
        this.emitters.computeIfAbsent(worldId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> this.remove(worldId, emitter));
        emitter.onTimeout(() -> this.remove(worldId, emitter));
        emitter.onError(ignored -> this.remove(worldId, emitter));
        return emitter;
    }

    @EventListener
    public void broadcast(SimulationEvent event) {
        List<SseEmitter> worldEmitters = this.emitters.getOrDefault(event.worldId(), List.of());
        for (SseEmitter emitter : worldEmitters) {
            try { emitter.send(SseEmitter.event().name("simulation-event").id(event.sequenceNumber() + ":" + event.eventIndex()).data(event)); } catch (IOException exception) { this.remove(event.worldId(), emitter); }
        }
    }

    private void remove(UUID worldId, SseEmitter emitter) {
        this.emitters.getOrDefault(worldId, List.of()).remove(emitter);
    }
}
