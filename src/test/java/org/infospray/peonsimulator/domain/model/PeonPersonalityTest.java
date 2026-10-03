package org.infospray.peonsimulator.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PeonPersonalityTest {
    @Test
    void shouldNormalizeOpposedTraits() {
        PeonPersonality personality = new PeonPersonality(73, 66, 50, 50, 80);
        assertThat(personality.getAggressiveness()).isEqualTo(27);
        assertThat(personality.getRiskAppetite()).isEqualTo(37);
    }

    @Test
    void shouldRemainCoherentWhateverJacksonSetterOrder() {
        PeonPersonality personality = new PeonPersonality();
        personality.setAggressiveness(66);
        personality.setRiskAppetite(80);
        personality.setPrudence(73);
        assertThat(personality.getPrudence() + personality.getAggressiveness()).isLessThanOrEqualTo(100);
        assertThat(personality.getPrudence() + personality.getRiskAppetite()).isLessThanOrEqualTo(110);
    }
}
