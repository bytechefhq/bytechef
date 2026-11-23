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
public record IntegrationConnectionWorkflowDTO(
    long id, long parentId, String parentName, ParentType parentType, int parentVersion, Usage usage,
    String workflowId, String workflowName, @Nullable String workflowUuid) {

    public enum ParentType {
        CONNECTED_USER, INTEGRATION
    }

    public enum Usage {
        DEPLOYMENT, EDITOR
    }
}
