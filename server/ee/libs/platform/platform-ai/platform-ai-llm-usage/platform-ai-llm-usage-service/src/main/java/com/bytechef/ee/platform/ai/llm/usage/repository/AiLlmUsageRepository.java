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

package com.bytechef.ee.platform.ai.llm.usage.repository;

import com.bytechef.ee.platform.ai.llm.usage.AiLlmUsage;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

/**
 * Spring Data JDBC repository for {@link AiLlmUsage}. Insert-only on the hot path; read methods power the gateway's
 * request-log dashboard, the alert evaluator, and AI Hub's spend-by-agent analytics. Workspace-aware queries filter
 * {@code ai_llm_usage.workspace_id} directly; a workspace-less row (null column) is invisible to every one of them,
 * which is the intended behavior.
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

    @Query("SELECT DISTINCT workspace_id FROM ai_llm_usage WHERE workspace_id IS NOT NULL")
    List<Long> findDistinctWorkspaceIds();
}
