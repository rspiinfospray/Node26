package org.infospray.peonsimulator.application.port.secondary;

import org.infospray.peonsimulator.application.dto.ActionRequest;

public interface ActionRequestPublisher {
    void publish(ActionRequest request);
}
