/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.dto;

import org.jspecify.annotations.Nullable;

/**
 * A workflow an app could be bound to.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AppBindableWorkflowDTO(
    long projectDeploymentId, String projectDeploymentName, String projectName, String workflowUuid,
    String workflowName, @Nullable String description) {
}
