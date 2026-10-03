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

package com.bytechef.platform.ai.auto.memory;

import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Manages long-term memories that outlive a single agent turn. Every operation on a single owner's memories is scoped
 * to an {@link AiAutoMemoryOwner} — a principal (user, deployment or integration instance) within one workspace and one
 * environment. The principal type keeps the owner kinds from colliding even when they share a principal id value.
 * {@link #listAllOwners} and {@link #listPrincipals} are the only cross-principal reads; they apply no per-owner
 * authorization and are meant for the management UI, which applies its own.
 *
 * <p>
 * Every per-owner lookup asks the repository with the caller's owner, so a row that belongs to another owner is
 * reported exactly like a missing row ({@link AiAutoMemoryNotFoundException} or an empty result), and a probe cannot
 * enumerate ids across owners.
 *
 * <p>
 * Writes are optimistically locked in two ways. {@link #update} and {@link #updateById} take the version the caller
 * based its edit on, and reject the write when the memory has moved past it. Independently, every update, rename and
 * delete fails with {@link AiAutoMemoryConcurrentModificationException} when another write lands between this call's
 * own load and save. A rename or delete carries no caller version, so it applies to the memory as it is when the call
 * runs. On the JDBC binding that second check is atomic; on the file-storage bindings it is check-then-write, so two
 * writes racing inside that window can both succeed.
 *
 * @author Ivica Cardic
 */
public interface AiAutoMemoryService {

    /**
     * Creates a new memory row. Throws {@link DuplicateAiAutoMemoryNameException} when the owner already has a memory
     * with this {@code name}. On the JDBC binding a unique constraint decides concurrent creates; the file-storage
     * bindings rely on the pre-check alone.
     */
    AiAutoMemory create(
        AiAutoMemoryOwner owner, String name, String title, @Nullable String description,
        AiAutoMemoryType memoryType, String content);

    /**
     * Loads the owner's memory with the given name. Returns empty when not found.
     */
    Optional<AiAutoMemory> read(AiAutoMemoryOwner owner, String name);

    /**
     * Applies {@code patch} to the owner's memory with this name. Throws {@link AiAutoMemoryNotFoundException} when the
     * owner has no memory with this name.
     *
     * <p>
     * {@code expectedVersion} is the {@link AiAutoMemory#getVersion() version} the caller based its edit on. When the
     * stored memory has moved past it, nothing is written and {@link AiAutoMemoryConcurrentModificationException} is
     * thrown.
     * </p>
     */
    AiAutoMemory update(
        AiAutoMemoryOwner owner, String name, long expectedVersion, AiAutoMemoryPatch patch);

    /**
     * Applies {@code patch} to the memory identified by its primary key, with the same rules — {@code expectedVersion}
     * included — as {@link #update(AiAutoMemoryOwner, String, long, AiAutoMemoryPatch)}. Used by the GraphQL management
     * endpoints and by every edit the agent's memory tools make, so a rule added here applies to both. A row owned by
     * anyone else — including the same principal in another environment — raises the same
     * {@link AiAutoMemoryNotFoundException} a missing row does.
     */
    AiAutoMemory updateById(
        AiAutoMemoryOwner owner, long memoryId, long expectedVersion, AiAutoMemoryPatch patch);

    /**
     * Deletes the owner's memory with the given name and returns it. Throws {@link AiAutoMemoryNotFoundException} when
     * the memory is missing.
     */
    AiAutoMemory delete(AiAutoMemoryOwner owner, String name);

    /**
     * Deletes the memory identified by its primary key and returns it. Used by the GraphQL management endpoints. A row
     * owned by anyone else is never deleted and raises {@link AiAutoMemoryNotFoundException}.
     */
    AiAutoMemory deleteById(AiAutoMemoryOwner owner, long memoryId);

    /**
     * Renames one of the owner's memories. Throws {@link DuplicateAiAutoMemoryNameException} when the target name
     * already exists and {@link AiAutoMemoryNotFoundException} when the source does not exist.
     */
    AiAutoMemory rename(AiAutoMemoryOwner owner, String oldName, String newName);

    /**
     * Lists the owner's memories, optionally filtered by type. Ordered by {@code updated_at DESC}.
     */
    List<AiAutoMemory> list(AiAutoMemoryOwner owner, @Nullable AiAutoMemoryType memoryType);

    /**
     * Lists every memory in the workspace + environment regardless of owner, optionally filtered by type. Ordered by
     * {@code updated_at DESC}. Backs the Memories page's "All" owner scope.
     *
     * <p>
     * Applies NO per-owner authorization — the caller must drop principals it may not address. Only
     * {@code AiAutoMemoryGraphQlController} should call it, since it holds the decision table that decides which owners
     * a given caller can see.
     */
    List<AiAutoMemory> listAllOwners(long workspaceId, Environment environment, @Nullable AiAutoMemoryType memoryType);

    /**
     * Loads a single memory by its primary key. Returns empty when no row matches or the row belongs to anyone other
     * than {@code owner} — the GraphQL layer surfaces this as a null result.
     */
    Optional<AiAutoMemory> findById(AiAutoMemoryOwner owner, long memoryId);

    /**
     * The owners holding memory in this workspace and environment. Unfiltered — the GraphQL layer applies the same
     * per-principal authorization it applies to reads.
     */
    List<AiAutoMemoryPrincipalCount> listPrincipals(long workspaceId, Environment environment);

    /**
     * Whether this application stores memory at all. An implementation that cannot — a stub standing in for the service
     * where memory is not hosted — returns {@code false}, so callers can report that up front instead of failing on the
     * first call.
     */
    default boolean isAvailable() {
        return true;
    }
}
