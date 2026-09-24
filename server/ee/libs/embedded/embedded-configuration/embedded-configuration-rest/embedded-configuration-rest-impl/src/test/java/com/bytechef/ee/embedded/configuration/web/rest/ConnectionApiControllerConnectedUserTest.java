/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserConnectionFacadeImpl;
import com.bytechef.ee.embedded.configuration.security.ConnectedUserConnectionMembership;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.web.rest.model.ConnectionModel;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectionApiControllerConnectedUserTest {

    private static final long CONNECTED_USER_A_ID = 100L;
    private static final long CONNECTED_USER_B_ID = 200L;
    private static final long CONNECTION_ID = 42L;
    private static final String EXTERNAL_USER_A_ID = "external-user-a";
    private static final String EXTERNAL_USER_B_ID = "external-user-b";
    private static final long UNKNOWN_CONNECTED_USER_ID = 300L;

    private final ConnectedUserConnectionMembership connectedUserConnectionMembership =
        mock(ConnectedUserConnectionMembership.class);
    private final ConnectedUserConnectionService connectedUserConnectionService =
        mock(ConnectedUserConnectionService.class);
    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final ConnectionFacade connectionFacade = mock(ConnectionFacade.class);
    private final PermissionService permissionService = mock(PermissionService.class);

    private ConnectionApiController connectionApiController;

    @BeforeEach
    void setUp() {
        ConversionService conversionService = mock(ConversionService.class);

        when(conversionService.convert(any(ConnectionModel.class), eq(ConnectionDTO.class)))
            .thenReturn(
                ConnectionDTO.builder()
                    .componentName("slack")
                    .build());
        when(conversionService.convert(any(ConnectionDTO.class), eq(ConnectionModel.class)))
            .thenAnswer(invocation -> new ConnectionModel());

        ConnectedUserConnectionFacadeImpl connectedUserConnectionFacade = new ConnectedUserConnectionFacadeImpl(
            connectedUserConnectionMembership, connectedUserConnectionService, connectedUserService,
            connectionFacade);

        PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager = new PreAuthorizeAuthorizationManager();

        preAuthorizeAuthorizationManager.setExpressionHandler(
            new AutomationMethodSecurityExpressionHandler(permissionService));

        ProxyFactory proxyFactory = new ProxyFactory(
            new ConnectionApiController(connectedUserConnectionFacade, connectionFacade, conversionService));

        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvisor(
            AuthorizationManagerBeforeMethodInterceptor.preAuthorize(preAuthorizeAuthorizationManager));

        connectionApiController = (ConnectionApiController) proxyFactory.getProxy();

        setUpConnectedUser(CONNECTED_USER_A_ID, EXTERNAL_USER_A_ID);
        setUpConnectedUser(CONNECTED_USER_B_ID, EXTERNAL_USER_B_ID);

        when(connectedUserService.fetchConnectedUser(UNKNOWN_CONNECTED_USER_ID)).thenReturn(Optional.empty());
        when(connectedUserConnectionMembership.getConnectionIds(anyLong(), any(Environment.class)))
            .thenReturn(Set.of(CONNECTION_ID));
        when(connectionFacade.create(any(ConnectionDTO.class), eq(PlatformType.EMBEDDED))).thenReturn(CONNECTION_ID);
        when(connectionFacade.getConnections(anyList(), eq(PlatformType.EMBEDDED)))
            .thenReturn(
                List.of(
                    ConnectionDTO.builder()
                        .id(CONNECTION_ID)
                        .componentName("slack")
                        .build()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetConnectedUserConnectionsDeniesConnectedUserOnAnotherConnectedUser() {
        authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

        assertThatThrownBy(
            () -> connectionApiController.getConnectedUserConnections(CONNECTED_USER_B_ID, null, null))
                .isInstanceOf(AccessDeniedException.class);

        verify(connectionFacade, never()).getConnections(anyList(), any());
    }

    @Test
    void testGetConnectedUserConnectionsDeniesConnectedUserOnUnknownConnectedUser() {
        authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

        assertThatThrownBy(
            () -> connectionApiController.getConnectedUserConnections(UNKNOWN_CONNECTED_USER_ID, null, null))
                .isInstanceOf(AccessDeniedException.class);

        verify(connectionFacade, never()).getConnections(anyList(), any());
    }

    @Test
    void testGetConnectedUserConnectionsDeniesConnectedUserFromAnotherEnvironment() {
        authenticate(createConnectedUserAuthentication(Environment.DEVELOPMENT));

        assertThatThrownBy(
            () -> connectionApiController.getConnectedUserConnections(CONNECTED_USER_A_ID, null, null))
                .isInstanceOf(AccessDeniedException.class);

        verify(connectionFacade, never()).getConnections(anyList(), any());
    }

    @Test
    void testGetConnectedUserConnectionsAllowsConnectedUserOnOwnConnectedUser() {
        authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

        ResponseEntity<List<ConnectionModel>> responseEntity = connectionApiController.getConnectedUserConnections(
            CONNECTED_USER_A_ID, null, null);

        assertThat(responseEntity.getBody()).hasSize(1);

        verify(connectedUserConnectionMembership).getConnectionIds(CONNECTED_USER_A_ID, Environment.PRODUCTION);
    }

    @Test
    void testGetConnectedUserConnectionsAllowsTenantAdminOnAnyConnectedUser() {
        authenticate(createTenantAdminAuthentication());

        ResponseEntity<List<ConnectionModel>> responseEntity = connectionApiController.getConnectedUserConnections(
            CONNECTED_USER_B_ID, null, null);

        assertThat(responseEntity.getBody()).hasSize(1);

        verify(connectedUserConnectionMembership).getConnectionIds(CONNECTED_USER_B_ID, Environment.PRODUCTION);
    }

    @Test
    void testGetConnectedUserConnectionsDeniesCallerWhoIsNeitherTenantAdminNorConnectedUser() {
        authenticate(createRegularUserAuthentication());

        assertThatThrownBy(
            () -> connectionApiController.getConnectedUserConnections(CONNECTED_USER_A_ID, null, null))
                .isInstanceOf(AccessDeniedException.class);

        verify(connectionFacade, never()).getConnections(anyList(), any());
    }

    @Test
    void testCreateConnectedUserConnectionDeniesConnectedUserOnAnotherConnectedUser() {
        authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

        assertThatThrownBy(
            () -> connectionApiController.createConnectedUserConnection(CONNECTED_USER_B_ID, new ConnectionModel()))
                .isInstanceOf(AccessDeniedException.class);

        verify(connectionFacade, never()).create(any(), any());
        verify(connectedUserConnectionService, never()).create(anyLong(), anyLong());
    }

    @Test
    void testCreateConnectedUserConnectionDeniesConnectedUserFromAnotherEnvironment() {
        authenticate(createConnectedUserAuthentication(Environment.DEVELOPMENT));

        assertThatThrownBy(
            () -> connectionApiController.createConnectedUserConnection(CONNECTED_USER_A_ID, new ConnectionModel()))
                .isInstanceOf(AccessDeniedException.class);

        verify(connectionFacade, never()).create(any(), any());
        verify(connectedUserConnectionService, never()).create(anyLong(), anyLong());
    }

    @Test
    void testCreateConnectedUserConnectionAllowsConnectedUserOnOwnConnectedUser() {
        authenticate(createConnectedUserAuthentication(Environment.PRODUCTION));

        ResponseEntity<Long> responseEntity = connectionApiController.createConnectedUserConnection(
            CONNECTED_USER_A_ID, new ConnectionModel());

        assertThat(responseEntity.getBody()).isEqualTo(CONNECTION_ID);

        verify(connectedUserConnectionService).create(CONNECTED_USER_A_ID, CONNECTION_ID);
    }

    @Test
    void testCreateConnectedUserConnectionAllowsTenantAdminOnAnyConnectedUser() {
        authenticate(createTenantAdminAuthentication());

        ResponseEntity<Long> responseEntity = connectionApiController.createConnectedUserConnection(
            CONNECTED_USER_B_ID, new ConnectionModel());

        assertThat(responseEntity.getBody()).isEqualTo(CONNECTION_ID);

        verify(connectedUserConnectionService).create(CONNECTED_USER_B_ID, CONNECTION_ID);
    }

    @Test
    void testCreateConnectedUserConnectionDeniesCallerWhoIsNeitherTenantAdminNorConnectedUser() {
        authenticate(createRegularUserAuthentication());

        assertThatThrownBy(
            () -> connectionApiController.createConnectedUserConnection(CONNECTED_USER_A_ID, new ConnectionModel()))
                .isInstanceOf(AccessDeniedException.class);

        verify(connectionFacade, never()).create(any(), any());
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
    }

    private static EmbeddedApiKeyAuthenticationToken createConnectedUserAuthentication(Environment environment) {
        return new EmbeddedApiKeyAuthenticationToken(
            environment.ordinal(), new User(EXTERNAL_USER_A_ID, "", List.of()));
    }

    private static Authentication createRegularUserAuthentication() {
        return new UsernamePasswordAuthenticationToken(
            "user@localhost.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private Authentication createTenantAdminAuthentication() {
        when(permissionService.isTenantAdmin()).thenReturn(true);

        return new UsernamePasswordAuthenticationToken(
            "admin@localhost.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private void setUpConnectedUser(long connectedUserId, String externalUserId) {
        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.getId()).thenReturn(connectedUserId);
        when(connectedUser.getExternalId()).thenReturn(externalUserId);
        when(connectedUser.getEnvironment()).thenReturn(Environment.PRODUCTION);
        when(connectedUser.getEnvironmentId()).thenReturn((long) Environment.PRODUCTION.ordinal());
        when(connectedUserService.fetchConnectedUser(connectedUserId)).thenReturn(Optional.of(connectedUser));
        when(connectedUserService.getConnectedUser(connectedUserId)).thenReturn(connectedUser);
    }
}
