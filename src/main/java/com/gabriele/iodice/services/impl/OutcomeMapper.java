package com.gabriele.iodice.services.impl;

import com.gabriele.iodice.entities.Outcome;

import java.util.EnumMap;
import java.util.Map;

/// Maps provider specific 1X2 keys to the standard {@link Outcome}.
/// Every invalid input ends in an {@link IllegalArgumentException}, which the API returns as 400.
final class OutcomeMapper {

    private OutcomeMapper() { }

    /// @param providerOdds odds keyed by the provider's outcome names, e.g. "1"/"X"/"2"
    /// @param outcomes     the provider's outcome names mapped to the standard outcomes
    /// @param formatHint   the expected format, used in the error message
    static Map<Outcome, Double> toOdds(Map<String, Double> providerOdds,
                                       Map<String, Outcome> outcomes,
                                       String formatHint) {
        if (providerOdds == null
                || providerOdds.size() != outcomes.size()
                || !providerOdds.keySet().equals(outcomes.keySet())) {
            throw new IllegalArgumentException(formatHint);
        }

        Map<Outcome, Double> odds = new EnumMap<>(Outcome.class);
        providerOdds.forEach((key, value) -> {
            if (value == null || value <= 1.0) {
                throw new IllegalArgumentException("Odds for \"" + key + "\" MUST be a number greater than 1.0");
            }
            odds.put(outcomes.get(key), value);
        });
        return odds;
    }

    /// @param providerOutcome the provider's outcome name, e.g. "X"
    /// @param fieldName       the provider's JSON field name, used in the error message
    static Outcome toOutcome(String providerOutcome, Map<String, Outcome> outcomes, String fieldName) {
        if (providerOutcome == null) {
            throw new IllegalArgumentException(fieldName + " is mandatory for settlement messages");
        }
        Outcome outcome = outcomes.get(providerOutcome);
        if (outcome == null) {
            throw new IllegalArgumentException(fieldName + " MUST be one of " + outcomes.keySet());
        }
        return outcome;
    }
}
