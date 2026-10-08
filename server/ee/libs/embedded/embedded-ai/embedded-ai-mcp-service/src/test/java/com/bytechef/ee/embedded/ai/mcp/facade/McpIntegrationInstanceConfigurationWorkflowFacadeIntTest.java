/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.service.PermissionService;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringJUnitConfig(McpIntegrationInstanceConfigurationWorkflowFacadeIntTest.MethodSecurityConfiguration.class)
class McpIntegrationInstanceConfigurationWorkflowFacadeIntTest {

    @Autowired
    private McpIntegrationInstanceConfigurationWorkflowFacade mcpIntegrationInstanceConfigurationWorkflowFacade;

    @Autowired
    private PermissionService permissionService;

    @BeforeEach
    void beforeEach() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        reset(permissionService);
    }

    @Test
    void testEveryMethodRequiresTenantAdmin() {
        List<Method> methods =
            Arrays.stream(McpIntegrationInstanceConfigurationWorkflowFacade.class.getDeclaredMethods())
                .filter(method -> !method.isSynthetic())
                .filter(method -> !method.isDefault())
                .toList();

        assertThat(methods).isNotEmpty();

        for (Method method : methods) {
            assertThatThrownBy(() -> invoke(method))
                .as("%s.%s", McpIntegrationInstanceConfigurationWorkflowFacade.class.getSimpleName(), method.getName())
                .isInstanceOf(AccessDeniedException.class);
        }

        verify(permissionService, times(methods.size())).isTenantAdmin();
    }

    private void invoke(Method method) throws Throwable {
        Object[] arguments = Arrays.stream(method.getParameterTypes())
            .map(McpIntegrationInstanceConfigurationWorkflowFacadeIntTest::defaultValue)
            .toArray();

        try {
            method.invoke(mcpIntegrationInstanceConfigurationWorkflowFacade, arguments);
        } catch (InvocationTargetException invocationTargetException) {
            throw invocationTargetException.getCause();
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }

        if (type == int.class) {
            return 0;
        }

        if (type == long.class) {
            return 0L;
        }

        return null;
    }

    @EnableMethodSecurity
    static class MethodSecurityConfiguration {

        @Bean
        static PermissionService permissionService() {
            return mock(PermissionService.class);
        }

        @Bean
        static MethodSecurityExpressionHandler methodSecurityExpressionHandler(PermissionService permissionService) {
            return new AutomationMethodSecurityExpressionHandler(permissionService);
        }

        @Bean
        McpIntegrationInstanceConfigurationWorkflowFacade mcpIntegrationInstanceConfigurationWorkflowFacade()
            throws ReflectiveOperationException {
            Constructor<?> constructor =
                McpIntegrationInstanceConfigurationWorkflowFacadeImpl.class.getDeclaredConstructors()[0];

            Object[] arguments = Arrays.stream(constructor.getParameterTypes())
                .map(parameterType -> (Object) mock(parameterType))
                .toArray();

            return (McpIntegrationInstanceConfigurationWorkflowFacade) constructor.newInstance(arguments);
        }
    }
}
