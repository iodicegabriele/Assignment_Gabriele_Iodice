package com.gabriele.iodice.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

public record AlphaFeedDTO(
        @NotBlank(message = "msg_type is mandatory")
                String msg_type,
        @NotBlank(message = "event_id is mandatory")
                String event_id,
        Map<String, Double> values,
        @Pattern(
                regexp = "^(1|X|2)$",
                message = "Possible outcomes can only be 1|X|2"
        )
        String outcome
) {}