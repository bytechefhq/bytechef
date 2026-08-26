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

import com.bytechef.automation.ai.mcp.config.McpProjectIntTestConfiguration;
import com.bytechef.automation.ai.mcp.config.McpProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.repository.McpProjectRepository;
import com.bytechef.automation.ai.mcp.repository.McpProjectWorkflowRepository;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.SystemProjects;
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
@SpringBootTest(classes = McpProjectIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@McpProjectIntTestConfigurationSharedMocks
public class WorkflowDeleteCascadeIntTest {

    private static final String OTHER_WORKFLOW_ID = "workflow3";
    private static final String SYSTEM_PROJECT_WORKFLOW_ID = "workflow2";
    private static final String WORKFLOW_ID = "workflow1";

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
    private WorkspaceRepository workspaceRepository;

    private McpServer mcpServer;
    private Project project;
    private Workspace workspace;

    @BeforeEach
    public void beforeEach() {
        mcpServer = mcpServerRepository.save(
            new McpServer("test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT));

        workspace = workspaceRepository.save(new Workspace("test-workspace"));

        project = projectRepository.save(
            Project.builder()
                .description("test-project")
                .name("test-project")
                .workspaceId(workspace.getId())
                .build());

        projectWorkflowRepository.save(new ProjectWorkflow(project.getId(), 1, WORKFLOW_ID, UUID.randomUUID()));
    }

    @AfterEach
    public void afterEach() {
        mcpProjectWorkflowRepository.deleteAll();
        mcpProjectRepository.deleteAll();
        projectDeploymentWorkflowRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        projectWorkflowRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
        mcpServerRepository.deleteAll();
    }

    @Test
    public void testDeleteWorkflowDeletesMcpProjectWorkflow() {
        McpProject mcpProject = mcpProjectFacade.createMcpProject(
            mcpServer.getId(), project.getId(), 1, List.of(WORKFLOW_ID));

        assertThat(mcpProjectWorkflowRepository.findAllByMcpProjectId(mcpProject.getId())).hasSize(1);
        assertThat(projectDeploymentWorkflowRepository.findAll()).hasSize(1);

        projectWorkflowFacade.deleteWorkflow(WORKFLOW_ID);

        assertThat(projectDeploymentWorkflowRepository.findAll()).isEmpty();
        assertThat(mcpProjectWorkflowRepository.findAll()).isEmpty();
    }

    @Test
    public void testDeleteWorkflowDeletesRegularAndMcpProjectDeploymentWorkflows() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setName("test-deployment");
        projectDeployment.setProjectId(project.getId());
        projectDeployment.setProjectVersion(1);
        projectDeployment.setUuid(UUID.randomUUID());

        projectDeployment = projectDeploymentRepository.save(projectDeployment);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setProjectDeploymentId(projectDeployment.getId());
        projectDeploymentWorkflow.setWorkflowId(WORKFLOW_ID);

        projectDeploymentWorkflowRepository.save(projectDeploymentWorkflow);

        mcpProjectFacade.createMcpProject(mcpServer.getId(), project.getId(), 1, List.of(WORKFLOW_ID));

        assertThat(projectDeploymentWorkflowRepository.findAllByWorkflowId(WORKFLOW_ID)).hasSize(2);

        projectWorkflowFacade.deleteWorkflow(WORKFLOW_ID);

        assertThat(projectDeploymentWorkflowRepository.findAll()).isEmpty();
        assertThat(mcpProjectWorkflowRepository.findAll()).isEmpty();
    }

    /**
     * The unowned row used to be asserted as SURVIVING here, which documented the MCP listener's ownership guard: a
     * listener must not delete a {@code project_deployment_workflow} row it does not own. That guard is still right and
     * still in force — but it was never the whole delete. Nothing else reached this row, because {@code deleteWorkflow}
     * drove its sweep off the deployment LIST query, which hides deployments of a system-named project. So the row
     * outlived its workflow and became a permanent orphan.
     * <p>
     * The sweep now runs unfiltered, after the listeners. The ownership invariant this test was protecting is now
     * covered by {@link #testDeleteWorkflowLeavesOtherWorkflowsProjectDeploymentWorkflowsAlone()}, which is where it
     * actually belongs: the thing that must survive is another WORKFLOW's row, not this workflow's orphan.
     */
    @Test
    public void testDeleteWorkflowSweepsProjectDeploymentWorkflowsOfASystemNamedProject() {
        Project systemProject = projectRepository.save(
            Project.builder()
                .description("test-system-project")
                .name(SystemProjects.AI_AGENT_NAME_PREFIX + "test-project")
                .workspaceId(workspace.getId())
                .build());

        projectWorkflowRepository.save(
            new ProjectWorkflow(systemProject.getId(), 1, SYSTEM_PROJECT_WORKFLOW_ID, UUID.randomUUID()));

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setName("test-system-project-deployment");
        projectDeployment.setProjectId(systemProject.getId());
        projectDeployment.setProjectVersion(1);
        projectDeployment.setUuid(UUID.randomUUID());

        projectDeployment = projectDeploymentRepository.save(projectDeployment);

        ProjectDeploymentWorkflow unownedProjectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        unownedProjectDeploymentWorkflow.setProjectDeploymentId(projectDeployment.getId());
        unownedProjectDeploymentWorkflow.setWorkflowId(SYSTEM_PROJECT_WORKFLOW_ID);

        unownedProjectDeploymentWorkflow = projectDeploymentWorkflowRepository.save(unownedProjectDeploymentWorkflow);

        McpProject mcpProject = mcpProjectFacade.createMcpProject(
            mcpServer.getId(), systemProject.getId(), 1, List.of(SYSTEM_PROJECT_WORKFLOW_ID));

        assertThat(projectDeploymentWorkflowRepository.findAllByWorkflowId(SYSTEM_PROJECT_WORKFLOW_ID)).hasSize(2);
        assertThat(mcpProjectWorkflowRepository.findAllByMcpProjectId(mcpProject.getId())).hasSize(1);

        projectWorkflowFacade.deleteWorkflow(SYSTEM_PROJECT_WORKFLOW_ID);

        assertThat(mcpProjectWorkflowRepository.findAll()).isEmpty();
        assertThat(projectDeploymentWorkflowRepository.findAllByWorkflowId(SYSTEM_PROJECT_WORKFLOW_ID)).isEmpty();
        assertThat(projectDeploymentWorkflowRepository.findById(unownedProjectDeploymentWorkflow.getId())).isEmpty();
    }

    /**
     * The sweep is scoped to the workflow being deleted, not to the deployment. A sibling row for a different workflow
     * of the same deployment is not an orphan and must survive — this is the over-deletion the unfiltered sweep could
     * otherwise cause, and the invariant the previous version of the test above was reaching for.
     */
    @Test
    public void testDeleteWorkflowLeavesOtherWorkflowsProjectDeploymentWorkflowsAlone() {
        projectWorkflowRepository.save(
            new ProjectWorkflow(project.getId(), 1, OTHER_WORKFLOW_ID, UUID.randomUUID()));

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setName("test-project-deployment");
        projectDeployment.setProjectId(project.getId());
        projectDeployment.setProjectVersion(1);
        projectDeployment.setUuid(UUID.randomUUID());

        projectDeployment = projectDeploymentRepository.save(projectDeployment);

        ProjectDeploymentWorkflow deletedWorkflowRow = new ProjectDeploymentWorkflow();

        deletedWorkflowRow.setProjectDeploymentId(projectDeployment.getId());
        deletedWorkflowRow.setWorkflowId(WORKFLOW_ID);

        projectDeploymentWorkflowRepository.save(deletedWorkflowRow);

        ProjectDeploymentWorkflow otherWorkflowRow = new ProjectDeploymentWorkflow();

        otherWorkflowRow.setProjectDeploymentId(projectDeployment.getId());
        otherWorkflowRow.setWorkflowId(OTHER_WORKFLOW_ID);

        otherWorkflowRow = projectDeploymentWorkflowRepository.save(otherWorkflowRow);

        projectWorkflowFacade.deleteWorkflow(WORKFLOW_ID);

        assertThat(projectDeploymentWorkflowRepository.findAllByWorkflowId(WORKFLOW_ID)).isEmpty();
        assertThat(projectDeploymentWorkflowRepository.findAllByWorkflowId(OTHER_WORKFLOW_ID))
            .extracting(ProjectDeploymentWorkflow::getId)
            .containsExactly(otherWorkflowRow.getId());
    }
}
