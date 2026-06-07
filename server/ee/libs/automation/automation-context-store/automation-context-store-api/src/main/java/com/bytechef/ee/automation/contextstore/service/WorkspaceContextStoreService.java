/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.service;

import com.bytechef.ee.automation.contextstore.domain.WorkspaceContextStore;
import com.bytechef.ee.platform.contextstore.domain.ContextStore;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Optional;

/**
 * Workspace-scoped service over the {@link WorkspaceContextStore} relation table. Parallels
 * {@code WorkspaceKnowledgeBaseService}; the platform-side store service has no workspace knowledge — this is the
 * single workspace-aware service over the relation.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface WorkspaceContextStoreService {

    WorkspaceContextStore create(Long contextStoreId, Long workspaceId);

    void deleteByContextStoreId(Long contextStoreId);

    List<ContextStore> getAllStoresByWorkspaceId(Long workspaceId);

    List<ContextStore> getAllStoresByWorkspaceIdAndEnvironment(Long workspaceId, Environment environment);

    Optional<Long> fetchWorkspaceIdByContextStoreId(Long contextStoreId);

    boolean isStoreInWorkspace(Long workspaceId, Long contextStoreId);
}
