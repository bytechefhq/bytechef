/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.tool.usage;

import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record ToolUsageContext(long workspaceId, long userId, int environment, @Nullable Long ownerId) {

    public ToolUsageContext {
        if (workspaceId <= 0) {
            throw new IllegalArgumentException("ToolUsageContext.workspaceId must be > 0 (got " + workspaceId + ")");
        }

        if (userId <= 0) {
            throw new IllegalArgumentException("ToolUsageContext.userId must be > 0 (got " + userId + ")");
        }

        if (environment < 0) {
            throw new IllegalArgumentException(
                "ToolUsageContext.environment must be >= 0 (got " + environment + ")");
        }
    }

    public static ToolUsageContext ofWorkspaceUser(long workspaceId, long userId, int environment) {
        return new ToolUsageContext(workspaceId, userId, environment, null);
    }
}
