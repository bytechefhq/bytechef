/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.dto;

import org.jspecify.annotations.Nullable;

/**
 * A workflow binding with the names of what it points at. {@code available} is false when the bound workflow can no
 * longer be resolved to a callable workflow, in which case the workflow name is null.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record AppWorkflowBindingDTO(
    long id, long appId, String workflowKey, long projectDeploymentId, String workflowUuid,
    @Nullable String projectDeploymentName, @Nullable String workflowName, boolean available) {
}
