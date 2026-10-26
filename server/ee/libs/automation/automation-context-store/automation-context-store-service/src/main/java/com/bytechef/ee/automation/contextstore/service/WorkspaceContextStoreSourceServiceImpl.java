/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.service;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSourceService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
public class WorkspaceContextStoreSourceServiceImpl implements WorkspaceContextStoreSourceService {

    private final ContextStoreSourceService contextStoreSourceService;

    @SuppressFBWarnings("EI2")
    public WorkspaceContextStoreSourceServiceImpl(ContextStoreSourceService contextStoreSourceService) {
        this.contextStoreSourceService = contextStoreSourceService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextStoreSource> getAllSourcesByWorkspaceId(Long workspaceId) {
        return contextStoreSourceService.getAllByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextStoreSource> getAllSourcesByWorkspaceId(Long workspaceId, Long environmentId) {
        return getAllSourcesByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextStoreSource> getAllEnabledSourcesByWorkspaceId(Long workspaceId) {
        return contextStoreSourceService.getAllEnabledByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextStoreSource> getAllEnabledSourcesByWorkspaceId(Long workspaceId, Long environmentId) {
        return getAllEnabledSourcesByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> fetchWorkspaceIdByContextStoreSourceId(Long contextStoreSourceId) {
        return contextStoreSourceService.fetch(contextStoreSourceId)
            .map(ContextStoreSource::getWorkspaceId);
    }
}
