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

package com.bytechef.automation.ai.mcp.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.ai.mcp.config.McpProjectIntTestConfiguration;
import com.bytechef.automation.ai.mcp.config.McpProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.domain.WorkspaceMcpServer;
import com.bytechef.automation.ai.mcp.repository.McpProjectRepository;
import com.bytechef.automation.ai.mcp.repository.McpProjectWorkflowRepository;
import com.bytechef.automation.ai.mcp.repository.WorkspaceMcpServerRepository;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
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
class McpProjectWorkflowOwnershipResolverIntTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private McpProjectRepository mcpProjectRepository;

    @Autowired
    private McpProjectWorkflowOwnershipResolver mcpProjectWorkflowOwnershipResolver;

    @Autowired
    private McpProjectWorkflowRepository mcpProjectWorkflowRepository;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkspaceMcpServerRepository workspaceMcpServerRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private McpProject mcpProject;
    private ProjectDeploymentWorkflow projectDeploymentWorkflow;
    private long workspaceId;

    @BeforeEach
    void beforeEach() {
        Workspace workspace = workspaceRepository.save(new Workspace("mcp-project-workflow-ownership"));

        workspaceId = workspace.getId();

        McpServer mcpServer = mcpServerRepository.save(
            new McpServer("mcp-project-workflow-ownership", PlatformType.AUTOMATION, Environment.DEVELOPMENT));

        workspaceMcpServerRepository.save(new WorkspaceMcpServer(mcpServer.getId(), workspaceId));

        Category category = categoryRepository.save(new Category("mcp-project-workflow-ownership"));

        Project project = projectRepository.save(
            Project.builder()
                .categoryId(category.getId())
                .name("mcp-project-workflow-ownership")
                .workspaceId(workspaceId)
                .build());

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnabled(true);
        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setName("mcp-project-workflow-ownership");
        projectDeployment.setProjectId(project.getId());
        projectDeployment.setProjectVersion(1);

        projectDeployment = projectDeploymentRepository.save(projectDeployment);

        ProjectDeploymentWorkflow newProjectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        newProjectDeploymentWorkflow.setProjectDeploymentId(projectDeployment.getId());
        newProjectDeploymentWorkflow.setWorkflowId("mcp-project-workflow-ownership");

        projectDeploymentWorkflow = projectDeploymentWorkflowRepository.save(newProjectDeploymentWorkflow);

        mcpProject = mcpProjectRepository.save(new McpProject(projectDeployment.getId(), mcpServer.getId()));
    }

    @AfterEach
    void afterEach() {
        mcpProjectWorkflowRepository.deleteAll();
        mcpProjectRepository.deleteAll();
        projectDeploymentWorkflowRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        projectRepository.deleteAll();
        categoryRepository.deleteAll();
        workspaceMcpServerRepository.deleteAll();
        mcpServerRepository.deleteAll();
        workspaceRepository.deleteAll();
    }

    @Test
    void testResourceType() {
        assertThat(mcpProjectWorkflowOwnershipResolver.resourceType()).isEqualTo("McpProjectWorkflow");
    }

    @Test
    void testResolvesWorkspaceViaProjectAndServer() {
        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowRepository.save(
            new McpProjectWorkflow(mcpProject.getId(), projectDeploymentWorkflow.getId()));

        ResourceOwner resourceOwner = mcpProjectWorkflowOwnershipResolver.resolveOwner(mcpProjectWorkflow.getId());

        assertThat(resourceOwner.workspaceId()).hasValue(workspaceId);
    }

    @Test
    void testUnknownIsUnknown() {
        ResourceOwner resourceOwner = mcpProjectWorkflowOwnershipResolver.resolveOwner(Long.MAX_VALUE);

        assertThat(resourceOwner.workspaceId()).isEmpty();
    }
}
