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

package com.bytechef.ee.platform.ai.llm.usage;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Identifying context for a single LLM-call usage row. Carries the workspace + user the call ran under, the
 * {@link LlmUsageSource source} that issued it, an opaque {@code ownerId} that ties the row back to whatever container
 * originated the call (a AI Hub task id, an AI agent run id), and a small set of dimensions common to every agent
 * surface. Source-specific dims that don't generalize (the AI gateway's routing strategy, routed provider, routing
 * policy id, api-key id, etc.) intentionally stay on the gateway's own table — that surface keeps
 * {@code ai_gateway_request_log} for routing-aware audit and can register its own {@link LlmUsageRecorder}
 * implementation if it wants to share the recorder API.
 *
 * <p>
 * Two columns let CC keep its agent-attribution dashboards working against the unified table:
 * </p>
 *
 * <ul>
 * <li>{@code agentName} — AI Hub routing dim, e.g. {@code AI_HUB_BUILD} / {@code RESEARCH}</li>
 * <li>{@code parentAgent} — set on subagent rows to attribute spend back to the dispatching agent</li>
 * </ul>
 *
 * <p>
 * Anything else a caller wants to retain (a model-quality score, a request-shape hash, etc) goes into
 * {@link #metadata()} and is persisted as a JSON blob. Callers must keep metadata small — the column is intended for
 * human inspection, not indexed query.
 * </p>
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

    /**
     * Convenience factory for AI Hub calls. Populates the CC-specific columns ({@code agentName}, {@code parentAgent},
     * {@code ownerId} = task id) and leaves {@code metadata} null.
     */
    public static LlmUsageContext forAiHub(
        long workspaceId, long userId, @Nullable Long taskId, @Nullable String agentName,
        @Nullable String parentAgent) {

        return new LlmUsageContext(
            workspaceId, userId, LlmUsageSource.AI_HUB, taskId, agentName, parentAgent, null);
    }
}
