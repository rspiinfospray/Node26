package org.infospray.peonsimulator.application.port.secondary;

public interface SimulationClock {
    long getDelayMilliseconds();
    int getSpeedMultiplier();
    void setSpeedMultiplier(int multiplier);
}
