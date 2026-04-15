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

import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicy;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = WorkspaceConnectionFacadeIntTest.WorkspaceConnectionFacadeIntTestConfiguration.class,
    properties = "bytechef.edition=ee")
@MockitoBean(types = {
    ConnectionFacade.class, ConnectionLifecycleFacade.class, ConnectionService.class,
    ProjectDeploymentWorkflowService.class, ProjectService.class, ResourceGrantService.class,
    ResourceVisibilityResolver.class, UserService.class, WorkflowTestConfigurationService.class,
    WorkspaceFacade.class, WorkspaceUserService.class
})
class WorkspaceConnectionFacadeIntTest {

    private static final String BODY_REACHED = "body reached";
    private static final long CONNECTION_ID = 11L;
    private static final String CONNECTION_TYPE = "Connection";
    private static final long USER_ID = 5L;
    private static final long WORKSPACE_ID = 9L;

    @MockitoBean(name = "permissionService")
    private PermissionService permissionService;

    @Autowired
    private WorkspaceConnectionFacade workspaceConnectionFacade;

    @MockitoBean
    private WorkspaceConnectionService workspaceConnectionService;

    @BeforeEach
    void beforeEach() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "member", "n/a", List.of(new SimpleGrantedAuthority(AuthorityConstants.USER))));

        when(workspaceConnectionService.getWorkspaceConnections(anyLong()))
            .thenThrow(new IllegalStateException(BODY_REACHED));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "setConnectionVisibility", "grantConnectionAccess", "revokeConnectionAccess", "getConnectionGrants"
    })
    void testGateDeniesACallerWhoNeitherOwnsTheConnectionNorHoldsTheWorkspaceAdminRole(String methodName) {
        assertThatThrownBy(() -> invokeGatedMethod(methodName)).isInstanceOf(AccessDeniedException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "setConnectionVisibility", "grantConnectionAccess", "revokeConnectionAccess", "getConnectionGrants"
    })
    void testGateAllowsTheConnectionOwner(String methodName) {
        when(permissionService.isResourceOwner(CONNECTION_TYPE, CONNECTION_ID)).thenReturn(true);

        assertBodyReached(() -> invokeGatedMethod(methodName));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "setConnectionVisibility", "grantConnectionAccess", "revokeConnectionAccess", "getConnectionGrants"
    })
    void testGateAllowsAWorkspaceAdmin(String methodName) {
        when(permissionService.hasResourceRole(CONNECTION_ID, CONNECTION_TYPE, "ADMIN")).thenReturn(true);

        assertBodyReached(() -> invokeGatedMethod(methodName));
    }

    @Test
    void testEverySharingMutationIsCovered() {
        List<String> annotatedMethods = Arrays.stream(WorkspaceConnectionFacadeImpl.class.getDeclaredMethods())
            .filter(method -> method.getAnnotation(PreAuthorize.class) != null)
            .map(Method::getName)
            .distinct()
            .sorted()
            .toList();

        assertThat(annotatedMethods)
            .containsExactly(
                "getConnectionGrants", "grantConnectionAccess", "revokeConnectionAccess", "setConnectionVisibility");
    }

    private void invokeGatedMethod(String methodName) {
        switch (methodName) {
            case "setConnectionVisibility" -> workspaceConnectionFacade.setConnectionVisibility(
                WORKSPACE_ID, CONNECTION_ID, ResourceVisibility.WORKSPACE);
            case "grantConnectionAccess" -> workspaceConnectionFacade.grantConnectionAccess(
                WORKSPACE_ID, CONNECTION_ID, USER_ID);
            case "revokeConnectionAccess" -> workspaceConnectionFacade.revokeConnectionAccess(
                WORKSPACE_ID, CONNECTION_ID, USER_ID);
            case "getConnectionGrants" -> workspaceConnectionFacade.getConnectionGrants(WORKSPACE_ID, CONNECTION_ID);
            default -> throw new IllegalArgumentException("No invocation for gated method " + methodName);
        }
    }

    private static void assertBodyReached(ThrowingCallable throwingCallable) {
        assertThatThrownBy(throwingCallable)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(BODY_REACHED);
    }

    @SpringBootConfiguration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import(WorkspaceConnectionFacadeImpl.class)
    static class WorkspaceConnectionFacadeIntTestConfiguration {

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
    }
}
