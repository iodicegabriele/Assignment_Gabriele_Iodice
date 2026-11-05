package com.gabriele.iodice.services.impl;

import com.gabriele.iodice.dtos.AlphaFeedDTO;
import com.gabriele.iodice.dtos.BetaFeedDTO;
import com.gabriele.iodice.entities.Outcome;
import com.gabriele.iodice.entities.impl.BetSettlementMessage;
import com.gabriele.iodice.entities.impl.OddsChangeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BetaConverterServiceTest {

    private final BetaConverterService converter = new BetaConverterService();

    @Test
    void convertsOddsIntoOddsChangeMessage() {
        BetaFeedDTO dto = new BetaFeedDTO("ODDS", "ev456",
                Map.of("home", 1.95, "draw", 3.2, "away", 4.0), null);

        assertThat(converter.Convert(dto)).isEqualTo(new OddsChangeMessage("ev456",
                Map.of(Outcome.HOME, 1.95, Outcome.DRAW, 3.2, Outcome.AWAY, 4.0)));
    }

    @ParameterizedTest
    @CsvSource({"home, HOME", "draw, DRAW", "away, AWAY"})
    void convertsSettlementIntoBetSettlementMessage(String result, Outcome expected) {
        BetaFeedDTO dto = new BetaFeedDTO("SETTLEMENT", "ev456", null, result);

        assertThat(converter.Convert(dto)).isEqualTo(new BetSettlementMessage("ev456", expected));
    }

    @Test
    void producesSameStandardMessagesAsAlphaForEquivalentInput() {
        AlphaConverterService alpha = new AlphaConverterService();

        assertThat(converter.Convert(new BetaFeedDTO(
                "ODDS", "ev1", Map.of("home", 1.5, "draw", 3.0, "away", 5.0), null)))
                .isEqualTo(alpha.Convert(new AlphaFeedDTO(
                        "odds_update", "ev1", Map.of("1", 1.5, "X", 3.0, "2", 5.0), null)));

        assertThat(converter.Convert(new BetaFeedDTO("SETTLEMENT", "ev1", null, "draw")))
                .isEqualTo(alpha.Convert(new AlphaFeedDTO("settlement", "ev1", null, "X")));
    }

    @Test
    void rejectsUnknownMessageType() {
        BetaFeedDTO dto = new BetaFeedDTO("UNKNOWN", "ev456", null, null);

        assertThatThrownBy(() -> converter.Convert(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Beta Feed message type not supported: UNKNOWN");
    }

    @Test
    void rejectsOddsWithoutOddsField() {
        BetaFeedDTO dto = new BetaFeedDTO("ODDS", "ev456", null, null);

        assertThatThrownBy(() -> converter.Convert(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\"odds\" MUST be in the format");
    }

    @Test
    void rejectsOddsWithMissingOutcomeKey() {
        BetaFeedDTO dto = new BetaFeedDTO("ODDS", "ev456",
                Map.of("home", 1.95, "away", 4.0), null);

        assertThatThrownBy(() -> converter.Convert(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\"odds\" MUST be in the format");
    }

    @Test
    void rejectsOddsWithOddNotGreaterThanOne() {
        BetaFeedDTO dto = new BetaFeedDTO("ODDS", "ev456",
                Map.of("home", 1.95, "draw", -3.2, "away", 4.0), null);

        assertThatThrownBy(() -> converter.Convert(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Odds for \"draw\" MUST be a number greater than 1.0");
    }

    @Test
    void rejectsSettlementWithoutResult() {
        BetaFeedDTO dto = new BetaFeedDTO("SETTLEMENT", "ev456", null, null);

        assertThatThrownBy(() -> converter.Convert(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("result is mandatory for settlement messages");
    }
}
