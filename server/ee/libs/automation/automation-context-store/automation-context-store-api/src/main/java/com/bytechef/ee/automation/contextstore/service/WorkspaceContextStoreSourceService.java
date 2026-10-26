/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.service;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 * @version ee
 */
public interface WorkspaceContextStoreSourceService {

    List<ContextStoreSource> getAllSourcesByWorkspaceId(Long workspaceId);

    List<ContextStoreSource> getAllSourcesByWorkspaceId(Long workspaceId, Long environmentId);

    List<ContextStoreSource> getAllEnabledSourcesByWorkspaceId(Long workspaceId);

    List<ContextStoreSource> getAllEnabledSourcesByWorkspaceId(Long workspaceId, Long environmentId);

    Optional<Long> fetchWorkspaceIdByContextStoreSourceId(Long contextStoreSourceId);
}
