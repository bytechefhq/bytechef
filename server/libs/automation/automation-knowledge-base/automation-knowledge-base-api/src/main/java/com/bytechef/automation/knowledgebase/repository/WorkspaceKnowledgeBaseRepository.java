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

package com.bytechef.automation.knowledgebase.repository;

import com.bytechef.automation.knowledgebase.domain.WorkspaceKnowledgeBase;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * Repository interface for managing {@link WorkspaceKnowledgeBase} entities.
 *
 * @author Ivica Cardic
 */
public interface WorkspaceKnowledgeBaseRepository extends ListCrudRepository<WorkspaceKnowledgeBase, Long> {

    /**
     * Find all workspace knowledge base relationships by workspace ID.
     *
     * @param workspaceId the workspace ID
     * @return list of workspace knowledge base relationships
     */
    List<WorkspaceKnowledgeBase> findAllByWorkspaceId(Long workspaceId);

    /**
     * Find all workspace knowledge base relationships by knowledge base ID.
     *
     * @param knowledgeBaseId the knowledge base ID
     * @return list of workspace knowledge base relationships
     */
    List<WorkspaceKnowledgeBase> findByKnowledgeBaseId(Long knowledgeBaseId);

    /**
     * Find workspace knowledge base relationship by workspace ID and knowledge base ID.
     *
     * @param workspaceId     the workspace ID
     * @param knowledgeBaseId the knowledge base ID
     * @return workspace knowledge base relationship if found
     */
    WorkspaceKnowledgeBase findByWorkspaceIdAndKnowledgeBaseId(Long workspaceId, Long knowledgeBaseId);
}
