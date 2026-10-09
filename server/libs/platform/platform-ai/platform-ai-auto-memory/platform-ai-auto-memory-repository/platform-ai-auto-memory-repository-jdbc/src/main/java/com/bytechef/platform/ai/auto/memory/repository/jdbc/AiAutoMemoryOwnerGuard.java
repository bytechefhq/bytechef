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
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.relational.core.conversion.MutableAggregateChange;
import org.springframework.data.relational.core.mapping.event.BeforeDeleteCallback;
import org.springframework.data.relational.core.mapping.event.BeforeSaveCallback;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

/**
 * @author Ivica Cardic
 */
public class AiAutoMemoryOwnerGuard implements BeforeSaveCallback<AiAutoMemory>, BeforeDeleteCallback<AiAutoMemory> {

    private static final String FOREIGN_ROW_COUNT_SQL = """
        SELECT COUNT(*) FROM ai_auto_memory
        WHERE id = :id
          AND NOT (workspace_id = :workspaceId
            AND principal_type = :principalType
            AND principal_id = :principalId
            AND environment = :environment)
        """;

    private final NamedParameterJdbcOperations namedParameterJdbcOperations;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiAutoMemoryOwnerGuard(NamedParameterJdbcOperations namedParameterJdbcOperations) {
        this.namedParameterJdbcOperations = namedParameterJdbcOperations;
    }

    @Override
    public AiAutoMemory onBeforeSave(AiAutoMemory memory, MutableAggregateChange<AiAutoMemory> aggregateChange) {
        verifyOwner(memory);

        return memory;
    }

    @Override
    public AiAutoMemory onBeforeDelete(AiAutoMemory memory, MutableAggregateChange<AiAutoMemory> aggregateChange) {
        verifyOwner(memory);

        return memory;
    }

    private void verifyOwner(AiAutoMemory memory) {
        Long id = memory.getId();

        if (id == null) {
            return;
        }

        AiAutoMemoryPrincipalType principalType = memory.getPrincipalType();

        Map<String, Object> parameters = Map.of(
            "id", id, "workspaceId", memory.getWorkspaceId(), "principalType", principalType.ordinal(),
            "principalId", memory.getPrincipalId(), "environment", memory.getEnvironmentOrdinal());

        Integer foreignRowCount = namedParameterJdbcOperations.queryForObject(
            FOREIGN_ROW_COUNT_SQL, parameters, Integer.class);

        if (foreignRowCount != null && foreignRowCount > 0) {
            throw new OptimisticLockingFailureException("Memory " + id + " does not belong to this owner");
        }
    }
}
