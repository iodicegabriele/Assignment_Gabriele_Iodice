package com.gabriele.iodice.services.impl;

import com.gabriele.iodice.dtos.AlphaFeedDTO;
import com.gabriele.iodice.entities.Outcome;
import com.gabriele.iodice.entities.StandardMessage;
import com.gabriele.iodice.entities.impl.BetSettlementMessage;
import com.gabriele.iodice.entities.impl.OddsChangeMessage;
import com.gabriele.iodice.services.ConverterService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AlphaConverterService implements ConverterService<AlphaFeedDTO> {

    public static final String ODDS_UPDATE = "odds_update";
    public static final String SETTLEMENT = "settlement";

    /// Provider Alpha keys mapped to the standard outcomes
    private static final Map<String, Outcome> OUTCOMES = Map.of(
            "1", Outcome.HOME,
            "X", Outcome.DRAW,
            "2", Outcome.AWAY);

    private static final String ODDS_FORMAT = """
            "values" MUST be in the format {"1": 2.0, "X": 3.1, "2": 3.8}""";

    @Override
    public StandardMessage Convert(AlphaFeedDTO message) {
        if (ODDS_UPDATE.equals(message.msg_type())) {
            return new OddsChangeMessage(message.event_id(),
                    OutcomeMapper.toOdds(message.values(), OUTCOMES, ODDS_FORMAT));
        }
        if (SETTLEMENT.equals(message.msg_type())) {
            return new BetSettlementMessage(message.event_id(),
                    OutcomeMapper.toOutcome(message.outcome(), OUTCOMES, "outcome"));
        }
        throw new IllegalArgumentException("Alpha Feed message type not supported: " + message.msg_type());
    }
}
