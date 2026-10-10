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

package com.bytechef.automation.ai.mcp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.config.McpMethodSecurityTestConfiguration;
import com.bytechef.automation.ai.mcp.config.McpProjectIntTestConfiguration;
import com.bytechef.automation.ai.mcp.config.McpProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
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
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.Map;
import java.util.Optional;
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
@Import(PostgreSQLContainerConfiguration.class)
@McpProjectIntTestConfigurationSharedMocks
class McpProjectWorkflowServiceIntTest {

    private static final String INVALID_PROJECT_DEPLOYMENT_WORKFLOW =
        "Invalid projectDeploymentWorkflowId for the given MCP project";

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

    private ProjectDeploymentWorkflow foreignProjectDeploymentWorkflow;
    private McpProject mcpProject;
    private McpProject mcpProject2;
    private ProjectDeploymentWorkflow projectDeploymentWorkflow;

    @BeforeEach
    void beforeEach() {
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

        ProjectDeployment foreignProjectDeployment = new ProjectDeployment();

        foreignProjectDeployment.setName("test-foreign-deployment");
        foreignProjectDeployment.setDescription("test foreign deployment");
        foreignProjectDeployment.setEnabled(true);
        foreignProjectDeployment.setEnvironment(Environment.STAGING);
        foreignProjectDeployment.setProjectId(project.getId());
        foreignProjectDeployment.setProjectVersion(1);

        foreignProjectDeployment = projectDeploymentRepository.save(foreignProjectDeployment);

        foreignProjectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        foreignProjectDeploymentWorkflow.setProjectDeploymentId(foreignProjectDeployment.getId());
        foreignProjectDeploymentWorkflow.setWorkflowId("test-foreign-workflow");

        foreignProjectDeploymentWorkflow = projectDeploymentWorkflowRepository.save(foreignProjectDeploymentWorkflow);

        mcpProject = new McpProject(projectDeployment.getId(), mcpServerId);
        mcpProject = mcpProjectRepository.save(mcpProject);

        mcpProject2 = new McpProject(projectDeployment.getId(), mcpServerId2);
        mcpProject2 = mcpProjectRepository.save(mcpProject2);
    }

    @AfterEach
    void afterEach() {
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
    void testCreate() {
        McpProjectWorkflow mcpProjectWorkflow = getMcpProjectWorkflow();

        mcpProjectWorkflow = mcpProjectWorkflowService.create(mcpProjectWorkflow);

        assertThat(mcpProjectWorkflow)
            .hasFieldOrPropertyWithValue("mcpProjectId", mcpProject.getId())
            .hasFieldOrPropertyWithValue("projectDeploymentWorkflowId", projectDeploymentWorkflow.getId());
        assertThat(mcpProjectWorkflow.getId()).isNotNull();
    }

    @Test
    void testCreateWithParameters() {
        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowService.create(
            mcpProject.getId(), projectDeploymentWorkflow.getId());

        assertThat(mcpProjectWorkflow)
            .hasFieldOrPropertyWithValue("mcpProjectId", mcpProject.getId())
            .hasFieldOrPropertyWithValue("projectDeploymentWorkflowId", projectDeploymentWorkflow.getId());
        assertThat(mcpProjectWorkflow.getId()).isNotNull();
    }

    @Test
    void testUpdate() {
        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowRepository.save(getMcpProjectWorkflow());

        Long newMcpProjectId = mcpProject2.getId();
        mcpProjectWorkflow.setMcpProjectId(newMcpProjectId);

        mcpProjectWorkflow = mcpProjectWorkflowService.update(mcpProjectWorkflow);

        assertThat(mcpProjectWorkflow)
            .hasFieldOrPropertyWithValue("mcpProjectId", newMcpProjectId)
            .hasFieldOrPropertyWithValue("projectDeploymentWorkflowId", projectDeploymentWorkflow.getId());
    }

    @Test
    void testUpdateWithParameters() {
        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowRepository.save(getMcpProjectWorkflow());

        Long newMcpProjectId = mcpProject2.getId();
        mcpProjectWorkflow = mcpProjectWorkflowService.update(
            mcpProjectWorkflow.getId(), newMcpProjectId, null);

        assertThat(mcpProjectWorkflow)
            .hasFieldOrPropertyWithValue("mcpProjectId", newMcpProjectId)
            .hasFieldOrPropertyWithValue("projectDeploymentWorkflowId", projectDeploymentWorkflow.getId());
    }

    @Test
    void testCreateDefaultsToEnabled() {
        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowService.create(
            mcpProject.getId(), projectDeploymentWorkflow.getId());

        long mcpProjectWorkflowId = Validate.notNull(mcpProjectWorkflow.getId(), "id");

        assertThat(mcpProjectWorkflow.isEnabled()).isTrue();
        assertThat(mcpProjectWorkflowRepository.findById(mcpProjectWorkflowId))
            .get()
            .extracting(McpProjectWorkflow::isEnabled)
            .isEqualTo(true);
    }

    @Test
    void testUpdateEnabledPersistsTheFlag() {
        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowRepository.save(getMcpProjectWorkflow());

        long mcpProjectWorkflowId = Validate.notNull(mcpProjectWorkflow.getId(), "id");

        McpProjectWorkflow disabledMcpProjectWorkflow = mcpProjectWorkflowService.updateEnabled(
            mcpProjectWorkflowId, false);

        assertThat(disabledMcpProjectWorkflow.isEnabled()).isFalse();
        assertThat(mcpProjectWorkflowRepository.findById(mcpProjectWorkflowId))
            .get()
            .extracting(McpProjectWorkflow::isEnabled)
            .isEqualTo(false);

        McpProjectWorkflow enabledMcpProjectWorkflow = mcpProjectWorkflowService.updateEnabled(
            mcpProjectWorkflowId, true);

        assertThat(enabledMcpProjectWorkflow.isEnabled()).isTrue();
        assertThat(mcpProjectWorkflowRepository.findById(mcpProjectWorkflowId))
            .get()
            .extracting(McpProjectWorkflow::isEnabled)
            .isEqualTo(true);
    }

    @Test
    void testDelete() {
        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowRepository.save(getMcpProjectWorkflow());

        mcpProjectWorkflowService.delete(Validate.notNull(mcpProjectWorkflow.getId(), "id"));

        assertThat(mcpProjectWorkflowRepository.findById(mcpProjectWorkflow.getId()))
            .isNotPresent();
    }

    @Test
    void testFetchMcpProjectWorkflow() {
        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowRepository.save(getMcpProjectWorkflow());

        Optional<McpProjectWorkflow> fetchedWorkflow = mcpProjectWorkflowService.fetchMcpProjectWorkflow(
            Validate.notNull(mcpProjectWorkflow.getId(), "id"));

        assertThat(fetchedWorkflow).isPresent();
        assertThat(fetchedWorkflow.get()).isEqualTo(mcpProjectWorkflow);
    }

    @Test
    void testGetMcpProjectMcpProjectWorkflows() {
        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowRepository.save(getMcpProjectWorkflow());

        assertThat(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(mcpProject.getId())).hasSize(1);
        assertThat(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(mcpProject.getId())
            .get(0))
                .isEqualTo(mcpProjectWorkflow);

        // Test with non-existing project
        assertThat(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void testGetProjectDeploymentWorkflowMcpProjectWorkflows() {
        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowRepository.save(getMcpProjectWorkflow());

        assertThat(mcpProjectWorkflowService.getProjectDeploymentWorkflowMcpProjectWorkflows(
            projectDeploymentWorkflow.getId())).hasSize(1);
        assertThat(mcpProjectWorkflowService.getProjectDeploymentWorkflowMcpProjectWorkflows(
            projectDeploymentWorkflow.getId())
            .get(0)).isEqualTo(mcpProjectWorkflow);

        // Test with non-existing workflow
        assertThat(mcpProjectWorkflowService.getProjectDeploymentWorkflowMcpProjectWorkflows(Long.MAX_VALUE))
            .isEmpty();
    }

    @Test
    void testCreateRejectsProjectDeploymentWorkflowOfAnotherDeployment() {
        Long mcpProjectId = mcpProject.getId();
        Long foreignProjectDeploymentWorkflowId = foreignProjectDeploymentWorkflow.getId();

        assertThatThrownBy(() -> mcpProjectWorkflowService.create(mcpProjectId, foreignProjectDeploymentWorkflowId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(INVALID_PROJECT_DEPLOYMENT_WORKFLOW);

        assertThat(mcpProjectWorkflowRepository.findAll()).isEmpty();
    }

    @Test
    void testCreateRejectsUnknownProjectDeploymentWorkflowIndistinguishably() {
        Long mcpProjectId = mcpProject.getId();

        assertThatThrownBy(() -> mcpProjectWorkflowService.create(mcpProjectId, Long.MAX_VALUE))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(INVALID_PROJECT_DEPLOYMENT_WORKFLOW);

        assertThat(mcpProjectWorkflowRepository.findAll()).isEmpty();
    }

    @Test
    void testUpdateRejectsProjectDeploymentWorkflowOfAnotherDeployment() {
        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowRepository.save(getMcpProjectWorkflow());

        long mcpProjectWorkflowId = Validate.notNull(mcpProjectWorkflow.getId(), "id");
        Long foreignProjectDeploymentWorkflowId = foreignProjectDeploymentWorkflow.getId();

        assertThatThrownBy(
            () -> mcpProjectWorkflowService.update(mcpProjectWorkflowId, null, foreignProjectDeploymentWorkflowId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(INVALID_PROJECT_DEPLOYMENT_WORKFLOW);

        assertThat(mcpProjectWorkflowRepository.findById(mcpProjectWorkflowId))
            .get()
            .extracting(McpProjectWorkflow::getProjectDeploymentWorkflowId)
            .isEqualTo(projectDeploymentWorkflow.getId());
    }

    private McpProjectWorkflow getMcpProjectWorkflow() {
        return new McpProjectWorkflow(mcpProject.getId(), projectDeploymentWorkflow.getId());
    }

    @Nested
    @Import({
        McpMethodSecurityTestConfiguration.class, PostgreSQLContainerConfiguration.class
    })
    @WithMockUser
    class MethodSecurity {

        @Autowired
        private PermissionEvaluator permissionEvaluator;

        @AfterEach
        void resetPermissionEvaluator() {
            reset(permissionEvaluator);
        }

        @Test
        void testCreateRequiresProjectEditor() {
            when(permissionEvaluator.hasPermission(any(), eq(5L), eq("McpProject"), eq("MCP_EDIT"))).thenReturn(false);

            assertThatThrownBy(() -> mcpProjectWorkflowService.create(5L, 6L))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void testCreateFromEntityRequiresProjectEditor() {
            McpProjectWorkflow mcpProjectWorkflow = new McpProjectWorkflow(5L, 6L);

            when(permissionEvaluator.hasPermission(any(), eq(5L), eq("McpProject"), eq("MCP_EDIT"))).thenReturn(false);

            assertThatThrownBy(() -> mcpProjectWorkflowService.create(mcpProjectWorkflow))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void testFetchRequiresViewer() {
            when(permissionEvaluator.hasPermission(any(), eq(8L), eq("McpProjectWorkflow"), eq("MCP_VIEW")))
                .thenReturn(false);

            assertThatThrownBy(() -> mcpProjectWorkflowService.fetchMcpProjectWorkflow(8L))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void testDeleteServiceRequiresEditor() {
            when(permissionEvaluator.hasPermission(any(), eq(8L), eq("McpProjectWorkflow"), eq("MCP_EDIT")))
                .thenReturn(false);

            assertThatThrownBy(() -> mcpProjectWorkflowService.delete(8L))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void testUpdateRequiresEditorAndTargetProjectEditor() {
            when(permissionEvaluator.hasPermission(any(), eq(8L), eq("McpProjectWorkflow"), eq("MCP_EDIT")))
                .thenReturn(false);

            assertThatThrownBy(() -> mcpProjectWorkflowService.update(8L, 5L, 6L))
                .isInstanceOf(AccessDeniedException.class);

            reset(permissionEvaluator);

            when(permissionEvaluator.hasPermission(any(), eq(5L), eq("McpProject"), eq("MCP_EDIT"))).thenReturn(false);

            assertThatThrownBy(() -> mcpProjectWorkflowService.update(8L, 5L, 6L))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void testUpdateFromEntityRequiresEditorAndTargetProjectEditor() {
            McpProjectWorkflow mcpProjectWorkflow = new McpProjectWorkflow(5L, 6L);

            mcpProjectWorkflow.setId(8L);

            when(permissionEvaluator.hasPermission(any(), eq(8L), eq("McpProjectWorkflow"), eq("MCP_EDIT")))
                .thenReturn(false);

            assertThatThrownBy(() -> mcpProjectWorkflowService.update(mcpProjectWorkflow))
                .isInstanceOf(AccessDeniedException.class);

            reset(permissionEvaluator);

            when(permissionEvaluator.hasPermission(any(), eq(5L), eq("McpProject"), eq("MCP_EDIT"))).thenReturn(false);

            assertThatThrownBy(() -> mcpProjectWorkflowService.update(mcpProjectWorkflow))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void testUpdateParametersRequiresEditor() {
            when(permissionEvaluator.hasPermission(any(), eq(8L), eq("McpProjectWorkflow"), eq("MCP_EDIT")))
                .thenReturn(false);

            assertThatThrownBy(() -> mcpProjectWorkflowService.updateParameters(8L, Map.of()))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void testUpdateEnabledRequiresEditor() {
            when(permissionEvaluator.hasPermission(any(), eq(8L), eq("McpProjectWorkflow"), eq("MCP_EDIT")))
                .thenReturn(false);

            assertThatThrownBy(() -> mcpProjectWorkflowService.updateEnabled(8L, false))
                .isInstanceOf(AccessDeniedException.class);
        }
    }
}
