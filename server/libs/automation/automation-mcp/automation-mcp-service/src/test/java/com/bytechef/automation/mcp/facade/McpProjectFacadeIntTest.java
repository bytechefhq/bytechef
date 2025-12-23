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

package com.bytechef.automation.mcp.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.mcp.config.McpProjectIntTestConfiguration;
import com.bytechef.automation.mcp.config.McpProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.mcp.domain.McpProject;
import com.bytechef.automation.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.mcp.repository.McpProjectRepository;
import com.bytechef.automation.mcp.repository.McpProjectWorkflowRepository;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = McpProjectIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@McpProjectIntTestConfigurationSharedMocks
public class McpProjectFacadeIntTest {

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
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Project project;
    private ProjectDeployment projectDeployment;
    private McpServer mcpServer;

    @BeforeEach
    public void beforeEach() {
        mcpServer = mcpServerRepository.save(
            new McpServer("test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT));

        Category category = categoryRepository.save(new Category("test-category"));
        Workspace workspace = workspaceRepository.save(new Workspace("test-workspace"));

        project = Project.builder()
            .categoryId(category.getId())
            .description("test-project")
            .name("test-project")
            .workspaceId(workspace.getId())
            .build();

        project = projectRepository.save(project);

        projectDeployment = new ProjectDeployment();
        projectDeployment.setName("test-deployment");
        projectDeployment.setDescription("test deployment");
        projectDeployment.setEnabled(true);
        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setProjectId(project.getId());
        projectDeployment.setProjectVersion(1);

        projectDeployment = projectDeploymentRepository.save(projectDeployment);
    }

    @AfterEach
    public void afterEach() {
        mcpProjectWorkflowRepository.deleteAll();
        mcpProjectRepository.deleteAll();
        projectDeploymentWorkflowRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
        categoryRepository.deleteAll();
        mcpServerRepository.deleteAll();
    }

    @Test
    public void testCreateMcpProject() {
        List<String> selectedWorkflowIds = List.of("workflow1", "workflow2");

        McpProject mcpProject = mcpProjectFacade.createMcpProject(
            mcpServer.getId(), project.getId(), 1, selectedWorkflowIds);

        assertThat(mcpProject).isNotNull();
        assertThat(mcpProject.getId()).isNotNull();
        assertThat(mcpProject.getMcpServerId()).isEqualTo(mcpServer.getId());
        assertThat(mcpProject.getProjectDeploymentId()).isNotNull();
        assertThat(mcpProjectRepository.findById(mcpProject.getId())).isPresent();
    }

    @Test
    public void testCreateMcpProjectEmptyWorkflowList() {
        List<String> selectedWorkflowIds = List.of();

        McpProject mcpProject = mcpProjectFacade.createMcpProject(
            mcpServer.getId(), project.getId(), 1, selectedWorkflowIds);

        assertThat(mcpProject).isNotNull();
        assertThat(mcpProject.getId()).isNotNull();
        assertThat(mcpProject.getMcpServerId()).isEqualTo(mcpServer.getId());
        assertThat(mcpProject.getProjectDeploymentId()).isNotNull();
    }

    @Test
    public void testCreateMcpProjectMultipleVersions() {
        McpProject mcpProject1 = mcpProjectFacade.createMcpProject(
            mcpServer.getId(), project.getId(), 1, List.of("workflow1"));

        McpProject mcpProject2 = mcpProjectFacade.createMcpProject(
            mcpServer.getId(), project.getId(), 2, List.of("workflow2"));

        assertThat(mcpProject1).isNotNull();
        assertThat(mcpProject2).isNotNull();
        assertThat(mcpProject1.getId()).isNotEqualTo(mcpProject2.getId());
        assertThat(mcpProject1.getMcpServerId()).isEqualTo(mcpServer.getId());
        assertThat(mcpProject2.getMcpServerId()).isEqualTo(mcpServer.getId());
        assertThat(mcpProject1.getProjectDeploymentId()).isNotEqualTo(mcpProject2.getProjectDeploymentId());
    }

    @Test
    public void testDeleteMcpProject() {
        McpProject mcpProject = new McpProject(projectDeployment.getId(), mcpServer.getId());

        mcpProject = mcpProjectRepository.save(mcpProject);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setProjectDeploymentId(projectDeployment.getId());
        projectDeploymentWorkflow.setWorkflowId("workflow1");

        projectDeploymentWorkflow = projectDeploymentWorkflowRepository.save(projectDeploymentWorkflow);

        McpProjectWorkflow workflow1 = new McpProjectWorkflow(mcpProject.getId(), projectDeploymentWorkflow.getId());

        mcpProjectWorkflowRepository.save(workflow1);

        projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setProjectDeploymentId(projectDeployment.getId());
        projectDeploymentWorkflow.setWorkflowId("workflow2");

        projectDeploymentWorkflow = projectDeploymentWorkflowRepository.save(projectDeploymentWorkflow);

        McpProjectWorkflow workflow2 = new McpProjectWorkflow(mcpProject.getId(), projectDeploymentWorkflow.getId());

        mcpProjectWorkflowRepository.save(workflow2);

        assertThat(mcpProjectRepository.findById(mcpProject.getId())).isPresent();

        List<McpProjectWorkflow> mcpProjectWorkflows =
            mcpProjectWorkflowRepository.findAllByMcpProjectId(mcpProject.getId());
        assertThat(mcpProjectWorkflows).hasSize(2);

        mcpProjectFacade.deleteMcpProject(mcpProject.getId());

        assertThat(mcpProjectRepository.findById(mcpProject.getId())).isNotPresent();

        List<McpProjectWorkflow> remainingWorkflows =
            mcpProjectWorkflowRepository.findAllByMcpProjectId(mcpProject.getId());
        assertThat(remainingWorkflows).isEmpty();
    }
}
