package org.infospray.peonsimulator.adapter.secondary.kafka;

import org.infospray.peonsimulator.application.port.secondary.DomainEventPublisher;
import org.infospray.peonsimulator.domain.event.SimulationEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KafkaDomainEventPublisher implements DomainEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final boolean kafkaEnabled;

    public KafkaDomainEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, ApplicationEventPublisher applicationEventPublisher, @Value("${simulator.kafka.enabled:false}") boolean kafkaEnabled) {
        this.kafkaTemplate = kafkaTemplate;
        this.applicationEventPublisher = applicationEventPublisher;
        this.kafkaEnabled = kafkaEnabled;
    }

    @Override
    public void publish(List<SimulationEvent> events) {
        for (SimulationEvent event : events) {
            this.applicationEventPublisher.publishEvent(event);
            if (this.kafkaEnabled) { this.kafkaTemplate.send("peon-simulator.domain-events.v1", event.worldId().toString(), event); }
        }
    }
}
