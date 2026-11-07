/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.knowledgebase.service;

import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSourceStatus;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseSourceRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
class KnowledgeBaseSourceServiceImpl implements KnowledgeBaseSourceService {

    private final KnowledgeBaseSourceRepository knowledgeBaseSourceRepository;

    KnowledgeBaseSourceServiceImpl(KnowledgeBaseSourceRepository knowledgeBaseSourceRepository) {
        this.knowledgeBaseSourceRepository = knowledgeBaseSourceRepository;
    }

    @Override
    public KnowledgeBaseSource create(KnowledgeBaseSource source) {
        return knowledgeBaseSourceRepository.save(source);
    }

    @Override
    public KnowledgeBaseSource update(KnowledgeBaseSource source) {
        return knowledgeBaseSourceRepository.save(source);
    }

    @Override
    public void delete(Long id) {
        knowledgeBaseSourceRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeBaseSource get(Long id) {
        return knowledgeBaseSourceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("KnowledgeBaseSource " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<KnowledgeBaseSource> fetch(Long id) {
        return knowledgeBaseSourceRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBaseSource> getAllByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        return knowledgeBaseSourceRepository.findAllByIdIn(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBaseSource> getAllEnabledByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        return knowledgeBaseSourceRepository.findAllByIdInAndEnabled(ids, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBaseSource> getAllByWorkspaceId(long workspaceId) {
        return knowledgeBaseSourceRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBaseSource> getAllEnabledByWorkspaceId(long workspaceId) {
        return knowledgeBaseSourceRepository.findAllByWorkspaceIdAndEnabled(workspaceId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBaseSource> findAllByKnowledgeBaseId(Long knowledgeBaseId) {
        return knowledgeBaseSourceRepository.findAllByKnowledgeBaseId(knowledgeBaseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBaseSource> findAllByWorkflowId(String workflowId) {
        return knowledgeBaseSourceRepository.findAllByWorkflowId(workflowId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBaseSource> findAllActiveAcrossKnowledgeBases() {
        return knowledgeBaseSourceRepository.findAllByEnabled(true);
    }

    @Override
    public void updateStatus(
        Long id, KnowledgeBaseSourceStatus status, @Nullable Instant lastSyncRunAt, @Nullable Long jobExecutionId) {

        KnowledgeBaseSource source = get(id);

        source.setStatus(status);

        if (lastSyncRunAt != null) {
            source.setLastSyncRunAt(lastSyncRunAt);
        }

        if (jobExecutionId != null) {
            source.setLastSyncJobExecutionId(jobExecutionId);
        }

        knowledgeBaseSourceRepository.save(source);
    }

    @Override
    public void updateLastSyncMetadata(Long id, @Nullable Instant lastSyncRunAt, @Nullable Long jobExecutionId) {
        KnowledgeBaseSource source = get(id);

        if (lastSyncRunAt != null) {
            source.setLastSyncRunAt(lastSyncRunAt);
        }

        if (jobExecutionId != null) {
            source.setLastSyncJobExecutionId(jobExecutionId);
        }

        knowledgeBaseSourceRepository.save(source);
    }

    @Override
    public void setEnabled(Long id, boolean enabled) {
        KnowledgeBaseSource source = get(id);

        source.setEnabled(enabled);

        knowledgeBaseSourceRepository.save(source);
    }
}
