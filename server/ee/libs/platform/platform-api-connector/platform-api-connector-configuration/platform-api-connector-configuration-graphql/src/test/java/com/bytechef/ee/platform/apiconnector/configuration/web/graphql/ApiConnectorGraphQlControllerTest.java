/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * Evaluates the real {@link PreAuthorize} expressions on every {@link ApiConnectorGraphQlController} query and mutation
 * through the real {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator}. API
 * connectors are tenant-wide component definitions, so each query and mutation must decide on the caller's
 * {@code ROLE_ADMIN} authority alone.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ApiConnectorGraphQlControllerTest {

    private static final Set<String> ENDPOINT_NAMES = Set.of(
        "apiConnector", "apiConnectors", "cancelGenerationJob", "createApiConnector", "deleteApiConnector",
        "enableApiConnector", "generateFromDocumentation", "generateSpecification", "generationJobStatus",
        "importOpenApiSpecification", "startGenerateFromDocumentationPreview", "updateApiConnector");

    static Stream<Arguments> endpoints() {
        return endpointMethods().flatMap(method -> Stream.of(Arguments.of(method, false), Arguments.of(method, true)));
    }

    @Test
    void testEveryEndpointIsEvaluated() {
        Set<String> endpointNames = endpointMethods()
            .map(Method::getName)
            .collect(Collectors.toCollection(TreeSet::new));

        assertThat(endpointNames).isEqualTo(new TreeSet<>(ENDPOINT_NAMES));
    }

    @ParameterizedTest(name = "{0} admin={1}")
    @MethodSource("endpoints")
    void testEndpointRequiresTheAdminAuthority(Method method, boolean admin) {
        PermissionService permissionService = mock(PermissionService.class);

        assertThat(evaluateGuard(permissionService, method, admin))
            .as("%s must %s a caller %s ROLE_ADMIN", method.getName(), admin ? "allow" : "deny",
                admin ? "holding" : "without")
            .isEqualTo(admin);

        verifyNoInteractions(permissionService);
    }

    @SuppressFBWarnings(
        value = "SPEL_INJECTION",
        justification = "The expression is this repository's own @PreAuthorize value, not untrusted input.")
    private static boolean evaluateGuard(PermissionService permissionService, Method method, boolean admin) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("%s must carry a @PreAuthorize guard", method.getName())
            .isNotNull();

        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        List<GrantedAuthority> authorities = admin
            ? List.of(new SimpleGrantedAuthority(AuthorityConstants.ADMIN))
            : List.of(new SimpleGrantedAuthority(AuthorityConstants.USER));

        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", "credentials", authorities);

        SimpleMethodInvocation methodInvocation = new SimpleMethodInvocation(
            new Object(), method, new Object[method.getParameterCount()]);

        EvaluationContext evaluationContext =
            expressionHandler.createEvaluationContext(() -> authentication, methodInvocation);

        Expression expression = expressionHandler.getExpressionParser()
            .parseExpression(preAuthorize.value());

        return Boolean.TRUE.equals(expression.getValue(evaluationContext, Boolean.class));
    }

    private static Stream<Method> endpointMethods() {
        return Arrays.stream(ApiConnectorGraphQlController.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(MutationMapping.class) ||
                method.isAnnotationPresent(QueryMapping.class));
    }
}
