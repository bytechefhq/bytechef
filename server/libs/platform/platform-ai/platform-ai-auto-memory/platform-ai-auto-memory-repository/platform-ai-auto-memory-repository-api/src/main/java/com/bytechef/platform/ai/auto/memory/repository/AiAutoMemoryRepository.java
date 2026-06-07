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

package com.bytechef.platform.ai.auto.memory.repository;

import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import java.util.List;
import java.util.Optional;

/**
 * Storage contract for {@link AiAutoMemory} rows. Backend-agnostic on purpose: the JDBC implementation lives in
 * {@code platform-ai-auto-memory-repository-jdbc}. Workspace-aware queries JOIN through
 * {@code workspace_ai_auto_memory} — non-JDBC backends MUST honor the documented filter shape (matching workspace
 * membership AND principalType AND principalId AND environment).
 *
 * @author Ivica Cardic
 */
public interface AiAutoMemoryRepository {

    AiAutoMemory save(AiAutoMemory memory);

    void delete(AiAutoMemory memory);

    void deleteById(long id);

    void deleteAll();

    Optional<AiAutoMemory> findById(long id);

    /**
     * Returns the workspace's memories for the given principal + environment, newest first by {@code updatedAt}.
     * Consumers depend on the ordering. The principal is discriminated by {@code principalType} (the persisted INT
     * ordinal of {@link com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType}) and {@code principalId}. Joins
     * through {@code workspace_ai_auto_memory} on the JDBC backend.
     */
    List<AiAutoMemory> findByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentOrderByUpdatedAtDesc(
        long workspaceId, int principalType, long principalId, int environment);

    /**
     * Same shape as {@link #findByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentOrderByUpdatedAtDesc} narrowed
     * by {@link AiAutoMemory#getMemoryType() memoryType} (passed as the persisted INT ordinal — see
     * {@link com.bytechef.platform.ai.auto.memory.AiAutoMemoryType}).
     */
    List<AiAutoMemory> findByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentAndMemoryTypeOrderByUpdatedAtDesc(
        long workspaceId, int principalType, long principalId, int environment, int memoryType);

    /**
     * Returns rows matching the (workspace, principalType, principalId, environment, name) tuple. The DB no longer
     * enforces uniqueness on name — the service layer's duplicate-name check in {@code create()} is the policy gate;
     * multiple matches theoretically possible.
     */
    List<AiAutoMemory> findAllByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentAndName(
        long workspaceId, int principalType, long principalId, int environment, String name);
}
