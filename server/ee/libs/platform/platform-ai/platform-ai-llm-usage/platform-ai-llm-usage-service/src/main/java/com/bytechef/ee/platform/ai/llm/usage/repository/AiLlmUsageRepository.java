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
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JDBC repository for {@link AiLlmUsage}. Insert-only on the hot path; read methods power the gateway's
 * request-log dashboard, the alert evaluator, and AI Hub's spend-by-agent analytics. The entity is workspace-agnostic —
 * workspace-aware queries JOIN through {@code workspace_ai_llm_usage}.
 *
 * @author Ivica Cardic
 */
public interface AiLlmUsageRepository extends ListCrudRepository<AiLlmUsage, Long> {

    List<AiLlmUsage> findAllByCreatedDateBetween(Instant start, Instant end);

    @Query("""
        SELECT r.* FROM ai_llm_usage r
        JOIN workspace_ai_llm_usage wr ON wr.ai_llm_usage_id = r.id
        WHERE wr.workspace_id = :workspaceId
          AND r.created_date BETWEEN :start AND :end
        """)
    List<AiLlmUsage> findAllByWorkspaceIdAndCreatedDateBetween(
        @Param("workspaceId") Long workspaceId, @Param("start") Instant start, @Param("end") Instant end);

    List<AiLlmUsage> findAllByStatusAndCreatedDateAfter(Integer status, Instant after);

    void deleteAllByCreatedDateBefore(Instant date);

    @Modifying
    @Query("""
        DELETE FROM ai_llm_usage
        WHERE id IN (
            SELECT r.id FROM ai_llm_usage r
            JOIN workspace_ai_llm_usage wr ON wr.ai_llm_usage_id = r.id
            WHERE wr.workspace_id = :workspaceId
              AND r.created_date < :date
        )
        """)
    void deleteAllByWorkspaceIdAndCreatedDateBefore(
        @Param("workspaceId") Long workspaceId, @Param("date") Instant date);

    @Query("SELECT DISTINCT workspace_id FROM workspace_ai_llm_usage")
    List<Long> findDistinctWorkspaceIds();
}
