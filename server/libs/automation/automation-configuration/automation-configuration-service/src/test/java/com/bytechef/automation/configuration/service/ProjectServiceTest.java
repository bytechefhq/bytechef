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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Calls {@link ProjectServiceImpl} through the real Spring method-security interceptor, backed by the real
 * {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator}.
 *
 * @author Ivica Cardic
 */
class ProjectServiceTest {

    private static final String BODY_REACHED = "body reached";
    private static final String PROJECT_DELETE = "PROJECT_DELETE";
    private static final long PROJECT_ID = 42L;
    private static final String PROJECT_PUBLISH = "PROJECT_PUBLISH";
    private static final String PROJECT_TYPE = "Project";
    private static final String WORKFLOW_EDIT = "WORKFLOW_EDIT";

    private PermissionService permissionService;
    private ProjectService projectService;

    @BeforeEach
    void beforeEach() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken("member", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        SecurityContextHolder.setContext(securityContext);

        permissionService = mock(PermissionService.class);

        ProjectRepository projectRepository = mock(ProjectRepository.class, invocation -> {
            throw new IllegalStateException(BODY_REACHED);
        });

        projectService = secure(
            new ProjectServiceImpl(mock(ApplicationContext.class), projectRepository), permissionService);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDeleteDeniesWithoutTheDeleteScope() {
        denyOnly(PROJECT_DELETE);

        assertDenied(() -> projectService.delete(PROJECT_ID));

        verify(permissionService).hasResourceScope(PROJECT_ID, PROJECT_TYPE, PROJECT_DELETE);
    }

    @Test
    void testDeleteAllowsWithTheDeleteScope() {
        grant(PROJECT_DELETE);

        assertBodyReached(() -> projectService.delete(PROJECT_ID));
    }

    @Test
    void testPublishProjectDeniesWithoutThePublishScope() {
        denyOnly(PROJECT_PUBLISH);

        assertDenied(() -> projectService.publishProject(PROJECT_ID, "description", false));

        verify(permissionService).hasResourceScope(PROJECT_ID, PROJECT_TYPE, PROJECT_PUBLISH);
    }

    @Test
    void testPublishProjectAllowsWithThePublishScope() {
        grant(PROJECT_PUBLISH);

        assertBodyReached(() -> projectService.publishProject(PROJECT_ID, "description", false));
    }

    @Test
    void testUpdateTagsDeniesWithoutTheEditScope() {
        denyOnly(WORKFLOW_EDIT);

        assertDenied(() -> projectService.update(PROJECT_ID, List.of()));

        verify(permissionService).hasResourceScope(PROJECT_ID, PROJECT_TYPE, WORKFLOW_EDIT);
    }

    @Test
    void testUpdateTagsAllowsWithTheEditScope() {
        grant(WORKFLOW_EDIT);

        assertBodyReached(() -> projectService.update(PROJECT_ID, List.of()));
    }

    @Test
    void testUpdateDeniesWithoutTheEditScope() {
        denyOnly(WORKFLOW_EDIT);

        assertDenied(() -> projectService.update(newProject()));

        verify(permissionService).hasResourceScope(PROJECT_ID, PROJECT_TYPE, WORKFLOW_EDIT);
    }

    @Test
    void testUpdateAllowsWithTheEditScope() {
        grant(WORKFLOW_EDIT);

        assertBodyReached(() -> projectService.update(newProject()));
    }

    private void assertBodyReached(ThrowingCallable call) {
        assertThatThrownBy(call)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(BODY_REACHED);
    }

    private void assertDenied(ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOf(AccessDeniedException.class);
    }

    private void denyOnly(String scope) {
        when(permissionService.hasResourceScope(any(), anyString(), anyString())).thenReturn(true);
        when(permissionService.hasResourceScope(PROJECT_ID, PROJECT_TYPE, scope)).thenReturn(false);
    }

    private void grant(String scope) {
        when(permissionService.hasResourceScope(PROJECT_ID, PROJECT_TYPE, scope)).thenReturn(true);
    }

    private static Project newProject() {
        Project project = new Project();

        project.setId(PROJECT_ID);
        project.setName("project");

        return project;
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
