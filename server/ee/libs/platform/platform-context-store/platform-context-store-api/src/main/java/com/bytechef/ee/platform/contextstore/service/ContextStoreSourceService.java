/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.service;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSourceStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * CRUD service for {@link ContextStoreSource}. Workspace scoping is the nullable
 * {@code context_store_source.workspace_id} column; the automation-side service is the one that decides which workspace
 * a caller may see. Lifecycle helpers ({@link #updateStatus}, {@link #updateLastSyncMetadata}, {@link #setEnabled})
 * wrap a load + mutate + save round-trip so callers don't have to reach for the repository directly to flip a status
 * flag.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface ContextStoreSourceService {

    ContextStoreSource create(ContextStoreSource source);

    ContextStoreSource update(ContextStoreSource source);

    void delete(Long id);

    ContextStoreSource get(Long id);

    Optional<ContextStoreSource> fetch(Long id);

    List<ContextStoreSource> getAllByIds(List<Long> ids);

    List<ContextStoreSource> getAllEnabledByIds(List<Long> ids);

    List<ContextStoreSource> getAllByWorkspaceId(long workspaceId);

    List<ContextStoreSource> getAllEnabledByWorkspaceId(long workspaceId);

    List<ContextStoreSource> findAllActiveAcrossWorkspaces();

    /**
     * Lists every source under a given parent {@link com.bytechef.ee.platform.contextstore.domain.ContextStore},
     * regardless of enabled state. Used by the store-level {@code contextStore.searchByStore} action to fan a single
     * structured query out across all replicas in the store and return a merged result.
     */
    List<ContextStoreSource> findAllByContextStoreId(Long contextStoreId);

    /**
     * Every source whose generated sync workflow is {@code workflowId}. Used by the workflow-delete cascade.
     */
    List<ContextStoreSource> findAllByWorkflowId(String workflowId);

    void updateStatus(
        Long id, ContextStoreSourceStatus status, @Nullable Instant lastSyncRunAt, @Nullable Long jobExecutionId);

    /**
     * Updates only the {@code lastSyncRunAt} + {@code lastSyncJobExecutionId} on the given source row, leaving status
     * untouched. Used by the listener's PARTIAL-mode branch on COMPLETED — a partial sync run "happened" (so timestamps
     * advance) but didn't prove the full source's readiness (so status stays where it was).
     */
    void updateLastSyncMetadata(Long id, @Nullable Instant lastSyncRunAt, @Nullable Long jobExecutionId);

    void setEnabled(Long id, boolean enabled);
}
