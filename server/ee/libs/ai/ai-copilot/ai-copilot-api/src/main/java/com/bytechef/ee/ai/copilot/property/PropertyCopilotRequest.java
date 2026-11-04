/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.property;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 * @author Ivica Cardic
 */
public record PropertyCopilotRequest(
    String prompt,
    PropertyCopilotMode mode,
    String workflowId,
    String workflowNodeName,
    String propertyPath,
    @Nullable String propertyType,
    boolean dynamic,
    long environmentId) {

    public PropertyCopilotRequest {
        Objects.requireNonNull(prompt, "prompt");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(workflowId, "workflowId");
        Objects.requireNonNull(workflowNodeName, "workflowNodeName");
        Objects.requireNonNull(propertyPath, "propertyPath");
    }

    public PropertyCopilotRequest withEnvironmentId(long environmentId) {
        return new PropertyCopilotRequest(
            prompt, mode, workflowId, workflowNodeName, propertyPath, propertyType, dynamic, environmentId);
    }
}
