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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.security.web.expression.MethodSecurityExpressionContributor;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
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
        IntegrationIntTestConfiguration.class, IntegrationInstanceAdminFacadeIntTest.MethodSecurityConfiguration.class
    },
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class IntegrationInstanceAdminFacadeIntTest {

    private static final long CONNECTED_USER_ID = 5L;
    private static final long CONNECTION_ID = 9L;
    private static final long INTEGRATION_INSTANCE_ID = 1L;
    private static final String WORKFLOW_ID = "workflow-1";

    @Autowired
    private IntegrationInstanceAdminFacade integrationInstanceAdminFacade;

    @MockitoBean
    private ConnectedUserConnectionFacade connectedUserConnectionFacade;

    @MockitoBean
    private ConnectedUserIntegrationFacade connectedUserIntegrationFacade;

    @MockitoBean
    private IntegrationInstanceFacade integrationInstanceFacade;

    @MockitoBean
    private IntegrationInstanceService integrationInstanceService;

    @MockitoBean
    private PermissionService permissionService;

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testConnectedUserIsDenied() {
        authenticate(
            new EmbeddedApiKeyAuthenticationToken(
                Environment.PRODUCTION.ordinal(), 1L, new User("external-user-1", "", List.of()), false));

        assertThatThrownBy(
            () -> integrationInstanceAdminFacade.enableIntegrationInstanceWorkflow(
                INTEGRATION_INSTANCE_ID, WORKFLOW_ID, true))
                    .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(integrationInstanceFacade);
    }

    @Test
    void testNonAdminIsDenied() {
        authenticate(
            new UsernamePasswordAuthenticationToken("user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        when(permissionService.isTenantAdmin()).thenReturn(false);

        assertThatThrownBy(
            () -> integrationInstanceAdminFacade.enableIntegrationInstanceWorkflow(
                INTEGRATION_INSTANCE_ID, WORKFLOW_ID, true))
                    .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(integrationInstanceFacade);
    }

    @Test
    void testTenantAdminReachesTheSharedFacade() {
        authenticate(
            new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        when(permissionService.isTenantAdmin()).thenReturn(true);

        integrationInstanceAdminFacade.enableIntegrationInstanceWorkflow(INTEGRATION_INSTANCE_ID, WORKFLOW_ID, true);

        verify(integrationInstanceFacade).enableIntegrationInstanceWorkflow(INTEGRATION_INSTANCE_ID, WORKFLOW_ID, true);
    }

    @Test
    void testProvisionIntegrationInstanceIsDeniedForConnectedUsersAndNonAdmins() {
        authenticate(
            new EmbeddedApiKeyAuthenticationToken(
                Environment.PRODUCTION.ordinal(), 1L, new User("external-user-1", "", List.of()), false));

        assertThatThrownBy(
            () -> integrationInstanceAdminFacade.provisionIntegrationInstance(1L, 2L, Map.of("key", "v"), List.of()))
                .isInstanceOf(AccessDeniedException.class);

        authenticate(
            new UsernamePasswordAuthenticationToken("user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        when(permissionService.isTenantAdmin()).thenReturn(false);

        assertThatThrownBy(
            () -> integrationInstanceAdminFacade.provisionIntegrationInstance(1L, 2L, Map.of("key", "v"), List.of()))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(connectedUserIntegrationFacade);
    }

    @Test
    void testProvisionIntegrationInstanceDelegatesForTenantAdmin() {
        authenticate(
            new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        when(permissionService.isTenantAdmin()).thenReturn(true);

        integrationInstanceAdminFacade.provisionIntegrationInstance(1L, 2L, Map.of("key", "v"), List.of());

        verify(connectedUserIntegrationFacade).provisionIntegrationInstance(
            1L, 2L, Map.of("key", "v"), List.of(), "admin");
    }

    @Test
    void testGetProvisionableIntegrationsIsDeniedForConnectedUsersAndNonAdmins() {
        authenticate(
            new EmbeddedApiKeyAuthenticationToken(
                Environment.PRODUCTION.ordinal(), 1L, new User("external-user-1", "", List.of()), false));

        assertThatThrownBy(() -> integrationInstanceAdminFacade.getProvisionableIntegrations(1L))
            .isInstanceOf(AccessDeniedException.class);

        authenticate(
            new UsernamePasswordAuthenticationToken("user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        when(permissionService.isTenantAdmin()).thenReturn(false);

        assertThatThrownBy(() -> integrationInstanceAdminFacade.getProvisionableIntegrations(1L))
            .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(connectedUserIntegrationFacade);
    }

    @Test
    void testGetProvisionableIntegrationsDelegatesForTenantAdmin() {
        authenticate(
            new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        when(permissionService.isTenantAdmin()).thenReturn(true);

        integrationInstanceAdminFacade.getProvisionableIntegrations(1L);

        verify(connectedUserIntegrationFacade).getProvisionableIntegrations(1L);
    }

    @Test
    void testGetIntegrationInstanceConnectionReturnsProvisionedConnection() {
        authenticateTenantAdmin();

        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .id(CONNECTION_ID)
            .build();

        when(integrationInstanceService.fetchIntegrationInstance(INTEGRATION_INSTANCE_ID))
            .thenReturn(Optional.of(createIntegrationInstance("admin")));
        when(connectedUserConnectionFacade.getConnectedUserConnectionsForAdmin(
            CONNECTED_USER_ID, null, List.of(CONNECTION_ID))).thenReturn(List.of(connectionDTO));

        assertThat(integrationInstanceAdminFacade.getIntegrationInstanceConnection(INTEGRATION_INSTANCE_ID))
            .isEqualTo(connectionDTO);
    }

    @Test
    void testGetIntegrationInstanceConnectionThrowsForSelfActivatedInstance() {
        authenticateTenantAdmin();

        when(integrationInstanceService.fetchIntegrationInstance(INTEGRATION_INSTANCE_ID))
            .thenReturn(Optional.of(createIntegrationInstance(null)));

        assertThatThrownBy(
            () -> integrationInstanceAdminFacade.getIntegrationInstanceConnection(INTEGRATION_INSTANCE_ID))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Integration instance id=%s not found".formatted(INTEGRATION_INSTANCE_ID));

        verifyNoInteractions(connectedUserConnectionFacade);
    }

    @Test
    void testGetIntegrationInstanceConnectionThrowsForUnknownInstance() {
        authenticateTenantAdmin();

        when(integrationInstanceService.fetchIntegrationInstance(INTEGRATION_INSTANCE_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> integrationInstanceAdminFacade.getIntegrationInstanceConnection(INTEGRATION_INSTANCE_ID))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void testUpdateIntegrationInstanceCredentialsDelegatesForProvisionedInstance() {
        authenticateTenantAdmin();

        when(integrationInstanceService.fetchIntegrationInstance(INTEGRATION_INSTANCE_ID))
            .thenReturn(Optional.of(createIntegrationInstance("admin")));

        integrationInstanceAdminFacade.updateIntegrationInstanceCredentials(
            INTEGRATION_INSTANCE_ID, Map.of("token", "new"), 4);

        verify(connectedUserConnectionFacade).updateIntegrationInstanceConnectionCredentials(
            CONNECTED_USER_ID, CONNECTION_ID, Map.of("token", "new"), 4);
    }

    @Test
    void testUpdateIntegrationInstanceCredentialsThrowsForSelfActivatedInstance() {
        authenticateTenantAdmin();

        when(integrationInstanceService.fetchIntegrationInstance(INTEGRATION_INSTANCE_ID))
            .thenReturn(Optional.of(createIntegrationInstance(null)));

        assertThatThrownBy(
            () -> integrationInstanceAdminFacade.updateIntegrationInstanceCredentials(
                INTEGRATION_INSTANCE_ID, Map.of("token", "new"), 4))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("Integration instance id=%s not found".formatted(INTEGRATION_INSTANCE_ID));

        verify(connectedUserConnectionFacade, never())
            .updateIntegrationInstanceConnectionCredentials(anyLong(), anyLong(), any(), anyInt());
    }

    @Test
    void testIntegrationInstanceCredentialsEndpointsAreDeniedForConnectedUsersAndNonAdmins() {
        authenticate(
            new EmbeddedApiKeyAuthenticationToken(
                Environment.PRODUCTION.ordinal(), 1L, new User("external-user-1", "", List.of()), false));

        assertThatThrownBy(() -> integrationInstanceAdminFacade.getIntegrationInstanceConnection(1L))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> integrationInstanceAdminFacade.updateIntegrationInstanceCredentials(1L, Map.of("token", "new"), 0))
                .isInstanceOf(AccessDeniedException.class);

        authenticate(
            new UsernamePasswordAuthenticationToken("user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        when(permissionService.isTenantAdmin()).thenReturn(false);

        assertThatThrownBy(() -> integrationInstanceAdminFacade.getIntegrationInstanceConnection(1L))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
            () -> integrationInstanceAdminFacade.updateIntegrationInstanceCredentials(1L, Map.of("token", "new"), 0))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(connectedUserConnectionFacade);
    }

    private void authenticateTenantAdmin() {
        authenticate(
            new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        when(permissionService.isTenantAdmin()).thenReturn(true);
    }

    private static IntegrationInstance createIntegrationInstance(String provisionedBy) {
        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setConnectedUserId(CONNECTED_USER_ID);
        integrationInstance.setConnectionId(CONNECTION_ID);
        integrationInstance.setId(INTEGRATION_INSTANCE_ID);
        integrationInstance.setProvisionedBy(provisionedBy);

        return integrationInstance;
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
            @Lazy PermissionService permissionService,
            ObjectProvider<MethodSecurityExpressionContributor> methodSecurityExpressionContributorProvider) {

            return new AutomationMethodSecurityExpressionHandler(
                permissionService, methodSecurityExpressionContributorProvider);
        }
    }
}
