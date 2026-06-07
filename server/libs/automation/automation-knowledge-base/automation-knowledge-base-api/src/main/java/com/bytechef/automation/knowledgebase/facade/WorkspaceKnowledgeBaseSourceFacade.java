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

import com.bytechef.automation.knowledgebase.dto.CreateKnowledgeBaseSourceInput;
import com.bytechef.automation.knowledgebase.dto.UpdateKnowledgeBaseSourceInput;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource;

/**
 * Facade entry point for managing Knowledge Base sources within a workspace. Each source owns an auto-generated
 * workflow of shape {@code [schedule.cronTrigger] -> [data-stream.stream(SOURCE=<component>.<reader>,
 * DESTINATION=knowledgeBase.writeAsDocument)]} that Atlas runs on its existing schedule + Worker dispatch
 * infrastructure.
 *
 * <p>
 * The facade encapsulates the workspace-aware control-plane plumbing: persisting the source row, the workflow
 * definition, the workspace-relation row, and the {@code ProjectDeploymentWorkflow} that wires the workflow into the
 * workspace's deployment surface. Workspace-internal projects/deployments are auto-created on first use and reused for
 * subsequent sources.
 *
 * <p>
 * The {@code workspaceId} argument is the authoritative scoping signal — every method takes it explicitly so the facade
 * does not have to resolve it from the source row indirectly. Callers (the GraphQL controller and the AI Hub tool
 * callbacks) read {@code workspaceId} from the chat / request context and pass it through. Mutating methods
 * additionally verify membership through the workspace-relation table and throw if the source does not belong to the
 * supplied workspace.
 *
 * @author Ivica Cardic
 */
public interface WorkspaceKnowledgeBaseSourceFacade {

    /**
     * Creates a new Knowledge Base source, registers it under the given workspace, auto-generates the source-bound
     * workflow, and triggers an initial sync.
     */
    KnowledgeBaseSource create(Long workspaceId, CreateKnowledgeBaseSourceInput input);

    /**
     * Updates a Knowledge Base source. A cadence change mutates the workflow's trigger cron parameter only; the rest of
     * the workflow definition is preserved. An enabled flag change toggles the workflow's
     * {@code ProjectDeploymentWorkflow}.
     */
    KnowledgeBaseSource update(Long workspaceId, Long sourceId, UpdateKnowledgeBaseSourceInput input);

    /**
     * Deletes a Knowledge Base source, its auto-generated workflow, the corresponding {@code
     * ProjectDeploymentWorkflow}, and the workspace-relation row. Synced documents on the target Knowledge Base are
     * preserved; the FK on {@code knowledge_base_document.source_id ON DELETE SET NULL} orphans them so they remain
     * indistinguishable from manual uploads after the source is gone.
     */
    void delete(Long workspaceId, Long sourceId);

    /**
     * Manually triggers a sync run by creating a {@code Job} against the source's workflow.
     *
     * @return the created job's ID (Atlas job, not Spring Batch job execution).
     */
    long refreshNow(Long workspaceId, Long sourceId);

    /**
     * Toggles the enabled flag on the source and the corresponding {@code ProjectDeploymentWorkflow} (which controls
     * trigger activation in the scheduler).
     */
    void setEnabled(Long workspaceId, Long sourceId, boolean enabled);
}
