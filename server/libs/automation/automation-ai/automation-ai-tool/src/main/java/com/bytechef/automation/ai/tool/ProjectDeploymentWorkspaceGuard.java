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

import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @author Ivica Cardic
 */
final class ProjectDeploymentWorkspaceGuard {

    private ProjectDeploymentWorkspaceGuard() {
    }

    static @Nullable String findWorkspaceError(
        ProjectDeploymentFacade projectDeploymentFacade, @Nullable ToolContext toolContext, long projectDeploymentId) {

        AutomationToolInvocationContext invocationContext =
            AutomationToolInvocationContext.fromToolContext(toolContext);

        Long workspaceId = invocationContext == null ? null : invocationContext.workspaceId();

        if (workspaceId == null) {
            return "Workspace context unavailable - open this chat from the AI Hub of a workspace.";
        }

        List<ProjectDeploymentDTO> workspaceProjectDeployments =
            projectDeploymentFacade.getWorkspaceProjectDeployments(workspaceId, null, null, null, false);

        boolean inWorkspace = workspaceProjectDeployments.stream()
            .anyMatch(projectDeployment -> Objects.equals(projectDeployment.id(), projectDeploymentId));

        if (!inWorkspace) {
            return "Project deployment " + projectDeploymentId + " not found in the current workspace";
        }

        return null;
    }
}
