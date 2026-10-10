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

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.mcp.config.McpIntTestWorkflows;
import com.bytechef.automation.ai.mcp.config.McpProjectIntTestConfiguration;
import com.bytechef.automation.ai.mcp.config.McpProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.facade.McpProjectFacade;
import com.bytechef.automation.ai.mcp.repository.McpProjectRepository;
import com.bytechef.automation.ai.mcp.repository.McpProjectWorkflowRepository;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = McpProjectIntTestConfiguration.class, properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
@McpProjectIntTestConfigurationSharedMocks
class McpWorkflowPreDeleteListenerIntTest {

    @Autowired
    private McpProjectFacade mcpProjectFacade;

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
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private McpServer mcpServer;
    private String otherWorkflowId;
    private Project project;
    private String systemProjectWorkflowId;
    private String workflowId;
    private Workspace workspace;

    @BeforeEach
    void beforeEach() {
        mcpServer = mcpServerRepository.save(
            new McpServer("test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT));

        workspace = workspaceRepository.save(new Workspace("test-workspace"));

        Project newProject = Project.builder()
            .description("test-project")
            .name("test-project")
            .workspaceId(workspace.getId())
            .build();

        newProject.publish("v1");

        project = projectRepository.save(newProject);

        workflowId = McpIntTestWorkflows.createNewWorkflowCallWorkflow(workflowService);
        systemProjectWorkflowId = McpIntTestWorkflows.createNewWorkflowCallWorkflow(workflowService);
        otherWorkflowId = McpIntTestWorkflows.createNewWorkflowCallWorkflow(workflowService);

        projectWorkflowRepository.save(new ProjectWorkflow(project.getId(), 1, workflowId, UUID.randomUUID()));
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
        mcpServerRepository.deleteAll();

        McpIntTestWorkflows.deleteWorkflows(
            workflowService, List.of(workflowId, systemProjectWorkflowId, otherWorkflowId));
    }

    @Test
    void testDeleteWorkflowDeletesMcpProjectWorkflow() {
        McpProject mcpProject = mcpProjectFacade.createMcpProject(
            mcpServer.getId(), project.getId(), 1, List.of(workflowId));

        assertThat(mcpProjectWorkflowRepository.findAllByMcpProjectId(mcpProject.getId())).hasSize(1);
        assertThat(projectDeploymentWorkflowRepository.findAll()).hasSize(1);

        projectWorkflowFacade.deleteWorkflow(workflowId);

        assertThat(projectDeploymentWorkflowRepository.findAll()).isEmpty();
        assertThat(mcpProjectWorkflowRepository.findAll()).isEmpty();
    }

    @Test
    void testDeleteWorkflowDeletesRegularAndMcpProjectDeploymentWorkflows() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setName("test-deployment");
        projectDeployment.setProjectId(project.getId());
        projectDeployment.setProjectVersion(1);

        projectDeployment = projectDeploymentRepository.save(projectDeployment);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setProjectDeploymentId(projectDeployment.getId());
        projectDeploymentWorkflow.setWorkflowId(workflowId);

        projectDeploymentWorkflowRepository.save(projectDeploymentWorkflow);

        mcpProjectFacade.createMcpProject(mcpServer.getId(), project.getId(), 1, List.of(workflowId));

        assertThat(projectDeploymentWorkflowRepository.findAllByWorkflowId(workflowId)).hasSize(2);

        projectWorkflowFacade.deleteWorkflow(workflowId);

        assertThat(projectDeploymentWorkflowRepository.findAll()).isEmpty();
        assertThat(mcpProjectWorkflowRepository.findAll()).isEmpty();
    }

    @Test
    void testDeleteWorkflowSweepsProjectDeploymentWorkflowsOfASystemNamedProject() {
        Project newSystemProject = Project.builder()
            .description("test-system-project")
            .name("__EMBEDDED__test-project")
            .workspaceId(workspace.getId())
            .build();

        newSystemProject.publish("v1");

        Project systemProject = projectRepository.save(newSystemProject);

        projectWorkflowRepository.save(
            new ProjectWorkflow(systemProject.getId(), 1, systemProjectWorkflowId, UUID.randomUUID()));

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setName("test-system-project-deployment");
        projectDeployment.setProjectId(systemProject.getId());
        projectDeployment.setProjectVersion(1);

        projectDeployment = projectDeploymentRepository.save(projectDeployment);

        ProjectDeploymentWorkflow unownedProjectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        unownedProjectDeploymentWorkflow.setProjectDeploymentId(projectDeployment.getId());
        unownedProjectDeploymentWorkflow.setWorkflowId(systemProjectWorkflowId);

        unownedProjectDeploymentWorkflow = projectDeploymentWorkflowRepository.save(unownedProjectDeploymentWorkflow);

        McpProject mcpProject = mcpProjectFacade.createMcpProject(
            mcpServer.getId(), systemProject.getId(), 1, List.of(systemProjectWorkflowId));

        assertThat(projectDeploymentWorkflowRepository.findAllByWorkflowId(systemProjectWorkflowId)).hasSize(2);
        assertThat(mcpProjectWorkflowRepository.findAllByMcpProjectId(mcpProject.getId())).hasSize(1);

        projectWorkflowFacade.deleteWorkflow(systemProjectWorkflowId);

        assertThat(mcpProjectWorkflowRepository.findAll()).isEmpty();
        assertThat(projectDeploymentWorkflowRepository.findAllByWorkflowId(systemProjectWorkflowId)).isEmpty();
        assertThat(projectDeploymentWorkflowRepository.findById(unownedProjectDeploymentWorkflow.getId())).isEmpty();
    }

    @Test
    void testDeleteWorkflowLeavesOtherWorkflowsProjectDeploymentWorkflowsAlone() {
        projectWorkflowRepository.save(
            new ProjectWorkflow(project.getId(), 1, otherWorkflowId, UUID.randomUUID()));

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setName("test-project-deployment");
        projectDeployment.setProjectId(project.getId());
        projectDeployment.setProjectVersion(1);

        projectDeployment = projectDeploymentRepository.save(projectDeployment);

        ProjectDeploymentWorkflow deletedWorkflowRow = new ProjectDeploymentWorkflow();

        deletedWorkflowRow.setProjectDeploymentId(projectDeployment.getId());
        deletedWorkflowRow.setWorkflowId(workflowId);

        projectDeploymentWorkflowRepository.save(deletedWorkflowRow);

        ProjectDeploymentWorkflow otherWorkflowRow = new ProjectDeploymentWorkflow();

        otherWorkflowRow.setProjectDeploymentId(projectDeployment.getId());
        otherWorkflowRow.setWorkflowId(otherWorkflowId);

        otherWorkflowRow = projectDeploymentWorkflowRepository.save(otherWorkflowRow);

        projectWorkflowFacade.deleteWorkflow(workflowId);

        assertThat(projectDeploymentWorkflowRepository.findAllByWorkflowId(workflowId)).isEmpty();
        assertThat(projectDeploymentWorkflowRepository.findAllByWorkflowId(otherWorkflowId))
            .extracting(ProjectDeploymentWorkflow::getId)
            .containsExactly(otherWorkflowRow.getId());
    }
}
