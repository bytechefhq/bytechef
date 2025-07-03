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

package com.bytechef.automation.configuration.event;

import com.bytechef.automation.configuration.domain.McpProject;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.McpProjectService;
import com.bytechef.platform.configuration.domain.McpServer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.data.relational.core.mapping.event.AbstractRelationalEventListener;
import org.springframework.data.relational.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;

/**
 * Event listener that triggers actions after an MCP server is saved. Specifically, it is responsible for managing
 * project deployment triggers based on the state of the saved MCP server entity.
 *
 * This class extends the generic {@link AbstractRelationalEventListener}, allowing it to listen to and process
 * {@link AfterSaveEvent} events for entities of type {@link McpServer}.
 *
 * The primary function of this listener is to enable or disable associated project deployments based on the saved MCP
 * server's properties.
 *
 * @author Ivica Cardic
 */
@Component
public class McpServerAfterSaveEventListener extends AbstractRelationalEventListener<McpServer> {

    private final McpProjectService mcpProjectService;
    private final ProjectDeploymentFacade projectDeploymentFacade;

    @SuppressFBWarnings("EI")
    public McpServerAfterSaveEventListener(
        McpProjectService mcpProjectService, ProjectDeploymentFacade projectDeploymentFacade) {

        this.mcpProjectService = mcpProjectService;
        this.projectDeploymentFacade = projectDeploymentFacade;
    }

    @Override
    protected void onAfterSave(AfterSaveEvent<McpServer> event) {
        McpServer mcpServer = event.getEntity();

        checkProjectDeploymentTriggers(mcpServer.getId(), mcpServer.isEnabled());
    }

    private void checkProjectDeploymentTriggers(long mcpServerId, boolean enabled) {
        List<McpProject> mcpProjects = mcpProjectService.getMcpServerMcpProjects(mcpServerId);

        for (McpProject mcpProject : mcpProjects) {
            projectDeploymentFacade.enableProjectDeployment(mcpProject.getProjectDeploymentId(), enabled);
        }
    }
}
