/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.service;

import com.bytechef.ee.automation.contextstore.domain.WorkspaceContextStore;
import com.bytechef.ee.automation.contextstore.repository.WorkspaceContextStoreRepository;
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
 * Implements {@link WorkspaceContextStoreService} on top of the workspace ↔ context store relation table. Env-scoped
 * lookups read {@link ContextStore#getEnvironment()} on the parent rows; the relation table itself does not carry
 * environment.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
public class WorkspaceContextStoreServiceImpl implements WorkspaceContextStoreService {

    private final ContextStoreService contextStoreService;
    private final WorkspaceContextStoreRepository workspaceContextStoreRepository;

    @SuppressFBWarnings("EI2")
    public WorkspaceContextStoreServiceImpl(
        ContextStoreService contextStoreService, WorkspaceContextStoreRepository workspaceContextStoreRepository) {

        this.contextStoreService = contextStoreService;
        this.workspaceContextStoreRepository = workspaceContextStoreRepository;
    }

    @Override
    public WorkspaceContextStore create(Long contextStoreId, Long workspaceId) {
        return workspaceContextStoreRepository.save(new WorkspaceContextStore(contextStoreId, workspaceId));
    }

    @Override
    public void deleteByContextStoreId(Long contextStoreId) {
        workspaceContextStoreRepository.findByContextStoreId(contextStoreId)
            .ifPresent(relation -> workspaceContextStoreRepository.deleteById(relation.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextStore> getAllStoresByWorkspaceId(Long workspaceId) {
        List<Long> storeIds = workspaceContextStoreRepository.findAllByWorkspaceId(workspaceId)
            .stream()
            .map(WorkspaceContextStore::getContextStoreId)
            .toList();

        return contextStoreService.getAllByIds(storeIds);
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
        return workspaceContextStoreRepository.findByContextStoreId(contextStoreId)
            .map(WorkspaceContextStore::getWorkspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isStoreInWorkspace(Long workspaceId, Long contextStoreId) {
        return workspaceContextStoreRepository.findByWorkspaceIdAndContextStoreId(workspaceId, contextStoreId)
            .isPresent();
    }
}
