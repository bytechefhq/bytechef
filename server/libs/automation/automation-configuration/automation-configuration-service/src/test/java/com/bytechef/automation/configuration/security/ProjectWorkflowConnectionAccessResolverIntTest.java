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

package com.bytechef.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.automation.configuration.util.ProjectDeploymentFacadeHelper;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacadeImpl;
import com.bytechef.platform.configuration.repository.WorkflowTestConfigurationRepository;
import com.bytechef.platform.configuration.security.WorkflowConnectionAccessResolver;
import com.bytechef.platform.configuration.security.WorkflowConnectionAccessResolver.ConnectionAccess;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        ProjectIntTestConfiguration.class,
        ProjectWorkflowConnectionAccessResolverIntTest.WorkflowTestConfigurationFacadeConfiguration.class
    },
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
@ProjectIntTestConfigurationSharedMocks
class ProjectWorkflowConnectionAccessResolverIntTest {

    private static final long ENVIRONMENT_ID = 0;
    private static final long FOREIGN_CONNECTION_ID = 9002;
    private static final long OTHER_ENVIRONMENT_CONNECTION_ID = 9003;
    private static final long OTHER_ENVIRONMENT_ID = 1;
    private static final long OWN_CONNECTION_ID = 9001;
    private static final String WORKFLOW_DEFINITION = """
        {
            "label": "Connection Access",
            "tasks": [
                {
                    "name": "slack_1",
                    "type": "slack/v1/sendChannelMessage",
                    "parameters": {}
                }
            ]
        }
        """;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private ProjectDeploymentFacade projectDeploymentFacade;

    @Autowired
    private ProjectFacade projectFacade;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectWorkflowConnectionAccessResolver projectWorkflowConnectionAccessResolver;

    @Autowired
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private WorkflowTestConfigurationFacade workflowTestConfigurationFacade;

    @Autowired
    private WorkflowTestConfigurationRepository workflowTestConfigurationRepository;

    @Autowired
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    @Autowired
    private WorkspaceConnectionRepository workspaceConnectionRepository;

    @Autowired
    private WorkspaceConnectionService workspaceConnectionService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private String workflowId;

    @BeforeEach
    void beforeEach() {
        Workspace workspace = workspaceRepository.save(new Workspace("connection-access-own"));
        Workspace foreignWorkspace = workspaceRepository.save(new Workspace("connection-access-foreign"));

        workspaceConnectionService.create(OWN_CONNECTION_ID, workspace.getId());
        workspaceConnectionService.create(FOREIGN_CONNECTION_ID, foreignWorkspace.getId());
        workspaceConnectionService.create(OTHER_ENVIRONMENT_CONNECTION_ID, workspace.getId());

        ProjectDeploymentFacadeHelper projectDeploymentFacadeHelper = new ProjectDeploymentFacadeHelper(
            categoryRepository, projectFacade, projectRepository, projectDeploymentFacade, projectWorkflowFacade,
            projectWorkflowRepository);

        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectWorkflowDTO projectWorkflowDTO = projectDeploymentFacadeHelper.addTestWorkflow(
            projectDTO, WORKFLOW_DEFINITION);

        workflowId = projectWorkflowDTO.getId();

        when(connectionService.getConnection(OWN_CONNECTION_ID)).thenReturn(createConnection(ENVIRONMENT_ID));
        when(connectionService.getConnection(FOREIGN_CONNECTION_ID)).thenReturn(createConnection(ENVIRONMENT_ID));
        when(connectionService.getConnection(OTHER_ENVIRONMENT_CONNECTION_ID))
            .thenReturn(createConnection(OTHER_ENVIRONMENT_ID));
        when(componentConnectionFacade.getComponentConnection(workflowId, "slack_1", "slack"))
            .thenReturn(new ComponentConnection("slack", 1, "slack_1", "slack", true));
        when(componentConnectionFacade.getClusterElementComponentConnection(
            workflowId, "aiAgent_1", "TOOLS", "slack_1", "slack"))
                .thenReturn(new ComponentConnection("slack", 1, "aiAgent_1", "slack", true));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        workflowTestConfigurationRepository.deleteAll();
        workspaceConnectionRepository.deleteAll();
        projectWorkflowRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
        categoryRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    void testResolveConnectionAccessGrantsAConnectionOfTheWorkflowWorkspace() {
        assertThat(projectWorkflowConnectionAccessResolver.resolveConnectionAccess(workflowId, OWN_CONNECTION_ID,
            ENVIRONMENT_ID))
                .isEqualTo(ConnectionAccess.GRANTED);
    }

    @Test
    void testResolveConnectionAccessDeniesAConnectionOfAnotherWorkspace() {
        assertThat(projectWorkflowConnectionAccessResolver.resolveConnectionAccess(workflowId, FOREIGN_CONNECTION_ID,
            ENVIRONMENT_ID))
                .isEqualTo(ConnectionAccess.DENIED);
    }

    @Test
    void testResolveConnectionAccessAbstainsForAWorkflowOutsideAnyProject() {
        assertThat(projectWorkflowConnectionAccessResolver.resolveConnectionAccess("unknown", OWN_CONNECTION_ID,
            ENVIRONMENT_ID))
                .isEqualTo(ConnectionAccess.ABSTAIN);
    }

    @Test
    void testSaveWorkflowTestConfigurationConnectionBindsAConnectionOfTheWorkflowWorkspace() {
        authenticate(AuthorityConstants.USER);

        workflowTestConfigurationFacade.saveWorkflowTestConfigurationConnection(
            workflowId, "slack_1", "slack", OWN_CONNECTION_ID, ENVIRONMENT_ID);

        assertThat(fetchBoundConnectionId()).contains(OWN_CONNECTION_ID);
    }

    @Test
    void testSaveWorkflowTestConfigurationConnectionRejectsAConnectionOfAnotherWorkspace() {
        authenticate(AuthorityConstants.USER);

        assertThatThrownBy(() -> workflowTestConfigurationFacade.saveWorkflowTestConfigurationConnection(
            workflowId, "slack_1", "slack", FOREIGN_CONNECTION_ID, ENVIRONMENT_ID))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(fetchBoundConnectionId()).isEmpty();
    }

    @Test
    void testSaveWorkflowTestConfigurationConnectionLetsATenantAdminBindAConnectionOfAnotherWorkspace() {
        authenticate(AuthorityConstants.ADMIN);

        workflowTestConfigurationFacade.saveWorkflowTestConfigurationConnection(
            workflowId, "slack_1", "slack", FOREIGN_CONNECTION_ID, ENVIRONMENT_ID);

        assertThat(fetchBoundConnectionId()).contains(FOREIGN_CONNECTION_ID);
    }

    @Test
    void testSaveWorkflowTestConnectionRejectsAConnectionOfAnotherWorkspaceWithoutAnAuthentication() {
        assertThatThrownBy(() -> projectWorkflowFacade.saveWorkflowTestConnection(
            workflowId, "slack_1", "slack", FOREIGN_CONNECTION_ID, ENVIRONMENT_ID))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(fetchBoundConnectionId()).isEmpty();
    }

    @Test
    void testSaveWorkflowTestConnectionBindsAConnectionOfTheWorkflowWorkspaceWithoutAnAuthentication() {
        projectWorkflowFacade.saveWorkflowTestConnection(
            workflowId, "slack_1", "slack", OWN_CONNECTION_ID, ENVIRONMENT_ID);

        assertThat(fetchBoundConnectionId()).contains(OWN_CONNECTION_ID);
    }

    @Test
    void testSaveWorkflowTestConfigurationRejectsAConnectionOfAnotherWorkspace() {
        authenticate(AuthorityConstants.USER);

        WorkflowTestConfiguration workflowTestConfiguration = new WorkflowTestConfiguration();

        workflowTestConfiguration.setConnections(
            List.of(new WorkflowTestConfigurationConnection(FOREIGN_CONNECTION_ID, "slack", "slack_1")));
        workflowTestConfiguration.setEnvironmentId(ENVIRONMENT_ID);
        workflowTestConfiguration.setInputs(Map.of());
        workflowTestConfiguration.setWorkflowId(workflowId);

        assertThatThrownBy(() -> workflowTestConfigurationFacade.saveWorkflowTestConfiguration(
            workflowTestConfiguration))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(workflowTestConfigurationService.fetchWorkflowTestConfiguration(workflowId, ENVIRONMENT_ID))
            .isEmpty();
    }

    @Test
    void testSaveWorkflowTestConfigurationKeepsAnUnchangedConnectionATenantAdminBound() {
        authenticate(AuthorityConstants.ADMIN);

        workflowTestConfigurationFacade.saveWorkflowTestConfigurationConnection(
            workflowId, "slack_1", "slack", FOREIGN_CONNECTION_ID, ENVIRONMENT_ID);

        authenticate(AuthorityConstants.USER);

        workflowTestConfigurationFacade.saveWorkflowTestConfiguration(
            createWorkflowTestConfiguration(FOREIGN_CONNECTION_ID, Map.of("key", "value")));

        Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            workflowId, ENVIRONMENT_ID);

        assertThat(fetchBoundConnectionId()).contains(FOREIGN_CONNECTION_ID);
        assertThat(inputs.get("key")).isEqualTo("value");
    }

    @Test
    void testSaveWorkflowTestConfigurationRejectsAChangeToAConnectionOfAnotherWorkspace() {
        authenticate(AuthorityConstants.USER);

        workflowTestConfigurationFacade.saveWorkflowTestConfigurationConnection(
            workflowId, "slack_1", "slack", OWN_CONNECTION_ID, ENVIRONMENT_ID);

        assertThatThrownBy(() -> workflowTestConfigurationFacade.saveWorkflowTestConfiguration(
            createWorkflowTestConfiguration(FOREIGN_CONNECTION_ID, Map.of())))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(fetchBoundConnectionId()).contains(OWN_CONNECTION_ID);
    }

    private WorkflowTestConfiguration createWorkflowTestConfiguration(long connectionId, Map<String, Object> inputs) {
        WorkflowTestConfiguration workflowTestConfiguration = new WorkflowTestConfiguration();

        workflowTestConfiguration.setConnections(
            List.of(new WorkflowTestConfigurationConnection(connectionId, "slack", "slack_1")));
        workflowTestConfiguration.setEnvironmentId(ENVIRONMENT_ID);
        workflowTestConfiguration.setInputs(inputs);
        workflowTestConfiguration.setWorkflowId(workflowId);

        return workflowTestConfiguration;
    }

    @Test
    void testResolveConnectionAccessDeniesAConnectionOfTheWorkflowWorkspaceInAnotherEnvironment() {
        assertThat(
            projectWorkflowConnectionAccessResolver.resolveConnectionAccess(
                workflowId, OTHER_ENVIRONMENT_CONNECTION_ID, ENVIRONMENT_ID))
                    .isEqualTo(ConnectionAccess.DENIED);
        assertThat(
            projectWorkflowConnectionAccessResolver.resolveConnectionAccess(
                workflowId, OTHER_ENVIRONMENT_CONNECTION_ID, OTHER_ENVIRONMENT_ID))
                    .isEqualTo(ConnectionAccess.GRANTED);
    }

    @Test
    void testSaveWorkflowTestConfigurationConnectionRejectsAConnectionOfAnotherEnvironment() {
        authenticate(AuthorityConstants.USER);

        assertThatThrownBy(() -> workflowTestConfigurationFacade.saveWorkflowTestConfigurationConnection(
            workflowId, "slack_1", "slack", OTHER_ENVIRONMENT_CONNECTION_ID, ENVIRONMENT_ID))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(fetchBoundConnectionId()).isEmpty();
    }

    @Test
    void testSaveClusterElementTestConfigurationConnectionBindsAConnectionOfTheWorkflowWorkspaceAndEnvironment() {
        authenticate(AuthorityConstants.USER);

        workflowTestConfigurationFacade.saveClusterElementTestConfigurationConnection(
            workflowId, "aiAgent_1", "TOOLS", "slack_1", "slack", OWN_CONNECTION_ID, ENVIRONMENT_ID);

        assertThat(fetchBoundConnectionId("aiAgent_1")).contains(OWN_CONNECTION_ID);
    }

    @Test
    void testSaveClusterElementTestConfigurationConnectionRejectsAConnectionOfAnotherEnvironment() {
        authenticate(AuthorityConstants.USER);

        assertThatThrownBy(() -> workflowTestConfigurationFacade.saveClusterElementTestConfigurationConnection(
            workflowId, "aiAgent_1", "TOOLS", "slack_1", "slack", OTHER_ENVIRONMENT_CONNECTION_ID, ENVIRONMENT_ID))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(fetchBoundConnectionId("aiAgent_1")).isEmpty();
    }

    @Test
    void testSaveWorkflowTestConfigurationBindsAConnectionOfTheWorkflowWorkspaceAndEnvironment() {
        authenticate(AuthorityConstants.USER);

        workflowTestConfigurationFacade.saveWorkflowTestConfiguration(
            createWorkflowTestConfiguration(OWN_CONNECTION_ID, Map.of()));

        assertThat(fetchBoundConnectionId()).contains(OWN_CONNECTION_ID);
    }

    @Test
    void testSaveWorkflowTestConfigurationRejectsAConnectionOfAnotherEnvironment() {
        authenticate(AuthorityConstants.USER);

        assertThatThrownBy(() -> workflowTestConfigurationFacade.saveWorkflowTestConfiguration(
            createWorkflowTestConfiguration(OTHER_ENVIRONMENT_CONNECTION_ID, Map.of())))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(fetchBoundConnectionId()).isEmpty();
    }

    @Test
    void testSaveWorkflowTestConnectionRejectsAConnectionOfAnotherEnvironmentWithoutAnAuthentication() {
        assertThatThrownBy(() -> projectWorkflowFacade.saveWorkflowTestConnection(
            workflowId, "slack_1", "slack", OTHER_ENVIRONMENT_CONNECTION_ID, ENVIRONMENT_ID))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(fetchBoundConnectionId()).isEmpty();
    }

    private static Connection createConnection(long environmentId) {
        Connection connection = new Connection();

        connection.setComponentName("slack");
        connection.setEnvironmentId(Math.toIntExact(environmentId));

        return connection;
    }

    private static void authenticate(String authority) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                    "user@localhost.com", null, List.of(new SimpleGrantedAuthority(authority))));
    }

    private Optional<Long> fetchBoundConnectionId() {
        return fetchBoundConnectionId("slack_1");
    }

    private Optional<Long> fetchBoundConnectionId(String workflowNodeName) {
        return workflowTestConfigurationService.fetchWorkflowTestConfigurationConnectionId(
            workflowId, workflowNodeName, ENVIRONMENT_ID);
    }

    @TestConfiguration
    static class WorkflowTestConfigurationFacadeConfiguration {

        @Bean
        WorkflowTestConfigurationFacade workflowTestConfigurationFacade(
            ComponentConnectionFacade componentConnectionFacade, ConnectionService connectionService,
            ObjectProvider<WorkflowConnectionAccessResolver> workflowConnectionAccessResolvers,
            WorkflowService workflowService, WorkflowTestConfigurationService workflowTestConfigurationService) {

            return new WorkflowTestConfigurationFacadeImpl(
                connectionService, componentConnectionFacade, workflowConnectionAccessResolvers, workflowService,
                workflowTestConfigurationService);
        }
    }
}
