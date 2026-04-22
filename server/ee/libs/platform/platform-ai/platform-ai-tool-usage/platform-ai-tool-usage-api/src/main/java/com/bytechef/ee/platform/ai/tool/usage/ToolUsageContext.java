/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ee.platform.ai.tool.usage;

import org.jspecify.annotations.Nullable;

/**
 * Immutable identifying context for a tool-usage row. Carries the workspace and user the call was executed under, the
 * environment ordinal the call ran in, plus an optional opaque {@code ownerId} that ties the row to whatever container
 * the agent platform attaches the call to (a AI Hub task, an AI agent run, etc.). Storing the environment ordinal
 * directly on the row (rather than joining a parent table) keeps analytics queries (SUM(cost) GROUP BY environment,
 * tool_name) on the hot dashboard path index-only.
 *
 * <p>
 * Kept deliberately minimal: extra dimensions (parent agent, source) are the recorder's concern and should be derived
 * from its own state at persist time, not threaded through the wrapper.
 * </p>
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

    /**
     * Convenience factory for callers that have no owner row to attach the usage to (ad-hoc tool runs, agents that
     * don't model long-lived containers). The resulting {@link #ownerId()} is {@code null}.
     */
    public static ToolUsageContext ofWorkspaceUser(long workspaceId, long userId, int environment) {
        return new ToolUsageContext(workspaceId, userId, environment, null);
    }
}
