package com.gabriele.iodice.entities.impl;

import com.gabriele.iodice.entities.Outcome;
import com.gabriele.iodice.entities.StandardMessage;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/// Standardized ODDS_CHANGE message: the new odds of every 1X2 outcome for an event.
public record OddsChangeMessage(
        String id,
        Map<Outcome, Double> odds
) implements StandardMessage {

    public OddsChangeMessage {
        // EnumMap keeps a stable HOME, DRAW, AWAY order; the copy makes the record immutable
        odds = Collections.unmodifiableMap(new EnumMap<>(odds));
    }
}
