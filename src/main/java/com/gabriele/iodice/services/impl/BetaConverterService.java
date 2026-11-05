package com.gabriele.iodice.services.impl;

import com.gabriele.iodice.dtos.BetaFeedDTO;
import com.gabriele.iodice.entities.Outcome;
import com.gabriele.iodice.entities.StandardMessage;
import com.gabriele.iodice.entities.impl.BetSettlementMessage;
import com.gabriele.iodice.entities.impl.OddsChangeMessage;
import com.gabriele.iodice.services.ConverterService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class BetaConverterService implements ConverterService<BetaFeedDTO> {

    public static final String ODDS_UPDATE = "ODDS";
    public static final String SETTLEMENT = "SETTLEMENT";

    /// Provider Beta keys mapped to the standard outcomes
    private static final Map<String, Outcome> OUTCOMES = Map.of(
            "home", Outcome.HOME,
            "draw", Outcome.DRAW,
            "away", Outcome.AWAY);

    private static final String ODDS_FORMAT = """
            "odds" MUST be in the format {"home": 1.95, "draw": 3.2, "away": 4.0}""";

    @Override
    public StandardMessage Convert(BetaFeedDTO message) {
        if (ODDS_UPDATE.equals(message.type())) {
            return new OddsChangeMessage(message.event_id(),
                    OutcomeMapper.toOdds(message.odds(), OUTCOMES, ODDS_FORMAT));
        }
        if (SETTLEMENT.equals(message.type())) {
            return new BetSettlementMessage(message.event_id(),
                    OutcomeMapper.toOutcome(message.result(), OUTCOMES, "result"));
        }
        throw new IllegalArgumentException("Beta Feed message type not supported: " + message.type());
    }
}
