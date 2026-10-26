/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.service;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSourceStatus;
import com.bytechef.ee.platform.contextstore.repository.ContextStoreSourceRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
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
public class ContextStoreSourceServiceImpl implements ContextStoreSourceService {

    private final ContextStoreSourceRepository contextStoreSourceRepository;

    public ContextStoreSourceServiceImpl(ContextStoreSourceRepository contextStoreSourceRepository) {
        this.contextStoreSourceRepository = contextStoreSourceRepository;
    }

    @Override
    public ContextStoreSource create(ContextStoreSource source) {
        return contextStoreSourceRepository.save(source);
    }

    @Override
    public ContextStoreSource update(ContextStoreSource source) {
        return contextStoreSourceRepository.save(source);
    }

    @Override
    public void delete(Long id) {
        contextStoreSourceRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ContextStoreSource get(Long id) {
        return contextStoreSourceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("ContextStoreSource " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ContextStoreSource> fetch(Long id) {
        return contextStoreSourceRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextStoreSource> getAllByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        return contextStoreSourceRepository.findAllByIdIn(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextStoreSource> getAllEnabledByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        return contextStoreSourceRepository.findAllByIdInAndEnabled(ids, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextStoreSource> getAllByWorkspaceId(long workspaceId) {
        return contextStoreSourceRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextStoreSource> getAllEnabledByWorkspaceId(long workspaceId) {
        return contextStoreSourceRepository.findAllByWorkspaceIdAndEnabled(workspaceId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextStoreSource> findAllActiveAcrossWorkspaces() {
        return contextStoreSourceRepository.findAllByEnabled(true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextStoreSource> findAllByContextStoreId(Long contextStoreId) {
        return contextStoreSourceRepository.findAllByContextStoreId(contextStoreId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextStoreSource> findAllByWorkflowId(String workflowId) {
        return contextStoreSourceRepository.findAllByWorkflowId(workflowId);
    }

    @Override
    public void updateStatus(
        Long id, ContextStoreSourceStatus status, @Nullable Instant lastSyncRunAt, @Nullable Long jobExecutionId) {

        ContextStoreSource source = get(id);

        source.setStatus(status);

        if (lastSyncRunAt != null) {
            source.setLastSyncRunAt(lastSyncRunAt);
        }

        if (jobExecutionId != null) {
            source.setLastSyncJobExecutionId(jobExecutionId);
        }

        contextStoreSourceRepository.save(source);
    }

    @Override
    public void updateLastSyncMetadata(
        Long id, @Nullable Instant lastSyncRunAt, @Nullable Long jobExecutionId) {

        ContextStoreSource source = get(id);

        if (lastSyncRunAt != null) {
            source.setLastSyncRunAt(lastSyncRunAt);
        }

        if (jobExecutionId != null) {
            source.setLastSyncJobExecutionId(jobExecutionId);
        }

        contextStoreSourceRepository.save(source);
    }

    @Override
    public void setEnabled(Long id, boolean enabled) {
        ContextStoreSource source = get(id);

        source.setEnabled(enabled);

        contextStoreSourceRepository.save(source);
    }
}
