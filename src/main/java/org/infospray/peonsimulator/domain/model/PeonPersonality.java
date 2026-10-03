package org.infospray.peonsimulator.domain.model;

public class PeonPersonality {
    private int prudence = 50;
    private int aggressiveness = 50;
    private int curiosity = 50;
    private int solidarity = 50;
    private int riskAppetite = 50;

    public PeonPersonality() {
    }

    public PeonPersonality(int prudence, int aggressiveness, int curiosity, int solidarity, int riskAppetite) {
        this.prudence = this.bounded(prudence);
        this.aggressiveness = this.bounded(aggressiveness);
        this.curiosity = this.bounded(curiosity);
        this.solidarity = this.bounded(solidarity);
        this.riskAppetite = this.bounded(riskAppetite);
        this.normalizeOpposedTraits();
    }

    public int getPrudence() { return this.prudence; }
    public int getAggressiveness() { return this.aggressiveness; }
    public int getCuriosity() { return this.curiosity; }
    public int getSolidarity() { return this.solidarity; }
    public int getRiskAppetite() { return this.riskAppetite; }
    public void setPrudence(int value) { this.prudence = this.bounded(value); this.normalizeOpposedTraits(); }
    public void setAggressiveness(int value) { this.aggressiveness = this.bounded(value); this.normalizeOpposedTraits(); }
    public void setCuriosity(int value) { this.curiosity = this.bounded(value); }
    public void setSolidarity(int value) { this.solidarity = this.bounded(value); }
    public void setRiskAppetite(int value) { this.riskAppetite = this.bounded(value); this.normalizeOpposedTraits(); }

    private void normalizeOpposedTraits() {
        this.aggressiveness = Math.min(this.aggressiveness, 100 - this.prudence);
        this.riskAppetite = Math.min(this.riskAppetite, Math.min(100, 110 - this.prudence));
    }

    private int bounded(int value) { return Math.max(0, Math.min(100, value)); }
}
