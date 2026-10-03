package org.infospray.peonsimulator.adapter.secondary.kafka;

import org.infospray.peonsimulator.application.dto.ActionRequest;
import org.infospray.peonsimulator.application.port.secondary.ActionRequestPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaActionRequestPublisher implements ActionRequestPublisher {
    public record LocalActionRequested(ActionRequest request) { }
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final boolean kafkaEnabled;

    public KafkaActionRequestPublisher(KafkaTemplate<String, Object> kafkaTemplate, ApplicationEventPublisher applicationEventPublisher, @Value("${simulator.kafka.enabled:false}") boolean kafkaEnabled) {
        this.kafkaTemplate = kafkaTemplate;
        this.applicationEventPublisher = applicationEventPublisher;
        this.kafkaEnabled = kafkaEnabled;
    }

    @Override
    public void publish(ActionRequest request) {
        if (this.kafkaEnabled) {
            this.kafkaTemplate.send("peon-simulator.action-requests.v1", request.worldId().toString(), request);
        } else {
            this.applicationEventPublisher.publishEvent(new LocalActionRequested(request));
        }
    }
}
