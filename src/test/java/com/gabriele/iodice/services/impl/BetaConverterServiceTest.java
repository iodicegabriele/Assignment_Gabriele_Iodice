package com.gabriele.iodice.services.impl;

import com.gabriele.iodice.dtos.AlphaFeedDTO;
import com.gabriele.iodice.dtos.BetaFeedDTO;
import com.gabriele.iodice.entities.StandardMessage;
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

        StandardMessage result = converter.Convert(dto);

        assertThat(result).isInstanceOf(OddsChangeMessage.class);
        OddsChangeMessage odds = (OddsChangeMessage) result;
        assertThat(odds.id()).isEqualTo("ev456");
        assertThat(odds.scores()).containsExactly(1.95, 3.2, 4.0);
    }

    @ParameterizedTest
    @CsvSource({"home, 1", "draw, 0", "away, 2"})
    void convertsSettlementIntoBetSettlementMessage(String outcome, int expectedResult) {
        BetaFeedDTO dto = new BetaFeedDTO("SETTLEMENT", "ev456", null, outcome);

        StandardMessage result = converter.Convert(dto);

        assertThat(result).isEqualTo(new BetSettlementMessage("ev456", expectedResult));
    }

    @Test
    void producesSameStandardMessageAsAlphaForEquivalentInput() {
        AlphaConverterService alpha = new AlphaConverterService();

        OddsChangeMessage fromAlpha = (OddsChangeMessage) alpha.Convert(new AlphaFeedDTO(
                "odds_update", "ev1", Map.of("1", 1.5, "X", 3.0, "2", 5.0), null));
        OddsChangeMessage fromBeta = (OddsChangeMessage) converter.Convert(new BetaFeedDTO(
                "ODDS", "ev1", Map.of("home", 1.5, "draw", 3.0, "away", 5.0), null));

        assertThat(fromBeta.id()).isEqualTo(fromAlpha.id());
        assertThat(fromBeta.scores()).containsExactly(fromAlpha.scores());
        assertThat(converter.Convert(new BetaFeedDTO("SETTLEMENT", "ev1", null, "draw")))
                .isEqualTo(alpha.Convert(new AlphaFeedDTO("settlement", "ev1", null, "X")));
    }

    @Test
    void rejectsUnknownMessageType() {
        BetaFeedDTO dto = new BetaFeedDTO("UNKNOWN", "ev456", null, null);

        assertThatThrownBy(() -> converter.Convert(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not supported");
    }

    @Test
    void rejectsOddsWithMissingOutcomeKey() {
        BetaFeedDTO dto = new BetaFeedDTO("ODDS", "ev456",
                Map.of("home", 1.95, "away", 4.0), null);

        assertThatThrownBy(() -> converter.Convert(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsSettlementWithoutResult() {
        BetaFeedDTO dto = new BetaFeedDTO("SETTLEMENT", "ev456", null, null);

        assertThatThrownBy(() -> converter.Convert(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
