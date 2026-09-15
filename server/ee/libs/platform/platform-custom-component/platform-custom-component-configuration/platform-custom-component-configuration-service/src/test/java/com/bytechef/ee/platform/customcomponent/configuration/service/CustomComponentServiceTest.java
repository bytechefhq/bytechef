/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.platform.customcomponent.configuration.repository.CustomComponentRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Calls {@link CustomComponentServiceImpl#enableCustomComponent} through Spring Security's real {@code @PreAuthorize}
 * method interceptor, backed by the real {@link AutomationMethodSecurityExpressionHandler} and
 * {@link AutomationPermissionEvaluator}. It is an authority check, so it must decide on the caller's {@code ROLE_ADMIN}
 * authority alone and never consult {@link PermissionService}; an allowed call proves it entered the method body by
 * reaching the repository stub that throws.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class CustomComponentServiceTest {

    private static final String BODY_REACHED = "body reached";

    private final CustomComponentRepository customComponentRepository = mock(CustomComponentRepository.class);
    private final PermissionService permissionService = mock(PermissionService.class);

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "admin={0}")
    @ValueSource(booleans = {
        false, true
    })
    void testEnableCustomComponentRequiresTheAdminAuthority(boolean admin) {
        when(customComponentRepository.findById(anyLong())).thenThrow(new IllegalStateException(BODY_REACHED));

        CustomComponentService customComponentService = createSecuredCustomComponentService(admin);

        if (admin) {
            assertThatThrownBy(() -> customComponentService.enableCustomComponent(1L, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);
        } else {
            assertThatThrownBy(() -> customComponentService.enableCustomComponent(1L, true))
                .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(customComponentRepository);
        }

        verifyNoInteractions(permissionService);
    }

    private CustomComponentService createSecuredCustomComponentService(boolean admin) {
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

        ProxyFactory proxyFactory = new ProxyFactory(new CustomComponentServiceImpl(customComponentRepository));

        proxyFactory.addAdvisor(
            AuthorizationManagerBeforeMethodInterceptor.preAuthorize(preAuthorizeAuthorizationManager));

        return (CustomComponentService) proxyFactory.getProxy();
    }
}
