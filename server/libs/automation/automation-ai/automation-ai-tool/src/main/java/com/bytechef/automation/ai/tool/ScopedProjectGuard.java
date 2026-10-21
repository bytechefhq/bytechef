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

import com.bytechef.ai.copilot.tool.ScopedConnectionGuard;
import com.bytechef.ai.copilot.tool.context.ScopedProjectToolContext;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectOwnershipService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.exception.ErrorType;
import com.bytechef.exception.ExecutionException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

/**
 * Decides which projects, workflows and connections an automation tool call may touch. A run that carries a scoped
 * project is confined to that project and its caller's connections; a confined run without one is rejected; any other
 * run is limited to its resolved workspace.
 *
 * @author Ivica Cardic
 */
@Component
public class ScopedProjectGuard {

    static final String OUT_OF_SCOPE_MESSAGE = "This chat can only work with the projects and workflows of its own " +
        "project.";
    static final String PROJECT_CHANGE_MESSAGE = "This chat cannot create, change, delete or publish projects.";
    static final String UNSCOPED_MESSAGE = "This chat is not bound to a project, so it cannot work with projects or " +
        "workflows.";

    private static final Logger log = LoggerFactory.getLogger(ScopedProjectGuard.class);

    private final ProjectOwnershipService projectOwnershipService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final ScopedConnectionGuard scopedConnectionGuard;
    private final WorkspaceScopeResolver workspaceScopeResolver;

    @SuppressFBWarnings("EI")
    public ScopedProjectGuard(
        ProjectOwnershipService projectOwnershipService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, ScopedConnectionGuard scopedConnectionGuard,
        WorkspaceScopeResolver workspaceScopeResolver) {

        this.projectOwnershipService = projectOwnershipService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.scopedConnectionGuard = scopedConnectionGuard;
        this.workspaceScopeResolver = workspaceScopeResolver;
    }

    public void checkWorkflow(
        String workflowId, @Nullable Long workspaceId, @Nullable ToolContext toolContext, ErrorType errorType) {

        Long scopedProjectId = getScopedProjectIdOrNullIfUnconfined(toolContext, errorType);

        if (scopedProjectId == null) {
            ProjectWorkspaceScope.checkWorkspaceWorkflow(
                projectOwnershipService, projectService, projectWorkflowService, workflowId,
                resolveWorkspaceId(workspaceId, toolContext, errorType), errorType);

            return;
        }

        ProjectWorkflow projectWorkflow;

        try {
            projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new ExecutionException(getWorkflowNotFoundMessage(workflowId), errorType);
        } catch (RuntimeException exception) {
            log.warn("Rejected workflow {}: looking up its project failed", workflowId, exception);

            throw new ExecutionException(getWorkflowNotFoundMessage(workflowId), exception, errorType);
        }

        if (projectWorkflow.getProjectId() != scopedProjectId.longValue()) {
            log.warn("Rejected workflow {}: it is outside the scoped project {}", workflowId, scopedProjectId);

            throw new ExecutionException(getWorkflowNotFoundMessage(workflowId), errorType);
        }
    }

    public Project getProject(
        long projectId, @Nullable Long workspaceId, @Nullable ToolContext toolContext, ErrorType errorType) {

        Long scopedProjectId = getScopedProjectIdOrNullIfUnconfined(toolContext, errorType);

        if (scopedProjectId == null) {
            return ProjectWorkspaceScope.getWorkspaceProject(
                projectService, projectOwnershipService, projectId,
                resolveWorkspaceId(workspaceId, toolContext, errorType), errorType);
        }

        if (projectId != scopedProjectId) {
            log.warn("Rejected project {}: it is not the scoped project {}", projectId, scopedProjectId);

            throw new ExecutionException(OUT_OF_SCOPE_MESSAGE, errorType);
        }

        return projectService.getProject(projectId);
    }

    public Set<Long> getAccessibleProjectIds(
        @Nullable Long workspaceId, @Nullable ToolContext toolContext, ErrorType errorType) {

        Long scopedProjectId = getScopedProjectIdOrNullIfUnconfined(toolContext, errorType);

        if (scopedProjectId == null) {
            return Set.copyOf(
                projectService.getWorkspaceProjectIds(
                    resolveWorkspaceId(workspaceId, toolContext, errorType),
                    ProjectWorkspaceScope.HIDDEN_PROJECT_OWNERS));
        }

        return Set.of(scopedProjectId);
    }

    public List<Project> getAccessibleProjects(
        @Nullable Long workspaceId, @Nullable ToolContext toolContext, ErrorType errorType) {

        Long scopedProjectId = getScopedProjectIdOrNullIfUnconfined(toolContext, errorType);

        if (scopedProjectId == null) {
            return ProjectWorkspaceScope.getWorkspaceProjects(
                projectService, resolveWorkspaceId(workspaceId, toolContext, errorType));
        }

        return List.of(projectService.getProject(scopedProjectId));
    }

    public void checkProjectChangeAllowed(@Nullable ToolContext toolContext, ErrorType errorType) {
        if (ScopedProjectToolContext.isConfined(toolContext)) {
            log.warn("Rejected a project change from a confined run");

            throw new ExecutionException(PROJECT_CHANGE_MESSAGE, errorType);
        }
    }

    public long resolveWorkspaceId(@Nullable Long workspaceId, @Nullable ToolContext toolContext, ErrorType errorType) {
        if (ScopedProjectToolContext.isConfined(toolContext)) {
            log.warn("Rejected workspace resolution for a confined run");

            throw new ExecutionException(OUT_OF_SCOPE_MESSAGE, errorType);
        }

        return ProjectWorkspaceScope.resolveWorkspaceId(workspaceScopeResolver, workspaceId, toolContext, errorType);
    }

    public void checkConnection(long connectionId, @Nullable ToolContext toolContext, ErrorType errorType) {
        String denialMessage = scopedConnectionGuard.fetchDenialMessage(null, connectionId, toolContext);

        if (denialMessage != null) {
            throw new ExecutionException(denialMessage, errorType);
        }
    }

    private static @Nullable Long getScopedProjectIdOrNullIfUnconfined(
        @Nullable ToolContext toolContext, ErrorType errorType) {

        Long scopedProjectId = ScopedProjectToolContext.fetchScopedProjectId(toolContext);

        if (scopedProjectId == null && ScopedProjectToolContext.isConfined(toolContext)) {
            log.warn("Rejected a tool call: the run skips automation authorization but carries no scoped project");

            throw new ExecutionException(UNSCOPED_MESSAGE, errorType);
        }

        return scopedProjectId;
    }

    private static String getWorkflowNotFoundMessage(String workflowId) {
        return "Workflow " + workflowId + " was not found in this chat's project. Check the workflow id.";
    }
}
