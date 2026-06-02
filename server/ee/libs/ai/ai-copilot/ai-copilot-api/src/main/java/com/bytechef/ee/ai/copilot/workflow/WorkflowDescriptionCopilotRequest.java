/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.workflow;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record WorkflowDescriptionCopilotRequest(String workflowId, String workflowNodeName, long environmentId) {
}
