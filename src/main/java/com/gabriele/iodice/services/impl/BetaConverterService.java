package com.gabriele.iodice.services.impl;

import com.gabriele.iodice.dtos.BetaFeedDTO;
import com.gabriele.iodice.entities.StandardMessage;
import com.gabriele.iodice.entities.impl.BetSettlementMessage;
import com.gabriele.iodice.entities.impl.OddsChangeMessage;
import com.gabriele.iodice.services.ConverterService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class BetaConverterService implements ConverterService<BetaFeedDTO> {

    public static final String ONE = "home";
    public static final String X = "draw";
    public static final String TWO = "away";

    public static final String ODDS_UPDATE = "ODDS";
    public static final String SETTLEMENT = "SETTLEMENT";

    @Override
    public StandardMessage Convert(BetaFeedDTO message)
    {
        StandardMessage convertedMessage;
        if (ODDS_UPDATE.equals(message.type())) {
            convertedMessage = new OddsChangeMessage(message.event_id(), getOdds(message.odds()));
        }
        else if(SETTLEMENT.equals(message.type())) {
            convertedMessage = new BetSettlementMessage(message.event_id(), getResult(message.result()));
        }
        else throw new IllegalArgumentException("Alpha Feed message type not supported");
        return convertedMessage;
    }

    private Double[] getOdds(Map<String,Double> values) {
        Double[] newScores = new Double[3];
        if(values.containsKey(ONE) && values.containsKey(X) && values.containsKey(TWO)) {
            newScores[0] = values.get(ONE);
            newScores[1] = values.get(X);
            newScores[2] = values.get(TWO);
        }
        else throw new IllegalArgumentException("""
                New Scores MUST be in the format \
                "values": {
                "home": 2.0,
                "draw": 3.1,
                "away": 3.8
                }""");
        return newScores;
    }

    private int getResult(String result) {
        return switch (result) {
            case ONE -> 1;
            case X -> 0;
            case TWO -> 2;
            case null, default ->
                    throw new IllegalArgumentException("This should never happen, the validation is on the DTO");
        };
    }
}
