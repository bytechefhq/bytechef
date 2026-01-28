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
import java.util.List;

/**
 * Service interface for managing workspace knowledge base relationships.
 *
 * @author Ivica Cardic
 */
public interface WorkspaceKnowledgeBaseService {

    /**
     * Gets knowledge bases filtered by workspace ID.
     *
     * @param workspaceId the workspace ID to filter by
     * @return a list of workspace knowledge base relationships in the specified workspace
     */
    List<WorkspaceKnowledgeBase> getWorkspaceKnowledgeBases(Long workspaceId);

    /**
     * Assigns a knowledge base to a workspace.
     *
     * @param knowledgeBaseId the knowledge base ID
     * @param workspaceId     the workspace ID
     */
    void assignKnowledgeBaseToWorkspace(Long knowledgeBaseId, Long workspaceId);

    /**
     * Removes a knowledge base from a workspace.
     *
     * @param knowledgeBaseId the knowledge base ID
     */
    void removeKnowledgeBaseFromWorkspace(Long knowledgeBaseId);
}
