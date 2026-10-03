package org.infospray.peonsimulator.domain.model;

public class ActionKnowledge {
    private double expectedReward;
    private int attempts;
    private int successes;
    private int failures;

    public ActionKnowledge() {
    }

    public double getExpectedReward() { return this.expectedReward; }
    public int getAttempts() { return this.attempts; }
    public int getSuccesses() { return this.successes; }
    public int getFailures() { return this.failures; }
    public void setExpectedReward(double value) { this.expectedReward = value; }
    public void setAttempts(int value) { this.attempts = value; }
    public void setSuccesses(int value) { this.successes = value; }
    public void setFailures(int value) { this.failures = value; }

    public void learn(double reward, double learningRate) {
        this.expectedReward += learningRate * (reward - this.expectedReward);
        this.attempts++;
        if (reward >= 0) { this.successes++; } else { this.failures++; }
    }
}
