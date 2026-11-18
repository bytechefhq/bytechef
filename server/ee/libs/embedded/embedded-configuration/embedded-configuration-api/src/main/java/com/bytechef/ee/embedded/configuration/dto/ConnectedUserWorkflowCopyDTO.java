/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record ConnectedUserWorkflowCopyDTO(
    String copiedFromWorkflowUuid, String workflowUuid, String externalUserId, @Nullable String connectedUserName,
    @Nullable String connectedUserEmail, boolean enabled) {
}
