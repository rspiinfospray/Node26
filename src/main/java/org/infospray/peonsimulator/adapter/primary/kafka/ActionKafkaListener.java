package org.infospray.peonsimulator.adapter.primary.kafka;

import org.infospray.peonsimulator.application.Illuvatar;
import org.infospray.peonsimulator.application.dto.ActionRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "simulator.kafka.enabled", havingValue = "true")
public class ActionKafkaListener {
    private final Illuvatar illuvatar;

    public ActionKafkaListener(Illuvatar illuvatar) { this.illuvatar = illuvatar; }

    @KafkaListener(topics = "peon-simulator.action-requests.v1", groupId = "illuvatar")
    public void onAction(ActionRequest request) { this.illuvatar.processAction(request); }
}
