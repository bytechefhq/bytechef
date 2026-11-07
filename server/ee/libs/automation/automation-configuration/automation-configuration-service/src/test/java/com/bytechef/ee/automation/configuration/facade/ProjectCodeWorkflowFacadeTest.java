/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectOwnershipService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowDraftService;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.security.web.expression.MethodSecurityExpressionContributor;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectCodeWorkflowFacadeTest {

    private static final long WORKSPACE_ID = 42L;
    private static final String RESOURCE_TYPE = "Workspace";

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testSaveAllowsWhenBothProjectCreateAndPublishScopesAreGranted() {
        assertSaveGuard(true, true, false);
    }

    @Test
    void testSaveDeniesWhenTheProjectCreateScopeIsRefused() {
        assertSaveGuard(false, true, false);
    }

    @Test
    void testSaveDeniesWhenTheProjectPublishScopeIsRefused() {
        assertSaveGuard(true, false, false);
    }

    @Test
    void testSaveWithEnvironmentAllowsWhenBothProjectCreateAndPublishScopesAreGranted() {
        assertSaveGuard(true, true, true);
    }

    @Test
    void testSaveWithEnvironmentDeniesWhenTheProjectCreateScopeIsRefused() {
        assertSaveGuard(false, true, true);
    }

    @Test
    void testSaveWithEnvironmentDeniesWhenTheProjectPublishScopeIsRefused() {
        assertSaveGuard(true, false, true);
    }

    private void assertSaveGuard(
        boolean projectCreateGranted, boolean projectPublishGranted, boolean withEnvironment) {

        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScope(WORKSPACE_ID, RESOURCE_TYPE, "PROJECT_CREATE"))
            .thenReturn(projectCreateGranted);
        when(permissionService.hasResourceScope(WORKSPACE_ID, RESOURCE_TYPE, "PROJECT_PUBLISH"))
            .thenReturn(projectPublishGranted);

        ProjectCodeWorkflowFacade securedProjectCodeWorkflowFacade = secure(permissionService);

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("alice", "credentials", List.of()));

        SecurityContextHolder.setContext(securityContext);

        ThrowingCallable save = withEnvironment
            ? () -> securedProjectCodeWorkflowFacade.save(
                WORKSPACE_ID, new byte[0], Language.RUBY, Environment.STAGING, true)
            : () -> securedProjectCodeWorkflowFacade.save(WORKSPACE_ID, new byte[0], Language.RUBY);

        if (projectCreateGranted && projectPublishGranted) {
            assertThatThrownBy(save).isInstanceOf(ConfigurationException.class);
        } else {
            assertThatThrownBy(save).isInstanceOf(AccessDeniedException.class);
        }
    }

    private ProjectCodeWorkflowFacade secure(PermissionService permissionService) {
        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(
                permissionService,
                new StaticListableBeanFactory().getBeanProvider(MethodSecurityExpressionContributor.class));

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager = new PreAuthorizeAuthorizationManager();

        preAuthorizeAuthorizationManager.setExpressionHandler(expressionHandler);

        ProxyFactory proxyFactory = new ProxyFactory(projectCodeWorkflowFacade());

        proxyFactory.addAdvisor(
            AuthorizationManagerBeforeMethodInterceptor.preAuthorize(preAuthorizeAuthorizationManager));

        return (ProjectCodeWorkflowFacade) proxyFactory.getProxy();
    }

    private static ProjectCodeWorkflowFacadeImpl projectCodeWorkflowFacade() {
        return new ProjectCodeWorkflowFacadeImpl(
            new ApplicationProperties(), mock(CacheManager.class), mock(ProjectService.class),
            mock(ProjectWorkflowService.class), mock(ProjectFacade.class), mock(ProjectCodeWorkflowService.class),
            mock(CodeWorkflowContainerService.class), mock(CodeWorkflowFileStorage.class),
            mock(ProjectCodeWorkflowDraftService.class), mock(TagService.class), mock(ProjectDeploymentFacade.class),
            mock(ProjectDeploymentService.class), mock(ProjectDeploymentWorkflowService.class),
            mock(ComponentConnectionFacade.class), mock(WorkflowService.class), mock(ProjectOwnershipService.class));
    }
}
