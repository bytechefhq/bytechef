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

package com.bytechef.automation.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.facade.WorkspaceApiKeyFacade;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.web.graphql.config.AutomationConfigurationGraphQlConfigurationSharedMocks;
import com.bytechef.automation.configuration.web.graphql.config.AutomationConfigurationGraphQlTestConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Calls {@link WorkspaceApiKeyGraphQlController#createWorkspaceApiKey} through the real Spring method-security
 * interceptor, backed by the real {@link AutomationMethodSecurityExpressionHandler} and
 * {@link AutomationPermissionEvaluator}, asserting the guard in both directions and the exact check that reaches
 * {@link PermissionService}.
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AutomationConfigurationGraphQlTestConfiguration.class, WorkspaceApiKeyGraphQlController.class,
    WorkspaceApiKeyGraphQlControllerIntTest.MethodSecurityConfiguration.class
})
@GraphQlTest(
    controllers = WorkspaceApiKeyGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@AutomationConfigurationGraphQlConfigurationSharedMocks
class WorkspaceApiKeyGraphQlControllerIntTest {

    private static final String BODY_REACHED = "body reached";
    private static final long ENVIRONMENT_ID = 2L;
    private static final long WORKSPACE_ID = 42L;

    @Autowired
    private WorkspaceApiKeyGraphQlController workspaceApiKeyGraphQlController;

    @MockitoBean
    private ApiKeyFacade apiKeyFacade;

    @MockitoBean
    private EnvironmentService environmentService;

    @MockitoBean
    private PermissionService permissionService;

    @MockitoBean
    private WorkspaceApiKeyFacade workspaceApiKeyFacade;

    @BeforeEach
    void beforeEach() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken("member", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        SecurityContextHolder.setContext(securityContext);

        when(environmentService.getEnvironment(anyLong())).thenThrow(new IllegalStateException(BODY_REACHED));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateWorkspaceApiKeyDeniesWhenTheApiKeyCreateScopeIsRefusedInTheNamedEnvironment() {
        assertEnvironmentGuard(false);
    }

    @Test
    void testCreateWorkspaceApiKeyAllowsWhenTheApiKeyCreateScopeIsGrantedInTheNamedEnvironment() {
        assertEnvironmentGuard(true);
    }

    @Test
    void testCreateWorkspaceApiKeyWithoutAnEnvironmentDeniesUnlessEveryEnvironmentGrants() {
        Environment[] environments = Environment.values();
        Environment lastEnvironment = environments[environments.length - 1];

        for (Environment environment : environments) {
            when(permissionService.hasWorkspaceScope(WORKSPACE_ID, "API_KEY_CREATE", environment))
                .thenReturn(environment != lastEnvironment);
        }

        assertThatThrownBy(() -> workspaceApiKeyGraphQlController.createWorkspaceApiKey(WORKSPACE_ID, "name", null))
            .isInstanceOf(AccessDeniedException.class);

        for (Environment environment : environments) {
            verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, "API_KEY_CREATE", environment);
        }

        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testCreateWorkspaceApiKeyWithoutAnEnvironmentAllowsWhenEveryEnvironmentGrants() {
        for (Environment environment : Environment.values()) {
            when(permissionService.hasWorkspaceScope(WORKSPACE_ID, "API_KEY_CREATE", environment)).thenReturn(true);
        }

        assertThatThrownBy(() -> workspaceApiKeyGraphQlController.createWorkspaceApiKey(WORKSPACE_ID, "name", null))
            .isInstanceOf(NullPointerException.class)
            .satisfies(exception -> assertThat(exception.getStackTrace()[0].getMethodName())
                .isEqualTo("createWorkspaceApiKey"));

        for (Environment environment : Environment.values()) {
            verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, "API_KEY_CREATE", environment);
        }

        verifyNoMoreInteractions(permissionService);
    }

    private void assertEnvironmentGuard(boolean granted) {
        Environment environment = Environment.values()[(int) ENVIRONMENT_ID];

        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, "API_KEY_CREATE", environment)).thenReturn(granted);

        assertInvocationOutcome(
            () -> workspaceApiKeyGraphQlController.createWorkspaceApiKey(WORKSPACE_ID, "name", ENVIRONMENT_ID),
            granted);

        verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, "API_KEY_CREATE", environment);
        verifyNoMoreInteractions(permissionService);
    }

    private static void assertInvocationOutcome(ThrowingCallable invocation, boolean allowed) {
        if (allowed) {
            assertThatThrownBy(invocation)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);
        } else {
            assertThatThrownBy(invocation).isInstanceOf(AccessDeniedException.class);
        }
    }

    @EnableMethodSecurity
    static class MethodSecurityConfiguration {

        @Bean
        static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            @Lazy PermissionService permissionService) {

            AutomationMethodSecurityExpressionHandler expressionHandler =
                new AutomationMethodSecurityExpressionHandler(permissionService);

            expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

            return expressionHandler;
        }
    }
}
