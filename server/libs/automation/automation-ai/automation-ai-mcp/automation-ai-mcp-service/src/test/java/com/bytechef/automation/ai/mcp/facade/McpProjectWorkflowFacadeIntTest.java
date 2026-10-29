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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.SourceType;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.mcp.config.McpMethodSecurityTestConfiguration;
import com.bytechef.automation.ai.mcp.config.McpProjectIntTestConfiguration;
import com.bytechef.automation.ai.mcp.config.McpProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.dto.McpProjectWorkflowSummaryDTO;
import com.bytechef.automation.ai.mcp.repository.McpProjectRepository;
import com.bytechef.automation.ai.mcp.repository.McpProjectWorkflowRepository;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = McpProjectIntTestConfiguration.class,
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import({
    McpMethodSecurityTestConfiguration.class, PostgreSQLContainerConfiguration.class
})
@McpProjectIntTestConfigurationSharedMocks
@WithMockUser
class McpProjectWorkflowFacadeIntTest {

    private static final String WORKFLOW_DEFINITION = """
        {"label": "Weather Lookup", "triggers": [], "tasks": []}""";

    @Autowired
    private McpProjectRepository mcpProjectRepository;

    @Autowired
    private McpProjectWorkflowFacade mcpProjectWorkflowFacade;

    @Autowired
    private McpProjectWorkflowRepository mcpProjectWorkflowRepository;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @Autowired
    private PermissionEvaluator permissionEvaluator;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private McpProjectWorkflow mcpProjectWorkflow;
    private long mcpServerId;
    private String workflowId;

    @BeforeEach
    void beforeEach() {
        McpServer mcpServer = mcpServerRepository.save(
            new McpServer("test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT));

        mcpServerId = Validate.notNull(mcpServer.getId(), "id");

        Workspace workspace = workspaceRepository.save(new Workspace("test-workspace"));

        Project project = projectRepository.save(
            Project.builder()
                .name("test-project")
                .workspaceId(workspace.getId())
                .build());

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnabled(true);
        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setName("test-deployment");
        projectDeployment.setProjectId(project.getId());
        projectDeployment.setProjectVersion(1);

        projectDeployment = projectDeploymentRepository.save(projectDeployment);

        Workflow workflow = workflowService.create(WORKFLOW_DEFINITION, Workflow.Format.JSON, SourceType.JDBC);

        workflowId = workflow.getId();

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setProjectDeploymentId(projectDeployment.getId());
        projectDeploymentWorkflow.setWorkflowId(workflowId);

        projectDeploymentWorkflow = projectDeploymentWorkflowRepository.save(projectDeploymentWorkflow);

        McpProject mcpProject = mcpProjectRepository.save(
            new McpProject(Validate.notNull(projectDeployment.getId(), "id"), mcpServerId));

        McpProjectWorkflow newMcpProjectWorkflow = new McpProjectWorkflow(
            Validate.notNull(mcpProject.getId(), "id"), Validate.notNull(projectDeploymentWorkflow.getId(), "id"));

        newMcpProjectWorkflow.setParameters(
            Map.of("toolName", "old_name", "city", "fromAi('city', 'STRING', {required: true})", "unit", "C"));

        mcpProjectWorkflow = mcpProjectWorkflowRepository.save(newMcpProjectWorkflow);
    }

    @AfterEach
    void afterEach() {
        reset(permissionEvaluator);

        mcpProjectWorkflowRepository.deleteAll();
        mcpProjectRepository.deleteAll();
        projectDeploymentWorkflowRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
        mcpServerRepository.deleteAll();

        workflowService.delete(workflowId);
    }

    @Test
    void testDeleteRequiresEditor() {
        when(permissionEvaluator.hasPermission(any(), eq(8L), eq("McpProjectWorkflow"), eq("MCP_EDIT")))
            .thenReturn(false);

        assertThatThrownBy(() -> mcpProjectWorkflowFacade.deleteMcpProjectWorkflow(8L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testGetMcpServerMcpProjectWorkflowsRequiresViewer() {
        when(permissionEvaluator.hasPermission(any(), eq(5L), eq("McpServer"), eq("MCP_VIEW")))
            .thenReturn(false);

        assertThatThrownBy(() -> mcpProjectWorkflowFacade.getMcpServerMcpProjectWorkflows(5L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testGetMcpServerMcpProjectWorkflowsReturnsNothingForAServerWithoutProjects() {
        when(permissionEvaluator.hasPermission(any(), eq(5L), eq("McpServer"), eq("MCP_VIEW")))
            .thenReturn(true);

        assertThat(mcpProjectWorkflowFacade.getMcpServerMcpProjectWorkflows(5L)).isEmpty();
    }

    @Test
    void testUpdateMcpProjectWorkflowParametersRequiresEditor() {
        when(permissionEvaluator.hasPermission(any(), eq(8L), eq("McpProjectWorkflow"), eq("MCP_EDIT")))
            .thenReturn(false);

        assertThatThrownBy(
            () -> mcpProjectWorkflowFacade.updateMcpProjectWorkflowParameters(8L, "tool", null, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testUpdateMcpProjectWorkflowParametersRejectsAnUnknownMcpProjectWorkflow() {
        when(permissionEvaluator.hasPermission(any(), eq(8L), eq("McpProjectWorkflow"), eq("MCP_EDIT")))
            .thenReturn(true);

        assertThatThrownBy(
            () -> mcpProjectWorkflowFacade.updateMcpProjectWorkflowParameters(8L, "tool", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("McpProjectWorkflow not found: 8");
    }

    @Test
    void testGetMcpServerMcpProjectWorkflowsListsTheToolMappingState() {
        when(permissionEvaluator.hasPermission(any(), eq(mcpServerId), eq("McpServer"), eq("MCP_VIEW")))
            .thenReturn(true);

        List<McpProjectWorkflowSummaryDTO> mcpProjectWorkflowSummaryDTOs =
            mcpProjectWorkflowFacade.getMcpServerMcpProjectWorkflows(mcpServerId);

        assertThat(mcpProjectWorkflowSummaryDTOs).hasSize(1);

        McpProjectWorkflowSummaryDTO mcpProjectWorkflowSummaryDTO = mcpProjectWorkflowSummaryDTOs.getFirst();

        assertThat(mcpProjectWorkflowSummaryDTO.mcpProjectWorkflowId()).isEqualTo(mcpProjectWorkflow.getId());
        assertThat(mcpProjectWorkflowSummaryDTO.workflowId()).isEqualTo(workflowId);
        assertThat(mcpProjectWorkflowSummaryDTO.workflowLabel()).isEqualTo("Weather Lookup");
        assertThat(mcpProjectWorkflowSummaryDTO.toolName()).isEqualTo("old_name");
        assertThat(mcpProjectWorkflowSummaryDTO.toolCallable()).isFalse();
    }

    @Test
    void testUpdateMcpProjectWorkflowParametersMergesIntoTheExistingParameters() {
        long mcpProjectWorkflowId = Validate.notNull(mcpProjectWorkflow.getId(), "id");

        when(permissionEvaluator.hasPermission(
            any(), eq(mcpProjectWorkflowId), eq("McpProjectWorkflow"), eq("MCP_EDIT")))
                .thenReturn(true);

        McpProjectWorkflow updatedMcpProjectWorkflow = mcpProjectWorkflowFacade.updateMcpProjectWorkflowParameters(
            mcpProjectWorkflowId, "get_weather", "Fetch weather", Map.of("unit", "F"));

        Map<String, Object> parameters = new HashMap<>(updatedMcpProjectWorkflow.getParameters());

        assertThat(parameters)
            .containsEntry("toolName", "get_weather")
            .containsEntry("toolDescription", "Fetch weather")
            .containsEntry("city", "fromAi('city', 'STRING', {required: true})")
            .containsEntry("unit", "F");
    }
}
