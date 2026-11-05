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

package com.bytechef.automation.ai.mcp.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.config.McpProjectIntTestConfiguration;
import com.bytechef.automation.ai.mcp.config.McpProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.ai.mcp.domain.WorkspaceMcpServer;
import com.bytechef.automation.ai.mcp.repository.WorkspaceMcpServerRepository;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ClusterElementPropertyFacade;
import com.bytechef.platform.configuration.facade.ClusterElementPropertyFacadeImpl;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = McpProjectIntTestConfiguration.class)
@EnableMethodSecurity
@Import({
    ClusterElementPropertyFacadeImpl.class, PostgreSQLContainerConfiguration.class
})
@McpProjectIntTestConfigurationSharedMocks
class McpComponentConnectionSecurityIntTest {

    private static final long FOREIGN_WORKSPACE_CONNECTION_ID = 9203L;
    private static final long OTHER_ENVIRONMENT_CONNECTION_ID = 9202L;
    private static final long SAME_ENVIRONMENT_CONNECTION_ID = 9201L;

    @MockitoBean
    private ClusterElementDefinitionFacade clusterElementDefinitionFacade;

    @Autowired
    private ClusterElementPropertyFacade clusterElementPropertyFacade;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private McpComponentConnectionSecurity mcpComponentConnectionSecurity;

    @Autowired
    private McpComponentRepository mcpComponentRepository;

    @Autowired
    private McpComponentService mcpComponentService;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @Autowired
    private WorkspaceConnectionRepository workspaceConnectionRepository;

    @Autowired
    private WorkspaceConnectionService workspaceConnectionService;

    @Autowired
    private WorkspaceMcpServerRepository workspaceMcpServerRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private long embeddedMcpComponentId;
    private long mcpComponentId;
    private long mcpServerId;

    @BeforeEach
    void beforeEach() {
        authenticate(AuthorityConstants.ADMIN);

        Workspace workspace = workspaceRepository.save(new Workspace("mcp-component-connection"));
        Workspace foreignWorkspace = workspaceRepository.save(new Workspace("mcp-component-connection-foreign"));

        workspaceConnectionService.create(SAME_ENVIRONMENT_CONNECTION_ID, workspace.getId());
        workspaceConnectionService.create(OTHER_ENVIRONMENT_CONNECTION_ID, workspace.getId());
        workspaceConnectionService.create(FOREIGN_WORKSPACE_CONNECTION_ID, foreignWorkspace.getId());

        McpServer mcpServer = mcpServerRepository.save(
            new McpServer("mcp-component-connection", PlatformType.AUTOMATION, Environment.DEVELOPMENT));

        mcpServerId = mcpServer.getId();

        workspaceMcpServerRepository.save(new WorkspaceMcpServer(mcpServerId, workspace.getId()));

        McpComponent mcpComponent = mcpComponentRepository.save(
            new McpComponent("slack", 1, mcpServer.getId(), null));

        mcpComponentId = mcpComponent.getId();

        McpServer embeddedMcpServer = mcpServerRepository.save(
            new McpServer("mcp-component-connection-embedded", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

        McpComponent embeddedMcpComponent = mcpComponentRepository.save(
            new McpComponent("slack", 1, embeddedMcpServer.getId(), null));

        embeddedMcpComponentId = embeddedMcpComponent.getId();

        when(connectionService.getConnection(SAME_ENVIRONMENT_CONNECTION_ID))
            .thenReturn(createConnection(Environment.DEVELOPMENT));
        when(connectionService.getConnection(OTHER_ENVIRONMENT_CONNECTION_ID))
            .thenReturn(createConnection(Environment.PRODUCTION));
        when(connectionService.getConnection(FOREIGN_WORKSPACE_CONNECTION_ID))
            .thenReturn(createConnection(Environment.DEVELOPMENT));

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void afterEach() {
        authenticate(AuthorityConstants.ADMIN);

        mcpComponentRepository.deleteAll();
        workspaceMcpServerRepository.deleteAll();
        mcpServerRepository.deleteAll();
        workspaceConnectionRepository.deleteAll();
        workspaceRepository.deleteAll();

        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateMcpComponentAcceptsAConnectionOfTheMcpServerWorkspaceAndEnvironment() {
        authenticate(AuthorityConstants.USER);

        McpComponent mcpComponent = mcpComponentService.create(
            new McpComponent("gmail", 1, mcpServerId, SAME_ENVIRONMENT_CONNECTION_ID), List.of());

        assertThat(mcpComponent.getConnectionId()).isEqualTo(SAME_ENVIRONMENT_CONNECTION_ID);
    }

    @Test
    void testCreateMcpComponentRejectsAConnectionOfAnotherWorkspace() {
        authenticate(AuthorityConstants.USER);

        McpComponent mcpComponent = new McpComponent("gmail", 1, mcpServerId, FOREIGN_WORKSPACE_CONNECTION_ID);

        assertThatThrownBy(() -> mcpComponentService.create(mcpComponent, List.of()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> mcpComponentService.create(mcpComponent))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testCreateMcpComponentRejectsAConnectionOfTheSameWorkspaceInAnotherEnvironment() {
        authenticate(AuthorityConstants.USER);

        McpComponent mcpComponent = new McpComponent("gmail", 1, mcpServerId, OTHER_ENVIRONMENT_CONNECTION_ID);

        assertThatThrownBy(() -> mcpComponentService.create(mcpComponent, List.of()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testCreateMcpComponentLetsATenantAdminUseAConnectionOfAnotherWorkspace() {
        authenticate(AuthorityConstants.ADMIN);

        McpComponent mcpComponent = mcpComponentService.create(
            new McpComponent("gmail", 1, mcpServerId, FOREIGN_WORKSPACE_CONNECTION_ID), List.of());

        assertThat(mcpComponent.getConnectionId()).isEqualTo(FOREIGN_WORKSPACE_CONNECTION_ID);
    }

    @Test
    void testUpdateMcpComponentAcceptsAConnectionOfTheMcpServerWorkspaceAndEnvironment() {
        authenticate(AuthorityConstants.USER);

        McpComponent mcpComponent = mcpComponentService.update(
            createMcpComponentUpdate(SAME_ENVIRONMENT_CONNECTION_ID), null, List.of());

        assertThat(mcpComponent.getConnectionId()).isEqualTo(SAME_ENVIRONMENT_CONNECTION_ID);
    }

    @Test
    void testUpdateMcpComponentRejectsAConnectionOfAnotherWorkspace() {
        authenticate(AuthorityConstants.USER);

        McpComponent mcpComponentUpdate = createMcpComponentUpdate(FOREIGN_WORKSPACE_CONNECTION_ID);

        assertThatThrownBy(() -> mcpComponentService.update(mcpComponentUpdate, null, List.of()))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(mcpComponentRepository.findById(mcpComponentId))
            .hasValueSatisfying(mcpComponent -> assertThat(mcpComponent.getConnectionId()).isNull());
    }

    @Test
    void testUpdateMcpComponentRejectsAConnectionOfTheSameWorkspaceInAnotherEnvironment() {
        authenticate(AuthorityConstants.USER);

        McpComponent mcpComponentUpdate = createMcpComponentUpdate(OTHER_ENVIRONMENT_CONNECTION_ID);

        assertThatThrownBy(() -> mcpComponentService.update(mcpComponentUpdate, null, List.of()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testUpdateMcpComponentKeepsAnUnchangedConnectionATenantAdminBound() {
        authenticate(AuthorityConstants.ADMIN);

        mcpComponentService.update(createMcpComponentUpdate(FOREIGN_WORKSPACE_CONNECTION_ID), null, List.of());

        authenticate(AuthorityConstants.USER);

        McpComponent mcpComponent = mcpComponentService.update(
            createMcpComponentUpdate(FOREIGN_WORKSPACE_CONNECTION_ID), null, List.of());

        assertThat(mcpComponent.getConnectionId()).isEqualTo(FOREIGN_WORKSPACE_CONNECTION_ID);
    }

    @Test
    void testIsMcpComponentConnectionRequiresTheWorkspaceAndEnvironmentOfTheMcpServer() {
        assertThat(
            mcpComponentConnectionSecurity.isMcpComponentConnection(mcpComponentId, SAME_ENVIRONMENT_CONNECTION_ID))
                .isTrue();
        assertThat(
            mcpComponentConnectionSecurity.isMcpComponentConnection(mcpComponentId, OTHER_ENVIRONMENT_CONNECTION_ID))
                .isFalse();
        assertThat(
            mcpComponentConnectionSecurity.isMcpComponentConnection(mcpComponentId, FOREIGN_WORKSPACE_CONNECTION_ID))
                .isFalse();
        assertThat(
            mcpComponentConnectionSecurity.isMcpComponentConnection(
                embeddedMcpComponentId, SAME_ENVIRONMENT_CONNECTION_ID))
                    .isFalse();
        assertThat(
            mcpComponentConnectionSecurity.isMcpComponentConnection(Long.MAX_VALUE, SAME_ENVIRONMENT_CONNECTION_ID))
                .isFalse();
    }

    @Test
    void testGetClusterElementOptionsAllowsAConnectionInTheMcpServerEnvironment() {
        authenticate(AuthorityConstants.USER);

        getClusterElementOptions(mcpComponentId, SAME_ENVIRONMENT_CONNECTION_ID);

        verifyClusterElementOptionsExecuted(SAME_ENVIRONMENT_CONNECTION_ID);
    }

    @Test
    void testGetClusterElementOptionsRejectsAConnectionOfTheSameWorkspaceInAnotherEnvironment() {
        authenticate(AuthorityConstants.USER);

        assertThatThrownBy(() -> getClusterElementOptions(mcpComponentId, OTHER_ENVIRONMENT_CONNECTION_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testGetClusterElementOptionsRejectsAConnectionOfAnotherWorkspace() {
        authenticate(AuthorityConstants.USER);

        assertThatThrownBy(() -> getClusterElementOptions(mcpComponentId, FOREIGN_WORKSPACE_CONNECTION_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testGetClusterElementOptionsRejectsAnEmbeddedMcpComponentForANonAdmin() {
        authenticate(AuthorityConstants.USER);

        assertThatThrownBy(() -> getClusterElementOptions(embeddedMcpComponentId, SAME_ENVIRONMENT_CONNECTION_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testGetClusterElementOptionsAllowsAMissingConnection() {
        authenticate(AuthorityConstants.USER);

        getClusterElementOptions(mcpComponentId, null);

        verifyClusterElementOptionsExecuted(null);
    }

    @Test
    void testGetClusterElementOptionsLetsATenantAdminUseAConnectionOfAnotherEnvironment() {
        authenticate(AuthorityConstants.ADMIN);

        getClusterElementOptions(mcpComponentId, OTHER_ENVIRONMENT_CONNECTION_ID);

        verifyClusterElementOptionsExecuted(OTHER_ENVIRONMENT_CONNECTION_ID);
    }

    @Test
    void testGetClusterElementDynamicPropertiesAllowsAConnectionInTheMcpServerEnvironment() {
        authenticate(AuthorityConstants.USER);

        getClusterElementDynamicProperties(SAME_ENVIRONMENT_CONNECTION_ID);

        verify(clusterElementDefinitionFacade).executeDynamicProperties(
            "component", 1, "clusterElement", "property", Map.of(), List.of(), SAME_ENVIRONMENT_CONNECTION_ID);
    }

    @Test
    void testGetClusterElementDynamicPropertiesRejectsAConnectionOfTheSameWorkspaceInAnotherEnvironment() {
        authenticate(AuthorityConstants.USER);

        assertThatThrownBy(() -> getClusterElementDynamicProperties(OTHER_ENVIRONMENT_CONNECTION_ID))
            .isInstanceOf(AccessDeniedException.class);
    }

    private static void authenticate(String authority) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                    "user@localhost.com", null, List.of(new SimpleGrantedAuthority(authority))));
    }

    private McpComponent createMcpComponentUpdate(long connectionId) {
        McpComponent currentMcpComponent = mcpComponentRepository.findById(mcpComponentId)
            .orElseThrow();

        currentMcpComponent.setConnectionId(connectionId);

        return currentMcpComponent;
    }

    private static Connection createConnection(Environment environment) {
        Connection connection = new Connection();

        connection.setComponentName("slack");
        connection.setEnvironmentId(environment.ordinal());

        return connection;
    }

    private void getClusterElementDynamicProperties(long connectionId) {
        clusterElementPropertyFacade.getClusterElementDynamicProperties(
            "component", 1, "clusterElement", "property", Map.of(), List.of(), mcpComponentId, connectionId);
    }

    private void getClusterElementOptions(long curMcpComponentId, Long connectionId) {
        clusterElementPropertyFacade.getClusterElementOptions(
            "component", 1, "clusterElement", "property", Map.of(), List.of(), curMcpComponentId, connectionId);
    }

    private void verifyClusterElementOptionsExecuted(Long connectionId) {
        verify(clusterElementDefinitionFacade).executeOptions(
            "component", 1, "clusterElement", "property", Map.of(), Map.of(), List.of(), null, connectionId,
            Map.of(), Map.of());
    }
}
