package org.infospray.peonsimulator.application.port.secondary;

public interface PeonDecisionResolver {
    PeonDecisionProvider resolve(String candidateId);
}
