package org.infospray.peonsimulator.adapter.primary.kafka;

import org.infospray.peonsimulator.adapter.secondary.kafka.KafkaActionRequestPublisher;
import org.infospray.peonsimulator.application.Illuvatar;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "simulator.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class LocalActionListener {
    private final Illuvatar illuvatar;

    public LocalActionListener(Illuvatar illuvatar) { this.illuvatar = illuvatar; }

    @EventListener
    public void onAction(KafkaActionRequestPublisher.LocalActionRequested event) { this.illuvatar.processAction(event.request()); }
}
