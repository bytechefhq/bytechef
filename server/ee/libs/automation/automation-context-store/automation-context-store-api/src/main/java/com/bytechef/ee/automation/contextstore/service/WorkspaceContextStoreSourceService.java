/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.service;

import com.bytechef.ee.automation.contextstore.domain.WorkspaceContextStoreSource;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import java.util.List;
import java.util.Optional;

/**
 * Workspace-scoped service over the {@link WorkspaceContextStoreSource} relation table. Provides the workspace-aware
 * lookups that used to live on {@code ContextStoreSourceRepository.findAllByWorkspaceId(...)} — the platform-side
 * source service no longer carries any workspace knowledge after the platform-relation refactor.
 *
 * <p>
 * Environment scoping: the {@code environmentId} overloads accept an environment for forward compatibility with the
 * Phase 15 UI. The relation table does not carry an environment column today (sources own a single
 * {@code ProjectDeploymentWorkflow} pinned to {@code DEVELOPMENT}); the parameter is reserved so callers can pass the
 * active environment without further GraphQL surface changes when per-environment scoping lands.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface WorkspaceContextStoreSourceService {

    WorkspaceContextStoreSource create(Long contextStoreSourceId, Long workspaceId);

    void deleteByContextStoreSourceId(Long contextStoreSourceId);

    List<ContextStoreSource> getAllSourcesByWorkspaceId(Long workspaceId);

    List<ContextStoreSource> getAllSourcesByWorkspaceId(Long workspaceId, Long environmentId);

    List<ContextStoreSource> getAllEnabledSourcesByWorkspaceId(Long workspaceId);

    List<ContextStoreSource> getAllEnabledSourcesByWorkspaceId(Long workspaceId, Long environmentId);

    Optional<Long> fetchWorkspaceIdByContextStoreSourceId(Long contextStoreSourceId);
}
