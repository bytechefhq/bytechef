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

package com.bytechef.automation.ai.mcp.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.audit.McpProjectAuditPublisher;
import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
class McpProjectFacadeTest {

    private final McpProjectAuditPublisher mcpProjectAuditPublisher = mock(McpProjectAuditPublisher.class);
    private final McpProjectService mcpProjectService = mock(McpProjectService.class);
    private final McpProjectWorkflowService mcpProjectWorkflowService = mock(McpProjectWorkflowService.class);
    private final McpServerService mcpServerService = mock(McpServerService.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
        mock(ProjectDeploymentWorkflowService.class);

    private final McpProjectFacade mcpProjectFacade = new McpProjectFacadeImpl(
        mcpProjectAuditPublisher, mcpProjectService, mcpProjectWorkflowService, mcpServerService,
        projectDeploymentService, projectDeploymentWorkflowService);

    @Test
    void testCreateMcpProjectUsesMcpServerEnvironment() {
        when(mcpServerService.getMcpServer(1L)).thenReturn(
            new McpServer("test-server", PlatformType.AUTOMATION, Environment.PRODUCTION));
        when(projectDeploymentService.create(any(ProjectDeployment.class)))
            .thenAnswer(invocation -> {
                ProjectDeployment projectDeployment = invocation.getArgument(0);

                projectDeployment.setId(10L);

                return projectDeployment;
            });
        when(mcpProjectService.create(any(McpProject.class)))
            .thenAnswer(invocation -> {
                McpProject mcpProject = invocation.getArgument(0);

                mcpProject.setId(20L);

                return mcpProject;
            });

        mcpProjectFacade.createMcpProject(1L, 100L, 1, List.of());

        ArgumentCaptor<ProjectDeployment> projectDeploymentArgumentCaptor =
            ArgumentCaptor.forClass(ProjectDeployment.class);

        verify(projectDeploymentService).create(projectDeploymentArgumentCaptor.capture());

        ProjectDeployment projectDeployment = projectDeploymentArgumentCaptor.getValue();

        assertThat(projectDeployment.getEnvironment()).isEqualTo(Environment.PRODUCTION);
    }
}
