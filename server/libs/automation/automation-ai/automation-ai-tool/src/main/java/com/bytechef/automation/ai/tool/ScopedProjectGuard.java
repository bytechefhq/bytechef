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
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.exception.ErrorType;
import com.bytechef.exception.ExecutionException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Confines a run that carries a scoped project to that project, its workflows and its caller's connections, and keeps
 * such a run from creating, changing, deleting or publishing projects.
 *
 * @author Ivica Cardic
 */
@Component
public class ScopedProjectGuard {

    static final String OUT_OF_SCOPE_MESSAGE = "This chat can only work with its own project and that project's " +
        "workflows.";
    static final String PROJECT_CHANGE_MESSAGE = "This chat cannot create, change, delete or publish projects.";

    public static final String WORKFLOW_CREATE = "WORKFLOW_CREATE";
    public static final String WORKFLOW_DELETE = "WORKFLOW_DELETE";
    public static final String WORKFLOW_EDIT = "WORKFLOW_EDIT";
    public static final String WORKFLOW_VIEW = "WORKFLOW_VIEW";

    private static final Logger log = LoggerFactory.getLogger(ScopedProjectGuard.class);

    private final @Nullable PermissionService permissionService;
    private final @Nullable ProjectWorkflowService projectWorkflowService;
    private final ScopedConnectionGuard scopedConnectionGuard;

    @Autowired
    @SuppressFBWarnings("EI")
    public ScopedProjectGuard(
        ObjectProvider<PermissionService> permissionServiceProvider,
        ObjectProvider<ProjectWorkflowService> projectWorkflowServiceProvider,
        ScopedConnectionGuard scopedConnectionGuard) {

        this(
            permissionServiceProvider.getIfAvailable(), projectWorkflowServiceProvider.getIfAvailable(),
            scopedConnectionGuard);
    }

    @SuppressFBWarnings("EI")
    public ScopedProjectGuard(
        @Nullable PermissionService permissionService, @Nullable ProjectWorkflowService projectWorkflowService,
        ScopedConnectionGuard scopedConnectionGuard) {

        this.permissionService = permissionService;
        this.projectWorkflowService = projectWorkflowService;
        this.scopedConnectionGuard = scopedConnectionGuard;
    }

    public void checkWorkflow(
        String workflowId, String scope, @Nullable ToolContext toolContext, ErrorType errorType) {

        Long scopedProjectId = ScopedProjectToolContext.fetchScopedProjectId(toolContext);

        if (scopedProjectId == null) {
            if (!AutomationAuthorizationContext.isSkipChecks() &&
                (permissionService == null || !permissionService.hasWorkflowScope(workflowId, scope))) {

                log.warn("Rejected workflow {}: the caller lacks the {} scope", workflowId, scope);

                throw new ExecutionException(getWorkflowNotAccessibleMessage(workflowId), errorType);
            }

            return;
        }

        if (projectWorkflowService == null) {
            log.error("Rejected workflow {}: no project workflow lookup is available", workflowId);

            throw new ExecutionException(getWorkflowUnverifiedMessage(workflowId), errorType);
        }

        ProjectWorkflow projectWorkflow;

        try {
            projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new ExecutionException(getWorkflowNotFoundMessage(workflowId), errorType);
        } catch (RuntimeException exception) {
            log.error("Rejected workflow {}: looking up its project failed", workflowId, exception);

            throw new ExecutionException(getWorkflowUnverifiedMessage(workflowId), exception, errorType);
        }

        if (projectWorkflow.getProjectId() != scopedProjectId.longValue()) {
            log.warn("Rejected workflow {}: it is outside the scoped project {}", workflowId, scopedProjectId);

            throw new ExecutionException(getWorkflowNotFoundMessage(workflowId), errorType);
        }
    }

    public void checkProject(long projectId, String scope, @Nullable ToolContext toolContext, ErrorType errorType) {
        Long scopedProjectId = ScopedProjectToolContext.fetchScopedProjectId(toolContext);

        if (scopedProjectId == null) {
            if (!AutomationAuthorizationContext.isSkipChecks() &&
                (permissionService == null || !permissionService.hasWorkspaceScopeForProject(projectId, scope))) {

                log.warn("Rejected project {}: the caller lacks the {} scope", projectId, scope);

                throw new ExecutionException(getProjectNotAccessibleMessage(projectId), errorType);
            }

            return;
        }

        if (scopedProjectId != projectId) {
            log.warn("Rejected project {}: it is not the scoped project {}", projectId, scopedProjectId);

            throw new ExecutionException(OUT_OF_SCOPE_MESSAGE, errorType);
        }
    }

    public void checkProjectChangeAllowed(@Nullable ToolContext toolContext, ErrorType errorType) {
        if (ScopedProjectToolContext.isConfined(toolContext)) {
            log.warn("Rejected a project change from a confined run");

            throw new ExecutionException(PROJECT_CHANGE_MESSAGE, errorType);
        }
    }

    public void checkConnection(long connectionId, @Nullable ToolContext toolContext, ErrorType errorType) {
        String denialMessage = scopedConnectionGuard.fetchDenialMessage(null, connectionId, toolContext);

        if (denialMessage != null) {
            throw new ExecutionException(denialMessage, errorType);
        }
    }

    public @Nullable Long fetchScopedProjectId(@Nullable ToolContext toolContext) {
        return ScopedProjectToolContext.fetchScopedProjectId(toolContext);
    }

    private static String getProjectNotAccessibleMessage(long projectId) {
        return "Project " + projectId + " was not found or you do not have access to it.";
    }

    private static String getWorkflowNotAccessibleMessage(String workflowId) {
        return "Workflow " + workflowId + " was not found or you do not have access to it.";
    }

    private static String getWorkflowNotFoundMessage(String workflowId) {
        return "Workflow " + workflowId + " was not found in this chat's project. Check the workflow id.";
    }

    private static String getWorkflowUnverifiedMessage(String workflowId) {
        return "This chat could not verify that workflow " + workflowId + " belongs to its project. Try again later.";
    }
}
