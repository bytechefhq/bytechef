/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.security.facade.SigningKeyFacade;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.stubbing.Answer;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Enforces the real {@code @PreAuthorize} guard on every {@link SigningKeyApiController} endpoint through the real
 * {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator}. A signing key signs the
 * JWTs of the tenant's connected users, so each endpoint must decide on {@link PermissionService#isTenantAdmin()}
 * alone.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class SigningKeyApiControllerTest {

    private static final Answer<Object> BODY_REACHED = invocation -> {
        throw new IllegalStateException("body reached");
    };

    private static final Set<String> ENDPOINT_NAMES = Set.of(
        "createSigningKey", "deleteSigningKey", "getSigningKey", "getSigningKeys", "updateSigningKey");

    private final PermissionService permissionService = mock(PermissionService.class);

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testEveryEndpointIsEvaluated() {
        Set<String> endpointNames = endpointMethods()
            .map(Method::getName)
            .collect(Collectors.toCollection(TreeSet::new));

        assertThat(endpointNames).isEqualTo(new TreeSet<>(ENDPOINT_NAMES));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpointMethods")
    void testEndpointDeniesAUserWhoIsNotATenantAdmin(Method method) {
        authenticate("user@localhost.com", "ROLE_USER", false);

        assertThatThrownBy(() -> invokeSecured(method))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).isTenantAdmin();
        verifyNoMoreInteractions(permissionService);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpointMethods")
    void testEndpointAdmitsATenantAdmin(Method method) {
        authenticate("admin@localhost.com", "ROLE_ADMIN", true);

        assertThatThrownBy(() -> invokeSecured(method))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("body reached");

        verify(permissionService).isTenantAdmin();
        verifyNoMoreInteractions(permissionService);
    }

    private void authenticate(String login, String authority, boolean tenantAdmin) {
        when(permissionService.isTenantAdmin()).thenReturn(tenantAdmin);

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                login, null, List.of(new SimpleGrantedAuthority(authority))));

        SecurityContextHolder.setContext(securityContext);
    }

    private void invokeSecured(Method method) throws Throwable {
        Object securedController = secure(
            new SigningKeyApiController(
                mock(SigningKeyFacade.class, BODY_REACHED),
                mock(SigningKeyService.class, BODY_REACHED),
                mock(ConversionService.class, BODY_REACHED)));

        try {
            method.invoke(securedController, createArguments(method));
        } catch (InvocationTargetException invocationTargetException) {
            throw invocationTargetException.getCause();
        }
    }

    private Object secure(Object target) {
        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager = new PreAuthorizeAuthorizationManager();

        preAuthorizeAuthorizationManager.setExpressionHandler(expressionHandler);

        ProxyFactory proxyFactory = new ProxyFactory(target);

        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvisor(
            AuthorizationManagerBeforeMethodInterceptor.preAuthorize(preAuthorizeAuthorizationManager));

        return proxyFactory.getProxy();
    }

    private static Object[] createArguments(Executable executable) throws ReflectiveOperationException {
        Class<?>[] parameterTypes = executable.getParameterTypes();
        Object[] arguments = new Object[parameterTypes.length];

        for (int index = 0; index < parameterTypes.length; index++) {
            arguments[index] = createArgument(parameterTypes[index]);
        }

        return arguments;
    }

    private static Object createArgument(Class<?> parameterType) throws ReflectiveOperationException {
        if (parameterType == String.class) {
            return "1";
        } else if (parameterType == Long.class || parameterType == long.class) {
            return 1L;
        } else if (parameterType == Integer.class || parameterType == int.class) {
            return 1;
        } else if (parameterType == Boolean.class || parameterType == boolean.class) {
            return true;
        } else if (parameterType == List.class) {
            return List.of();
        } else if (parameterType == Map.class) {
            return Map.of();
        } else if (parameterType.isRecord()) {
            Constructor<?> canonicalConstructor = parameterType.getDeclaredConstructor(
                Arrays.stream(parameterType.getRecordComponents())
                    .map(RecordComponent::getType)
                    .toArray(Class<?>[]::new));

            canonicalConstructor.setAccessible(true);

            return canonicalConstructor.newInstance(createArguments(canonicalConstructor));
        } else if (parameterType.isEnum()) {
            return parameterType.getEnumConstants()[0];
        } else if (parameterType.isPrimitive()) {
            return Array.get(Array.newInstance(parameterType, 1), 0);
        } else if (parameterType.getName()
            .startsWith("com.bytechef.")) {
            return parameterType.getDeclaredConstructor()
                .newInstance();
        }

        return null;
    }

    private static Stream<Method> endpointMethods() {
        return Arrays.stream(SigningKeyApiController.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic() && Modifier.isPublic(method.getModifiers()));
    }
}
