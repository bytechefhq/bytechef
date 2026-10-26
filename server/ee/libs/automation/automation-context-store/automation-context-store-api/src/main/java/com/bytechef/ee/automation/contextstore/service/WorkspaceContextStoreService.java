/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.service;

import com.bytechef.ee.platform.contextstore.domain.ContextStore;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 * @version ee
 */
public interface WorkspaceContextStoreService {

    List<ContextStore> getAllStoresByWorkspaceId(Long workspaceId);

    List<ContextStore> getAllStoresByWorkspaceIdAndEnvironment(Long workspaceId, Environment environment);

    Optional<Long> fetchWorkspaceIdByContextStoreId(Long contextStoreId);

    boolean isStoreInWorkspace(Long workspaceId, Long contextStoreId);
}
