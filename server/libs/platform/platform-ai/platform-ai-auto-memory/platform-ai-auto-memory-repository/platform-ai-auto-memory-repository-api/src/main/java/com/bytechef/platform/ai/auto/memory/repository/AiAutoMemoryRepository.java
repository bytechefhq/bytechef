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
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryOwner;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalCount;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Storage contract for {@link AiAutoMemory} rows, implemented by the JDBC binding
 * ({@code platform-ai-auto-memory-repository-jdbc}) and the file-storage binding
 * ({@code platform-ai-auto-memory-repository-file-storage}). Every per-owner finder matches workspace AND principalType
 * AND principalId AND environment; the workspace-wide finders match workspace AND environment. Every parameter is typed
 * — an owner, an {@link Environment}, an {@link AiAutoMemoryType} — so two adjacent ordinals can never be swapped at a
 * call site; a binding converts them to its stored form itself.
 *
 * <p>
 * Name uniqueness per owner is enforced only by the JDBC binding, through the {@code uk_ai_auto_memory_owner_env_name}
 * unique constraint (surfacing as Spring's {@code DuplicateKeyException}). The file-storage binding enforces no name
 * uniqueness, so concurrent creates of the same name can both succeed there.
 * </p>
 *
 * <p>
 * {@link #save} and {@link #delete} honor {@link AiAutoMemory#getVersion()} on every backend: writing or deleting a
 * copy whose version is no longer the stored one — or whose row is gone, or belongs to another owner — throws Spring's
 * {@code OptimisticLockingFailureException}. On the JDBC binding the version check is atomic; on the file-storage
 * binding it is check-then-write, so two writers racing within that window can both succeed. Callers continue with the
 * instance {@code save} returns, which carries the assigned id and version, and must not reuse the argument: the JDBC
 * binding advances its version in place even when the save then fails, so a retry with it could pass the version check
 * against a row the caller never read.
 * </p>
 *
 * <p>
 * Every finder skips a row whose stored ordinals this build cannot map (see {@link AiAutoMemory#hasKnownOrdinals()}),
 * and {@link #listPrincipals} leaves it out of the counts, so one such row never fails a whole listing.
 * </p>
 *
 * @author Ivica Cardic
 */
public interface AiAutoMemoryRepository {

    AiAutoMemory save(AiAutoMemory memory);

    void delete(AiAutoMemory memory);

    /**
     * The memory with this id, but only when it belongs to {@code owner}; a row of any other owner is reported as
     * missing.
     */
    Optional<AiAutoMemory> findOwnedById(AiAutoMemoryOwner owner, long id);

    /**
     * The owner's memories, newest first by {@code updatedAt}, optionally narrowed by {@code memoryType}. Consumers
     * depend on the ordering.
     */
    List<AiAutoMemory> findByOwner(AiAutoMemoryOwner owner, @Nullable AiAutoMemoryType memoryType);

    /**
     * The owner's memories with this name. At most one on the JDBC binding; the file-storage binding does not enforce
     * unique names, so callers must tolerate more than one.
     */
    List<AiAutoMemory> findAllByOwnerAndName(AiAutoMemoryOwner owner, String name);

    /**
     * Every memory in the workspace and environment regardless of owner, newest first by {@code updatedAt}, optionally
     * narrowed by {@code memoryType}. Backs the Memories page's "All" owner scope. Carries NO authorization of its own
     * — the caller is responsible for dropping principals it may not address, which is why nothing but
     * {@code AiAutoMemoryGraphQlController} (with its owner decision table) should reach it.
     */
    List<AiAutoMemory> findByWorkspace(
        long workspaceId, Environment environment, @Nullable AiAutoMemoryType memoryType);

    /**
     * The distinct {@code (principalType, principalId)} pairs holding memory in this workspace and environment, with
     * per-principal counts, ordered by principal type then id.
     */
    List<AiAutoMemoryPrincipalCount> listPrincipals(long workspaceId, Environment environment);
}
