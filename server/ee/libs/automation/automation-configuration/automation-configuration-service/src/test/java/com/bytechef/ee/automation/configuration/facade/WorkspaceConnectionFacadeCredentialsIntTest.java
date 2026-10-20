/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacadeImpl;
import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.security.ConnectionEnvironmentResolver;
import com.bytechef.automation.configuration.security.ConnectionOwnershipResolver;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.ee.automation.configuration.config.EeAutomationConfigurationIntTestConfiguration;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.automation.configuration.security.scope.ApiKeyPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ApiPlatformPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ConnectionPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.DataTablePermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.DeploymentPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ExecutionPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.KnowledgeBasePermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.McpPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ProjectPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.WorkflowPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.WorkspacePermissionScopeProvider;
import com.bytechef.ee.automation.configuration.service.CurrentUserResolver;
import com.bytechef.ee.automation.configuration.service.CustomRoleScopeResolverImpl;
import com.bytechef.ee.automation.configuration.service.EmbeddedScopeCacheService;
import com.bytechef.ee.automation.configuration.service.PermissionScopeRegistry;
import com.bytechef.ee.automation.configuration.service.PermissionServiceImpl;
import com.bytechef.ee.automation.configuration.service.WorkspaceScopeCacheService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        EeAutomationConfigurationIntTestConfiguration.class,
        WorkspaceConnectionFacadeCredentialsIntTest.RealPermissionServiceConfiguration.class
    })
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
class WorkspaceConnectionFacadeCredentialsIntTest {

    private static final String DEVELOPMENT_EDITOR_PRODUCTION_VIEWER = "development-editor-production-viewer";
    private static final String EDITOR = "editor";
    private static final String TENANT_ADMIN = "tenant-admin";
    private static final String VIEWER = "viewer";
    private static final Map<String, Long> USER_IDS_BY_LOGIN = Map.of(
        DEVELOPMENT_EDITOR_PRODUCTION_VIEWER, 9203L, EDITOR, 9202L, TENANT_ADMIN, 9204L, VIEWER, 9201L);
    private static final int DEVELOPMENT = Environment.DEVELOPMENT.ordinal();
    private static final int PRODUCTION = Environment.PRODUCTION.ordinal();

    @Autowired
    private ConnectionFacade connectionFacade;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private WorkspaceConnectionFacade workspaceConnectionFacade;

    @Autowired
    private WorkspaceConnectionRepository workspaceConnectionRepository;

    @Autowired
    private WorkspaceConnectionService workspaceConnectionService;

    @Autowired
    private WorkspaceUserRepository workspaceUserRepository;

    private long developmentConnectionId;
    private long otherDevelopmentConnectionId;
    private long productionConnectionId;
    private long workspaceId;

    @BeforeEach
    void seedWorkspaceConnectionsAndMembers() {
        reset(connectionFacade, connectionService, workspaceConnectionService);

        workspaceId = insertWorkspace("credentials-" + UUID.randomUUID());

        developmentConnectionId = seedConnection(DEVELOPMENT);
        otherDevelopmentConnectionId = seedConnection(DEVELOPMENT);
        productionConnectionId = seedConnection(PRODUCTION);

        when(workspaceConnectionService.getWorkspaceConnections(workspaceId))
            .thenReturn(
                List.of(
                    new WorkspaceConnection(developmentConnectionId, workspaceId),
                    new WorkspaceConnection(otherDevelopmentConnectionId, workspaceId),
                    new WorkspaceConnection(productionConnectionId, workspaceId)));
        when(
            connectionFacade.getConnections(
                any(), any(), anyList(), any(), any(), eq(PlatformType.AUTOMATION)))
                    .thenReturn(
                        List.of(
                            connectionDTO(developmentConnectionId, DEVELOPMENT),
                            connectionDTO(otherDevelopmentConnectionId, DEVELOPMENT),
                            connectionDTO(productionConnectionId, PRODUCTION)));

        workspaceUserRepository.save(
            WorkspaceUser.forRole(USER_IDS_BY_LOGIN.get(VIEWER), workspaceId, WorkspaceRole.VIEWER));
        workspaceUserRepository.save(
            WorkspaceUser.forRole(USER_IDS_BY_LOGIN.get(EDITOR), workspaceId, WorkspaceRole.EDITOR));
        workspaceUserRepository.save(
            WorkspaceUser.forRole(
                USER_IDS_BY_LOGIN.get(DEVELOPMENT_EDITOR_PRODUCTION_VIEWER), workspaceId, WorkspaceRole.EDITOR,
                Environment.DEVELOPMENT));
        workspaceUserRepository.save(
            WorkspaceUser.forRole(
                USER_IDS_BY_LOGIN.get(DEVELOPMENT_EDITOR_PRODUCTION_VIEWER), workspaceId, WorkspaceRole.VIEWER,
                Environment.PRODUCTION));
        workspaceUserRepository.save(
            WorkspaceUser.forRole(
                USER_IDS_BY_LOGIN.get(DEVELOPMENT_EDITOR_PRODUCTION_VIEWER), workspaceId, WorkspaceRole.VIEWER,
                Environment.STAGING));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testViewerGetsNoCredentialsById() {
        authenticate(VIEWER, "ROLE_USER");

        ConnectionDTO connectionDTO = workspaceConnectionFacade.getConnection(developmentConnectionId);

        assertWithoutCredentials(connectionDTO, developmentConnectionId);
    }

    @Test
    void testViewerGetsNoCredentialsInAList() {
        authenticate(VIEWER, "ROLE_USER");

        List<ConnectionDTO> connectionDTOs = workspaceConnectionFacade.getConnections(
            workspaceId, null, null, null, null);

        assertThat(connectionDTOs).hasSize(3);

        for (ConnectionDTO connectionDTO : connectionDTOs) {
            assertWithoutCredentials(connectionDTO, connectionDTO.id());
        }
    }

    @Test
    void testEditorGetsCredentialsById() {
        authenticate(EDITOR, "ROLE_USER");

        ConnectionDTO connectionDTO = workspaceConnectionFacade.getConnection(developmentConnectionId);

        assertWithCredentials(connectionDTO, developmentConnectionId);
    }

    @Test
    void testEditorGetsCredentialsInAList() {
        authenticate(EDITOR, "ROLE_USER");

        List<ConnectionDTO> connectionDTOs = workspaceConnectionFacade.getConnections(
            workspaceId, null, null, null, null);

        assertThat(connectionDTOs).hasSize(3);

        for (ConnectionDTO connectionDTO : connectionDTOs) {
            assertWithCredentials(connectionDTO, connectionDTO.id());
        }
    }

    @Test
    void testTenantAdminGetsCredentialsByIdAndInAList() {
        authenticate(TENANT_ADMIN, AuthorityConstants.ADMIN);

        assertWithCredentials(workspaceConnectionFacade.getConnection(productionConnectionId), productionConnectionId);

        List<ConnectionDTO> connectionDTOs = workspaceConnectionFacade.getConnections(
            workspaceId, null, null, null, null);

        assertThat(connectionDTOs).hasSize(3);

        for (ConnectionDTO connectionDTO : connectionDTOs) {
            assertWithCredentials(connectionDTO, connectionDTO.id());
        }
    }

    @Test
    void testMemberWhoEditsOnlyInDevelopmentGetsCredentialsOnlyForDevelopmentConnections() {
        authenticate(DEVELOPMENT_EDITOR_PRODUCTION_VIEWER, "ROLE_USER");

        assertWithCredentials(workspaceConnectionFacade.getConnection(developmentConnectionId),
            developmentConnectionId);
        assertWithoutCredentials(
            workspaceConnectionFacade.getConnection(productionConnectionId), productionConnectionId);

        List<ConnectionDTO> connectionDTOs = workspaceConnectionFacade.getConnections(
            workspaceId, null, null, null, null);

        assertThat(connectionDTOs).hasSize(3);

        for (ConnectionDTO connectionDTO : connectionDTOs) {
            if (connectionDTO.environmentId() == DEVELOPMENT) {
                assertWithCredentials(connectionDTO, connectionDTO.id());
            } else {
                assertWithoutCredentials(connectionDTO, connectionDTO.id());
            }
        }
    }

    private static void assertWithCredentials(ConnectionDTO connectionDTO, long connectionId) {
        assertThat(connectionDTO.id()).isEqualTo(connectionId);
        assertThat(connectionDTO.authorizationParameters()
            .get("access_token")).isEqualTo("secret-token");
        assertThat(connectionDTO.connectionParameters()
            .get("apiKey")).isEqualTo("secret-key");
        assertThat(connectionDTO.parameters()
            .get("password")).isEqualTo("secret-password");
    }

    private static void assertWithoutCredentials(ConnectionDTO connectionDTO, long connectionId) {
        assertThat(connectionDTO.id()).isEqualTo(connectionId);
        assertThat(connectionDTO.name()).isEqualTo("connection-" + connectionId);
        assertThat(connectionDTO.authorizationParameters()).isEmpty();
        assertThat(connectionDTO.connectionParameters()).isEmpty();
        assertThat(connectionDTO.parameters()).isEmpty();
    }

    private static void authenticate(String login, String authority) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(login, "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    private static ConnectionDTO connectionDTO(long connectionId, int environmentId) {
        return ConnectionDTO.builder()
            .authorizationParameters(Map.of("access_token", "secret-token"))
            .componentName("slack")
            .connectionParameters(Map.of("apiKey", "secret-key"))
            .environmentId(environmentId)
            .id(connectionId)
            .name("connection-" + connectionId)
            .parameters(Map.of("password", "secret-password"))
            .tags(List.of())
            .build();
    }

    private long insertWorkspace(String name) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        Long insertedWorkspaceId = jdbcTemplate.queryForObject(
            "INSERT INTO workspace (name, created_date, created_by, last_modified_date, last_modified_by, version) "
                + "VALUES (?, NOW(), 'test', NOW(), 'test', 0) RETURNING id",
            Long.class, name);

        assertThat(insertedWorkspaceId).isNotNull();

        return insertedWorkspaceId;
    }

    private long seedConnection(int environmentId) {
        long connectionId = Math.abs(UUID.randomUUID()
            .getMostSignificantBits() % 1_000_000_000L);

        Connection connection = new Connection();

        connection.setEnvironmentId(environmentId);
        connection.setId(connectionId);
        connection.setType(PlatformType.AUTOMATION);

        when(connectionService.fetchConnection(connectionId)).thenReturn(Optional.of(connection));
        when(connectionFacade.getConnection(connectionId)).thenReturn(connectionDTO(connectionId, environmentId));

        workspaceConnectionRepository.save(new WorkspaceConnection(connectionId, workspaceId));

        return connectionId;
    }

    @TestConfiguration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import({
        ApiKeyPermissionScopeProvider.class, ApiPlatformPermissionScopeProvider.class,
        ConnectionEnvironmentResolver.class, ConnectionOwnershipResolver.class,
        ConnectionPermissionScopeProvider.class, CurrentUserResolver.class, CustomRoleScopeResolverImpl.class,
        DataTablePermissionScopeProvider.class, DeploymentPermissionScopeProvider.class,
        EmbeddedScopeCacheService.class, ExecutionPermissionScopeProvider.class,
        KnowledgeBasePermissionScopeProvider.class, McpPermissionScopeProvider.class, PermissionScopeRegistry.class,
        PermissionServiceImpl.class, ProjectPermissionScopeProvider.class, WorkflowPermissionScopeProvider.class,
        WorkspacePermissionScopeProvider.class, WorkspaceScopeCacheService.class
    })
    static class RealPermissionServiceConfiguration {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        ConnectionFacade connectionFacade() {
            return mock(ConnectionFacade.class);
        }

        @Bean
        ConnectionService connectionService() {
            return mock(ConnectionService.class);
        }

        @Bean
        UserService userService() {
            UserService userService = mock(UserService.class);

            for (Map.Entry<String, Long> entry : USER_IDS_BY_LOGIN.entrySet()) {
                User user = new User();

                user.setId(entry.getValue());
                user.setLogin(entry.getKey());

                when(userService.fetchUserByLogin(entry.getKey())).thenReturn(Optional.of(user));
                when(userService.getUser(entry.getKey())).thenReturn(user);
            }

            return userService;
        }

        @Bean
        WorkspaceConnectionFacade workspaceConnectionFacade(
            ConnectionFacade connectionFacade, ConnectionService connectionService,
            PermissionService permissionService, WorkspaceConnectionService workspaceConnectionService) {

            return new WorkspaceConnectionFacadeImpl(
                connectionFacade, mock(ConnectionLifecycleFacade.class), connectionService, permissionService,
                mock(ProjectDeploymentWorkflowService.class), mock(WorkflowTestConfigurationService.class),
                workspaceConnectionService);
        }

        @Bean
        WorkspaceConnectionService workspaceConnectionService() {
            return mock(WorkspaceConnectionService.class);
        }
    }
}
