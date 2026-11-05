package com.gabriele.iodice.entities.impl;

import com.gabriele.iodice.entities.StandardMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Arrays;

public record OddsChangeMessage(
        @NotBlank(message = "id is mandatory") String id,
        @NotEmpty(message = "scores is mandatory") Double[] scores
) implements StandardMessage {
    @Override
    @NotNull
    public String toString() {
        return "OddsChangeMessage[" +
                "id='" + id + '\'' +
                ", scores=" + Arrays.toString(scores) +
                ']';
    }
}
