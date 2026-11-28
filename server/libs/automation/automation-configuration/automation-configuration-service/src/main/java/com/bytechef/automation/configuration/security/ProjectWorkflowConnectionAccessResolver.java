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

package com.bytechef.automation.configuration.security;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.platform.configuration.security.WorkflowConnectionAccessResolver;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ProjectWorkflowConnectionAccessResolver implements WorkflowConnectionAccessResolver {

    private final ConnectionService connectionService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkspaceConnectionService workspaceConnectionService;

    @SuppressFBWarnings("EI")
    public ProjectWorkflowConnectionAccessResolver(
        ConnectionService connectionService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, WorkspaceConnectionService workspaceConnectionService) {

        this.connectionService = connectionService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.workspaceConnectionService = workspaceConnectionService;
    }

    @Override
    public ConnectionAccess resolveConnectionAccess(String workflowId, long connectionId, long environmentId) {
        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getWorkflowProjectWorkflows(
            List.of(workflowId));

        if (projectWorkflows.isEmpty()) {
            return ConnectionAccess.ABSTAIN;
        }

        ProjectWorkflow projectWorkflow = projectWorkflows.getFirst();

        Project project = projectService.getProject(projectWorkflow.getProjectId());

        List<WorkspaceConnection> workspaceConnections = workspaceConnectionService.getWorkspaceConnections(
            project.getWorkspaceId());

        boolean workspaceConnection = workspaceConnections.stream()
            .anyMatch(curWorkspaceConnection -> Objects.equals(curWorkspaceConnection.getConnectionId(), connectionId));

        if (!workspaceConnection) {
            return ConnectionAccess.DENIED;
        }

        Connection connection = connectionService.getConnection(connectionId);

        return connection.getEnvironmentId() == environmentId ? ConnectionAccess.GRANTED : ConnectionAccess.DENIED;
    }
}
