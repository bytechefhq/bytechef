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
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.mcp.config.McpProjectIntTestConfiguration;
import com.bytechef.automation.mcp.config.McpProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.mcp.domain.McpProject;
import com.bytechef.automation.mcp.repository.McpProjectRepository;
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
public class McpProjectServiceIntTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private McpProjectService mcpProjectService;

    @Autowired
    private McpProjectRepository mcpProjectRepository;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository1;

    private ProjectDeployment projectDeployment;
    private Long mcpServerId = 1L;
    private Long mcpServerId2;

    @BeforeEach
    public void beforeEach() {
        McpServer mcpServer1 = new McpServer("test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT);

        mcpServer1 = mcpServerRepository.save(mcpServer1);

        mcpServerId = mcpServer1.getId();

        McpServer mcpServer2 = new McpServer("test-server-2", PlatformType.AUTOMATION, Environment.DEVELOPMENT);

        mcpServer2 = mcpServerRepository.save(mcpServer2);

        mcpServerId2 = mcpServer2.getId();

        Category category = categoryRepository.save(new Category("test-category"));
        Workspace workspace = workspaceRepository.save(new Workspace("test-workspace"));

        Project project = Project.builder()
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
        mcpProjectRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
        categoryRepository.deleteAll();
        mcpServerRepository.deleteAll();
    }

    @Test
    public void testCreate() {
        McpProject mcpProject = getMcpProject();

        mcpProject = mcpProjectService.create(mcpProject);

        assertThat(mcpProject)
            .hasFieldOrPropertyWithValue("mcpServerId", mcpServerId)
            .hasFieldOrPropertyWithValue("projectDeploymentId", projectDeployment.getId());
        assertThat(mcpProject.getId()).isNotNull();
    }

    @Test
    public void testUpdate() {
        McpProject mcpProject = mcpProjectRepository.save(getMcpProject());

        mcpProject.setMcpServerId(mcpServerId2);

        mcpProject = mcpProjectService.update(mcpProject);

        assertThat(mcpProject)
            .hasFieldOrPropertyWithValue("mcpServerId", mcpServerId2)
            .hasFieldOrPropertyWithValue("projectDeploymentId", projectDeployment.getId());
    }

    @Test
    public void testDelete() {
        McpProject mcpProject = mcpProjectRepository.save(getMcpProject());

        mcpProjectService.delete(Validate.notNull(mcpProject.getId(), "id"));

        assertThat(mcpProjectRepository.findById(mcpProject.getId()))
            .isNotPresent();
    }

    @Test
    public void testFetchMcpProject() {
        McpProject mcpProject = mcpProjectRepository.save(getMcpProject());

        Optional<McpProject> fetchedMcpProject =
            mcpProjectService.fetchMcpProject(Validate.notNull(mcpProject.getId(), "id"));

        assertThat(fetchedMcpProject).isPresent();
        assertThat(fetchedMcpProject.get()).isEqualTo(mcpProject);
    }

    @Test
    public void testGetMcpProjects() {
        McpProject mcpProject = mcpProjectRepository.save(getMcpProject());

        assertThat(mcpProjectService.getMcpProjects()).hasSize(1);
        assertThat(mcpProjectService.getMcpProjects()
            .getFirst()).isEqualTo(mcpProject);
    }

    @Test
    public void testGetMcpServerMcpProjects() {
        McpProject mcpProject = mcpProjectRepository.save(getMcpProject());

        assertThat(mcpProjectService.getMcpServerMcpProjects(mcpServerId)).hasSize(1);
        assertThat(mcpProjectService.getMcpServerMcpProjects(mcpServerId)
            .getFirst()).isEqualTo(mcpProject);

        // Test with non-existing server
        assertThat(mcpProjectService.getMcpServerMcpProjects(Long.MAX_VALUE)).hasSize(0);
    }

    private McpProject getMcpProject() {
        return new McpProject(projectDeployment.getId(), mcpServerId);
    }
}
