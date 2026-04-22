/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * @version ee
 */
@SuppressFBWarnings("EI")
public record AiGatewayChatCompletionRequest(
    @NotBlank String model,
    @NotEmpty @Size(max = 1000) List<AiGatewayChatMessage> messages,
    @DecimalMin("0") @DecimalMax("2") Double temperature,
    @Positive Integer maxTokens,
    @DecimalMin("0") @DecimalMax("1") Double topP,
    boolean stream,
    String routingPolicy,
    Boolean cache,
    AiGatewayToolChoice toolChoice,
    List<AiGatewayTool> tools,
    @Size(max = 50) Map<String, String> tags) {

    public AiGatewayChatCompletionRequest(
        String model, List<AiGatewayChatMessage> messages, Double temperature, Integer maxTokens, Double topP,
        boolean stream, String routingPolicy, Boolean cache) {

        this(model, messages, temperature, maxTokens, topP, stream, routingPolicy, cache, null, null, null);
    }

    public AiGatewayChatCompletionRequest(
        String model, List<AiGatewayChatMessage> messages, Double temperature, Integer maxTokens, Double topP,
        boolean stream, String routingPolicy, Boolean cache, AiGatewayToolChoice toolChoice,
        List<AiGatewayTool> tools) {

        this(model, messages, temperature, maxTokens, topP, stream, routingPolicy, cache, toolChoice, tools, null);
    }
}
