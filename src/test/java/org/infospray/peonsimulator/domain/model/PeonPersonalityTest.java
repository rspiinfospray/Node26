package org.infospray.peonsimulator.domain.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PeonPersonalityTest {
    @Test
    void shouldNormalizeOpposedTraits() {
        PeonPersonality personality = new PeonPersonality(73, 66, 50, 50);
        assertThat(personality.getAggressiveness()).isEqualTo(27);
    }

    @Test
    void shouldRemainCoherentWhateverJacksonSetterOrder() {
        PeonPersonality personality = new PeonPersonality();
        personality.setAggressiveness(66);
        personality.setPrudence(73);
        assertThat(personality.getPrudence() + personality.getAggressiveness()).isLessThanOrEqualTo(100);
    }

    @Test
    void shouldIgnoreLegacyRiskAppetiteWhenLoading() throws Exception {
        PeonPersonality personality = new ObjectMapper().readValue("{\"prudence\":73,\"aggressiveness\":66,\"curiosity\":50,\"solidarity\":50,\"riskAppetite\":80}", PeonPersonality.class);

        assertThat(personality.getPrudence()).isEqualTo(73);
        assertThat(personality.getAggressiveness()).isEqualTo(27);
    }
}
