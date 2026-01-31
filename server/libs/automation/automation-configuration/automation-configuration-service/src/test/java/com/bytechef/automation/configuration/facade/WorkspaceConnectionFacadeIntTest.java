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

package com.bytechef.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.util.ProjectDeploymentFacadeHelper;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.repository.WorkflowTestConfigurationRepository;
import com.bytechef.platform.tag.repository.TagRepository;
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
@SpringBootTest(
    classes = ProjectIntTestConfiguration.class,
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
@ProjectIntTestConfigurationSharedMocks
public class WorkspaceConnectionFacadeIntTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProjectFacade projectFacade;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

    @Autowired
    private ProjectDeploymentFacade projectDeploymentFacade;

    @Autowired
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private WorkflowTestConfigurationRepository workflowTestConfigurationRepository;

    @Autowired
    private WorkspaceConnectionFacade workspaceConnectionFacade;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private ProjectDeploymentFacadeHelper projectDeploymentFacadeHelper;

    private Workspace workspace;

    @AfterEach
    public void afterEach() {
        workflowTestConfigurationRepository.deleteAll();
        projectDeploymentWorkflowRepository.deleteAll();
        projectWorkflowRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();

        categoryRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @BeforeEach
    void beforeEach() {
        workspace = workspaceRepository.save(new Workspace("test"));

        projectDeploymentFacadeHelper = new ProjectDeploymentFacadeHelper(
            categoryRepository, projectFacade, projectRepository, projectDeploymentFacade, projectWorkflowFacade,
            projectWorkflowRepository);
    }

    @Test
    public void testDisconnectConnectionRemovesProjectDeploymentWorkflowConnection() {
        // Given - Create a project with deployment and add a connection to the workflow
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO =
            projectDeploymentFacadeHelper.createProjectDeployment(workspace.getId(), projectDTO);

        // Get the project deployment workflow and add a connection
        List<ProjectDeploymentWorkflow> workflows =
            projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(projectDeploymentDTO.id());

        assertThat(workflows).hasSize(1);

        ProjectDeploymentWorkflow workflow = workflows.getFirst();

        long connectionId = 12345L;

        workflow.setConnections(
            List.of(new ProjectDeploymentWorkflowConnection(connectionId, "connectionKey", "nodeName")));

        projectDeploymentWorkflowRepository.save(workflow);

        // Verify the connection was added
        List<ProjectDeploymentWorkflow> workflowsWithConnection =
            projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(projectDeploymentDTO.id());

        assertThat(workflowsWithConnection.getFirst()
            .getConnections()).hasSize(1);
        assertThat(workflowsWithConnection.getFirst()
            .getConnections()
            .getFirst()
            .getConnectionId())
                .isEqualTo(connectionId);

        // When - Disconnect the connection
        workspaceConnectionFacade.disconnectConnection(connectionId);

        // Then - Verify the connection was removed from the project deployment workflow
        List<ProjectDeploymentWorkflow> workflowsAfterDisconnect =
            projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(projectDeploymentDTO.id());

        assertThat(workflowsAfterDisconnect).hasSize(1);
        assertThat(workflowsAfterDisconnect.getFirst()
            .getConnections()).isEmpty();
    }

    @Test
    public void testDisconnectConnectionRemovesWorkflowTestConfigurationConnection() {
        // Given - Create a workflow test configuration with a connection
        long connectionId = 54321L;

        WorkflowTestConfiguration testConfiguration = new WorkflowTestConfiguration();

        testConfiguration.setEnvironmentId(1L);
        testConfiguration.setWorkflowId("test-workflow-id");
        testConfiguration.setConnections(
            List.of(new WorkflowTestConfigurationConnection(connectionId, "connectionKey", "nodeName")));

        workflowTestConfigurationRepository.save(testConfiguration);

        // Verify the connection was added
        List<WorkflowTestConfiguration> configurationsWithConnection = workflowTestConfigurationRepository.findAll();

        assertThat(configurationsWithConnection).hasSize(1);
        assertThat(configurationsWithConnection.getFirst()
            .getConnections()).hasSize(1);
        assertThat(configurationsWithConnection.getFirst()
            .getConnections()
            .getFirst()
            .getConnectionId())
                .isEqualTo(connectionId);

        // When - Disconnect the connection
        workspaceConnectionFacade.disconnectConnection(connectionId);

        // Then - Verify the connection was removed from the workflow test configuration
        List<WorkflowTestConfiguration> configurationsAfterDisconnect = workflowTestConfigurationRepository.findAll();

        assertThat(configurationsAfterDisconnect).hasSize(1);
        assertThat(configurationsAfterDisconnect.getFirst()
            .getConnections()).isEmpty();
    }

    @Test
    public void testDisconnectConnectionRemovesBothConnections() {
        // Given - Create a project with deployment and workflow test configuration, both with the same connection
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO =
            projectDeploymentFacadeHelper.createProjectDeployment(workspace.getId(), projectDTO);

        // Get the project deployment workflow and add a connection
        List<ProjectDeploymentWorkflow> workflows =
            projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(projectDeploymentDTO.id());

        ProjectDeploymentWorkflow workflow = workflows.getFirst();

        long connectionId = 99999L;

        workflow.setConnections(
            List.of(new ProjectDeploymentWorkflowConnection(connectionId, "connectionKey", "nodeName")));

        projectDeploymentWorkflowRepository.save(workflow);

        // Create a workflow test configuration with the same connection
        WorkflowTestConfiguration testConfiguration = new WorkflowTestConfiguration();

        testConfiguration.setEnvironmentId(1L);
        testConfiguration.setWorkflowId("test-workflow-id");
        testConfiguration.setConnections(
            List.of(new WorkflowTestConfigurationConnection(connectionId, "connectionKey", "nodeName")));

        workflowTestConfigurationRepository.save(testConfiguration);

        // When - Disconnect the connection
        workspaceConnectionFacade.disconnectConnection(connectionId);

        // Then - Verify both connections were removed
        List<ProjectDeploymentWorkflow> workflowsAfterDisconnect =
            projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(projectDeploymentDTO.id());

        assertThat(workflowsAfterDisconnect.getFirst()
            .getConnections()).isEmpty();

        List<WorkflowTestConfiguration> configurationsAfterDisconnect = workflowTestConfigurationRepository.findAll();

        assertThat(configurationsAfterDisconnect.getFirst()
            .getConnections()).isEmpty();
    }

    @Test
    public void testDisconnectConnectionWithNonExistentConnectionIdDoesNothing() {
        // Given - No connections exist with this ID
        long nonExistentConnectionId = 11111L;

        // When - Disconnect a non-existent connection
        workspaceConnectionFacade.disconnectConnection(nonExistentConnectionId);

        // Then - No exception should be thrown (method completes successfully)
    }
}
