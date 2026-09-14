/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = OrganizationConnectionFacadeIntTest.OrganizationConnectionFacadeIntTestConfiguration.class,
    properties = {
        "bytechef.coordinator.enabled=true", "bytechef.edition=ee"
    })
@MockitoBean(types = {
    ConnectionFacade.class, ConnectionService.class
})
class OrganizationConnectionFacadeIntTest {

    private static final String BODY_REACHED = "body reached";
    private static final long CONNECTION_ID = 5L;

    private static final String[] PUBLIC_METHODS = {
        "create", "delete", "getOrganizationConnections", "update"
    };

    @Autowired
    private ConnectionFacade connectionFacade;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private OrganizationConnectionFacade organizationConnectionFacade;

    @BeforeEach
    void beforeEach() {
        when(connectionFacade.create(any(ConnectionDTO.class), any(PlatformType.class)))
            .thenThrow(new IllegalStateException(BODY_REACHED));
        when(connectionService.getConnection(CONNECTION_ID)).thenThrow(new IllegalStateException(BODY_REACHED));
        when(connectionService.getConnectionsByVisibility(any(ResourceVisibility.class), any(PlatformType.class)))
            .thenThrow(new IllegalStateException(BODY_REACHED));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @MethodSource("publicMethods")
    void testPublicMethodIsDeniedToANonAdmin(String methodName) {
        authenticate(AuthorityConstants.USER);

        assertThatThrownBy(() -> invokePublicMethod(methodName)).isInstanceOf(AccessDeniedException.class);
    }

    @ParameterizedTest
    @MethodSource("publicMethods")
    void testPublicMethodIsAllowedToATenantAdmin(String methodName) {
        authenticate(AuthorityConstants.ADMIN);

        assertBodyReached(() -> invokePublicMethod(methodName));
    }

    @Test
    void testEveryPublicMethodIsEvaluated() {
        List<String> publicMethodNames = Arrays.stream(OrganizationConnectionFacadeImpl.class.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .filter(method -> !method.isSynthetic())
            .map(Method::getName)
            .toList();

        assertThat(publicMethodNames)
            .as("a method added to the facade must be added to PUBLIC_METHODS, or its guard goes unevaluated")
            .containsExactlyInAnyOrder(PUBLIC_METHODS);
    }

    private static Stream<String> publicMethods() {
        return Arrays.stream(PUBLIC_METHODS);
    }

    private void invokePublicMethod(String methodName) {
        switch (methodName) {
            case "create" -> organizationConnectionFacade.create(
                ConnectionDTO.builder()
                    .name("Shared Slack")
                    .visibility(ResourceVisibility.ORGANIZATION)
                    .build());
            case "delete" -> organizationConnectionFacade.delete(CONNECTION_ID);
            case "getOrganizationConnections" -> organizationConnectionFacade.getOrganizationConnections(null);
            case "update" -> organizationConnectionFacade.update(CONNECTION_ID, "Shared Slack", List.of(), 0);
            default -> throw new IllegalArgumentException("No invocation for public method " + methodName);
        }
    }

    private static void assertBodyReached(ThrowingCallable throwingCallable) {
        assertThatThrownBy(throwingCallable)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(BODY_REACHED);
    }

    private static void authenticate(String authority) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "member", "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    @SpringBootConfiguration
    @EnableMethodSecurity
    @Import(OrganizationConnectionFacadeImpl.class)
    static class OrganizationConnectionFacadeIntTestConfiguration {
    }
}
