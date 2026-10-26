/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.llm.usage.service;

import com.bytechef.ee.platform.ai.llm.usage.AiLlmUsage;
import com.bytechef.ee.platform.ai.llm.usage.LlmUsageSource;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiLlmUsageService {

    /**
     * Persists the usage row with its owning workspace. The workspace is required: every cost-dashboard query filters
     * {@code ai_llm_usage.workspace_id}, so a row saved without one would silently disappear from analytics.
     */
    void create(AiLlmUsage usage, Long workspaceId);

    void deleteOlderThan(Instant date);

    void deleteOlderThanByWorkspace(Instant date, Long workspaceId);

    /**
     * Deletes the workspace-less usage rows created before {@code date}. Per-workspace retention never reaches them.
     */
    void deleteOlderThanWithoutWorkspace(Instant date);

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
