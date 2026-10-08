/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        IntegrationIntTestConfiguration.class, ConnectionAdminFacadeIntTest.MethodSecurityConfiguration.class
    },
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class ConnectionAdminFacadeIntTest {

    @Autowired
    private ConnectionAdminFacade connectionAdminFacade;

    @Autowired
    private ConnectionFacade connectionFacade;

    @MockitoBean
    private ConnectedUserConnectionService connectedUserConnectionService;

    @MockitoBean
    private PermissionService permissionService;

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testConnectedUserIsDeniedEveryOperation() {
        authenticate(
            new EmbeddedApiKeyAuthenticationToken(
                Environment.PRODUCTION.ordinal(), 1L, new User("external-user-1", "", List.of()), false));

        assertEveryOperationIsDenied();
    }

    @Test
    void testNonAdminIsDeniedEveryOperation() {
        authenticate(
            new UsernamePasswordAuthenticationToken("user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        when(permissionService.isTenantAdmin()).thenReturn(false);

        assertEveryOperationIsDenied();
    }

    @Test
    void testTenantAdminReachesTheSharedFacadeTypedEmbedded() {
        authenticate(
            new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .componentName("slack")
            .build();

        when(permissionService.isTenantAdmin()).thenReturn(true);
        when(connectionFacade.create(connectionDTO, PlatformType.EMBEDDED)).thenReturn(7L);

        assertThat(connectionAdminFacade.createConnection(connectionDTO, true)).isEqualTo(7L);

        connectionAdminFacade.deleteConnection(7L);
        connectionAdminFacade.getConnection(7L);
        connectionAdminFacade.getConnections("slack", 1, 2L, 3L);
        connectionAdminFacade.getSharedConnectionIds();
        connectionAdminFacade.updateConnection(7L, "name", List.of(), true, 0);

        verify(connectionFacade).delete(7L);
        verify(connectionFacade).getConnection(7L);
        verify(connectionFacade).getConnections("slack", 1, List.of(), 3L, 2L, PlatformType.EMBEDDED);
        verify(connectionFacade).update(7L, "name", List.of(), 0);
        verify(connectedUserConnectionService, times(2)).updateShared(7L, true);
        verify(connectedUserConnectionService).deleteByConnectionId(7L);
        verify(connectedUserConnectionService).getSharedConnectionIds();
    }

    private void assertEveryOperationIsDenied() {
        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .componentName("slack")
            .build();

        assertThatThrownBy(() -> connectionAdminFacade.createConnection(connectionDTO, false))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionAdminFacade.deleteConnection(7L))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionAdminFacade.getConnection(7L))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionAdminFacade.getConnections(null, null, null, null))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionAdminFacade.getSharedConnectionIds())
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> connectionAdminFacade.updateConnection(7L, "name", List.of(), null, 0))
            .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(connectionFacade, connectedUserConnectionService);
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
    }

    @Configuration
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {

        @Bean
        static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            @Lazy PermissionService permissionService) {

            return new AutomationMethodSecurityExpressionHandler(permissionService);
        }
    }
}
