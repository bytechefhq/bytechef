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

package com.bytechef.ee.platform.ai.llm.usage.service;

import com.bytechef.ee.platform.ai.llm.usage.AiLlmUsage;
import com.bytechef.ee.platform.ai.llm.usage.LlmUsageSource;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Read/write service for the unified {@code ai_llm_usage} table. The gateway's facade calls {@link #create(AiLlmUsage)}
 * imperatively to persist a row built from routing-aware factories ({@code AiLlmUsage.forSuccess} / {@code .forError});
 * other agent surfaces (AI Hub, future ad-hoc AI agents) write through the
 * {@link com.bytechef.ee.platform.ai.llm.usage.LlmUsageRecorder} contract that the default implementation also exposes.
 *
 * @author Ivica Cardic
 */
public interface AiLlmUsageService {

    /**
     * Persists the usage row and inserts the {@code workspace_ai_llm_usage} membership row in the same transaction. The
     * membership row is the only place a workspace claims a usage row — every cost-dashboard query JOINs through it for
     * the workspace dimension. Failing to insert a membership row would silently lose the usage from analytics.
     */
    void create(AiLlmUsage usage, Long workspaceId);

    void deleteOlderThan(Instant date);

    void deleteOlderThanByWorkspace(Instant date, Long workspaceId);

    List<Long> findDistinctWorkspaceIds();

    Map<String, Double> getAverageLatencyByModel(Instant since);

    /**
     * Usage rows attributed to a specific owning container ({@code ownerId}) for one source — e.g. all AI_AGENT rows of
     * a workflow job. Feeds the per-job execution cost computation.
     */
    List<AiLlmUsage> getUsagesByOwner(LlmUsageSource source, long ownerId);

    List<AiLlmUsage> getRequestLogs(Instant start, Instant end);

    List<AiLlmUsage> getRequestLogsByWorkspace(Long workspaceId, Instant start, Instant end);
}
