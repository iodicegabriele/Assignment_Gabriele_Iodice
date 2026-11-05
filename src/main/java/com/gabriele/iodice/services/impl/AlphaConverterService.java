package com.gabriele.iodice.services.impl;

import com.gabriele.iodice.dtos.AlphaFeedDTO;
import com.gabriele.iodice.entities.StandardMessage;
import com.gabriele.iodice.entities.impl.BetSettlementMessage;
import com.gabriele.iodice.entities.impl.OddsChangeMessage;
import com.gabriele.iodice.services.ConverterService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AlphaConverterService implements ConverterService<AlphaFeedDTO> {

    public static final String ONE = "1";
    public static final String X = "X";
    public static final String TWO = "2";

    public static final String ODDS_UPDATE = "odds_update";
    public static final String SETTLEMENT = "settlement";

    @Override
    public StandardMessage Convert(AlphaFeedDTO message) {
        StandardMessage convertedMessage;
        if (ODDS_UPDATE.equals(message.msg_type())) {
            convertedMessage = new OddsChangeMessage(message.event_id(), getScores(message.values()));
        }
        else if(SETTLEMENT.equals(message.msg_type())) {
            convertedMessage = new BetSettlementMessage(message.event_id(), getOutcome(message.outcome()));
        }
        else throw new IllegalArgumentException("Alpha Feed message type not supported");
        return convertedMessage;
    }

    private Double[] getScores(Map<String,Double> values) {
        Double[] newScores = new Double[3];
        if(values.containsKey(ONE) && values.containsKey(X) && values.containsKey(TWO)) {
            newScores[0] = values.get(ONE);
            newScores[1] = values.get(X);
            newScores[2] = values.get(TWO);
        }
        else throw new IllegalArgumentException("""
                New Scores MUST be in the format \
                "values": {
                "1": 2.0,
                "X": 3.1,
                "2": 3.8
                }""");
        return newScores;
    }

    private int getOutcome(String outcome) {
        return switch (outcome) {
            case ONE -> 1;
            case X -> 0;
            case TWO -> 2;
            case null, default ->
                    throw new IllegalArgumentException("This should never happen, the validation is on the DTO");
        };
    }
}
