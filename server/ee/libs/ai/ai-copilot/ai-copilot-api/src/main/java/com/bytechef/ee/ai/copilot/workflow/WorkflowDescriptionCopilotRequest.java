/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.workflow;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 * @author Ivica Cardic
 */
public record WorkflowDescriptionCopilotRequest(
    String workflowId, @Nullable String workflowNodeName, long environmentId) {

    public WorkflowDescriptionCopilotRequest {
        Objects.requireNonNull(workflowId, "workflowId");
    }

    public WorkflowDescriptionCopilotRequest withEnvironmentId(long environmentId) {
        return new WorkflowDescriptionCopilotRequest(workflowId, workflowNodeName, environmentId);
    }
}
