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

package com.bytechef.automation.knowledgebase.facade;

import com.bytechef.automation.knowledgebase.domain.KnowledgeBase;
import java.util.List;

/**
 * Defines the interface for managing and retrieving knowledge bases associated with specific workspaces. This facade
 * abstracts the details of underlying services and provides a streamlined way to access knowledge base information tied
 * to a workspace.
 *
 * @author Ivica Cardic
 */
public interface WorkspaceKnowledgeBaseFacade {

    /**
     * Retrieves a list of knowledge bases associated with the specified workspace.
     *
     * @param workspaceId the unique identifier of the workspace for which the knowledge bases are to be retrieved
     * @return a list of {@code KnowledgeBase} objects associated with the workspace
     */
    List<KnowledgeBase> getWorkspaceKnowledgeBases(Long workspaceId);

    /**
     * Creates a new knowledge base and assigns it to the specified workspace.
     *
     * @param knowledgeBase the knowledge base to create
     * @param workspaceId   the workspace ID to assign the knowledge base to
     * @return the created knowledge base
     */
    KnowledgeBase createWorkspaceKnowledgeBase(KnowledgeBase knowledgeBase, Long workspaceId);

    /**
     * Deletes a knowledge base and removes it from all workspaces.
     *
     * @param knowledgeBaseId the ID of the knowledge base to delete
     */
    void deleteWorkspaceKnowledgeBase(Long knowledgeBaseId);
}
