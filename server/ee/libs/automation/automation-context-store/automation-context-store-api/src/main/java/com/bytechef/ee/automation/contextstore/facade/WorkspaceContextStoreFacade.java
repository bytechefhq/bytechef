/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.facade;

import com.bytechef.ee.platform.contextstore.domain.ContextStore;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import java.util.Optional;

/**
 * Workspace-scoped facade for {@link ContextStore} parent entities. Bridges the workspace ↔ store relation with the
 * platform-side store service, and enforces application-level uniqueness of {@code (workspace, environment, name)} that
 * can't be expressed as a SQL constraint.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface WorkspaceContextStoreFacade {

    /**
     * Returns Context Stores assigned to the workspace, filtered by environment.
     *
     * @param workspaceId   the workspace ID
     * @param environmentId the environment ordinal
     *                      ({@link com.bytechef.platform.configuration.domain.Environment#ordinal()})
     */
    List<ContextStore> getWorkspaceContextStores(Long workspaceId, long environmentId);

    /**
     * Creates a new Context Store and assigns it to the workspace at the given environment. Throws
     * {@link IllegalArgumentException} if a store with the same name already exists in the workspace at this
     * environment.
     */
    ContextStore createWorkspaceContextStore(ContextStore contextStore, Long workspaceId, long environmentId);

    /**
     * Updates the mutable fields ({@code name}, {@code description}) of an existing Context Store. Environment is not
     * mutable post-creation. Throws if the store does not belong to the supplied workspace.
     */
    ContextStore updateWorkspaceContextStore(Long workspaceId, ContextStore contextStore);

    /**
     * Deletes a Context Store and removes the workspace ↔ store relation row. Cascades through the source FK to delete
     * all sources, entities, and records under this store.
     *
     * @throws IllegalArgumentException if the store does not belong to the supplied workspace
     */
    void deleteWorkspaceContextStore(Long workspaceId, Long contextStoreId);

    /**
     * Resolves a {@code (workspace, name, environment)} triple to a Context Store id. Building block for env-aware
     * workflows that target a store by stable name rather than a hardcoded id — the same name typically exists in
     * multiple environments (DEVELOPMENT / STAGING / PRODUCTION) and a workflow running in the corresponding env
     * resolves the matching store via this lookup. Returns {@link Optional#empty()} when no store matches; the caller
     * decides whether that's a misconfigured workflow or an acceptable fallback.
     */
    Optional<Long> findContextStoreIdByName(Long workspaceId, String name, long environmentId);

    /**
     * Returns the deduplicated tag list used across every Context Store in the workspace. Drives the
     * {@code remainingTags} prop on the management page's {@code TagList}.
     */
    List<Tag> getWorkspaceContextStoreTags(Long workspaceId);

    /**
     * Replaces the tag list on a Context Store. Tags whose {@code id} is set are looked up; tags carrying only a
     * {@code name} are created (via {@link com.bytechef.platform.tag.service.TagService#save}) and the resulting ids
     * are assigned. Returns the resolved tags so the caller can refresh its remainingTags cache.
     *
     * @throws IllegalArgumentException if the store does not belong to the supplied workspace
     */
    List<Tag> updateWorkspaceContextStoreTags(Long workspaceId, Long contextStoreId, List<Tag> tags);
}
