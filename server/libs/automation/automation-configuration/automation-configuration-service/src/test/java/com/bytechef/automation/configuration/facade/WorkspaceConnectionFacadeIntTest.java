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
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.dto.ConnectionWorkflowDTO;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.util.ProjectDeploymentFacadeHelper;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.repository.WorkflowTestConfigurationRepository;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
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
    private WorkspaceConnectionRepository workspaceConnectionRepository;

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
        workspaceConnectionRepository.deleteAll();
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

        when(connectionService.getConnection(connectionId)).thenReturn(createConnection(ConnectionStatus.ACTIVE, 3));

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

        when(connectionService.getConnection(connectionId)).thenReturn(createConnection(ConnectionStatus.ACTIVE, 3));

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

        when(connectionService.getConnection(connectionId)).thenReturn(createConnection(ConnectionStatus.ACTIVE, 3));

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
    public void testGetConnectionWorkflowsListsDeployedAndEditorWorkflows() {
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO =
            projectDeploymentFacadeHelper.createProjectDeployment(workspace.getId(), projectDTO);

        long connectionId = 4242L;

        ProjectDeploymentWorkflow projectDeploymentWorkflow = saveProjectDeploymentWorkflowConnections(
            projectDeploymentDTO.id(), List.of(connectionId));

        WorkflowTestConfiguration editorTestConfiguration = saveWorkflowTestConfiguration(
            projectDeploymentWorkflow.getWorkflowId(), connectionId);

        saveWorkflowTestConfiguration("workflow-outside-any-project", connectionId);

        when(connectionService.getConnection(connectionId)).thenReturn(createConnection(ConnectionStatus.ACTIVE, 3));

        Optional<ProjectWorkflow> projectWorkflowOptional =
            projectWorkflowRepository.findByWorkflowId(projectDeploymentWorkflow.getWorkflowId());

        ProjectWorkflow projectWorkflow = projectWorkflowOptional.orElseThrow();

        List<ConnectionWorkflowDTO> connectionWorkflowDTOs =
            workspaceConnectionFacade.getConnectionWorkflows(connectionId);

        assertThat(connectionWorkflowDTOs)
            .extracting(
                ConnectionWorkflowDTO::id, ConnectionWorkflowDTO::projectId, ConnectionWorkflowDTO::projectName,
                ConnectionWorkflowDTO::projectVersion, ConnectionWorkflowDTO::usage, ConnectionWorkflowDTO::workflowId,
                ConnectionWorkflowDTO::workflowName, ConnectionWorkflowDTO::workflowUuid)
            .containsExactly(
                tuple(
                    projectDeploymentWorkflow.getId(), projectDTO.id(), projectDTO.name(),
                    projectDeploymentDTO.projectVersion(), ConnectionWorkflowDTO.Usage.DEPLOYMENT,
                    projectDeploymentWorkflow.getWorkflowId(), "Test Workflow", projectWorkflow.getUuidAsString()),
                tuple(
                    editorTestConfiguration.getId(), projectDTO.id(), projectDTO.name(),
                    projectDeploymentDTO.projectVersion(), ConnectionWorkflowDTO.Usage.EDITOR,
                    projectDeploymentWorkflow.getWorkflowId(), "Test Workflow", projectWorkflow.getUuidAsString()));
    }

    @Test
    public void testDisconnectConnectionFromOneDeploymentWorkflowKeepsEveryOtherUse() {
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO =
            projectDeploymentFacadeHelper.createProjectDeployment(workspace.getId(), projectDTO);

        long connectionId = 5151L;
        long otherConnectionId = 5252L;

        ProjectDeploymentWorkflow projectDeploymentWorkflow = saveProjectDeploymentWorkflowConnections(
            projectDeploymentDTO.id(), List.of(connectionId, otherConnectionId));

        WorkflowTestConfiguration workflowTestConfiguration = saveWorkflowTestConfiguration(
            projectDeploymentWorkflow.getWorkflowId(), connectionId);

        when(connectionService.getConnection(connectionId)).thenReturn(createConnection(ConnectionStatus.ACTIVE, 3));

        workspaceConnectionFacade.disconnectConnection(
            connectionId, ConnectionWorkflowDTO.Usage.DEPLOYMENT,
            Validate.notNull(projectDeploymentWorkflow.getId(), "id"));

        ProjectDeploymentWorkflow disconnectedProjectDeploymentWorkflow = findProjectDeploymentWorkflow(
            Validate.notNull(projectDeploymentWorkflow.getId(), "id"));

        assertThat(disconnectedProjectDeploymentWorkflow.getConnections())
            .extracting(ProjectDeploymentWorkflowConnection::getConnectionId)
            .containsExactly(otherConnectionId);

        WorkflowTestConfiguration untouchedWorkflowTestConfiguration = findWorkflowTestConfiguration(
            Validate.notNull(workflowTestConfiguration.getId(), "id"));

        assertThat(untouchedWorkflowTestConfiguration.getConnections())
            .extracting(WorkflowTestConfigurationConnection::getConnectionId)
            .containsExactly(connectionId);
    }

    @Test
    public void testDisconnectConnectionFromOneEditorWorkflowKeepsTheOtherEditorWorkflows() {
        long connectionId = 6161L;

        WorkflowTestConfiguration firstWorkflowTestConfiguration = saveWorkflowTestConfiguration(
            "first-workflow-id", connectionId);
        WorkflowTestConfiguration secondWorkflowTestConfiguration = saveWorkflowTestConfiguration(
            "second-workflow-id", connectionId);

        when(connectionService.getConnection(connectionId)).thenReturn(createConnection(ConnectionStatus.ACTIVE, 3));

        workspaceConnectionFacade.disconnectConnection(
            connectionId, ConnectionWorkflowDTO.Usage.EDITOR,
            Validate.notNull(firstWorkflowTestConfiguration.getId(), "id"));

        WorkflowTestConfiguration disconnectedWorkflowTestConfiguration = findWorkflowTestConfiguration(
            Validate.notNull(firstWorkflowTestConfiguration.getId(), "id"));
        WorkflowTestConfiguration untouchedWorkflowTestConfiguration = findWorkflowTestConfiguration(
            Validate.notNull(secondWorkflowTestConfiguration.getId(), "id"));

        assertThat(disconnectedWorkflowTestConfiguration.getConnections()).isEmpty();
        assertThat(untouchedWorkflowTestConfiguration.getConnections())
            .extracting(WorkflowTestConfigurationConnection::getConnectionId)
            .containsExactly(connectionId);
    }

    @Test
    public void testDisconnectConnectionRejectsAWorkflowThatDoesNotUseTheConnection() {
        long connectionId = 6262L;

        WorkflowTestConfiguration workflowTestConfiguration = saveWorkflowTestConfiguration(
            "unrelated-workflow-id", 6363L);

        when(connectionService.getConnection(connectionId)).thenReturn(createConnection(ConnectionStatus.ACTIVE, 3));

        long workflowTestConfigurationId = Validate.notNull(workflowTestConfiguration.getId(), "id");

        assertThatThrownBy(
            () -> workspaceConnectionFacade.disconnectConnection(
                connectionId, ConnectionWorkflowDTO.Usage.EDITOR, workflowTestConfigurationId))
                    .isInstanceOf(NoSuchElementException.class);

        WorkflowTestConfiguration untouchedWorkflowTestConfiguration =
            findWorkflowTestConfiguration(workflowTestConfigurationId);

        assertThat(untouchedWorkflowTestConfiguration.getConnections())
            .extracting(WorkflowTestConfigurationConnection::getConnectionId)
            .containsExactly(6363L);
    }

    @Test
    public void testGetConnectionWorkflowsRejectsEmbeddedConnection() {
        long connectionId = 7171L;

        when(connectionService.getConnection(connectionId)).thenReturn(createEmbeddedConnection());

        assertThatThrownBy(() -> workspaceConnectionFacade.getConnectionWorkflows(connectionId))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    public void testDisconnectConnectionRejectsUnknownConnection() {
        long unknownConnectionId = 11111L;

        when(connectionService.getConnection(unknownConnectionId))
            .thenThrow(new NoSuchElementException("Connection does not exist for id=" + unknownConnectionId));

        assertThatThrownBy(() -> workspaceConnectionFacade.disconnectConnection(unknownConnectionId))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Connection does not exist for id=" + unknownConnectionId);
    }

    @Test
    public void testGetConnectionTagsExcludesTagsOfOtherWorkspaces() {
        Workspace otherWorkspace = workspaceRepository.save(new Workspace("other"));

        Tag workspaceTag = tagRepository.save(new Tag("workspace-connection-tag"));
        Tag otherWorkspaceTag = tagRepository.save(new Tag("other-workspace-connection-tag"));

        workspaceConnectionRepository.save(new WorkspaceConnection(10L, workspace.getId()));
        workspaceConnectionRepository.save(new WorkspaceConnection(20L, otherWorkspace.getId()));

        Connection connection = createConnection(ConnectionStatus.ACTIVE, 0);

        connection.setTags(List.of(workspaceTag));

        Connection otherConnection = createConnection(ConnectionStatus.ACTIVE, 0);

        otherConnection.setTags(List.of(otherWorkspaceTag));

        when(connectionService.getConnections(List.of(10L))).thenReturn(List.of(connection));
        when(connectionService.getConnections(List.of(20L))).thenReturn(List.of(otherConnection));

        assertThat(workspaceConnectionFacade.getConnectionTags(workspace.getId()))
            .extracting(Tag::getName)
            .containsExactly("workspace-connection-tag");
        assertThat(workspaceConnectionFacade.getConnectionTags(otherWorkspace.getId()))
            .extracting(Tag::getName)
            .containsExactly("other-workspace-connection-tag");
    }

    @Test
    public void testGetConnectionTagsLeavesOutEmbeddedConnections() {
        Tag embeddedTag = tagRepository.save(new Tag("embedded-connection-tag"));

        workspaceConnectionRepository.save(new WorkspaceConnection(30L, workspace.getId()));

        Connection embeddedConnection = createEmbeddedConnection();

        embeddedConnection.setTags(List.of(embeddedTag));

        when(connectionService.getConnections(List.of(30L))).thenReturn(List.of(embeddedConnection));

        assertThat(workspaceConnectionFacade.getConnectionTags(workspace.getId())).isEmpty();
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
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Connection does not exist for id=1");

        verify(connectionFacade, never()).updateCredentials(anyLong(), anyMap(), anyInt());
    }

    @Test
    public void testGetConnectionReturnsAutomationConnection() {
        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .id(1L)
            .build();

        when(connectionService.getConnection(1L)).thenReturn(createConnection(ConnectionStatus.ACTIVE, 3));
        when(connectionFacade.getConnection(1L)).thenReturn(connectionDTO);

        assertThat(workspaceConnectionFacade.getConnection(1L)).isSameAs(connectionDTO);
    }

    @Test
    public void testGetConnectionRejectsEmbeddedConnection() {
        Connection connection = createConnection(ConnectionStatus.ACTIVE, 3);

        connection.setType(PlatformType.EMBEDDED);

        when(connectionService.getConnection(1L)).thenReturn(connection);

        assertThatThrownBy(() -> workspaceConnectionFacade.getConnection(1L))
            .isInstanceOf(NoSuchElementException.class);

        verify(connectionFacade, never()).getConnection(anyLong());
    }

    @Test
    public void testDeleteDelegatesForAutomationConnection() {
        when(connectionService.getConnection(1L)).thenReturn(createConnection(ConnectionStatus.ACTIVE, 3));

        workspaceConnectionFacade.delete(1L);

        verify(connectionFacade).delete(1L);
    }

    @Test
    public void testDeleteRejectsEmbeddedConnection() {
        when(connectionService.getConnection(1L)).thenReturn(createEmbeddedConnection());

        assertThatThrownBy(() -> workspaceConnectionFacade.delete(1L))
            .isInstanceOf(NoSuchElementException.class);

        verify(connectionFacade, never()).delete(anyLong());
    }

    @Test
    public void testDisconnectConnectionRejectsEmbeddedConnection() {
        when(connectionService.getConnection(1L)).thenReturn(createEmbeddedConnection());

        assertThatThrownBy(() -> workspaceConnectionFacade.disconnectConnection(1L))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("Connection does not exist for id=1");
    }

    @Test
    public void testUpdateDelegatesForAutomationConnection() {
        List<Tag> tags = List.of(new Tag("tag1"));

        when(connectionService.getConnection(1L)).thenReturn(createConnection(ConnectionStatus.ACTIVE, 3));

        workspaceConnectionFacade.update(1L, "renamed", tags, 3);
        workspaceConnectionFacade.update(1L, "renamed", Map.of("baseUri", "https://new.example.com"), tags, 3);

        verify(connectionFacade).update(1L, "renamed", tags, 3);
        verify(connectionFacade).update(1L, "renamed", Map.of("baseUri", "https://new.example.com"), tags, 3);
    }

    @Test
    public void testUpdateRejectsEmbeddedConnection() {
        when(connectionService.getConnection(1L)).thenReturn(createEmbeddedConnection());

        assertThatThrownBy(() -> workspaceConnectionFacade.update(1L, "renamed", List.of(), 3))
            .isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(
            () -> workspaceConnectionFacade.update(
                1L, "renamed", Map.of("baseUri", "https://attacker.example.com"), List.of(), 3))
                    .isInstanceOf(NoSuchElementException.class);

        verify(connectionFacade, never()).update(anyLong(), anyString(), anyList(), anyInt());
        verify(connectionFacade, never()).update(anyLong(), anyString(), anyMap(), anyList(), anyInt());
    }

    @Test
    public void testUpdateConnectionTagsDelegatesForAutomationConnection() {
        List<Tag> tags = List.of(new Tag("tag1"));

        when(connectionService.getConnection(1L)).thenReturn(createConnection(ConnectionStatus.ACTIVE, 3));

        workspaceConnectionFacade.updateConnectionTags(1L, tags);

        verify(connectionFacade).update(1L, tags);
    }

    @Test
    public void testUpdateConnectionTagsRejectsEmbeddedConnection() {
        when(connectionService.getConnection(1L)).thenReturn(createEmbeddedConnection());

        assertThatThrownBy(() -> workspaceConnectionFacade.updateConnectionTags(1L, List.of(new Tag("tag1"))))
            .isInstanceOf(NoSuchElementException.class);

        verify(connectionFacade, never()).update(anyLong(), anyList());
    }

    private ProjectDeploymentWorkflow saveProjectDeploymentWorkflowConnections(
        long projectDeploymentId, List<Long> connectionIds) {

        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows =
            projectDeploymentWorkflowRepository.findAllByProjectDeploymentId(projectDeploymentId);

        assertThat(projectDeploymentWorkflows).hasSize(1);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflows.getFirst();

        projectDeploymentWorkflow.setConnections(
            connectionIds.stream()
                .map(connectionId -> new ProjectDeploymentWorkflowConnection(
                    connectionId, "connectionKey" + connectionId, "nodeName"))
                .toList());

        return projectDeploymentWorkflowRepository.save(projectDeploymentWorkflow);
    }

    private WorkflowTestConfiguration saveWorkflowTestConfiguration(String workflowId, long connectionId) {
        WorkflowTestConfiguration workflowTestConfiguration = new WorkflowTestConfiguration();

        workflowTestConfiguration.setEnvironmentId(1L);
        workflowTestConfiguration.setWorkflowId(workflowId);
        workflowTestConfiguration.setConnections(
            List.of(new WorkflowTestConfigurationConnection(connectionId, "connectionKey", "nodeName")));

        return workflowTestConfigurationRepository.save(workflowTestConfiguration);
    }

    private static Connection createConnection(ConnectionStatus status, int version) {
        Connection connection = new Connection();

        connection.setId(1L);
        connection.setStatus(status);
        connection.setVersion(version);

        return connection;
    }

    private static Connection createEmbeddedConnection() {
        Connection connection = createConnection(ConnectionStatus.ACTIVE, 3);

        connection.setType(PlatformType.EMBEDDED);

        return connection;
    }

    private ProjectDeploymentWorkflow findProjectDeploymentWorkflow(long id) {
        Optional<ProjectDeploymentWorkflow> projectDeploymentWorkflowOptional =
            projectDeploymentWorkflowRepository.findById(id);

        return projectDeploymentWorkflowOptional.orElseThrow();
    }

    private WorkflowTestConfiguration findWorkflowTestConfiguration(long id) {
        Optional<WorkflowTestConfiguration> workflowTestConfigurationOptional =
            workflowTestConfigurationRepository.findById(id);

        return workflowTestConfigurationOptional.orElseThrow();
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
        void testDisconnectFromOneWorkflowIsAdminOnly() {
            assertRequiresAdminAuthority(
                () -> workspaceConnectionFacade.disconnectConnection(1L, ConnectionWorkflowDTO.Usage.EDITOR, 1L));
        }

        @Test
        void testGetConnectionWorkflowsDeniesAUserWithoutWorkflowViewOnTheConnectionWorkspace() {
            assertDenied(() -> workspaceConnectionFacade.getConnectionWorkflows(1L));
        }

        @Test
        void testGetConnectionWorkflowsAdmitsAUserWithWorkflowViewOnTheConnectionWorkspace() {
            when(permissionService.hasResourceScope(1L, "Connection", "WORKFLOW_VIEW")).thenReturn(true);

            assertNotDenied(() -> workspaceConnectionFacade.getConnectionWorkflows(1L));
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
