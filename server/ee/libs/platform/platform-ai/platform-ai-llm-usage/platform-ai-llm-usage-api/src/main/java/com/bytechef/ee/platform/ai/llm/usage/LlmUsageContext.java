/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.llm.usage;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings({
    "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
})
public record LlmUsageContext(
    long workspaceId, @Nullable Long userId, LlmUsageSource source, @Nullable Long ownerId,
    @Nullable String agentName, @Nullable String parentAgent, @Nullable Map<String, Object> metadata) {
    public LlmUsageContext {
        if (workspaceId <= 0) {
            throw new IllegalArgumentException("LlmUsageContext.workspaceId must be > 0 (got " + workspaceId + ")");
        }

        if (source == null) {
            throw new IllegalArgumentException("LlmUsageContext.source must not be null");
        }
    }

    public static LlmUsageContext forAiHub(
        long workspaceId, long userId, @Nullable Long taskId, @Nullable String agentName,
        @Nullable String parentAgent) {
        return new LlmUsageContext(
            workspaceId, userId, LlmUsageSource.AI_HUB, taskId, agentName, parentAgent, null);
    }
}
