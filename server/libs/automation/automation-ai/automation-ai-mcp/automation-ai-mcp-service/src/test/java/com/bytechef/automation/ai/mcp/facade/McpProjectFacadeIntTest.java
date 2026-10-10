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
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
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
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.UUID;
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
class McpProjectFacadeIntTest {

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
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Project project;
    private ProjectDeployment projectDeployment;
    private McpServer mcpServer;

    @BeforeEach
    void beforeEach() {
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

        project.publish("v1");
        project.publish("v2");

        project = projectRepository.save(project);

        projectWorkflowRepository.saveAll(
            List.of(
                new ProjectWorkflow(project.getId(), 1, "workflow1", UUID.randomUUID()),
                new ProjectWorkflow(project.getId(), 1, "workflow2", UUID.randomUUID()),
                new ProjectWorkflow(project.getId(), 2, "workflow3", UUID.randomUUID())));

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
    void afterEach() {
        mcpProjectWorkflowRepository.deleteAll();
        mcpProjectRepository.deleteAll();
        projectDeploymentWorkflowRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        projectWorkflowRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
        categoryRepository.deleteAll();
        mcpServerRepository.deleteAll();
    }

    @Test
    void testCreateMcpProject() {
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
    void testCreateMcpProjectUsesMcpServerEnvironment() {
        McpServer productionMcpServer = mcpServerRepository.save(
            new McpServer("production-server", PlatformType.AUTOMATION, Environment.PRODUCTION));

        McpProject mcpProject = mcpProjectFacade.createMcpProject(
            productionMcpServer.getId(), project.getId(), 1, List.of("workflow1"));

        ProjectDeployment mcpProjectDeployment = projectDeploymentRepository
            .findById(mcpProject.getProjectDeploymentId())
            .orElseThrow();

        assertThat(mcpProjectDeployment.getEnvironment()).isEqualTo(Environment.PRODUCTION);
    }

    @Test
    void testCreateMcpProjectEnablesTheProjectDeployment() {
        McpProject mcpProject = mcpProjectFacade.createMcpProject(
            mcpServer.getId(), project.getId(), 1, List.of("workflow1"));

        ProjectDeployment mcpProjectDeployment = projectDeploymentRepository
            .findById(mcpProject.getProjectDeploymentId())
            .orElseThrow();

        assertThat(mcpProjectDeployment.isEnabled()).isTrue();
    }

    @Test
    void testCreateMcpProjectEmptyWorkflowList() {
        List<String> selectedWorkflowIds = List.of();

        McpProject mcpProject = mcpProjectFacade.createMcpProject(
            mcpServer.getId(), project.getId(), 1, selectedWorkflowIds);

        assertThat(mcpProject).isNotNull();
        assertThat(mcpProject.getId()).isNotNull();
        assertThat(mcpProject.getMcpServerId()).isEqualTo(mcpServer.getId());
        assertThat(mcpProject.getProjectDeploymentId()).isNotNull();
    }

    @Test
    void testCreateMcpProjectMultipleVersions() {
        McpProject mcpProject1 = mcpProjectFacade.createMcpProject(
            mcpServer.getId(), project.getId(), 1, List.of("workflow1"));

        McpProject mcpProject2 = mcpProjectFacade.createMcpProject(
            mcpServer.getId(), project.getId(), 2, List.of("workflow3"));

        assertThat(mcpProject1).isNotNull();
        assertThat(mcpProject2).isNotNull();
        assertThat(mcpProject1.getId()).isNotEqualTo(mcpProject2.getId());
        assertThat(mcpProject1.getMcpServerId()).isEqualTo(mcpServer.getId());
        assertThat(mcpProject2.getMcpServerId()).isEqualTo(mcpServer.getId());
        assertThat(mcpProject1.getProjectDeploymentId()).isNotEqualTo(mcpProject2.getProjectDeploymentId());
    }

    @Test
    void testUpdateMcpProjectAddWorkflow() {
        McpProject mcpProject = mcpProjectFacade.createMcpProject(
            mcpServer.getId(), project.getId(), 1, List.of("workflow1"));

        McpProject updatedMcpProject = mcpProjectFacade.updateMcpProject(
            mcpProject.getId(), List.of("workflow1", "workflow2"));

        assertThat(updatedMcpProject).isNotNull();
        assertThat(updatedMcpProject.getId()).isEqualTo(mcpProject.getId());

        List<McpProjectWorkflow> mcpProjectWorkflows =
            mcpProjectWorkflowRepository.findAllByMcpProjectId(mcpProject.getId());

        assertThat(mcpProjectWorkflows).hasSize(2);
    }

    @Test
    void testUpdateMcpProjectRemoveWorkflow() {
        McpProject mcpProject = mcpProjectFacade.createMcpProject(
            mcpServer.getId(), project.getId(), 1, List.of("workflow1", "workflow2"));

        List<McpProjectWorkflow> mcpProjectWorkflowsBefore =
            mcpProjectWorkflowRepository.findAllByMcpProjectId(mcpProject.getId());

        assertThat(mcpProjectWorkflowsBefore).hasSize(2);

        mcpProjectFacade.updateMcpProject(mcpProject.getId(), List.of("workflow1"));

        List<McpProjectWorkflow> mcpProjectWorkflowsAfter =
            mcpProjectWorkflowRepository.findAllByMcpProjectId(mcpProject.getId());

        assertThat(mcpProjectWorkflowsAfter).hasSize(1);
    }

    @Test
    void testUpdateMcpProjectUnchangedWorkflows() {
        McpProject mcpProject = mcpProjectFacade.createMcpProject(
            mcpServer.getId(), project.getId(), 1, List.of("workflow1", "workflow2"));

        List<McpProjectWorkflow> mcpProjectWorkflowsBefore =
            mcpProjectWorkflowRepository.findAllByMcpProjectId(mcpProject.getId());

        assertThat(mcpProjectWorkflowsBefore).hasSize(2);

        mcpProjectFacade.updateMcpProject(mcpProject.getId(), List.of("workflow1", "workflow2"));

        List<McpProjectWorkflow> mcpProjectWorkflowsAfter =
            mcpProjectWorkflowRepository.findAllByMcpProjectId(mcpProject.getId());

        assertThat(mcpProjectWorkflowsAfter).hasSize(2);
    }

    @Nested
    class WorkflowOwnership {

        @Test
        void testCreateMcpProjectRejectsAWorkflowOfAnotherProject() {
            Project otherProject = projectRepository.save(
                Project.builder()
                    .categoryId(project.getCategoryId())
                    .name("other-project")
                    .workspaceId(project.getWorkspaceId())
                    .build());

            projectWorkflowRepository.save(new ProjectWorkflow(otherProject.getId(), 1, "foreign", UUID.randomUUID()));

            long mcpProjectCount = mcpProjectRepository.count();
            long projectDeploymentCount = projectDeploymentRepository.count();
            long mcpServerId = mcpServer.getId();
            long projectId = project.getId();
            List<String> selectedWorkflowIds = List.of("workflow1", "foreign");

            assertThatThrownBy(
                () -> mcpProjectFacade.createMcpProject(mcpServerId, projectId, 1, selectedWorkflowIds))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("foreign");

            assertThat(mcpProjectRepository.count()).isEqualTo(mcpProjectCount);
            assertThat(projectDeploymentRepository.count()).isEqualTo(projectDeploymentCount);
            assertThat(projectDeploymentWorkflowRepository.count()).isZero();
        }

        @Test
        void testCreateMcpProjectRejectsAWorkflowOfAnotherProjectVersion() {
            long projectDeploymentCount = projectDeploymentRepository.count();
            long mcpServerId = mcpServer.getId();
            long projectId = project.getId();
            List<String> selectedWorkflowIds = List.of("workflow1");

            assertThatThrownBy(
                () -> mcpProjectFacade.createMcpProject(mcpServerId, projectId, 2, selectedWorkflowIds))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("workflow1");

            assertThat(mcpProjectRepository.count()).isZero();
            assertThat(projectDeploymentRepository.count()).isEqualTo(projectDeploymentCount);
        }

        @Test
        void testUpdateMcpProjectRejectsAWorkflowOfAnotherProjectVersion() {
            McpProject mcpProject = mcpProjectFacade.createMcpProject(
                mcpServer.getId(), project.getId(), 2, List.of("workflow3"));

            long projectDeploymentWorkflowCount = projectDeploymentWorkflowRepository.count();
            long mcpProjectId = mcpProject.getId();
            List<String> selectedWorkflowIds = List.of("workflow3", "workflow1");

            assertThatThrownBy(() -> mcpProjectFacade.updateMcpProject(mcpProjectId, selectedWorkflowIds))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("workflow1");

            assertThat(projectDeploymentWorkflowRepository.count()).isEqualTo(projectDeploymentWorkflowCount);
            assertThat(mcpProjectWorkflowRepository.findAllByMcpProjectId(mcpProject.getId())).hasSize(1);
        }
    }

    @Nested
    class ProjectVersionPublication {

        @Test
        void testCreateMcpProjectRejectsAnUnpublishedProject() {
            Project unpublishedProject = projectRepository.save(
                Project.builder()
                    .categoryId(project.getCategoryId())
                    .name("unpublished-project")
                    .workspaceId(project.getWorkspaceId())
                    .build());

            projectWorkflowRepository.save(
                new ProjectWorkflow(unpublishedProject.getId(), 1, "unpublished", UUID.randomUUID()));

            long projectDeploymentCount = projectDeploymentRepository.count();
            long mcpServerId = mcpServer.getId();
            long projectId = unpublishedProject.getId();
            List<String> selectedWorkflowIds = List.of("unpublished");

            assertThatThrownBy(
                () -> mcpProjectFacade.createMcpProject(mcpServerId, projectId, 1, selectedWorkflowIds))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Project " + projectId + " is not published");

            assertThat(mcpProjectRepository.count()).isZero();
            assertThat(projectDeploymentRepository.count()).isEqualTo(projectDeploymentCount);
            assertThat(projectDeploymentWorkflowRepository.count()).isZero();
        }

        @Test
        void testCreateMcpProjectRejectsTheDraftProjectVersion() {
            projectWorkflowRepository.save(new ProjectWorkflow(project.getId(), 3, "draft", UUID.randomUUID()));

            long projectDeploymentCount = projectDeploymentRepository.count();
            long mcpServerId = mcpServer.getId();
            long projectId = project.getId();
            List<String> selectedWorkflowIds = List.of("draft");

            assertThatThrownBy(
                () -> mcpProjectFacade.createMcpProject(mcpServerId, projectId, 3, selectedWorkflowIds))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Version 3 of project " + projectId + " is not published");

            assertThat(mcpProjectRepository.count()).isZero();
            assertThat(projectDeploymentRepository.count()).isEqualTo(projectDeploymentCount);
            assertThat(projectDeploymentWorkflowRepository.count()).isZero();
        }

        @Test
        void testCreateMcpProjectAcceptsAPublishedProjectVersion() {
            McpProject mcpProject = mcpProjectFacade.createMcpProject(
                mcpServer.getId(), project.getId(), 2, List.of("workflow3"));

            assertThat(mcpProjectRepository.findById(mcpProject.getId())).isPresent();
            assertThat(projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(
                mcpProject.getProjectDeploymentId()))
                    .extracting(ProjectDeploymentWorkflow::getWorkflowId)
                    .containsExactly("workflow3");
        }

        @Test
        void testCloneMcpProjectRejectsAnUnpublishedProject() {
            Project unpublishedProject = projectRepository.save(
                Project.builder()
                    .categoryId(project.getCategoryId())
                    .name("unpublished-project")
                    .workspaceId(project.getWorkspaceId())
                    .build());

            ProjectDeployment unpublishedProjectDeployment = new ProjectDeployment();

            unpublishedProjectDeployment.setEnvironment(Environment.DEVELOPMENT);
            unpublishedProjectDeployment.setName("unpublished-deployment");
            unpublishedProjectDeployment.setProjectId(unpublishedProject.getId());
            unpublishedProjectDeployment.setProjectVersion(1);

            unpublishedProjectDeployment = projectDeploymentRepository.save(unpublishedProjectDeployment);

            McpProject sourceMcpProject = mcpProjectRepository.save(
                new McpProject(unpublishedProjectDeployment.getId(), mcpServer.getId()));

            McpServer targetMcpServer = mcpServerRepository.save(
                new McpServer("target-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT));

            long projectDeploymentCount = projectDeploymentRepository.count();
            long sourceMcpProjectId = sourceMcpProject.getId();
            long targetMcpServerId = targetMcpServer.getId();

            assertThatThrownBy(() -> mcpProjectFacade.cloneMcpProject(sourceMcpProjectId, targetMcpServerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not published");

            assertThat(mcpProjectRepository.count()).isEqualTo(1);
            assertThat(projectDeploymentRepository.count()).isEqualTo(projectDeploymentCount);
        }
    }

    @Test
    void testDeleteMcpProject() {
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
        void testCreateRequiresServerEditor() {
            when(permissionEvaluator.hasPermission(any(), eq(3L), eq("McpServer"), eq("MCP_EDIT"))).thenReturn(false);

            assertThatThrownBy(() -> mcpProjectFacade.createMcpProject(3L, 4L, 1, List.of()))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void testDeleteRequiresProjectEditor() {
            when(permissionEvaluator.hasPermission(any(), eq(5L), eq("McpProject"), eq("MCP_EDIT"))).thenReturn(false);

            assertThatThrownBy(() -> mcpProjectFacade.deleteMcpProject(5L))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void testUpdateRequiresProjectEditor() {
            when(permissionEvaluator.hasPermission(any(), eq(5L), eq("McpProject"), eq("MCP_EDIT"))).thenReturn(false);

            assertThatThrownBy(() -> mcpProjectFacade.updateMcpProject(5L, List.of()))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void testCloneRequiresProjectEditorAndTargetServerEditor() {
            when(permissionEvaluator.hasPermission(any(), eq(5L), eq("McpProject"), eq("MCP_EDIT"))).thenReturn(false);

            assertThatThrownBy(() -> mcpProjectFacade.cloneMcpProject(5L, 6L))
                .isInstanceOf(AccessDeniedException.class);

            reset(permissionEvaluator);

            when(permissionEvaluator.hasPermission(any(), eq(6L), eq("McpServer"), eq("MCP_EDIT"))).thenReturn(false);

            assertThatThrownBy(() -> mcpProjectFacade.cloneMcpProject(5L, 6L))
                .isInstanceOf(AccessDeniedException.class);
        }
    }
}
