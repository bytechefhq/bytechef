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
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryOwner;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalCount;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.ai.auto.memory.repository.AiAutoMemoryRepository;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.stereotype.Repository;

/**
 * @author Ivica Cardic
 */
@Repository
public interface JdbcAiAutoMemoryRepository
    extends org.springframework.data.repository.Repository<AiAutoMemory, Long>, AiAutoMemoryRepository {

    @Override
    default Optional<AiAutoMemory> findOwnedById(AiAutoMemoryOwner owner, long id) {
        return UnmappableRows.skipUnmappable(
            findOwnedRow(
                id, owner.workspaceId(), principalTypeOrdinal(owner), owner.principalId(), environmentOrdinal(owner)));
    }

    @Override
    default List<AiAutoMemory> findByOwner(AiAutoMemoryOwner owner, @Nullable AiAutoMemoryType memoryType) {
        if (memoryType == null) {
            return UnmappableRows.skipUnmappable(
                findOwnerRows(
                    owner.workspaceId(), principalTypeOrdinal(owner), owner.principalId(), environmentOrdinal(owner)));
        }

        return UnmappableRows.skipUnmappable(
            findOwnerRowsByMemoryType(
                owner.workspaceId(), principalTypeOrdinal(owner), owner.principalId(), environmentOrdinal(owner),
                memoryType.ordinal()));
    }

    @Override
    default List<AiAutoMemory> findAllByOwnerAndName(AiAutoMemoryOwner owner, String name) {
        return UnmappableRows.skipUnmappable(
            findOwnerRowsByName(
                owner.workspaceId(), principalTypeOrdinal(owner), owner.principalId(), environmentOrdinal(owner),
                name));
    }

    @Override
    default List<AiAutoMemory> findByWorkspace(
        long workspaceId, Environment environment, @Nullable AiAutoMemoryType memoryType) {

        if (memoryType == null) {
            return UnmappableRows.skipUnmappable(findWorkspaceRows(workspaceId, environment.ordinal()));
        }

        return UnmappableRows.skipUnmappable(
            findWorkspaceRowsByMemoryType(workspaceId, environment.ordinal(), memoryType.ordinal()));
    }

    @Override
    default List<AiAutoMemoryPrincipalCount> listPrincipals(long workspaceId, Environment environment) {
        List<AiAutoMemoryPrincipalCountRow> rows = findPrincipalCounts(
            workspaceId, environment.ordinal(), AiAutoMemoryPrincipalType.values().length,
            AiAutoMemoryType.values().length);

        return rows.stream()
            .map(AiAutoMemoryPrincipalCountRow::toAiAutoMemoryPrincipalCount)
            .toList();
    }

    @Query("""
        SELECT m.* FROM ai_auto_memory m
        WHERE m.id = :id
          AND m.workspace_id = :workspaceId
          AND m.principal_type = :principalType
          AND m.principal_id = :principalId
          AND m.environment = :environment
        """)
    Optional<AiAutoMemory> findOwnedRow(
        long id, long workspaceId, int principalType, long principalId, int environment);

    @Query("""
        SELECT m.* FROM ai_auto_memory m
        WHERE m.workspace_id = :workspaceId
          AND m.principal_type = :principalType
          AND m.principal_id = :principalId
          AND m.environment = :environment
        ORDER BY m.updated_at DESC
        """)
    List<AiAutoMemory> findOwnerRows(long workspaceId, int principalType, long principalId, int environment);

    @Query("""
        SELECT m.* FROM ai_auto_memory m
        WHERE m.workspace_id = :workspaceId
          AND m.principal_type = :principalType
          AND m.principal_id = :principalId
          AND m.environment = :environment
          AND m.memory_type = :memoryType
        ORDER BY m.updated_at DESC
        """)
    List<AiAutoMemory> findOwnerRowsByMemoryType(
        long workspaceId, int principalType, long principalId, int environment, int memoryType);

    @Query("""
        SELECT m.* FROM ai_auto_memory m
        WHERE m.workspace_id = :workspaceId
          AND m.principal_type = :principalType
          AND m.principal_id = :principalId
          AND m.environment = :environment
          AND m.name = :name
        """)
    List<AiAutoMemory> findOwnerRowsByName(
        long workspaceId, int principalType, long principalId, int environment, String name);

    @Query("""
        SELECT m.* FROM ai_auto_memory m
        WHERE m.workspace_id = :workspaceId
          AND m.environment = :environment
        ORDER BY m.updated_at DESC
        """)
    List<AiAutoMemory> findWorkspaceRows(long workspaceId, int environment);

    @Query("""
        SELECT m.* FROM ai_auto_memory m
        WHERE m.workspace_id = :workspaceId
          AND m.environment = :environment
          AND m.memory_type = :memoryType
        ORDER BY m.updated_at DESC
        """)
    List<AiAutoMemory> findWorkspaceRowsByMemoryType(long workspaceId, int environment, int memoryType);

    @Query("""
        SELECT m.principal_type, m.principal_id, CAST(COUNT(*) AS INT) AS memory_count
        FROM ai_auto_memory m
        WHERE m.workspace_id = :workspaceId
          AND m.environment = :environment
          AND m.principal_type < :principalTypeCount
          AND m.memory_type < :memoryTypeCount
        GROUP BY m.principal_type, m.principal_id
        ORDER BY m.principal_type, m.principal_id
        """)
    List<AiAutoMemoryPrincipalCountRow> findPrincipalCounts(
        long workspaceId, int environment, int principalTypeCount, int memoryTypeCount);

    private static int principalTypeOrdinal(AiAutoMemoryOwner owner) {
        AiAutoMemoryPrincipalType principalType = owner.principalType();

        return principalType.ordinal();
    }

    private static int environmentOrdinal(AiAutoMemoryOwner owner) {
        Environment environment = owner.environment();

        return environment.ordinal();
    }
}
