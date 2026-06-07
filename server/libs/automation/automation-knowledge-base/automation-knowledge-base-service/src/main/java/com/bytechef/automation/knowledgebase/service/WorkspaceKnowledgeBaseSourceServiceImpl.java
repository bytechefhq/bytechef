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

package com.bytechef.automation.knowledgebase.service;

import com.bytechef.automation.knowledgebase.domain.WorkspaceKnowledgeBaseSource;
import com.bytechef.automation.knowledgebase.repository.WorkspaceKnowledgeBaseSourceRepository;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseSourceService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements {@link WorkspaceKnowledgeBaseSourceService} on top of the workspace ↔ KB source relation table. Mirrors
 * the post-platform-pivot {@code WorkspaceContextStoreSourceServiceImpl} shape: relation-table CRUD here + delegation
 * to the platform-side {@link KnowledgeBaseSourceService} for the actual source rows.
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
public class WorkspaceKnowledgeBaseSourceServiceImpl implements WorkspaceKnowledgeBaseSourceService {

    private final KnowledgeBaseSourceService knowledgeBaseSourceService;
    private final WorkspaceKnowledgeBaseSourceRepository workspaceKnowledgeBaseSourceRepository;

    @SuppressFBWarnings("EI2")
    public WorkspaceKnowledgeBaseSourceServiceImpl(
        KnowledgeBaseSourceService knowledgeBaseSourceService,
        WorkspaceKnowledgeBaseSourceRepository workspaceKnowledgeBaseSourceRepository) {

        this.knowledgeBaseSourceService = knowledgeBaseSourceService;
        this.workspaceKnowledgeBaseSourceRepository = workspaceKnowledgeBaseSourceRepository;
    }

    @Override
    public WorkspaceKnowledgeBaseSource registerSourceForWorkspace(Long knowledgeBaseSourceId, Long workspaceId) {
        return workspaceKnowledgeBaseSourceRepository.save(
            new WorkspaceKnowledgeBaseSource(knowledgeBaseSourceId, workspaceId));
    }

    @Override
    public void unregisterSource(Long knowledgeBaseSourceId) {
        workspaceKnowledgeBaseSourceRepository.findByKnowledgeBaseSourceId(knowledgeBaseSourceId)
            .ifPresent(relation -> workspaceKnowledgeBaseSourceRepository.deleteById(relation.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> fetchWorkspaceIdByKnowledgeBaseSourceId(Long knowledgeBaseSourceId) {
        return workspaceKnowledgeBaseSourceRepository.findByKnowledgeBaseSourceId(knowledgeBaseSourceId)
            .map(WorkspaceKnowledgeBaseSource::getWorkspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBaseSource> getAllSourcesByWorkspaceId(Long workspaceId) {
        List<Long> sourceIds = workspaceKnowledgeBaseSourceRepository.findAllByWorkspaceId(workspaceId)
            .stream()
            .map(WorkspaceKnowledgeBaseSource::getKnowledgeBaseSourceId)
            .toList();

        return knowledgeBaseSourceService.getAllByIds(sourceIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBaseSource> getAllSourcesByWorkspaceId(Long workspaceId, Long environmentId) {
        // environmentId is accepted for forward compatibility (see service Javadoc); the relation table does not yet
        // carry an environment column, so the lookup is workspace-only today.
        return getAllSourcesByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBaseSource> getAllEnabledSourcesByWorkspaceId(Long workspaceId) {
        List<Long> sourceIds = workspaceKnowledgeBaseSourceRepository.findAllByWorkspaceId(workspaceId)
            .stream()
            .map(WorkspaceKnowledgeBaseSource::getKnowledgeBaseSourceId)
            .toList();

        return knowledgeBaseSourceService.getAllEnabledByIds(sourceIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBaseSource> getAllEnabledSourcesByWorkspaceId(Long workspaceId, Long environmentId) {
        // environmentId is accepted for forward compatibility (see service Javadoc); the relation table does not yet
        // carry an environment column, so the lookup is workspace-only today.
        return getAllEnabledSourcesByWorkspaceId(workspaceId);
    }
}
