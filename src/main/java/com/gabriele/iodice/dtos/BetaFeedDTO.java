package com.gabriele.iodice.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

public record BetaFeedDTO(
        @NotBlank(message = "type is mandatory")
                String type,
        @NotBlank(message = "event_id is mandatory")
                String event_id,
        Map<String, Double> odds,
        @Pattern(
                regexp = "^(home|draw|away)$",
                message = "Possible outcomes can only be home|draw|away"
        )
        String result
) {}