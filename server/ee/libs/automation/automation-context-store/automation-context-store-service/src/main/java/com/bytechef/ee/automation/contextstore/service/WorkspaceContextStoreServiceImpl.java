/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.service;

import com.bytechef.ee.platform.contextstore.domain.ContextStore;
import com.bytechef.ee.platform.contextstore.service.ContextStoreService;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements {@link WorkspaceContextStoreService} on top of the nullable {@code context_store.workspace_id} column.
 * Env-scoped lookups read {@link ContextStore#getEnvironment()} on the same rows; environment and workspace are
 * independent columns on the store.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
public class WorkspaceContextStoreServiceImpl implements WorkspaceContextStoreService {

    private final ContextStoreService contextStoreService;

    @SuppressFBWarnings("EI2")
    public WorkspaceContextStoreServiceImpl(ContextStoreService contextStoreService) {
        this.contextStoreService = contextStoreService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextStore> getAllStoresByWorkspaceId(Long workspaceId) {
        return contextStoreService.getAllByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextStore> getAllStoresByWorkspaceIdAndEnvironment(Long workspaceId, Environment environment) {
        Objects.requireNonNull(environment, "environment");

        return getAllStoresByWorkspaceId(workspaceId).stream()
            .filter(store -> store.getEnvironment() == environment)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> fetchWorkspaceIdByContextStoreId(Long contextStoreId) {
        return contextStoreService.fetch(contextStoreId)
            .map(ContextStore::getWorkspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isStoreInWorkspace(Long workspaceId, Long contextStoreId) {
        return contextStoreService.fetch(contextStoreId)
            .map(ContextStore::getWorkspaceId)
            .filter(storeWorkspaceId -> Objects.equals(storeWorkspaceId, workspaceId))
            .isPresent();
    }
}
