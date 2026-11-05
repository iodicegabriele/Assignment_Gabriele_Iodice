package com.gabriele.iodice.services.impl;

import com.gabriele.iodice.dtos.AlphaFeedDTO;
import com.gabriele.iodice.entities.StandardMessage;
import com.gabriele.iodice.entities.impl.BetSettlementMessage;
import com.gabriele.iodice.entities.impl.OddsChangeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlphaConverterServiceTest {

    private final AlphaConverterService converter = new AlphaConverterService();

    @Test
    void convertsOddsUpdateIntoOddsChangeMessage() {
        AlphaFeedDTO dto = new AlphaFeedDTO("odds_update", "ev123",
                Map.of("1", 2.0, "X", 3.1, "2", 3.8), null);

        StandardMessage result = converter.Convert(dto);

        assertThat(result).isInstanceOf(OddsChangeMessage.class);
        OddsChangeMessage odds = (OddsChangeMessage) result;
        assertThat(odds.id()).isEqualTo("ev123");
        assertThat(odds.scores()).containsExactly(2.0, 3.1, 3.8);
    }

    @ParameterizedTest
    @CsvSource({"1, 1", "X, 0", "2, 2"})
    void convertsSettlementIntoBetSettlementMessage(String outcome, int expectedResult) {
        AlphaFeedDTO dto = new AlphaFeedDTO("settlement", "ev123", null, outcome);

        StandardMessage result = converter.Convert(dto);

        assertThat(result).isEqualTo(new BetSettlementMessage("ev123", expectedResult));
    }

    @Test
    void rejectsUnknownMessageType() {
        AlphaFeedDTO dto = new AlphaFeedDTO("unknown", "ev123", null, null);

        assertThatThrownBy(() -> converter.Convert(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not supported");
    }

    @Test
    void rejectsOddsUpdateWithMissingOutcomeKey() {
        AlphaFeedDTO dto = new AlphaFeedDTO("odds_update", "ev123",
                Map.of("1", 2.0, "X", 3.1), null);

        assertThatThrownBy(() -> converter.Convert(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsOddsUpdateWithWrongKeys() {
        AlphaFeedDTO dto = new AlphaFeedDTO("odds_update", "ev123",
                Map.of("home", 2.0, "draw", 3.1, "away", 3.8), null);

        assertThatThrownBy(() -> converter.Convert(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsSettlementWithoutOutcome() {
        AlphaFeedDTO dto = new AlphaFeedDTO("settlement", "ev123", null, null);

        assertThatThrownBy(() -> converter.Convert(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
