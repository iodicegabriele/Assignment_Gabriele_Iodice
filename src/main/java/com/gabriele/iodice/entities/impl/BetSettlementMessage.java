package com.gabriele.iodice.entities.impl;

import com.gabriele.iodice.entities.StandardMessage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record BetSettlementMessage (
        @NotBlank(message = "id is mandatory") String id,
        @NotBlank(message = "result is mandatory")

        @Min(value = 0, message = "result MUST be 0-2")
        @Max(value = 2, message = "result MUST be 0-2")
            int result
) implements StandardMessage { }
