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
import com.bytechef.platform.ai.auto.memory.repository.AiAutoMemoryRepository;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JDBC binding for the {@code ai_auto_memory} table. Workspace-aware queries JOIN through
 * {@code workspace_ai_auto_memory}; the entity itself is workspace-agnostic.
 *
 * @author Ivica Cardic
 */
@Repository
public interface JdbcAiAutoMemoryRepository extends CrudRepository<AiAutoMemory, Long>, AiAutoMemoryRepository {

    @Override
    @Query("""
        SELECT m.* FROM ai_auto_memory m
        JOIN workspace_ai_auto_memory wam ON wam.ai_auto_memory_id = m.id
        WHERE wam.workspace_id = :workspaceId
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
        JOIN workspace_ai_auto_memory wam ON wam.ai_auto_memory_id = m.id
        WHERE wam.workspace_id = :workspaceId
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
        JOIN workspace_ai_auto_memory wam ON wam.ai_auto_memory_id = m.id
        WHERE wam.workspace_id = :workspaceId
          AND m.principal_type = :principalType
          AND m.principal_id = :principalId
          AND m.environment = :environment
          AND m.name = :name
        """)
    List<AiAutoMemory> findAllByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentAndName(
        long workspaceId, int principalType, long principalId, int environment, String name);
}
