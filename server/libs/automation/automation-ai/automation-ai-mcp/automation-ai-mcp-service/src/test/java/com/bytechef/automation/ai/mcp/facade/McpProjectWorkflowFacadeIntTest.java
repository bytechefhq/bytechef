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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.config.McpMethodSecurityTestConfiguration;
import com.bytechef.automation.ai.mcp.config.McpProjectIntTestConfiguration;
import com.bytechef.automation.ai.mcp.config.McpProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.repository.McpProjectRepository;
import com.bytechef.automation.ai.mcp.repository.McpProjectWorkflowRepository;
import com.bytechef.automation.ai.mcp.repository.WorkspaceMcpServerRepository;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
@SpringBootTest(classes = McpProjectIntTestConfiguration.class)
@Import({
    McpMethodSecurityTestConfiguration.class, PostgreSQLContainerConfiguration.class
})
@McpProjectIntTestConfigurationSharedMocks
@WithMockUser
class McpProjectWorkflowFacadeIntTest {

    private static final String INVALID_PROJECT_DEPLOYMENT_WORKFLOW =
        "Invalid projectDeploymentWorkflowId for the given MCP project";

    @Autowired
    private McpProjectWorkflowFacade mcpProjectWorkflowFacade;

    @Autowired
    private PermissionEvaluator permissionEvaluator;

    @AfterEach
    void resetPermissionEvaluator() {
        reset(permissionEvaluator);
    }

    @Test
    void testDeleteRequiresEditor() {
        when(permissionEvaluator.hasPermission(any(), eq(8L), eq("McpProjectWorkflow"), eq("MCP_EDIT")))
            .thenReturn(false);

        assertThatThrownBy(() -> mcpProjectWorkflowFacade.deleteMcpProjectWorkflow(8L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testListingTheWorkflowsOfAnMcpProjectRequiresAViewer() {
        when(permissionEvaluator.hasPermission(any(), eq(7L), eq("McpProject"), eq("MCP_VIEW")))
            .thenReturn(false);

        assertThatThrownBy(() -> mcpProjectWorkflowFacade.getMcpProjectMcpProjectWorkflows(7L))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionEvaluator).hasPermission(any(), eq(7L), eq("McpProject"), eq("MCP_VIEW"));
    }

    @Test
    void testListingToolEligibleWorkflowsRequiresAProjectWorkflowViewer() {
        when(permissionEvaluator.hasPermission(any(), eq(5L), eq("Project"), eq("WORKFLOW_VIEW")))
            .thenReturn(false);

        assertThatThrownBy(() -> mcpProjectWorkflowFacade.getToolEligibleProjectVersionWorkflows(5L, 1))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionEvaluator).hasPermission(any(), eq(5L), eq("Project"), eq("WORKFLOW_VIEW"));
    }

    @Test
    void testGetToolEligibleWorkflowRequiresAProjectWorkflowViewer() {
        when(permissionEvaluator.hasPermission(any(), eq(5L), eq("Project"), eq("WORKFLOW_VIEW")))
            .thenReturn(false);

        assertThatThrownBy(() -> mcpProjectWorkflowFacade.getToolEligibleWorkflow(5L, "workflow1"))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionEvaluator).hasPermission(any(), eq(5L), eq("Project"), eq("WORKFLOW_VIEW"));
    }

    @Test
    void testGetToolEligibleWorkflowRejectsWorkflowOfAnotherProject() {
        assertThatThrownBy(() -> mcpProjectWorkflowFacade.getToolEligibleWorkflow(5L, "workflowOfAnotherProject"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("workflowOfAnotherProject");
    }

    @Test
    void testCreateRequiresProjectEditor() {
        when(permissionEvaluator.hasPermission(any(), eq(5L), eq("McpProject"), eq("MCP_EDIT"))).thenReturn(false);

        assertThatThrownBy(() -> mcpProjectWorkflowFacade.createMcpProjectWorkflow(5L, 6L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testUpdateRequiresEditorAndTargetProjectEditor() {
        when(permissionEvaluator.hasPermission(any(), eq(8L), eq("McpProjectWorkflow"), eq("MCP_EDIT")))
            .thenReturn(false);

        assertThatThrownBy(() -> mcpProjectWorkflowFacade.updateMcpProjectWorkflow(8L, 5L, 6L))
            .isInstanceOf(AccessDeniedException.class);

        reset(permissionEvaluator);

        when(permissionEvaluator.hasPermission(any(), eq(5L), eq("McpProject"), eq("MCP_EDIT"))).thenReturn(false);

        assertThatThrownBy(() -> mcpProjectWorkflowFacade.updateMcpProjectWorkflow(8L, 5L, 6L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Nested
    class ProjectDeploymentWorkflowOwnership {

        @Autowired
        private CategoryRepository categoryRepository;

        @Autowired
        private McpProjectRepository mcpProjectRepository;

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
        private WorkspaceMcpServerService workspaceMcpServerService;

        @Autowired
        private WorkspaceRepository workspaceRepository;

        private ProjectDeploymentWorkflow foreignProjectDeploymentWorkflow;
        private McpProject mcpProject;
        private ProjectDeploymentWorkflow projectDeploymentWorkflow;

        @BeforeEach
        void beforeEach() {
            McpServer mcpServer = mcpServerRepository.save(
                new McpServer("test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT));

            Category category = categoryRepository.save(new Category("test-category"));
            Workspace workspace = workspaceRepository.save(new Workspace("test-workspace"));

            Project project = Project.builder()
                .categoryId(category.getId())
                .description("test-project")
                .name("test-project")
                .workspaceId(workspace.getId())
                .build();

            project = projectRepository.save(project);

            workspaceMcpServerService.assignMcpServerToWorkspace(mcpServer.getId(), workspace.getId());

            ProjectDeployment projectDeployment = saveProjectDeployment(
                "test-deployment", project.getId(), Environment.DEVELOPMENT);

            projectDeploymentWorkflow = saveProjectDeploymentWorkflow(projectDeployment.getId(), "test-workflow");

            ProjectDeployment foreignProjectDeployment = saveProjectDeployment(
                "test-foreign-deployment", project.getId(), Environment.STAGING);

            foreignProjectDeploymentWorkflow = saveProjectDeploymentWorkflow(
                foreignProjectDeployment.getId(), "test-foreign-workflow");

            mcpProject = mcpProjectRepository.save(new McpProject(projectDeployment.getId(), mcpServer.getId()));
        }

        @AfterEach
        void afterEach() {
            mcpProjectWorkflowRepository.deleteAll();
            mcpProjectRepository.deleteAll();
            workspaceMcpServerRepository.deleteAll();
            projectDeploymentWorkflowRepository.deleteAll();
            projectDeploymentRepository.deleteAll();
            projectRepository.deleteAll();
            workspaceRepository.deleteAll();
            categoryRepository.deleteAll();
            mcpServerRepository.deleteAll();
        }

        @Test
        void testCreateAcceptsProjectDeploymentWorkflowOfTheMcpProjectDeployment() {
            long mcpProjectId = Validate.notNull(mcpProject.getId(), "id");
            long projectDeploymentWorkflowId = Validate.notNull(projectDeploymentWorkflow.getId(), "id");

            McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowFacade.createMcpProjectWorkflow(
                mcpProjectId, projectDeploymentWorkflowId);

            assertThat(mcpProjectWorkflow)
                .hasFieldOrPropertyWithValue("mcpProjectId", mcpProjectId)
                .hasFieldOrPropertyWithValue("projectDeploymentWorkflowId", projectDeploymentWorkflowId);
            assertThat(mcpProjectWorkflow.getId()).isNotNull();
        }

        @Test
        void testCreateRejectsProjectDeploymentWorkflowOfAnotherDeployment() {
            long mcpProjectId = Validate.notNull(mcpProject.getId(), "id");
            long foreignProjectDeploymentWorkflowId = Validate.notNull(foreignProjectDeploymentWorkflow.getId(), "id");

            assertThatThrownBy(
                () -> mcpProjectWorkflowFacade.createMcpProjectWorkflow(
                    mcpProjectId, foreignProjectDeploymentWorkflowId))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage(INVALID_PROJECT_DEPLOYMENT_WORKFLOW);

            assertThat(mcpProjectWorkflowRepository.findAll()).isEmpty();
        }

        @Test
        void testCreateRejectsUnknownProjectDeploymentWorkflowIndistinguishably() {
            long mcpProjectId = Validate.notNull(mcpProject.getId(), "id");

            assertThatThrownBy(() -> mcpProjectWorkflowFacade.createMcpProjectWorkflow(mcpProjectId, Long.MAX_VALUE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(INVALID_PROJECT_DEPLOYMENT_WORKFLOW);

            assertThat(mcpProjectWorkflowRepository.findAll()).isEmpty();
        }

        @Test
        void testUpdateRejectsProjectDeploymentWorkflowOfAnotherDeployment() {
            McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowRepository.save(
                new McpProjectWorkflow(mcpProject.getId(), projectDeploymentWorkflow.getId()));

            long mcpProjectWorkflowId = Validate.notNull(mcpProjectWorkflow.getId(), "id");
            Long foreignProjectDeploymentWorkflowId = foreignProjectDeploymentWorkflow.getId();

            assertThatThrownBy(
                () -> mcpProjectWorkflowFacade.updateMcpProjectWorkflow(
                    mcpProjectWorkflowId, null, foreignProjectDeploymentWorkflowId))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage(INVALID_PROJECT_DEPLOYMENT_WORKFLOW);

            assertThat(mcpProjectWorkflowRepository.findById(mcpProjectWorkflowId))
                .get()
                .extracting(McpProjectWorkflow::getProjectDeploymentWorkflowId)
                .isEqualTo(projectDeploymentWorkflow.getId());
        }

        @Test
        void testUpdateKeepsTheStoredProjectDeploymentWorkflowWhenOnlyTheMcpProjectChanges() {
            McpServer otherMcpServer = mcpServerRepository.save(
                new McpServer("test-server-2", PlatformType.AUTOMATION, Environment.DEVELOPMENT));

            McpProject otherMcpProject = mcpProjectRepository.save(
                new McpProject(mcpProject.getProjectDeploymentId(), otherMcpServer.getId()));

            McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowRepository.save(
                new McpProjectWorkflow(mcpProject.getId(), projectDeploymentWorkflow.getId()));

            McpProjectWorkflow updatedMcpProjectWorkflow = mcpProjectWorkflowFacade.updateMcpProjectWorkflow(
                Validate.notNull(mcpProjectWorkflow.getId(), "id"), otherMcpProject.getId(), null);

            assertThat(updatedMcpProjectWorkflow)
                .hasFieldOrPropertyWithValue("mcpProjectId", otherMcpProject.getId())
                .hasFieldOrPropertyWithValue("projectDeploymentWorkflowId", projectDeploymentWorkflow.getId());
        }

        private ProjectDeployment saveProjectDeployment(String name, Long projectId, Environment environment) {
            ProjectDeployment projectDeployment = new ProjectDeployment();

            projectDeployment.setName(name);
            projectDeployment.setDescription(name);
            projectDeployment.setEnabled(true);
            projectDeployment.setEnvironment(environment);
            projectDeployment.setProjectId(projectId);
            projectDeployment.setProjectVersion(1);

            return projectDeploymentRepository.save(projectDeployment);
        }

        private ProjectDeploymentWorkflow saveProjectDeploymentWorkflow(Long projectDeploymentId, String workflowId) {
            ProjectDeploymentWorkflow newProjectDeploymentWorkflow = new ProjectDeploymentWorkflow();

            newProjectDeploymentWorkflow.setProjectDeploymentId(projectDeploymentId);
            newProjectDeploymentWorkflow.setWorkflowId(workflowId);

            return projectDeploymentWorkflowRepository.save(newProjectDeploymentWorkflow);
        }
    }
}
