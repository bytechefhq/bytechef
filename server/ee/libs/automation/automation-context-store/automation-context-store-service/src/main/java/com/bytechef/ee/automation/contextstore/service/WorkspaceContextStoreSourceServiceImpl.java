/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.service;

import com.bytechef.ee.automation.contextstore.domain.WorkspaceContextStoreSource;
import com.bytechef.ee.automation.contextstore.repository.WorkspaceContextStoreSourceRepository;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSourceService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements {@link WorkspaceContextStoreSourceService} on top of the workspace ↔ context store source relation table.
 * After the SPI removal this is the single workspace-aware service over the relation; the platform-CS facade no longer
 * exists, and the new {@code WorkspaceContextStoreSourceFacade} (in this same module) calls into this service directly
 * for membership lookups and registration.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
public class WorkspaceContextStoreSourceServiceImpl implements WorkspaceContextStoreSourceService {

    private final ContextStoreSourceService contextStoreSourceService;
    private final WorkspaceContextStoreSourceRepository workspaceContextStoreSourceRepository;

    @SuppressFBWarnings("EI2")
    public WorkspaceContextStoreSourceServiceImpl(
        ContextStoreSourceService contextStoreSourceService,
        WorkspaceContextStoreSourceRepository workspaceContextStoreSourceRepository) {

        this.contextStoreSourceService = contextStoreSourceService;
        this.workspaceContextStoreSourceRepository = workspaceContextStoreSourceRepository;
    }

    @Override
    public WorkspaceContextStoreSource create(Long contextStoreSourceId, Long workspaceId) {
        return workspaceContextStoreSourceRepository.save(
            new WorkspaceContextStoreSource(contextStoreSourceId, workspaceId));
    }

    @Override
    public void deleteByContextStoreSourceId(Long contextStoreSourceId) {
        workspaceContextStoreSourceRepository.findByContextStoreSourceId(contextStoreSourceId)
            .ifPresent(relation -> workspaceContextStoreSourceRepository.deleteById(relation.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextStoreSource> getAllSourcesByWorkspaceId(Long workspaceId) {
        List<Long> sourceIds = workspaceContextStoreSourceRepository.findAllByWorkspaceId(workspaceId)
            .stream()
            .map(WorkspaceContextStoreSource::getContextStoreSourceId)
            .toList();

        return contextStoreSourceService.getAllByIds(sourceIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextStoreSource> getAllSourcesByWorkspaceId(Long workspaceId, Long environmentId) {
        // environmentId is accepted for forward compatibility (see service Javadoc); the relation table does not yet
        // carry an environment column, so the lookup is workspace-only today.
        return getAllSourcesByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextStoreSource> getAllEnabledSourcesByWorkspaceId(Long workspaceId) {
        List<Long> sourceIds = workspaceContextStoreSourceRepository.findAllByWorkspaceId(workspaceId)
            .stream()
            .map(WorkspaceContextStoreSource::getContextStoreSourceId)
            .toList();

        return contextStoreSourceService.getAllEnabledByIds(sourceIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextStoreSource> getAllEnabledSourcesByWorkspaceId(Long workspaceId, Long environmentId) {
        // environmentId is accepted for forward compatibility (see service Javadoc); the relation table does not yet
        // carry an environment column, so the lookup is workspace-only today.
        return getAllEnabledSourcesByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> fetchWorkspaceIdByContextStoreSourceId(Long contextStoreSourceId) {
        return workspaceContextStoreSourceRepository.findByContextStoreSourceId(contextStoreSourceId)
            .map(WorkspaceContextStoreSource::getWorkspaceId);
    }
}
