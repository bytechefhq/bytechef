/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

    @Autowired
    private ConnectionService connectionService;

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
        when(connectionService.fetchConnection(7L)).thenReturn(Optional.of(connection(7L, PlatformType.EMBEDDED)));

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

        verifyNoInteractions(connectionFacade, connectionService, connectedUserConnectionService);
    }

    private static Connection connection(long id, PlatformType type) {
        Connection connection = new Connection();

        connection.setId(id);
        connection.setType(type);

        return connection;
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
    }

    @Nested
    class EmbeddedTypeGuard {

        private static final long AUTOMATION_CONNECTION_ID = 8L;
        private static final long UNKNOWN_CONNECTION_ID = 9L;

        @BeforeEach
        void beforeEach() {
            authenticate(
                new UsernamePasswordAuthenticationToken(
                    "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

            when(permissionService.isTenantAdmin()).thenReturn(true);
            when(connectionService.fetchConnection(AUTOMATION_CONNECTION_ID))
                .thenReturn(Optional.of(connection(AUTOMATION_CONNECTION_ID, PlatformType.AUTOMATION)));
            when(connectionService.fetchConnection(UNKNOWN_CONNECTION_ID)).thenReturn(Optional.empty());
        }

        @Test
        void testDeleteConnectionRejectsAutomationConnection() {
            assertThatThrownBy(() -> connectionAdminFacade.deleteConnection(AUTOMATION_CONNECTION_ID))
                .isInstanceOf(NoSuchElementException.class);

            verify(connectionFacade, never()).delete(anyLong());
            verify(connectedUserConnectionService, never()).deleteByConnectionId(anyLong());
        }

        @Test
        void testGetConnectionRejectsAutomationConnection() {
            assertThatThrownBy(() -> connectionAdminFacade.getConnection(AUTOMATION_CONNECTION_ID))
                .isInstanceOf(NoSuchElementException.class);

            verify(connectionFacade, never()).getConnection(anyLong());
        }

        @Test
        void testUpdateConnectionRejectsAutomationConnection() {
            assertThatThrownBy(
                () -> connectionAdminFacade.updateConnection(AUTOMATION_CONNECTION_ID, "name", List.of(), true, 0))
                    .isInstanceOf(NoSuchElementException.class);

            verify(connectionFacade, never()).update(anyLong(), anyString(), any(), anyInt());
            verify(connectedUserConnectionService, never()).updateShared(anyLong(), anyBoolean());
        }

        @Test
        void testUnknownConnectionIsRejectedLikeAnAutomationConnection() {
            assertThatThrownBy(() -> connectionAdminFacade.deleteConnection(UNKNOWN_CONNECTION_ID))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Connection id=%s not found".formatted(UNKNOWN_CONNECTION_ID));
            assertThatThrownBy(() -> connectionAdminFacade.getConnection(AUTOMATION_CONNECTION_ID))
                .hasMessage("Connection id=%s not found".formatted(AUTOMATION_CONNECTION_ID));

            verify(connectionFacade, never()).delete(anyLong());
        }
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
