/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.tool.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record TriggerExecutionInfo(
    @JsonProperty("type") @JsonPropertyDescription("The trigger type") @Nullable String type,
    @JsonProperty("status") @JsonPropertyDescription("The trigger execution status") @Nullable String status,
    @JsonProperty("output") @JsonPropertyDescription("The trigger output value") @Nullable Object output,
    @JsonProperty("error") @JsonPropertyDescription("The error message if the trigger failed") @Nullable String error) {
}
