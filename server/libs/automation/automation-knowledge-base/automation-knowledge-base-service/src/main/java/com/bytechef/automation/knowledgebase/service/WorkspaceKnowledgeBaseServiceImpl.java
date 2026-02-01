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

import com.bytechef.automation.knowledgebase.domain.WorkspaceKnowledgeBase;
import com.bytechef.automation.knowledgebase.repository.WorkspaceKnowledgeBaseRepository;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link WorkspaceKnowledgeBaseService} interface.
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnProperty(prefix = "bytechef.knowledge-base", name = "enabled", havingValue = "true")
public class WorkspaceKnowledgeBaseServiceImpl implements WorkspaceKnowledgeBaseService {

    private final WorkspaceKnowledgeBaseRepository workspaceKnowledgeBaseRepository;

    public WorkspaceKnowledgeBaseServiceImpl(WorkspaceKnowledgeBaseRepository workspaceKnowledgeBaseRepository) {
        this.workspaceKnowledgeBaseRepository = workspaceKnowledgeBaseRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceKnowledgeBase> getWorkspaceKnowledgeBases(Long workspaceId) {
        return workspaceKnowledgeBaseRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    public void assignKnowledgeBaseToWorkspace(Long knowledgeBaseId, Long workspaceId) {
        WorkspaceKnowledgeBase existing = workspaceKnowledgeBaseRepository.findByWorkspaceIdAndKnowledgeBaseId(
            workspaceId, knowledgeBaseId);

        if (existing == null) {
            WorkspaceKnowledgeBase workspaceKnowledgeBase = new WorkspaceKnowledgeBase(knowledgeBaseId, workspaceId);

            workspaceKnowledgeBaseRepository.save(workspaceKnowledgeBase);
        }
    }

    @Override
    public void removeKnowledgeBaseFromWorkspace(Long knowledgeBaseId) {
        List<WorkspaceKnowledgeBase> existingRelationships =
            workspaceKnowledgeBaseRepository.findByKnowledgeBaseId(knowledgeBaseId);

        if (!existingRelationships.isEmpty()) {
            workspaceKnowledgeBaseRepository.deleteAll(existingRelationships);
        }
    }
}
