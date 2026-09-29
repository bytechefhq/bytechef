/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserConnectionFacade;
import com.bytechef.ee.embedded.configuration.web.rest.model.ConnectionModel;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectionApiControllerTest {

    private static final Set<String> ENDPOINT_NAMES = Set.of(
        "createConnection", "deleteConnection", "getConnection", "getConnections", "updateConnection");

    private final ConnectedUserConnectionFacade connectedUserConnectionFacade =
        mock(ConnectedUserConnectionFacade.class);
    private final ConnectionFacade connectionFacade = mock(ConnectionFacade.class);
    private final ConversionService conversionService = mock(ConversionService.class);

    private ConnectionApiController connectionApiController;

    @BeforeEach
    void beforeEach() {
        connectionApiController = new ConnectionApiController(
            connectedUserConnectionFacade, connectionFacade, conversionService);
    }

    @Test
    @SuppressFBWarnings("HARD_CODE_PASSWORD")
    void testGetConnectedUserConnectionsObfuscatesAuthorizationParametersByKey() {
        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .componentName("slack")
            .id(20L)
            .build();

        Map<String, Object> authorizationParameters = new HashMap<>();

        authorizationParameters.put("clientId", "client-identifier");
        authorizationParameters.put("password", "supersecretpassword");
        authorizationParameters.put("token", "abcdefghijklmnopqrstuvwxyz");

        when(connectedUserConnectionFacade.getConnectedUserConnections(1L, "slack"))
            .thenReturn(List.of(connectionDTO));
        when(conversionService.convert(connectionDTO, ConnectionModel.class))
            .thenReturn(new ConnectionModel().authorizationParameters(authorizationParameters));

        List<ConnectionModel> connectionModels = Objects.requireNonNull(
            connectionApiController.getConnectedUserConnections(1L, "slack", List.of())
                .getBody());

        assertThat(connectionModels).hasSize(1);

        ConnectionModel connectionModel = connectionModels.getFirst();

        assertThat(connectionModel.getAuthorizationParameters())
            .containsEntry("clientId", "client-identifier")
            .containsEntry("password", ".".repeat(28))
            .containsEntry("token", ".".repeat(28) + "stuvwxyz");
    }

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

    /**
     * Evaluates the real {@code @PreAuthorize} expression on the plain connection CRUD endpoints through the real
     * {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator}. These endpoints read
     * or write any connection of the tenant, so each must decide on {@link PermissionService#isTenantAdmin()} alone.
     * {@code createConnectedUserConnection} and {@code getConnectedUserConnections} are intentionally out of scope
     * here: {@code ConnectedUserConnectionFacadeImpl} already scopes them to the connected user named in the path (or a
     * tenant admin), and the embedded workflow builder calls them on behalf of that connected user.
     */
    @ParameterizedTest(name = "{0} tenantAdmin={1}")
    @MethodSource("endpoints")
    void testEndpointRequiresATenantAdmin(Method method, boolean tenantAdmin) {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.isTenantAdmin()).thenReturn(tenantAdmin);

        assertThat(evaluateGuard(permissionService, method))
            .as("%s must %s when isTenantAdmin() returns %s", method.getName(), tenantAdmin ? "allow" : "deny",
                tenantAdmin)
            .isEqualTo(tenantAdmin);

        verify(permissionService).isTenantAdmin();
        verifyNoMoreInteractions(permissionService);
    }

    @SuppressFBWarnings(
        value = "SPEL_INJECTION",
        justification = "The expression is this repository's own @PreAuthorize value, not untrusted input.")
    private static boolean evaluateGuard(PermissionService permissionService, Method method) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("%s must carry a @PreAuthorize guard", method.getName())
            .isNotNull();

        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", "credentials", List.of());

        SimpleMethodInvocation methodInvocation = new SimpleMethodInvocation(
            new Object(), method, new Object[method.getParameterCount()]);

        EvaluationContext evaluationContext =
            expressionHandler.createEvaluationContext(() -> authentication, methodInvocation);

        Expression expression = expressionHandler.getExpressionParser()
            .parseExpression(preAuthorize.value());

        return Boolean.TRUE.equals(expression.getValue(evaluationContext, Boolean.class));
    }

    private static Stream<Method> endpointMethods() {
        return Arrays.stream(ConnectionApiController.class.getDeclaredMethods())
            .filter(method -> ENDPOINT_NAMES.contains(method.getName()));
    }
}
