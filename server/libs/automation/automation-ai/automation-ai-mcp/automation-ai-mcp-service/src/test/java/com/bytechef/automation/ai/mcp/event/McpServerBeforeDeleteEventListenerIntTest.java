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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.mcp.config.McpIntTestWorkflows;
import com.bytechef.automation.ai.mcp.config.McpProjectIntTestConfiguration;
import com.bytechef.automation.ai.mcp.config.McpProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.WorkspaceMcpServer;
import com.bytechef.automation.ai.mcp.facade.McpProjectFacade;
import com.bytechef.automation.ai.mcp.repository.McpProjectRepository;
import com.bytechef.automation.ai.mcp.repository.McpProjectWorkflowRepository;
import com.bytechef.automation.ai.mcp.repository.WorkspaceMcpServerRepository;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = McpProjectIntTestConfiguration.class, properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
@McpProjectIntTestConfigurationSharedMocks
class McpServerBeforeDeleteEventListenerIntTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private McpProjectFacade mcpProjectFacade;

    @Autowired
    private McpProjectRepository mcpProjectRepository;

    @Autowired
    private McpProjectWorkflowRepository mcpProjectWorkflowRepository;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @Autowired
    private McpServerService mcpServerService;

    @MockitoSpyBean
    private ProjectDeploymentFacade projectDeploymentFacade;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkspaceMcpServerRepository workspaceMcpServerRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private McpServer mcpServer;
    private McpServer otherMcpServer;
    private Project project;
    private String workflowId1;
    private String workflowId2;

    @BeforeEach
    void beforeEach() {
        Category category = categoryRepository.save(new Category("test-category"));
        Workspace workspace = workspaceRepository.save(new Workspace("test-workspace"));

        mcpServer = saveMcpServer("test-server", workspace.getId());
        otherMcpServer = saveMcpServer("other-server", workspace.getId());

        Project newProject = Project.builder()
            .categoryId(category.getId())
            .description("test-project")
            .name("test-project")
            .workspaceId(workspace.getId())
            .build();

        newProject.publish("v1");

        project = projectRepository.save(newProject);

        workflowId1 = McpIntTestWorkflows.createNewWorkflowCallWorkflow(workflowService);
        workflowId2 = McpIntTestWorkflows.createNewWorkflowCallWorkflow(workflowService);

        projectWorkflowRepository.saveAll(
            List.of(
                new ProjectWorkflow(project.getId(), 1, workflowId1, UUID.randomUUID()),
                new ProjectWorkflow(project.getId(), 1, workflowId2, UUID.randomUUID())));
    }

    @AfterEach
    void afterEach() {
        mcpProjectWorkflowRepository.deleteAll();
        mcpProjectRepository.deleteAll();
        workspaceMcpServerRepository.deleteAll();
        projectDeploymentWorkflowRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        projectWorkflowRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
        categoryRepository.deleteAll();
        mcpServerRepository.deleteAll();

        McpIntTestWorkflows.deleteWorkflows(workflowService, List.of(workflowId1, workflowId2));
    }

    @Test
    void testDeleteMcpServerDeletesTheSystemDeploymentOfEachOfItsMcpProjects() {
        McpProject mcpProject1 = mcpProjectFacade.createMcpProject(
            mcpServer.getId(), project.getId(), 1, List.of(workflowId1));
        McpProject mcpProject2 = mcpProjectFacade.createMcpProject(
            mcpServer.getId(), project.getId(), 1, List.of(workflowId2));
        McpProject otherMcpProject = mcpProjectFacade.createMcpProject(
            otherMcpServer.getId(), project.getId(), 1, List.of(workflowId1));

        long projectDeploymentId1 = mcpProject1.getProjectDeploymentId();
        long projectDeploymentId2 = mcpProject2.getProjectDeploymentId();
        long otherProjectDeploymentId = otherMcpProject.getProjectDeploymentId();

        deleteMcpServer(mcpServer);

        verify(projectDeploymentFacade).deleteProjectDeployment(projectDeploymentId1);
        verify(projectDeploymentFacade).deleteProjectDeployment(projectDeploymentId2);
        verify(projectDeploymentFacade, never()).deleteProjectDeployment(otherProjectDeploymentId);

        assertThat(mcpServerRepository.findById(mcpServer.getId())).isEmpty();
        assertThat(mcpProjectRepository.findById(mcpProject1.getId())).isEmpty();
        assertThat(mcpProjectRepository.findById(mcpProject2.getId())).isEmpty();
        assertThat(projectDeploymentRepository.findById(projectDeploymentId1)).isEmpty();
        assertThat(projectDeploymentRepository.findById(projectDeploymentId2)).isEmpty();
        assertThat(mcpProjectRepository.findById(otherMcpProject.getId())).isPresent();
        assertThat(projectDeploymentRepository.findById(otherProjectDeploymentId)).isPresent();
        assertThat(mcpProjectWorkflowRepository.findAllByMcpProjectId(otherMcpProject.getId())).hasSize(1);
    }

    @Test
    void testDeleteMcpServerWithoutMcpProjectsDeletesNoDeployment() {
        deleteMcpServer(mcpServer);

        verify(projectDeploymentFacade, never()).deleteProjectDeployment(anyLong());

        assertThat(mcpServerRepository.findById(mcpServer.getId())).isEmpty();
    }

    private void deleteMcpServer(McpServer deletedMcpServer) {
        workspaceMcpServerRepository.deleteAll(
            workspaceMcpServerRepository.findByMcpServerId(deletedMcpServer.getId()));

        mcpServerService.delete(deletedMcpServer.getId());
    }

    private McpServer saveMcpServer(String name, Long workspaceId) {
        McpServer savedMcpServer = mcpServerRepository.save(
            new McpServer(name, PlatformType.AUTOMATION, Environment.DEVELOPMENT));

        workspaceMcpServerRepository.save(new WorkspaceMcpServer(savedMcpServer.getId(), workspaceId));

        return savedMcpServer;
    }
}
