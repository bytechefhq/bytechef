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

import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Append-only write-side service for AI tool-call usage tracking. One row per expensive non-LLM tool call (e.g. a web
 * scrape, an image generation, a slide build) — wrapped via {@link MeteredToolCallback} on the agent's tool list.
 *
 * <p>
 * Designed to live above any single agent's domain so that future agents (AI Hub, the standalone AI agent runtime,
 * ad-hoc tool-running surfaces) can share one code path for tool metering. Implementations are responsible for their
 * own persistence + cost estimation; the interface only carries the call-site facts the wrapper can provide.
 * Implementations must never throw — recording failures should be logged and swallowed so a bookkeeping problem cannot
 * break the end-user request.
 * </p>
 *
 * @see MeteredToolCallback
 *
 * @author Ivica Cardic
 */
public interface ToolUsageRecorder {

    /**
     * Records a single expensive tool invocation.
     *
     * @param context    workspace + user + optional owner-context the tool ran under; never {@code null}
     * @param toolName   canonical name of the tool that ran; e.g. {@code research}, {@code generateImage}
     * @param unitCount  billable unit count for the call (typically {@code 1}; can be higher for batched tools)
     * @param durationMs wall-clock duration of the wrapped call, in milliseconds
     * @param metadata   free-form metadata to persist alongside the row (e.g. {@code {"query": "..."}}); may be
     *                   {@code null} or empty to skip
     */
    void recordTool(
        ToolUsageContext context, String toolName, int unitCount, long durationMs,
        @Nullable Map<String, Object> metadata);
}
