/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */

public record ConnectedUserWorkflowReferenceDTO(
    String automationWorkflowUuid, String externalUserId, String environment, boolean enabled, boolean dangling,
    String danglingReason, @Nullable String workflowId, @Nullable Long projectDeploymentId) {
}
