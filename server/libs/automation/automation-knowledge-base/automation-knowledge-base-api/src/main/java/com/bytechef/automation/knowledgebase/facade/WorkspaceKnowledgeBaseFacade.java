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

import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Defines the interface for managing and retrieving knowledge bases associated with specific workspaces. This facade
 * abstracts the details of underlying services and provides a streamlined way to access knowledge base information tied
 * to a workspace.
 *
 * @author Ivica Cardic
 */
public interface WorkspaceKnowledgeBaseFacade {

    /**
     * Retrieves a list of knowledge bases associated with the specified workspace and environment.
     *
     * @param workspaceId   the unique identifier of the workspace
     * @param environmentId the environment ordinal
     *                      ({@link com.bytechef.platform.configuration.domain.Environment#ordinal()}) to filter by
     * @return a list of {@code KnowledgeBase} objects associated with the workspace and environment
     */
    List<KnowledgeBase> getWorkspaceKnowledgeBases(Long workspaceId, long environmentId);

    /**
     * Creates a new knowledge base and assigns it to the specified workspace.
     *
     * @param knowledgeBase the knowledge base to create
     * @param workspaceId   the workspace ID to assign the knowledge base to
     * @param environmentId the environment ordinal
     *                      ({@link com.bytechef.platform.configuration.domain.Environment#ordinal()})
     * @return the created knowledge base
     */
    KnowledgeBase createWorkspaceKnowledgeBase(KnowledgeBase knowledgeBase, Long workspaceId, long environmentId);

    /**
     * Deletes a knowledge base and removes it from all workspaces.
     *
     * @param knowledgeBaseId the ID of the knowledge base to delete
     */
    void deleteWorkspaceKnowledgeBase(Long knowledgeBaseId);

    /**
     * Clones an existing knowledge base into the target environment within the same workspace, copying the chunking
     * configuration (max chunk size, min chunk size chars, overlap), description, and name. Documents are
     * <strong>not</strong> copied — the destination knowledge base starts empty so the caller can re-ingest documents
     * deliberately rather than having async embed jobs silently kicked off as a side effect of the clone.
     *
     * <p>
     * Same-workspace, cross-environment scope: the {@code workspaceId} carries over. The source knowledge base must
     * belong to the supplied workspace; if not, an {@link IllegalArgumentException} is thrown so a forged knowledge
     * base id from another workspace cannot bypass the boundary.
     * </p>
     *
     * @param knowledgeBaseId     id of the source knowledge base
     * @param workspaceId         owning workspace — verified against the source for defense in depth
     * @param targetEnvironmentId target environment ordinal
     *                            ({@link com.bytechef.platform.configuration.domain.Environment#ordinal()})
     * @param newName             optional name override for the clone; falls back to the source name when null
     * @return the persisted clone
     */
    KnowledgeBase cloneWorkspaceKnowledgeBase(
        Long knowledgeBaseId, Long workspaceId, long targetEnvironmentId, @Nullable String newName);
}
