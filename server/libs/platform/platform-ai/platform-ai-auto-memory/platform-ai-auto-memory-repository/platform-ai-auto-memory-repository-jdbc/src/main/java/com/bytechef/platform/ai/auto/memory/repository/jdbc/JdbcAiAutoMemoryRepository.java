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

package com.bytechef.platform.ai.auto.memory.repository.jdbc;

import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalCount;
import com.bytechef.platform.ai.auto.memory.repository.AiAutoMemoryRepository;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JDBC binding for the {@code ai_auto_memory} table. The workspace dimension is the table's own nullable
 * {@code workspace_id} column, so a workspace-scoped query never matches a memory that has no workspace.
 *
 * @author Ivica Cardic
 */
@Repository
public interface JdbcAiAutoMemoryRepository extends CrudRepository<AiAutoMemory, Long>, AiAutoMemoryRepository {

    @Override
    @Query("""
        SELECT m.* FROM ai_auto_memory m
        WHERE m.workspace_id = :workspaceId
          AND m.environment = :environment
        ORDER BY m.updated_at DESC
        """)
    List<AiAutoMemory> findByWorkspaceIdAndEnvironmentOrderByUpdatedAtDesc(long workspaceId, int environment);

    @Override
    @Query("""
        SELECT m.* FROM ai_auto_memory m
        WHERE m.workspace_id = :workspaceId
          AND m.environment = :environment
          AND m.memory_type = :memoryType
        ORDER BY m.updated_at DESC
        """)
    List<AiAutoMemory> findByWorkspaceIdAndEnvironmentAndMemoryTypeOrderByUpdatedAtDesc(
        long workspaceId, int environment, int memoryType);

    @Override
    @Query("""
        SELECT m.* FROM ai_auto_memory m
        WHERE m.workspace_id = :workspaceId
          AND m.principal_type = :principalType
          AND m.principal_id = :principalId
          AND m.environment = :environment
        ORDER BY m.updated_at DESC
        """)
    List<AiAutoMemory> findByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentOrderByUpdatedAtDesc(
        long workspaceId, int principalType, long principalId, int environment);

    @Override
    @Query("""
        SELECT m.* FROM ai_auto_memory m
        WHERE m.workspace_id = :workspaceId
          AND m.principal_type = :principalType
          AND m.principal_id = :principalId
          AND m.environment = :environment
          AND m.memory_type = :memoryType
        ORDER BY m.updated_at DESC
        """)
    List<AiAutoMemory> findByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentAndMemoryTypeOrderByUpdatedAtDesc(
        long workspaceId, int principalType, long principalId, int environment, int memoryType);

    @Override
    @Query("""
        SELECT m.* FROM ai_auto_memory m
        WHERE m.workspace_id = :workspaceId
          AND m.principal_type = :principalType
          AND m.principal_id = :principalId
          AND m.environment = :environment
          AND m.name = :name
        """)
    List<AiAutoMemory> findAllByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentAndName(
        long workspaceId, int principalType, long principalId, int environment, String name);

    /**
     * Grouped over the same {@code (principal_type, principal_id)} pair the per-principal finders filter on, so an
     * owner appears here exactly when one of those finders would return something for it. Ordered so the two backends
     * agree on more than set equality.
     */
    @Query("""
        SELECT m.principal_type, m.principal_id, CAST(COUNT(*) AS INT) AS memory_count
        FROM ai_auto_memory m
        WHERE m.workspace_id = :workspaceId
          AND m.environment = :environment
        GROUP BY m.principal_type, m.principal_id
        ORDER BY m.principal_type, m.principal_id
        """)
    List<AiAutoMemoryPrincipalCountRow> findPrincipalCounts(long workspaceId, int environment);

    @Override
    default List<AiAutoMemoryPrincipalCount> listPrincipals(long workspaceId, int environment) {
        return findPrincipalCounts(workspaceId, environment).stream()
            .map(AiAutoMemoryPrincipalCountRow::toAiAutoMemoryPrincipalCount)
            .toList();
    }
}
