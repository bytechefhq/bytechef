/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.facade;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent.Language;
import com.bytechef.ee.platform.customcomponent.configuration.service.CustomComponentService;
import com.bytechef.ee.platform.customcomponent.file.storage.CustomComponentFileStorage;
import com.bytechef.ee.platform.customcomponent.loader.ComponentHandlerLoader;
import com.bytechef.platform.security.web.expression.MethodSecurityExpressionContributor;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CustomComponentFacadeTest {

    private static final String BODY_REACHED = "body reached";

    private final CustomComponentFileStorage customComponentFileStorage = mock(CustomComponentFileStorage.class);
    private final CustomComponentService customComponentService = mock(CustomComponentService.class);
    private final PermissionService permissionService = mock(PermissionService.class);

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "admin={0}")
    @ValueSource(booleans = {
        false, true
    })
    void testSaveRequiresTheAdminAuthority(boolean admin) {
        CustomComponentFacade customComponentFacade = createSecuredCustomComponentFacade(admin);

        try (MockedStatic<ComponentHandlerLoader> componentHandlerLoaderMockedStatic =
            mockStatic(ComponentHandlerLoader.class)) {

            componentHandlerLoaderMockedStatic
                .when(() -> ComponentHandlerLoader.loadComponentDefinition(any(), any()))
                .thenThrow(new IllegalStateException(BODY_REACHED));

            assertGuard(() -> customComponentFacade.save(new byte[0], Language.JAVA), admin);
        }
    }

    @ParameterizedTest(name = "admin={0}")
    @ValueSource(booleans = {
        false, true
    })
    void testDeleteRequiresTheAdminAuthority(boolean admin) {
        CustomComponentFacade customComponentFacade = createSecuredCustomComponentFacade(admin);

        when(customComponentService.getCustomComponent(anyLong())).thenThrow(new IllegalStateException(BODY_REACHED));

        assertGuard(() -> customComponentFacade.delete(1L), admin);
    }

    @ParameterizedTest(name = "admin={0}")
    @ValueSource(booleans = {
        false, true
    })
    void testEnableCustomComponentRequiresTheAdminAuthority(boolean admin) {
        CustomComponentFacade customComponentFacade = createSecuredCustomComponentFacade(admin);

        doThrow(new IllegalStateException(BODY_REACHED)).when(customComponentService)
            .enableCustomComponent(anyLong(), anyBoolean());

        assertGuard(() -> customComponentFacade.enableCustomComponent(1L, true), admin);
    }

    @ParameterizedTest(name = "admin={0}")
    @ValueSource(booleans = {
        false, true
    })
    void testCreateEmptyCustomComponentRequiresTheAdminAuthority(boolean admin) {
        CustomComponentFacade customComponentFacade = createSecuredCustomComponentFacade(admin);

        when(customComponentService.fetchLatestCustomComponent(anyString()))
            .thenThrow(new IllegalStateException(BODY_REACHED));

        assertGuard(() -> customComponentFacade.createEmptyCustomComponent("acmeTasks", Language.JAVASCRIPT), admin);
    }

    @ParameterizedTest(name = "admin={0}")
    @ValueSource(booleans = {
        false, true
    })
    void testGetCustomComponentSourceRequiresTheAdminAuthority(boolean admin) {
        CustomComponentFacade customComponentFacade = createSecuredCustomComponentFacade(admin);

        when(customComponentService.getCustomComponent(anyLong())).thenThrow(new IllegalStateException(BODY_REACHED));

        assertGuard(() -> customComponentFacade.getCustomComponentSource(1L), admin);
    }

    @ParameterizedTest(name = "admin={0}")
    @ValueSource(booleans = {
        false, true
    })
    void testPublishCustomComponentRequiresTheAdminAuthority(boolean admin) {
        CustomComponentFacade customComponentFacade = createSecuredCustomComponentFacade(admin);

        when(customComponentService.publishCustomComponent(anyLong()))
            .thenThrow(new IllegalStateException(BODY_REACHED));

        assertGuard(() -> customComponentFacade.publishCustomComponent(1L), admin);
    }

    @ParameterizedTest(name = "admin={0}")
    @ValueSource(booleans = {
        false, true
    })
    void testUpdateCustomComponentSourceRequiresTheAdminAuthority(boolean admin) {
        CustomComponentFacade customComponentFacade = createSecuredCustomComponentFacade(admin);

        when(customComponentService.getCustomComponent(anyLong())).thenThrow(new IllegalStateException(BODY_REACHED));

        assertGuard(() -> customComponentFacade.updateCustomComponentSource(1L, "source"), admin);
    }

    private void assertGuard(ThrowingCallable callable, boolean admin) {
        if (admin) {
            assertThatThrownBy(callable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);
        } else {
            assertThatThrownBy(callable).isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(customComponentFileStorage, customComponentService);
        }

        verifyNoInteractions(permissionService);
    }

    private static ApplicationProperties createApplicationProperties() {
        ApplicationProperties.Component component = new ApplicationProperties.Component();

        ApplicationProperties.Component.CustomComponent customComponent = component.getCustomComponent();

        ApplicationProperties.Component.CustomComponent.Java java = customComponent.getJava();

        java.setEnabled(true);

        ApplicationProperties applicationProperties = mock(ApplicationProperties.class);

        when(applicationProperties.getComponent()).thenReturn(component);

        return applicationProperties;
    }

    @SuppressWarnings("unchecked")
    private CustomComponentFacade createSecuredCustomComponentFacade(boolean admin) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "alice", "credentials", List.of(new SimpleGrantedAuthority(admin ? "ROLE_ADMIN" : "ROLE_USER"))));

        SecurityContextHolder.setContext(securityContext);

        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

        ObjectProvider<MethodSecurityExpressionContributor> contributorProvider =
            beanFactory.getBeanProvider(MethodSecurityExpressionContributor.class);

        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService, contributorProvider);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager = new PreAuthorizeAuthorizationManager();

        preAuthorizeAuthorizationManager.setExpressionHandler(expressionHandler);

        ProxyFactory proxyFactory = new ProxyFactory(
            new CustomComponentFacadeImpl(
                createApplicationProperties(), customComponentService, customComponentFileStorage,
                mock(ObjectProvider.class)));

        proxyFactory.addAdvisor(
            AuthorizationManagerBeforeMethodInterceptor.preAuthorize(preAuthorizeAuthorizationManager));

        return (CustomComponentFacade) proxyFactory.getProxy();
    }
}
