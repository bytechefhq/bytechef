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

import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Manages per-principal, per-workspace long-term memories that outlive a single agent turn. All operations are scoped
 * to {@code (workspaceId, principalType, principalId)} — there is no cross-principal read or write path. The
 * {@code principalType} discriminator keeps USER-owned (AI Hub) and PROJECT_DEPLOYMENT-owned (workflow agent) memory
 * from colliding even when they share a {@code principalId} value.
 *
 * <p>
 * Ownership is enforced in the service layer: callers supply the requesting principal's type and id and the service
 * throws {@link AiAutoMemoryNotFoundException} when no row matches the (workspaceId, principalType, principalId, ...)
 * lookup. Cross-principal reads surface as the same not-found shape so a probe cannot enumerate ids across principals.
 *
 * @author Ivica Cardic
 */
public interface AiAutoMemoryService {

    /**
     * Creates a new memory row. Throws {@link DuplicateAiAutoMemoryNameException} when another row already uses the
     * same {@code name} for this {@code (workspaceId, principalType, principalId, environment)} tuple. The unique
     * constraint on the underlying table includes environment so the same memory name can co-exist in DEVELOPMENT,
     * STAGING, and PRODUCTION without colliding.
     */
    AiAutoMemory create(
        long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, int environment, String name,
        String title, @Nullable String description, AiAutoMemoryType memoryType, String content);

    /**
     * Loads the memory with the given name for this {@code (workspaceId, principalType, principalId, environment)}
     * tuple. Returns empty when not found.
     */
    Optional<AiAutoMemory> read(
        long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, int environment, String name);

    /**
     * Partial update — only non-null fields are applied. At least one field must be non-null; throws
     * {@link IllegalArgumentException} when all are null. Throws {@link AiAutoMemoryNotFoundException} when no row
     * exists for this {@code (workspaceId, principalType, principalId, environment, name)} or when the row belongs to
     * another principal (the not-found shape is reused for cross-principal lookups so a probe cannot enumerate ids).
     */
    AiAutoMemory update(
        long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, int environment, String name,
        @Nullable String title, @Nullable String description,
        @Nullable AiAutoMemoryType memoryType, @Nullable String content);

    /**
     * Updates the fields of the memory identified by its primary key, scoped to
     * {@code (workspaceId, principalType, principalId, environment)}. Used by the REST/GraphQL management endpoints.
     * Partial update — only non-null fields are applied. The primary key pins the row, but a row belongs to an
     * environment, and a session in one environment must not reach another's rows: a row in a different environment
     * raises the same {@link AiAutoMemoryNotFoundException} a missing row does.
     */
    AiAutoMemory updateById(
        long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, long memoryId,
        @Nullable String title, @Nullable String description,
        @Nullable AiAutoMemoryType memoryType, @Nullable String content, int environment);

    /**
     * Deletes the memory row identified by {@code name}. Returns the deleted row so the tool callback layer can capture
     * a pre-image for artifact reversal. Throws {@link AiAutoMemoryNotFoundException} when the memory is missing.
     */
    AiAutoMemory delete(
        long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, int environment, String name);

    /**
     * Deletes the memory identified by its primary key, scoped to
     * {@code (workspaceId, principalType, principalId, environment)}. Used by the REST/GraphQL management endpoints.
     * Environment is threaded for the same reason as {@link #updateById} — a row in another environment is
     * indistinguishable from a missing one and is never deleted.
     */
    AiAutoMemory deleteById(
        long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, long memoryId, int environment);

    /**
     * Renames a memory. Throws {@link DuplicateAiAutoMemoryNameException} when the target name already exists and
     * {@link AiAutoMemoryNotFoundException} when the source does not exist. Both names are resolved within the supplied
     * environment.
     */
    AiAutoMemory rename(
        long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, int environment, String oldName,
        String newName);

    /**
     * Lists memories for the given {@code (workspaceId, principalType, principalId, environment)}, optionally filtered
     * by type. Ordered by {@code updated_at DESC}.
     */
    List<AiAutoMemory> list(
        long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, int environment,
        @Nullable AiAutoMemoryType memoryType);

    /**
     * Lists every memory in the workspace + environment regardless of owner, optionally filtered by type. Ordered by
     * {@code updated_at DESC}. Backs the Memories page's "All" owner scope.
     *
     * <p>
     * Applies NO per-owner authorization — the caller must drop principals it may not address. Only the GraphQL
     * controller should call it, since it holds the resolvePrincipalForListing decision table that decides which owners
     * a given caller can see.
     */
    List<AiAutoMemory> listAllOwners(long workspaceId, int environment, @Nullable AiAutoMemoryType memoryType);

    /**
     * Loads a single memory by its primary key, verifying ownership against
     * {@code (workspaceId, principalType, principalId, environment)}. Returns empty when no row matches or the row
     * belongs to another principal or environment — the REST/GraphQL layer surfaces this as 404. The primary key pins
     * the row, but a row belongs to an environment, and a session in one environment must not read another's rows.
     */
    Optional<AiAutoMemory> findById(
        long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, long memoryId, int environment);

    /**
     * Returns the memories for the given {@code (workspaceId, principalType, principalId, environment)}. Intended for
     * the agent's memory-index injection — equivalent to
     * {@link #list(long, AiAutoMemoryPrincipalType, long, int, AiAutoMemoryType)} with {@code null} type but named
     * explicitly to make the intent at the call site obvious.
     */
    List<AiAutoMemory> listByPrincipalAndWorkspace(
        long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, int environment);

    /**
     * The owners holding memory in this workspace and environment. Unfiltered — the GraphQL layer applies the same
     * per-principal authorization it applies to reads.
     */
    List<AiAutoMemoryPrincipalCount> listPrincipals(long workspaceId, int environment);
}
