/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.tool.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record TaskExecutionInfo(
    @JsonProperty("name") @JsonPropertyDescription("The task name (workflow task label or type)") @Nullable String name,
    @JsonProperty("type") @JsonPropertyDescription("The component/action type, e.g. httpClient/v1/get") @Nullable String type,
    @JsonProperty("status") @JsonPropertyDescription("The task execution status") @Nullable String status,
    @JsonProperty("input") @JsonPropertyDescription("The resolved task input") @Nullable Map<String, ?> input,
    @JsonProperty("output") @JsonPropertyDescription("The task output value") @Nullable Object output,
    @JsonProperty("error") @JsonPropertyDescription("The error message if the task failed") @Nullable String error) {
}
