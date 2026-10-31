/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.service.WorkspaceService;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Calls every {@link AdminWorkspaceFacadeImpl} method through Spring Security's real {@code @PreAuthorize} method
 * interceptor, backed by the real {@link AutomationMethodSecurityExpressionHandler} and
 * {@link AutomationPermissionEvaluator}. The coarse {@code ROLE_ADMIN} gate was moved off
 * {@code WorkspaceApiController} onto this facade so it protects every caller; it is load-bearing because
 * {@code WorkspaceService.getWorkspaces()} is an intentionally unguarded trusted-caller method and
 * {@code WorkspaceService.getWorkspace(id)} only requires the {@code WORKSPACE_VIEW} scope. Each gate must decide on
 * the caller's {@code ROLE_ADMIN} authority alone and never consult {@link PermissionService}; an allowed call proves
 * it entered the method body by reaching the {@link WorkspaceService} mock, which throws on every call.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AdminWorkspaceFacadeTest {

    private static final String BODY_REACHED = "body reached";

    private static final Map<String, Consumer<AdminWorkspaceFacade>> INVOCATIONS = Map.of(
        "createWorkspace", adminWorkspaceFacade -> adminWorkspaceFacade.createWorkspace(new Workspace()),
        "deleteWorkspace", adminWorkspaceFacade -> adminWorkspaceFacade.deleteWorkspace(1L),
        "getWorkspace", adminWorkspaceFacade -> adminWorkspaceFacade.getWorkspace(1L),
        "getWorkspaces", AdminWorkspaceFacade::getWorkspaces,
        "updateWorkspace", adminWorkspaceFacade -> adminWorkspaceFacade.updateWorkspace(new Workspace()));

    private final PermissionService permissionService = mock(PermissionService.class);

    private final WorkspaceService workspaceService = mock(
        WorkspaceService.class, invocation -> {
            throw new IllegalStateException(BODY_REACHED);
        });

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "{0} admin={1}")
    @MethodSource("guardCases")
    void testGuardRequiresTheAdminAuthority(String methodName, boolean admin) {
        AdminWorkspaceFacade adminWorkspaceFacade = createSecuredAdminWorkspaceFacade(admin);

        Consumer<AdminWorkspaceFacade> invocation = INVOCATIONS.get(methodName);

        if (admin) {
            assertThatThrownBy(() -> invocation.accept(adminWorkspaceFacade))
                .as("%s must allow a caller holding ROLE_ADMIN", methodName)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);
        } else {
            assertThatThrownBy(() -> invocation.accept(adminWorkspaceFacade))
                .as("%s must deny a caller without ROLE_ADMIN", methodName)
                .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(workspaceService);
        }

        verifyNoInteractions(permissionService);
    }

    @Test
    void testEveryFacadeMethodHasAnEvaluatedCase() {
        Set<String> facadeMethodNames = Arrays.stream(AdminWorkspaceFacade.class.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toSet());

        assertThat(facadeMethodNames).containsExactlyInAnyOrderElementsOf(INVOCATIONS.keySet());
    }

    static Stream<Arguments> guardCases() {
        return INVOCATIONS.keySet()
            .stream()
            .sorted()
            .flatMap(methodName -> Stream.of(Arguments.of(methodName, false), Arguments.of(methodName, true)));
    }

    private AdminWorkspaceFacade createSecuredAdminWorkspaceFacade(boolean admin) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "alice", "credentials", List.of(new SimpleGrantedAuthority(admin ? "ROLE_ADMIN" : "ROLE_USER"))));

        SecurityContextHolder.setContext(securityContext);

        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager = new PreAuthorizeAuthorizationManager();

        preAuthorizeAuthorizationManager.setExpressionHandler(expressionHandler);

        ProxyFactory proxyFactory = new ProxyFactory(new AdminWorkspaceFacadeImpl(workspaceService));

        proxyFactory.addAdvisor(
            AuthorizationManagerBeforeMethodInterceptor.preAuthorize(preAuthorizeAuthorizationManager));

        return (AdminWorkspaceFacade) proxyFactory.getProxy();
    }
}
