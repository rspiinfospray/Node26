package org.infospray.peonsimulator.adapter.secondary.clock;

import org.infospray.peonsimulator.application.port.secondary.SimulationClock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AdjustableSimulationClock implements SimulationClock {
    private final long baseDelayMilliseconds;
    private final AtomicInteger speedMultiplier = new AtomicInteger(1);

    public AdjustableSimulationClock(@Value("${simulator.clock.delay-ms:600}") long baseDelayMilliseconds) {
        if (baseDelayMilliseconds <= 0) { throw new IllegalArgumentException("Le délai de simulation doit être supérieur à zéro"); }
        this.baseDelayMilliseconds = baseDelayMilliseconds;
    }

    @Override
    public long getDelayMilliseconds() { return Math.max(1, this.baseDelayMilliseconds / this.speedMultiplier.get()); }

    @Override
    public int getSpeedMultiplier() { return this.speedMultiplier.get(); }

    @Override
    public void setSpeedMultiplier(int multiplier) {
        if (multiplier != 1 && multiplier != 2) { throw new IllegalArgumentException("La vitesse doit être ×1 ou ×2"); }
        this.speedMultiplier.set(multiplier);
    }
}
