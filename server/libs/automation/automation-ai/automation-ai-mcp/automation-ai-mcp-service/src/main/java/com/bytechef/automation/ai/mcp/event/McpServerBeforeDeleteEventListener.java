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

package com.bytechef.automation.ai.mcp.event;

import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.platform.mcp.domain.McpServer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.data.relational.core.mapping.event.AbstractRelationalEventListener;
import org.springframework.data.relational.core.mapping.event.BeforeDeleteEvent;
import org.springframework.data.relational.core.mapping.event.Identifier;
import org.springframework.stereotype.Component;

/**
 * Event listener that handles before-delete events for {@link McpServer} entities. This listener is responsible for
 * cleaning up related MCP project data and the server's workspace assignment.
 *
 * @author Ivica Cardic
 */
@Component
public class McpServerBeforeDeleteEventListener extends AbstractRelationalEventListener<McpServer> {

    private final McpProjectService mcpProjectService;
    private final McpProjectWorkflowService mcpProjectWorkflowService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final WorkspaceMcpServerService workspaceMcpServerService;

    @SuppressFBWarnings("EI")
    public McpServerBeforeDeleteEventListener(
        McpProjectService mcpProjectService, McpProjectWorkflowService mcpProjectWorkflowService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ProjectDeploymentService projectDeploymentService, ProjectDeploymentFacade projectDeploymentFacade,
        WorkspaceMcpServerService workspaceMcpServerService) {

        this.mcpProjectService = mcpProjectService;
        this.mcpProjectWorkflowService = mcpProjectWorkflowService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.workspaceMcpServerService = workspaceMcpServerService;
    }

    @Override
    protected void onBeforeDelete(BeforeDeleteEvent<McpServer> beforeDeleteEvent) {
        Identifier identifier = beforeDeleteEvent.getId();

        long mcpServerId = (Long) identifier.getValue();

        runSkippingChecks(() -> deleteMcpProjects(mcpServerId));

        workspaceMcpServerService.removeMcpServerFromWorkspace(mcpServerId);
    }

    private void deleteMcpProjects(long mcpServerId) {
        List<McpProject> mcpProjects = mcpProjectService.getMcpServerMcpProjects(mcpServerId);

        for (McpProject mcpProject : mcpProjects) {
            projectDeploymentFacade.enableProjectDeployment(mcpProject.getProjectDeploymentId(), false);

            List<McpProjectWorkflow> mcpProjectWorkflows = mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(
                mcpProject.getId());

            for (McpProjectWorkflow mcpProjectWorkflow : mcpProjectWorkflows) {
                ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowService
                    .getProjectDeploymentWorkflow(mcpProjectWorkflow.getProjectDeploymentWorkflowId());

                mcpProjectWorkflowService.delete(mcpProjectWorkflow.getId());
                projectDeploymentWorkflowService.delete(projectDeploymentWorkflow.getId());
            }

            mcpProjectService.delete(mcpProject.getId());
            projectDeploymentService.delete(mcpProject.getProjectDeploymentId());
        }
    }

    private static void runSkippingChecks(Runnable runnable) {
        try {
            AutomationAuthorizationContext.callSkippingChecks(() -> {
                runnable.run();

                return null;
            });
        } catch (RuntimeException | Error exception) {
            throw exception;
        } catch (Throwable throwable) {
            throw new IllegalStateException(throwable);
        }
    }
}
