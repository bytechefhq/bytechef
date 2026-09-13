/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.security.ConnectedUserAccessDecider;
import com.bytechef.automation.configuration.security.ConnectionOwnershipResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.automation.configuration.service.CurrentUserResolver;
import com.bytechef.ee.automation.configuration.service.PermissionScopeRegistry;
import com.bytechef.ee.automation.configuration.service.PermissionServiceImpl;
import com.bytechef.ee.automation.configuration.service.WorkspaceScopeCacheService;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicy;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        WorkspaceConnectionFacadeIntTest.WorkspaceConnectionFacadeIntTestConfiguration.class,
        ConnectionOwnershipResolver.class
    },
    properties = "bytechef.edition=ee")
@WorkspaceConnectionFacadeIntTest.WorkspaceConnectionFacadeIntTestMocks
class WorkspaceConnectionFacadeIntTest {

    private static final String BODY_REACHED = "body reached";
    private static final long CONNECTION_ID = 11L;
    private static final String CONNECTION_TYPE = "Connection";
    private static final long WORKSPACE_ID = 9L;

    private static final String ADMIN_LOGIN = "ana";
    private static final long ADMIN_USER_ID = 8L;
    private static final String MEMBER_LOGIN = "marko";
    private static final long MEMBER_USER_ID = 9L;
    private static final String OWNER_LOGIN = "ivica";
    private static final long OWNER_USER_ID = 7L;
    private static final String TENANT_ADMIN_LOGIN = "root";
    private static final long TENANT_ADMIN_USER_ID = 10L;

    private static final String[] GATED_METHODS = {
        "setConnectionVisibility", "grantConnectionAccess", "revokeConnectionAccess", "getConnectionGrants"
    };

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkspaceConnectionFacade workspaceConnectionFacade;

    @Autowired
    private WorkspaceConnectionRepository workspaceConnectionRepository;

    @Autowired
    private WorkspaceConnectionService workspaceConnectionService;

    @Autowired
    private WorkspaceUserRepository workspaceUserRepository;

    @BeforeEach
    void beforeEach() {
        for (User user : List.of(
            user(ADMIN_USER_ID, ADMIN_LOGIN), user(MEMBER_USER_ID, MEMBER_LOGIN), user(OWNER_USER_ID, OWNER_LOGIN),
            user(TENANT_ADMIN_USER_ID, TENANT_ADMIN_LOGIN))) {

            when(userService.getUser(user.getLogin())).thenReturn(user);
        }

        when(userService.fetchUserByLogin(OWNER_LOGIN)).thenReturn(Optional.of(user(OWNER_USER_ID, OWNER_LOGIN)));

        Connection connection = new Connection();

        connection.setId(CONNECTION_ID);
        connection.setCreatedBy(OWNER_LOGIN);

        when(connectionService.fetchConnection(CONNECTION_ID)).thenReturn(Optional.of(connection));
        when(workspaceConnectionRepository.findByConnectionId(CONNECTION_ID))
            .thenReturn(Optional.of(new WorkspaceConnection(CONNECTION_ID, WORKSPACE_ID)));
        when(workspaceConnectionService.getWorkspaceConnections(anyLong()))
            .thenThrow(new IllegalStateException(BODY_REACHED));

        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(ADMIN_USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(new WorkspaceUser(ADMIN_USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN.ordinal())));
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(MEMBER_USER_ID, WORKSPACE_ID))
            .thenReturn(
                Optional.of(new WorkspaceUser(MEMBER_USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR.ordinal())));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @MethodSource("gatedMethods")
    void testGateAllowsTheConnectionOwner(String methodName) {
        authenticate(OWNER_LOGIN, AuthorityConstants.USER);

        assertBodyReached(() -> invokeGatedMethod(methodName));
    }

    @ParameterizedTest
    @MethodSource("gatedMethods")
    void testGateAllowsAWorkspaceAdminWhoIsNotTheOwner(String methodName) {
        authenticate(ADMIN_LOGIN, AuthorityConstants.USER);

        assertBodyReached(() -> invokeGatedMethod(methodName));
    }

    @ParameterizedTest
    @MethodSource("gatedMethods")
    void testGateAllowsAWorkspaceAdminWhenTheUserLookupIsUnsupported(String methodName) {
        when(userService.fetchUserByLogin(OWNER_LOGIN)).thenThrow(new UnsupportedOperationException());

        authenticate(ADMIN_LOGIN, AuthorityConstants.USER);

        assertBodyReached(() -> invokeGatedMethod(methodName));
    }

    @ParameterizedTest
    @MethodSource("gatedMethods")
    void testGateDeniesTheOwnerWhenTheUserLookupIsUnsupported(String methodName) {
        when(userService.fetchUserByLogin(OWNER_LOGIN)).thenThrow(new UnsupportedOperationException());

        authenticate(OWNER_LOGIN, AuthorityConstants.USER);

        assertThatThrownBy(() -> invokeGatedMethod(methodName)).isInstanceOf(AccessDeniedException.class);
    }

    @ParameterizedTest
    @MethodSource("gatedMethods")
    void testGateDeniesAnOrdinaryWorkspaceMember(String methodName) {
        authenticate(MEMBER_LOGIN, AuthorityConstants.USER);

        assertThatThrownBy(() -> invokeGatedMethod(methodName)).isInstanceOf(AccessDeniedException.class);
    }

    @ParameterizedTest
    @MethodSource("gatedMethods")
    void testGateAllowsATenantAdmin(String methodName) {
        authenticate(TENANT_ADMIN_LOGIN, AuthorityConstants.ADMIN);

        assertBodyReached(() -> invokeGatedMethod(methodName));
    }

    @Test
    void testEveryGatedMethodIsEvaluated() {
        List<String> annotatedMethods = Arrays.stream(WorkspaceConnectionFacadeImpl.class.getDeclaredMethods())
            .filter(method -> method.getAnnotation(PreAuthorize.class) != null)
            .map(Method::getName)
            .distinct()
            .sorted()
            .toList();

        assertThat(annotatedMethods)
            .as("a gate added to the facade must be added to GATED_METHODS, or it goes unevaluated")
            .containsExactlyInAnyOrder(GATED_METHODS);
    }

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @SpringBootTest(
        classes = WorkspaceConnectionFacadeIntTest.WorkspaceConnectionFacadeIntTestConfiguration.class,
        properties = "bytechef.edition=ee")
    @WorkspaceConnectionFacadeIntTestMocks
    class WithoutOwnershipResolver {

        @ParameterizedTest
        @MethodSource("com.bytechef.ee.automation.configuration.facade.WorkspaceConnectionFacadeIntTest#gatedMethods")
        void testGateDeniesTheConnectionOwner(String methodName) {
            authenticate(OWNER_LOGIN, AuthorityConstants.USER);

            assertThatThrownBy(() -> invokeGatedMethod(methodName)).isInstanceOf(AccessDeniedException.class);
        }

        @ParameterizedTest
        @MethodSource("com.bytechef.ee.automation.configuration.facade.WorkspaceConnectionFacadeIntTest#gatedMethods")
        void testGateDeniesAWorkspaceAdmin(String methodName) {
            authenticate(ADMIN_LOGIN, AuthorityConstants.USER);

            assertThatThrownBy(() -> invokeGatedMethod(methodName)).isInstanceOf(AccessDeniedException.class);
        }

        @ParameterizedTest
        @MethodSource("com.bytechef.ee.automation.configuration.facade.WorkspaceConnectionFacadeIntTest#gatedMethods")
        void testGateAllowsATenantAdmin(String methodName) {
            authenticate(TENANT_ADMIN_LOGIN, AuthorityConstants.ADMIN);

            assertBodyReached(() -> invokeGatedMethod(methodName));
        }
    }

    private static Stream<String> gatedMethods() {
        return Arrays.stream(GATED_METHODS);
    }

    private void invokeGatedMethod(String methodName) {
        switch (methodName) {
            case "setConnectionVisibility" -> workspaceConnectionFacade.setConnectionVisibility(
                WORKSPACE_ID, CONNECTION_ID, ResourceVisibility.WORKSPACE);
            case "grantConnectionAccess" -> workspaceConnectionFacade.grantConnectionAccess(
                WORKSPACE_ID, CONNECTION_ID, MEMBER_USER_ID);
            case "revokeConnectionAccess" -> workspaceConnectionFacade.revokeConnectionAccess(
                WORKSPACE_ID, CONNECTION_ID, MEMBER_USER_ID);
            case "getConnectionGrants" -> workspaceConnectionFacade.getConnectionGrants(WORKSPACE_ID, CONNECTION_ID);
            default -> throw new IllegalArgumentException("No invocation for gated method " + methodName);
        }
    }

    private static void assertBodyReached(ThrowingCallable throwingCallable) {
        assertThatThrownBy(throwingCallable)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(BODY_REACHED);
    }

    private static void authenticate(String login, String authority) {
        List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(authority);

        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                    new org.springframework.security.core.userdetails.User(login, "", authorities), null,
                    authorities));
    }

    private static User user(long id, String login) {
        User user = new User();

        user.setActivated(true);
        user.setId(id);
        user.setLogin(login);

        return user;
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @MockitoBean(types = {
        ConnectionFacade.class, ConnectionLifecycleFacade.class, ConnectionService.class,
        PermissionScopeRegistry.class, ProjectDeploymentWorkflowService.class, ProjectRepository.class,
        ProjectService.class, ResourceGrantService.class, UserService.class, WorkflowTestConfigurationService.class,
        WorkspaceConnectionRepository.class, WorkspaceConnectionService.class, WorkspaceFacade.class,
        WorkspaceScopeCacheService.class, WorkspaceUserRepository.class, WorkspaceUserService.class
    })
    @interface WorkspaceConnectionFacadeIntTestMocks {
    }

    @SpringBootConfiguration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import(WorkspaceConnectionFacadeImpl.class)
    static class WorkspaceConnectionFacadeIntTestConfiguration {

        @Bean("permissionService")
        PermissionServiceImpl permissionService(
            ObjectProvider<ConnectedUserAccessDecider> connectedUserAccessDeciderProvider,
            PermissionScopeRegistry permissionScopeRegistry, ProjectRepository projectRepository,
            ObjectProvider<ResourceOwnershipResolver> resourceOwnershipResolverProvider,
            ResourceVisibilityResolver resourceVisibilityResolver, UserService userService,
            WorkspaceScopeCacheService workspaceScopeCacheService, WorkspaceUserRepository workspaceUserRepository) {

            return new PermissionServiceImpl(
                new CurrentUserResolver(userService), permissionScopeRegistry, projectRepository,
                workspaceScopeCacheService, workspaceUserRepository, resourceOwnershipResolverProvider.orderedStream()
                    .toList(),
                List.of(), resourceVisibilityResolver, List.of(), connectedUserAccessDeciderProvider);
        }

        @Bean
        ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry() {
            return new ResourceVisibilityPolicyRegistry(
                List.of(
                    new ResourceVisibilityPolicy() {

                        @Override
                        public String resourceType() {
                            return CONNECTION_TYPE;
                        }

                        @Override
                        public ResourceVisibility defaultVisibility() {
                            return ResourceVisibility.WORKSPACE;
                        }

                        @Override
                        public Set<ResourceVisibility> supportedVisibilities() {
                            return Set.of(
                                ResourceVisibility.PRIVATE, ResourceVisibility.WORKSPACE,
                                ResourceVisibility.ORGANIZATION);
                        }
                    }));
        }

        @Bean
        ResourceVisibilityResolver resourceVisibilityResolver() {
            return (resourceType, workspaceId, candidates) -> candidates.stream()
                .map(VisibilityRecord::id)
                .collect(Collectors.toSet());
        }
    }
}
