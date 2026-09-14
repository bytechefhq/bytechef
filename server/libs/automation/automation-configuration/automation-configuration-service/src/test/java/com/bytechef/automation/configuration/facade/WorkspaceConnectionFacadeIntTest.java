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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.automation.configuration.util.ProjectDeploymentFacadeHelper;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.repository.WorkflowTestConfigurationRepository;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
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
import org.springframework.security.test.context.support.WithMockUser;
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
@WithMockUser(username = "admin@localhost.com", authorities = AuthorityConstants.ADMIN)
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

    @Nested
    @Import({
        MethodSecurityEnforcement.Config.class, PostgreSQLContainerConfiguration.class
    })
    class MethodSecurityEnforcement {

        private static final String BODY_REACHED = "body reached";
        private static final long CONNECTION_ID = 7L;
        private static final String CONNECTION_TYPE = "Connection";
        private static final long ENVIRONMENT_ID = 2L;
        private static final long WORKSPACE_ID = 42L;

        @Autowired
        private ConnectionFacade connectionFacade;

        @MockitoBean
        private PermissionService permissionService;

        @MockitoBean
        private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

        @Autowired
        private UserService userService;

        @MockitoBean
        private WorkspaceConnectionService workspaceConnectionService;

        @BeforeEach
        void authenticateAsNonAdmin() {
            SecurityContextHolder.getContext()
                .setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                        "viewer", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

            when(workspaceConnectionService.getWorkspaceConnections(anyLong()))
                .thenThrow(new IllegalStateException(BODY_REACHED));
            when(connectionFacade.create(any(ConnectionDTO.class), any(PlatformType.class)))
                .thenThrow(new IllegalStateException(BODY_REACHED));
            when(connectionFacade.getConnection(anyLong())).thenThrow(new IllegalStateException(BODY_REACHED));

            when(userService.fetchUserByLogin(anyString())).thenThrow(new IllegalStateException(BODY_REACHED));

            doThrow(new IllegalStateException(BODY_REACHED)).when(projectDeploymentWorkflowService)
                .deleteProjectDeploymentWorkflowConnection(anyLong());
            doThrow(new IllegalStateException(BODY_REACHED)).when(workspaceConnectionService)
                .deleteWorkspaceConnection(anyLong());
            doThrow(new IllegalStateException(BODY_REACHED)).when(connectionFacade)
                .update(anyLong(), anyString(), anyList(), anyInt());
            doThrow(new IllegalStateException(BODY_REACHED)).when(connectionFacade)
                .update(anyLong(), anyList());
        }

        @AfterEach
        void clearSecurityContext() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void testConnectionTagsRequireTheConnectionViewScopeInTheRequestedEnvironment() {
            long environmentId = Environment.PRODUCTION.ordinal();

            when(permissionService.hasWorkspaceScope(WORKSPACE_ID, "CONNECTION_VIEW", Environment.DEVELOPMENT))
                .thenReturn(true);

            assertThatThrownBy(() -> workspaceConnectionFacade.getConnectionTags(WORKSPACE_ID, environmentId))
                .isInstanceOf(AccessDeniedException.class);

            when(permissionService.hasWorkspaceScope(WORKSPACE_ID, "CONNECTION_VIEW", Environment.PRODUCTION))
                .thenReturn(true);

            assertThatThrownBy(() -> workspaceConnectionFacade.getConnectionTags(WORKSPACE_ID, environmentId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);
        }

        @Test
        void testDeleteDeniesWhenTheConnectionDeleteScopeIsRefusedAndTheCallerIsNotTheOwner() {
            assertScopeOrOwnerGuard(
                () -> workspaceConnectionFacade.delete(CONNECTION_ID), "CONNECTION_DELETE", false, false);
        }

        @Test
        void testDeleteAllowsWhenTheConnectionDeleteScopeIsGranted() {
            assertScopeOrOwnerGuard(
                () -> workspaceConnectionFacade.delete(CONNECTION_ID), "CONNECTION_DELETE", true, false);
        }

        @Test
        void testDeleteAllowsTheOwnerWithoutTheConnectionDeleteScope() {
            assertScopeOrOwnerGuard(
                () -> workspaceConnectionFacade.delete(CONNECTION_ID), "CONNECTION_DELETE", false, true);
        }

        @Test
        void testDeleteDeniesWithOnlyTheConnectionEditScope() {
            when(permissionService.hasResourceScope(CONNECTION_ID, CONNECTION_TYPE, "CONNECTION_EDIT"))
                .thenReturn(true);

            assertInvocationOutcome(() -> workspaceConnectionFacade.delete(CONNECTION_ID), false);
        }

        @Test
        void testGetConnectionDeniesWhenTheConnectionViewScopeIsRefused() {
            assertResourceGuard(
                () -> workspaceConnectionFacade.getConnection(CONNECTION_ID), "CONNECTION_VIEW", false);
        }

        @Test
        void testGetConnectionAllowsWhenTheConnectionViewScopeIsGranted() {
            assertResourceGuard(
                () -> workspaceConnectionFacade.getConnection(CONNECTION_ID), "CONNECTION_VIEW", true);
        }

        @Test
        void testGetConnectionDeniesTheOwnerWithoutTheConnectionViewScope() {
            when(permissionService.isResourceOwner(CONNECTION_TYPE, CONNECTION_ID)).thenReturn(true);

            assertResourceGuard(
                () -> workspaceConnectionFacade.getConnection(CONNECTION_ID), "CONNECTION_VIEW", false);
        }

        @Test
        void testUpdateDeniesWhenTheConnectionEditScopeIsRefusedAndTheCallerIsNotTheOwner() {
            assertScopeOrOwnerGuard(
                () -> workspaceConnectionFacade.update(CONNECTION_ID, "name", List.of(), 1), "CONNECTION_EDIT", false,
                false);
        }

        @Test
        void testUpdateAllowsWhenTheConnectionEditScopeIsGranted() {
            assertScopeOrOwnerGuard(
                () -> workspaceConnectionFacade.update(CONNECTION_ID, "name", List.of(), 1), "CONNECTION_EDIT", true,
                false);
        }

        @Test
        void testUpdateAllowsTheOwnerWithoutTheConnectionEditScope() {
            assertScopeOrOwnerGuard(
                () -> workspaceConnectionFacade.update(CONNECTION_ID, "name", List.of(), 1), "CONNECTION_EDIT", false,
                true);
        }

        @Test
        void testUpdateTagsDeniesWhenTheConnectionEditScopeIsRefusedAndTheCallerIsNotTheOwner() {
            assertScopeOrOwnerGuard(
                () -> workspaceConnectionFacade.updateTags(CONNECTION_ID, List.of()), "CONNECTION_EDIT", false, false);
        }

        @Test
        void testUpdateTagsAllowsWhenTheConnectionEditScopeIsGranted() {
            assertScopeOrOwnerGuard(
                () -> workspaceConnectionFacade.updateTags(CONNECTION_ID, List.of()), "CONNECTION_EDIT", true, false);
        }

        @Test
        void testUpdateTagsAllowsTheOwnerWithoutTheConnectionEditScope() {
            assertScopeOrOwnerGuard(
                () -> workspaceConnectionFacade.updateTags(CONNECTION_ID, List.of()), "CONNECTION_EDIT", false, true);
        }

        @Test
        void testGetConnectionsDeniesWhenTheConnectionViewScopeIsRefusedInTheNamedEnvironment() {
            assertWorkspaceGuard(
                () -> workspaceConnectionFacade.getConnections(WORKSPACE_ID, null, null, ENVIRONMENT_ID, null),
                "CONNECTION_VIEW", false);
        }

        @Test
        void testGetConnectionsAllowsWhenTheConnectionViewScopeIsGrantedInTheNamedEnvironment() {
            assertWorkspaceGuard(
                () -> workspaceConnectionFacade.getConnections(WORKSPACE_ID, null, null, ENVIRONMENT_ID, null),
                "CONNECTION_VIEW", true);
        }

        @Test
        void testCreateDeniesWhenTheConnectionCreateScopeIsRefusedInTheConnectionsEnvironment() {
            assertWorkspaceGuard(
                () -> workspaceConnectionFacade.create(WORKSPACE_ID, connectionDTO()), "CONNECTION_CREATE", false);
        }

        @Test
        void testCreateAllowsWhenTheConnectionCreateScopeIsGrantedInTheConnectionsEnvironment() {
            assertWorkspaceGuard(
                () -> workspaceConnectionFacade.create(WORKSPACE_ID, connectionDTO()), "CONNECTION_CREATE", true);
        }

        @Test
        void testDisconnectConnectionDeniesANonAdmin() {
            when(permissionService.hasResourceScope(CONNECTION_ID, CONNECTION_TYPE, "CONNECTION_DELETE"))
                .thenReturn(true);
            when(permissionService.isResourceOwner(CONNECTION_TYPE, CONNECTION_ID)).thenReturn(true);

            assertThatThrownBy(() -> workspaceConnectionFacade.disconnectConnection(CONNECTION_ID))
                .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(permissionService);
        }

        @Test
        void testDisconnectConnectionAllowsATenantAdmin() {
            SecurityContextHolder.getContext()
                .setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                        "admin", "n/a", List.of(new SimpleGrantedAuthority(AuthorityConstants.ADMIN))));

            assertInvocationOutcome(() -> workspaceConnectionFacade.disconnectConnection(CONNECTION_ID), true);

            verifyNoInteractions(permissionService);
        }

        private void assertResourceGuard(ThrowingCallable invocation, String expectedScope, boolean granted) {
            when(permissionService.hasResourceScope(CONNECTION_ID, CONNECTION_TYPE, expectedScope)).thenReturn(granted);

            assertInvocationOutcome(invocation, granted);

            verify(permissionService).hasResourceScope(CONNECTION_ID, CONNECTION_TYPE, expectedScope);
            verifyNoMoreInteractions(permissionService);
        }

        private void assertScopeOrOwnerGuard(
            ThrowingCallable invocation, String expectedScope, boolean scopeGranted, boolean owner) {

            when(permissionService.hasResourceScope(CONNECTION_ID, CONNECTION_TYPE, expectedScope))
                .thenReturn(scopeGranted);
            when(permissionService.isResourceOwner(CONNECTION_TYPE, CONNECTION_ID)).thenReturn(owner);

            assertInvocationOutcome(invocation, scopeGranted || owner);

            verify(permissionService).hasResourceScope(CONNECTION_ID, CONNECTION_TYPE, expectedScope);

            if (!scopeGranted) {
                verify(permissionService).isResourceOwner(CONNECTION_TYPE, CONNECTION_ID);
            }

            verifyNoMoreInteractions(permissionService);
        }

        private void assertWorkspaceGuard(ThrowingCallable invocation, String expectedScope, boolean granted) {
            Environment environment = Environment.values()[(int) ENVIRONMENT_ID];

            when(permissionService.hasWorkspaceScope(WORKSPACE_ID, expectedScope, environment)).thenReturn(granted);

            assertInvocationOutcome(invocation, granted);

            verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, expectedScope, environment);
            verifyNoMoreInteractions(permissionService);
        }

        private void assertInvocationOutcome(ThrowingCallable invocation, boolean allowed) {
            if (allowed) {
                assertThatThrownBy(invocation)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(BODY_REACHED);
            } else {
                assertThatThrownBy(invocation).isInstanceOf(AccessDeniedException.class);
            }
        }

        private ConnectionDTO connectionDTO() {
            return ConnectionDTO.builder()
                .environmentId((int) ENVIRONMENT_ID)
                .build();
        }

        @EnableMethodSecurity
        static class Config {
        }
    }
}
