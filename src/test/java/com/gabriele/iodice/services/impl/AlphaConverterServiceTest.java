package com.gabriele.iodice.services.impl;

import com.gabriele.iodice.dtos.AlphaFeedDTO;
import com.gabriele.iodice.entities.Outcome;
import com.gabriele.iodice.entities.impl.BetSettlementMessage;
import com.gabriele.iodice.entities.impl.OddsChangeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlphaConverterServiceTest {

    private final AlphaConverterService converter = new AlphaConverterService();

    @Test
    void convertsOddsUpdateIntoOddsChangeMessage() {
        AlphaFeedDTO dto = new AlphaFeedDTO("odds_update", "ev123",
                Map.of("1", 2.0, "X", 3.1, "2", 3.8), null);

        assertThat(converter.Convert(dto)).isEqualTo(new OddsChangeMessage("ev123",
                Map.of(Outcome.HOME, 2.0, Outcome.DRAW, 3.1, Outcome.AWAY, 3.8)));
    }

    @ParameterizedTest
    @CsvSource({"1, HOME", "X, DRAW", "2, AWAY"})
    void convertsSettlementIntoBetSettlementMessage(String outcome, Outcome expected) {
        AlphaFeedDTO dto = new AlphaFeedDTO("settlement", "ev123", null, outcome);

        assertThat(converter.Convert(dto)).isEqualTo(new BetSettlementMessage("ev123", expected));
    }

    @Test
    void rejectsUnknownMessageType() {
        AlphaFeedDTO dto = new AlphaFeedDTO("unknown", "ev123", null, null);

        assertThatThrownBy(() -> converter.Convert(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Alpha Feed message type not supported: unknown");
    }

    @Test
    void rejectsOddsUpdateWithoutValues() {
        AlphaFeedDTO dto = new AlphaFeedDTO("odds_update", "ev123", null, null);

        assertThatThrownBy(() -> converter.Convert(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\"values\" MUST be in the format");
    }

    @Test
    void rejectsOddsUpdateWithMissingOutcomeKey() {
        AlphaFeedDTO dto = new AlphaFeedDTO("odds_update", "ev123",
                Map.of("1", 2.0, "X", 3.1), null);

        assertThatThrownBy(() -> converter.Convert(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\"values\" MUST be in the format");
    }

    @Test
    void rejectsOddsUpdateWithExtraOrWrongKeys() {
        AlphaFeedDTO dto = new AlphaFeedDTO("odds_update", "ev123",
                Map.of("1", 2.0, "X", 3.1, "2", 3.8, "home", 1.5), null);

        assertThatThrownBy(() -> converter.Convert(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsOddsUpdateWithNullOdd() {
        Map<String, Double> values = new HashMap<>(Map.of("1", 2.0, "2", 3.8));
        values.put("X", null);
        AlphaFeedDTO dto = new AlphaFeedDTO("odds_update", "ev123", values, null);

        assertThatThrownBy(() -> converter.Convert(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Odds for \"X\" MUST be a number greater than 1.0");
    }

    @Test
    void rejectsOddsUpdateWithOddNotGreaterThanOne() {
        AlphaFeedDTO dto = new AlphaFeedDTO("odds_update", "ev123",
                Map.of("1", 1.0, "X", 3.1, "2", 3.8), null);

        assertThatThrownBy(() -> converter.Convert(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Odds for \"1\" MUST be a number greater than 1.0");
    }

    @Test
    void rejectsSettlementWithoutOutcome() {
        AlphaFeedDTO dto = new AlphaFeedDTO("settlement", "ev123", null, null);

        assertThatThrownBy(() -> converter.Convert(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("outcome is mandatory for settlement messages");
    }
}
