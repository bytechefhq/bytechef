/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.property;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record PropertyCopilotRequest(
    String prompt,
    PropertyCopilotMode mode,
    String workflowId,
    String workflowNodeName,
    String propertyPath,
    String propertyType,
    boolean dynamic,
    long environmentId) {
}
