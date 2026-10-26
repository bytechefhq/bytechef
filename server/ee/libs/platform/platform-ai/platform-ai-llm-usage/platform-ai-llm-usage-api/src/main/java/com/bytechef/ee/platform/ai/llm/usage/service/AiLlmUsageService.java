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

    void create(AiLlmUsage usage, Long workspaceId);

    void deleteOlderThan(Instant date);

    void deleteOlderThanByWorkspace(Instant date, Long workspaceId);

    void deleteOlderThanWithoutWorkspace(Instant date);

    List<Long> findDistinctWorkspaceIds();

    Map<String, Double> getAverageLatencyByModel(Instant since);

    List<AiLlmUsage> getUsagesByOwner(LlmUsageSource source, long ownerId);

    List<AiLlmUsage> getRequestLogs(Instant start, Instant end);

    List<AiLlmUsage> getRequestLogsByWorkspace(Long workspaceId, Instant start, Instant end);
}
