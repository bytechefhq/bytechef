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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.web.graphql.config.AutomationConfigurationGraphQlConfigurationSharedMocks;
import com.bytechef.automation.configuration.web.graphql.config.AutomationConfigurationGraphQlTestConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.graphql.test.tester.GraphQlTester;
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
 * Calls the guarded methods of {@link ProjectDeploymentGraphQlController} through the real Spring method-security
 * interceptor, backed by the real {@link AutomationMethodSecurityExpressionHandler} and
 * {@link AutomationPermissionEvaluator}.
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AutomationConfigurationGraphQlTestConfiguration.class, ProjectDeploymentGraphQlController.class,
    ProjectDeploymentGraphQlControllerIntTest.MethodSecurityConfiguration.class
})
@GraphQlTest(
    controllers = ProjectDeploymentGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.inspection.enabled=false",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@AutomationConfigurationGraphQlConfigurationSharedMocks
class ProjectDeploymentGraphQlControllerIntTest {

    private static final String BODY_REACHED = "body reached";
    private static final long ENVIRONMENT_ID = 2L;
    private static final long WORKSPACE_ID = 42L;

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private ProjectDeploymentGraphQlController projectDeploymentGraphQlController;

    @MockitoBean
    private EnvironmentService environmentService;

    @MockitoBean
    private PermissionService permissionService;

    @MockitoBean
    private ProjectDeploymentService projectDeploymentService;

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

    @ParameterizedTest
    @ValueSource(booleans = {
        false, true
    })
    void testWorkspaceProjectDeploymentsRequiresDeploymentViewInTheNamedEnvironment(boolean granted) {
        Environment environment = Environment.values()[(int) ENVIRONMENT_ID];

        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, "DEPLOYMENT_VIEW", environment)).thenReturn(granted);

        assertInvocationOutcome(
            () -> projectDeploymentGraphQlController.workspaceProjectDeployments(
                WORKSPACE_ID, ENVIRONMENT_ID, null, null),
            granted);

        verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, "DEPLOYMENT_VIEW", environment);
    }

    @Test
    void testWorkspaceProjectDeploymentsRequestsTheNonEmbeddedDeploymentsFromTheService() {
        doReturn(Environment.PRODUCTION).when(environmentService)
            .getEnvironment(2L);
        when(permissionService.hasWorkspaceScope(1L, "DEPLOYMENT_VIEW", Environment.PRODUCTION)).thenReturn(true);
        when(projectDeploymentService.getProjectDeployments(false, Environment.PRODUCTION, null, null, 1L))
            .thenReturn(List.of(projectDeployment(10L, 100L, "Orders")));

        graphQlTester
            .document("""
                query {
                    workspaceProjectDeployments(workspaceId: 1, environmentId: 2) {
                        id
                        name
                    }
                }
                """)
            .execute()
            .path("workspaceProjectDeployments[*].name")
            .entityList(String.class)
            .containsExactly("Orders");
    }

    private static ProjectDeployment projectDeployment(long id, long projectId, String name) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.PRODUCTION);
        projectDeployment.setId(id);
        projectDeployment.setName(name);
        projectDeployment.setProjectId(projectId);
        projectDeployment.setProjectVersion(1);

        return projectDeployment;
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
