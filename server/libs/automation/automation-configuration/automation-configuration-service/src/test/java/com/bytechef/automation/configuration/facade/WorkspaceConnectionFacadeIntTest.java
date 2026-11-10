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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.util.ProjectDeploymentFacadeHelper;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.repository.WorkflowTestConfigurationRepository;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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
    private ConnectionFacade connectionFacade;

    @Autowired
    private ConnectionService connectionService;

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

    @Test
    public void testUpdateConnectionCredentialsDelegatesForAutomationConnection() {
        Map<String, String> parameters = Map.of("token", "new-token");

        when(connectionService.getConnection(1L)).thenReturn(createConnection(ConnectionStatus.ACTIVE, 3));

        workspaceConnectionFacade.updateConnectionCredentials(1L, parameters, 3);

        verify(connectionFacade).updateCredentials(1L, parameters, 3);
    }

    @Test
    public void testUpdateConnectionCredentialsRejectsEmbeddedConnection() {
        Connection connection = createConnection(ConnectionStatus.ACTIVE, 3);

        connection.setType(PlatformType.EMBEDDED);

        when(connectionService.getConnection(1L)).thenReturn(connection);

        assertThatThrownBy(
            () -> workspaceConnectionFacade.updateConnectionCredentials(1L, Map.of("token", "new-token"), 3))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("automation connections");

        verify(connectionFacade, never()).updateCredentials(anyLong(), anyMap(), anyInt());
    }

    private static Connection createConnection(ConnectionStatus status, int version) {
        Connection connection = new Connection();

        connection.setId(1L);
        connection.setStatus(status);
        connection.setVersion(version);

        return connection;
    }

    @Nested
    @ContextConfiguration(classes = PreAuthorizeEnforcement.MethodSecurityConfiguration.class)
    class PreAuthorizeEnforcement {

        @MockitoBean
        private PermissionService permissionService;

        @BeforeEach
        void beforeEach() {
            reset(permissionService);

            authenticate(AuthorityConstants.USER);
        }

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void testDisconnectIsAdminOnly() {
            assertRequiresAdminAuthority(() -> workspaceConnectionFacade.disconnectConnection(1L));
        }

        @Test
        void testUpdateConnectionCredentialsDeniesAUserWhoIsNeitherOwnerNorAdmin() {
            assertDenied(() -> workspaceConnectionFacade.updateConnectionCredentials(1L, Map.of(), 0));
        }

        @Test
        void testUpdateConnectionCredentialsAdmitsTheConnectionOwner() {
            when(permissionService.isResourceOwner("Connection", 1L)).thenReturn(true);

            assertNotDenied(() -> workspaceConnectionFacade.updateConnectionCredentials(1L, Map.of(), 0));
        }

        @Test
        void testUpdateConnectionCredentialsAdmitsATenantAdmin() {
            when(permissionService.isTenantAdmin()).thenReturn(true);

            assertNotDenied(() -> workspaceConnectionFacade.updateConnectionCredentials(1L, Map.of(), 0));
        }

        private void assertRequiresAdminAuthority(ThrowingCallable call) {
            authenticate(AuthorityConstants.USER);

            assertDenied(call);

            authenticate(AuthorityConstants.ADMIN);

            assertNotDenied(call);

            authenticate(AuthorityConstants.USER);
        }

        private static void assertDenied(ThrowingCallable call) {
            assertThatThrownBy(call).isInstanceOf(AccessDeniedException.class);
        }

        private static void assertNotDenied(ThrowingCallable call) {
            Throwable throwable = catchThrowable(call);

            assertThat(throwable)
                .as("Expected the gate to let the call through, but it was denied: %s", throwable)
                .satisfiesAnyOf(
                    actual -> assertThat(actual).isNull(),
                    actual -> assertThat(actual).isNotInstanceOf(AccessDeniedException.class));
        }

        private static void authenticate(String authority) {
            SecurityContextHolder.getContext()
                .setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                        "user", "n/a", List.of(new SimpleGrantedAuthority(authority))));
        }

        @EnableMethodSecurity
        static class MethodSecurityConfiguration {
        }
    }
}
