/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.llm.usage.repository;

import com.bytechef.ee.platform.ai.llm.usage.AiLlmUsage;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiLlmUsageRepository extends ListCrudRepository<AiLlmUsage, Long> {

    List<AiLlmUsage> findAllByCreatedDateBetween(Instant start, Instant end);

    List<AiLlmUsage> findAllBySourceAndOwnerId(Integer source, Long ownerId);

    List<AiLlmUsage> findAllByWorkspaceIdAndCreatedDateBetween(Long workspaceId, Instant start, Instant end);

    List<AiLlmUsage> findAllByStatusAndCreatedDateAfter(Integer status, Instant after);

    void deleteAllByCreatedDateBefore(Instant date);

    void deleteAllByWorkspaceIdAndCreatedDateBefore(Long workspaceId, Instant date);

    void deleteAllByWorkspaceIdIsNullAndCreatedDateBefore(Instant date);

    @Query("SELECT DISTINCT workspace_id FROM ai_llm_usage WHERE workspace_id IS NOT NULL")
    List<Long> findDistinctWorkspaceIds();
}
