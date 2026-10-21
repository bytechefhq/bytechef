/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.dto;

import org.jspecify.annotations.Nullable;

/**
 * A deployed workflow an app can call in DEVELOPMENT, keyed for binding by {@code (projectDeploymentId,
 * workflowUuid)}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AppCallableWorkflowDTO(
    long projectDeploymentId, String workflowUuid, String workflowName, long projectId, String projectName,
    @Nullable String inputSchema) {
}
