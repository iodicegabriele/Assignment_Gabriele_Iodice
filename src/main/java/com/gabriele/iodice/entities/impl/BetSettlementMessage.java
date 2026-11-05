package com.gabriele.iodice.entities.impl;

import com.gabriele.iodice.entities.Outcome;
import com.gabriele.iodice.entities.StandardMessage;

/// Standardized BET_SETTLEMENT message: the winning 1X2 outcome of an event.
public record BetSettlementMessage(
        String id,
        Outcome result
) implements StandardMessage { }
