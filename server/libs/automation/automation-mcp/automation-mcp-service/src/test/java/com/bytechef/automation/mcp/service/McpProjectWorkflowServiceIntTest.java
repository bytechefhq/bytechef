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

package com.bytechef.automation.mcp.service;

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
import java.util.Optional;
import org.apache.commons.lang3.Validate;
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
public class McpProjectWorkflowServiceIntTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private McpProjectWorkflowService mcpProjectWorkflowService;

    @Autowired
    private McpProjectWorkflowRepository mcpProjectWorkflowRepository;

    @Autowired
    private McpProjectRepository mcpProjectRepository;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private McpServerRepository mcpServerRepository;

    private McpProject mcpProject;
    private McpProject mcpProject2;
    private ProjectDeploymentWorkflow projectDeploymentWorkflow;

    @BeforeEach
    public void beforeEach() {
        McpServer mcpServer1 = new McpServer("test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT);

        mcpServer1 = mcpServerRepository.save(mcpServer1);

        Long mcpServerId = mcpServer1.getId();

        McpServer mcpServer2 = new McpServer("test-server-2", PlatformType.AUTOMATION, Environment.DEVELOPMENT);

        mcpServer2 = mcpServerRepository.save(mcpServer2);

        Long mcpServerId2 = mcpServer2.getId();

        Category category = categoryRepository.save(new Category("test-category"));
        Workspace workspace = workspaceRepository.save(new Workspace("test-workspace"));

        Project project = Project.builder()
            .categoryId(category.getId())
            .description("test-project")
            .name("test-project")
            .workspaceId(workspace.getId())
            .build();

        project = projectRepository.save(project);

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setName("test-deployment");
        projectDeployment.setDescription("test deployment");
        projectDeployment.setEnabled(true);
        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setProjectId(project.getId());
        projectDeployment.setProjectVersion(1);

        projectDeployment = projectDeploymentRepository.save(projectDeployment);

        projectDeploymentWorkflow = new ProjectDeploymentWorkflow();
        projectDeploymentWorkflow.setProjectDeploymentId(projectDeployment.getId());
        projectDeploymentWorkflow.setWorkflowId("test-workflow");
        projectDeploymentWorkflow = projectDeploymentWorkflowRepository.save(projectDeploymentWorkflow);

        mcpProject = new McpProject(projectDeployment.getId(), mcpServerId);
        mcpProject = mcpProjectRepository.save(mcpProject);

        mcpProject2 = new McpProject(projectDeployment.getId(), mcpServerId2);
        mcpProject2 = mcpProjectRepository.save(mcpProject2);
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
    public void testCreate() {
        McpProjectWorkflow mcpProjectWorkflow = getMcpProjectWorkflow();

        mcpProjectWorkflow = mcpProjectWorkflowService.create(mcpProjectWorkflow);

        assertThat(mcpProjectWorkflow)
            .hasFieldOrPropertyWithValue("mcpProjectId", mcpProject.getId())
            .hasFieldOrPropertyWithValue("projectDeploymentWorkflowId", projectDeploymentWorkflow.getId());
        assertThat(mcpProjectWorkflow.getId()).isNotNull();
    }

    @Test
    public void testCreateWithParameters() {
        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowService.create(
            mcpProject.getId(), projectDeploymentWorkflow.getId());

        assertThat(mcpProjectWorkflow)
            .hasFieldOrPropertyWithValue("mcpProjectId", mcpProject.getId())
            .hasFieldOrPropertyWithValue("projectDeploymentWorkflowId", projectDeploymentWorkflow.getId());
        assertThat(mcpProjectWorkflow.getId()).isNotNull();
    }

    @Test
    public void testUpdate() {
        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowRepository.save(getMcpProjectWorkflow());

        Long newMcpProjectId = mcpProject2.getId();
        mcpProjectWorkflow.setMcpProjectId(newMcpProjectId);

        mcpProjectWorkflow = mcpProjectWorkflowService.update(mcpProjectWorkflow);

        assertThat(mcpProjectWorkflow)
            .hasFieldOrPropertyWithValue("mcpProjectId", newMcpProjectId)
            .hasFieldOrPropertyWithValue("projectDeploymentWorkflowId", projectDeploymentWorkflow.getId());
    }

    @Test
    public void testUpdateWithParameters() {
        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowRepository.save(getMcpProjectWorkflow());

        Long newMcpProjectId = mcpProject2.getId();
        mcpProjectWorkflow = mcpProjectWorkflowService.update(
            mcpProjectWorkflow.getId(), newMcpProjectId, null);

        assertThat(mcpProjectWorkflow)
            .hasFieldOrPropertyWithValue("mcpProjectId", newMcpProjectId)
            .hasFieldOrPropertyWithValue("projectDeploymentWorkflowId", projectDeploymentWorkflow.getId());
    }

    @Test
    public void testDelete() {
        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowRepository.save(getMcpProjectWorkflow());

        mcpProjectWorkflowService.delete(Validate.notNull(mcpProjectWorkflow.getId(), "id"));

        assertThat(mcpProjectWorkflowRepository.findById(mcpProjectWorkflow.getId()))
            .isNotPresent();
    }

    @Test
    public void testFetchMcpProjectWorkflow() {
        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowRepository.save(getMcpProjectWorkflow());

        Optional<McpProjectWorkflow> fetchedWorkflow = mcpProjectWorkflowService.fetchMcpProjectWorkflow(
            Validate.notNull(mcpProjectWorkflow.getId(), "id"));

        assertThat(fetchedWorkflow).isPresent();
        assertThat(fetchedWorkflow.get()).isEqualTo(mcpProjectWorkflow);
    }

    @Test
    public void testGetMcpProjectWorkflows() {
        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowRepository.save(getMcpProjectWorkflow());

        assertThat(mcpProjectWorkflowService.getMcpProjectWorkflows()).hasSize(1);
        assertThat(mcpProjectWorkflowService.getMcpProjectWorkflows()
            .get(0)).isEqualTo(mcpProjectWorkflow);
    }

    @Test
    public void testGetMcpProjectMcpProjectWorkflows() {
        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowRepository.save(getMcpProjectWorkflow());

        assertThat(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(mcpProject.getId())).hasSize(1);
        assertThat(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(mcpProject.getId())
            .get(0))
                .isEqualTo(mcpProjectWorkflow);

        // Test with non-existing project
        assertThat(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(Long.MAX_VALUE)).hasSize(0);
    }

    @Test
    public void testGetProjectDeploymentWorkflowMcpProjectWorkflows() {
        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowRepository.save(getMcpProjectWorkflow());

        assertThat(mcpProjectWorkflowService.getProjectDeploymentWorkflowMcpProjectWorkflows(
            projectDeploymentWorkflow.getId())).hasSize(1);
        assertThat(mcpProjectWorkflowService.getProjectDeploymentWorkflowMcpProjectWorkflows(
            projectDeploymentWorkflow.getId())
            .get(0)).isEqualTo(mcpProjectWorkflow);

        // Test with non-existing workflow
        assertThat(mcpProjectWorkflowService.getProjectDeploymentWorkflowMcpProjectWorkflows(Long.MAX_VALUE))
            .hasSize(0);
    }

    private McpProjectWorkflow getMcpProjectWorkflow() {
        return new McpProjectWorkflow(mcpProject.getId(), projectDeploymentWorkflow.getId());
    }
}
