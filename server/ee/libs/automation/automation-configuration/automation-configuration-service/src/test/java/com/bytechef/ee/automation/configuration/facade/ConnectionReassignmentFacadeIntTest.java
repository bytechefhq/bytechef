/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Runs the admin-only guards of {@link ConnectionReassignmentFacadeImpl} through the method-security proxy. The
 * internal {@code markConnectionsPendingReassignment} variant is invoked by {@code WorkspaceUserRemovalListener} on the
 * non-admin user-removal path, so it must stay reachable without the admin authority.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ConnectionReassignmentFacadeIntTest.ConnectionReassignmentFacadeIntTestConfiguration.class,
    properties = "bytechef.edition=ee")
@MockitoBean(types = {
    ConnectionService.class, ProjectDeploymentWorkflowService.class, UserService.class, WorkflowService.class,
    WorkspaceConnectionService.class
})
class ConnectionReassignmentFacadeIntTest {

    private static final String BODY_REACHED = "body reached";
    private static final long CONNECTION_ID = 3L;
    private static final String NEW_OWNER_LOGIN = "new-owner@example.com";
    private static final String USER_LOGIN = "departing-user@example.com";
    private static final long WORKSPACE_ID = 1L;

    private static final String[] ADMIN_GUARDED_METHODS = {
        "getAffectedWorkflows", "getUnresolvedConnections", "markConnectionsPendingReassignmentAsAdmin",
        "reassignAllConnections", "reassignConnection"
    };

    @Autowired
    private ConnectionReassignmentFacade connectionReassignmentFacade;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkspaceConnectionService workspaceConnectionService;

    @BeforeEach
    void beforeEach() {
        when(userService.fetchUserByLogin(NEW_OWNER_LOGIN)).thenReturn(Optional.of(new User()));
        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID))
            .thenThrow(new IllegalStateException(BODY_REACHED));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @MethodSource("adminGuardedMethods")
    void testAdminGuardedMethodIsDeniedToANonAdmin(String methodName) {
        authenticate(AuthorityConstants.USER);

        assertThatThrownBy(() -> invokeAdminGuardedMethod(methodName)).isInstanceOf(AccessDeniedException.class);
    }

    @ParameterizedTest
    @MethodSource("adminGuardedMethods")
    void testAdminGuardedMethodIsAllowedToATenantAdmin(String methodName) {
        authenticate(AuthorityConstants.ADMIN);

        assertBodyReached(() -> invokeAdminGuardedMethod(methodName));
    }

    @Test
    void testMarkConnectionsPendingReassignmentIsAllowedToANonAdmin() {
        authenticate(AuthorityConstants.USER);

        assertBodyReached(
            () -> connectionReassignmentFacade.markConnectionsPendingReassignment(WORKSPACE_ID, USER_LOGIN));
    }

    @Test
    void testEveryAdminGuardedMethodIsEvaluated() {
        List<String> annotatedMethods = Arrays.stream(ConnectionReassignmentFacadeImpl.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .filter(method -> method.getAnnotation(PreAuthorize.class) != null)
            .map(Method::getName)
            .toList();

        assertThat(annotatedMethods)
            .as("a guard added to the facade must be added to ADMIN_GUARDED_METHODS, or it goes unevaluated")
            .containsExactlyInAnyOrder(ADMIN_GUARDED_METHODS);
    }

    private static Stream<String> adminGuardedMethods() {
        return Arrays.stream(ADMIN_GUARDED_METHODS);
    }

    private void invokeAdminGuardedMethod(String methodName) {
        switch (methodName) {
            case "getAffectedWorkflows" -> connectionReassignmentFacade.getAffectedWorkflows(WORKSPACE_ID, USER_LOGIN);
            case "getUnresolvedConnections" ->
                connectionReassignmentFacade.getUnresolvedConnections(WORKSPACE_ID, USER_LOGIN);
            case "markConnectionsPendingReassignmentAsAdmin" ->
                connectionReassignmentFacade.markConnectionsPendingReassignmentAsAdmin(WORKSPACE_ID, USER_LOGIN);
            case "reassignAllConnections" ->
                connectionReassignmentFacade.reassignAllConnections(WORKSPACE_ID, USER_LOGIN, NEW_OWNER_LOGIN);
            case "reassignConnection" ->
                connectionReassignmentFacade.reassignConnection(WORKSPACE_ID, CONNECTION_ID, NEW_OWNER_LOGIN);
            default -> throw new IllegalArgumentException("No invocation for guarded method " + methodName);
        }
    }

    private static void assertBodyReached(ThrowingCallable throwingCallable) {
        assertThatThrownBy(throwingCallable)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(BODY_REACHED);
    }

    private static void authenticate(String authority) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "member", "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    @SpringBootConfiguration
    @EnableMethodSecurity
    @Import(ConnectionReassignmentFacadeImpl.class)
    static class ConnectionReassignmentFacadeIntTestConfiguration {
    }
}
