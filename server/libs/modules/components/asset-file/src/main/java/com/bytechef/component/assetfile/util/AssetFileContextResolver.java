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

package com.bytechef.component.assetfile.util;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.component.definition.ActionContextAware;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Ivica Cardic
 */
public class AssetFileContextResolver {

    private static final int DEVELOPMENT_ENVIRONMENT = 0;

    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;

    @SuppressFBWarnings("EI2")
    public AssetFileContextResolver(ProjectService projectService, ProjectWorkflowService projectWorkflowService) {
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
    }

    public long resolveWorkspaceId(ActionContext actionContext) {
        ActionContextAware actionContextAware = (ActionContextAware) actionContext;

        String workflowId = actionContextAware.getWorkflowId();

        if (workflowId == null) {
            throw new IllegalStateException(
                "Asset file actions require a workflow execution context to resolve the owning workspace");
        }

        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        Project project = projectService.getProject(projectWorkflow.getProjectId());

        Long workspaceId = project.getWorkspaceId();

        if (workspaceId == null) {
            throw new IllegalStateException(
                "Project %d of the executing workflow has no workspace".formatted(project.getId()));
        }

        return workspaceId;
    }

    public int resolveEnvironment(ActionContext actionContext) {
        ActionContextAware actionContextAware = (ActionContextAware) actionContext;

        Long environmentId = actionContextAware.getEnvironmentId();

        return environmentId == null ? DEVELOPMENT_ENVIRONMENT : environmentId.intValue();
    }
}
