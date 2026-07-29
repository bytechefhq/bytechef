/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.service;

import com.bytechef.ee.platform.contextstore.domain.ContextStore;
import java.util.List;
import java.util.Optional;

/**
 * CRUD service for {@link ContextStore} parent entities. Workspace scoping is the nullable
 * {@code context_store.workspace_id} column; the automation-side service is the one that decides which workspace a
 * caller may see.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface ContextStoreService {

    ContextStore create(ContextStore contextStore);

    ContextStore update(ContextStore contextStore);

    void delete(Long id);

    ContextStore get(Long id);

    Optional<ContextStore> fetch(Long id);

    List<ContextStore> getAllByIds(List<Long> ids);

    List<ContextStore> getAllByWorkspaceId(long workspaceId);
}
