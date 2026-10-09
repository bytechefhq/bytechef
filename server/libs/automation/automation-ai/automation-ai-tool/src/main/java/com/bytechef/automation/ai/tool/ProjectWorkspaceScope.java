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

package com.bytechef.automation.ai.tool;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.exception.ErrorType;
import com.bytechef.exception.ExecutionException;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @author Ivica Cardic
 */
final class ProjectWorkspaceScope {

    static final String WORKSPACE_ID_DESCRIPTION =
        "The workspace ID. Optional when the account has exactly one workspace or the conversation is already " +
            "scoped to a workspace; otherwise required — a workspace_required error lists the candidates.";

    private ProjectWorkspaceScope() {
    }

    static long resolveWorkspaceId(
        WorkspaceScopeResolver workspaceScopeResolver, @Nullable Long workspaceId, @Nullable ToolContext toolContext,
        ErrorType errorType) {

        WorkspaceScopeResolver.WorkspaceScope workspaceScope =
            workspaceScopeResolver.resolveWorkspace(workspaceId, toolContext);

        if (workspaceScope instanceof WorkspaceScopeResolver.Rejected rejected) {
            throw new ExecutionException(rejected.response(), errorType);
        }

        return ((WorkspaceScopeResolver.Resolved) workspaceScope).workspaceId();
    }

    static Project getWorkspaceProject(
        ProjectService projectService, long projectId, long workspaceId, ErrorType errorType) {

        Project project = projectService.fetchProject(projectId)
            .orElse(null);

        if (project == null || !Objects.equals(project.getWorkspaceId(), workspaceId)) {
            throw new ExecutionException(
                "Project " + projectId + " not found in workspace " + workspaceId, errorType);
        }

        return project;
    }

    static List<Project> getWorkspaceProjects(ProjectService projectService, long workspaceId) {
        return projectService.getProjects(projectService.getWorkspaceProjectIds(workspaceId));
    }

    static void checkWorkspaceWorkflow(
        ProjectService projectService, ProjectWorkflowService projectWorkflowService, String workflowId,
        long workspaceId, ErrorType errorType) {

        ProjectWorkflow projectWorkflow;

        try {
            projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new ExecutionException(
                "Workflow " + workflowId + " not found in workspace " + workspaceId, illegalArgumentException,
                errorType);
        }

        Project project = projectService.fetchProject(projectWorkflow.getProjectId())
            .orElse(null);

        if (project == null || !Objects.equals(project.getWorkspaceId(), workspaceId)) {
            throw new ExecutionException("Workflow " + workflowId + " not found in workspace " + workspaceId,
                errorType);
        }
    }
}
