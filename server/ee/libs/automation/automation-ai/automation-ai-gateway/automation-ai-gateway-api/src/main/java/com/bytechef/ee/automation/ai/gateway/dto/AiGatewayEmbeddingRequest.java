/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

/**
 * @version ee
 */
@SuppressFBWarnings("EI")
public record AiGatewayEmbeddingRequest(
    @NotBlank(message = "model must not be blank") String model,
    @NotEmpty(message = "input must not be empty") List<String> input,
    Map<String, String> tags) {

    public AiGatewayEmbeddingRequest(String model, List<String> input) {
        this(model, input, null);
    }
}
