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

package com.bytechef.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Calls {@link ProjectDeploymentServiceImpl} through the real Spring method-security interceptor, backed by the real
 * {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator}.
 *
 * @author Ivica Cardic
 */
class ProjectDeploymentServiceTest {

    private static final String BODY_REACHED = "body reached";
    private static final long PROJECT_DEPLOYMENT_ID = 11L;
    private static final long PROJECT_ID = 42L;
    private static final String PROJECT_DEPLOYMENT_TYPE = "ProjectDeployment";

    @BeforeEach
    void beforeEach() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken("member", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testServiceUpdateIsKeyedOnTheStoredDeploymentNotTheSuppliedProject() {
        assertUpdateGuard(false);
    }

    @Test
    void testServiceUpdateAllowsWhenTheStoredDeploymentGrantsTheScope() {
        assertUpdateGuard(true);
    }

    private void assertUpdateGuard(boolean granted) {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScope(PROJECT_DEPLOYMENT_ID, PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_CREATE"))
            .thenReturn(granted);

        ProjectDeploymentRepository projectDeploymentRepository =
            mock(ProjectDeploymentRepository.class, invocation -> {
                throw new IllegalStateException(BODY_REACHED);
            });

        ProjectDeploymentService projectDeploymentService = secure(
            new ProjectDeploymentServiceImpl(projectDeploymentRepository), permissionService);

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(PROJECT_DEPLOYMENT_ID);
        projectDeployment.setName("deployment");
        projectDeployment.setProjectId(PROJECT_ID);

        if (granted) {
            assertThatThrownBy(() -> projectDeploymentService.update(projectDeployment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);
        } else {
            assertThatThrownBy(() -> projectDeploymentService.update(projectDeployment))
                .isInstanceOf(AccessDeniedException.class);
        }

        verify(permissionService).hasResourceScope(PROJECT_DEPLOYMENT_ID, PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_CREATE");
        verifyNoMoreInteractions(permissionService);
    }

    @SuppressWarnings("unchecked")
    private static <T> T secure(T target, PermissionService permissionService) {
        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager = new PreAuthorizeAuthorizationManager();

        preAuthorizeAuthorizationManager.setExpressionHandler(expressionHandler);

        ProxyFactory proxyFactory = new ProxyFactory(target);

        proxyFactory.addAdvice(AuthorizationManagerBeforeMethodInterceptor.preAuthorize(
            preAuthorizeAuthorizationManager));

        return (T) proxyFactory.getProxy();
    }
}
