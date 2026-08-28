/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.owner.Owner;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.security.web.authentication.PrincipalEnvironment;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ConnectedUserOwnerResolverTest {

    @Mock
    private ConnectedUserProjectService connectedUserProjectService;

    @Mock
    private ConnectedUserService connectedUserService;

    @Mock
    private IntegrationInstanceService integrationInstanceService;

    private ConnectedUserOwnerResolver connectedUserOwnerResolver;

    @BeforeEach
    void setUp() {
        connectedUserOwnerResolver = new ConnectedUserOwnerResolver(
            connectedUserProjectService, connectedUserService, integrationInstanceService);
    }

    @Test
    void testEmbeddedResolvesThroughTheIntegrationInstance() {
        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setConnectedUserId(1055L);

        when(integrationInstanceService.getIntegrationInstance(77L)).thenReturn(integrationInstance);

        Optional<Owner> owner = connectedUserOwnerResolver.resolveJobPrincipal(77L, PlatformType.EMBEDDED);

        assertEquals(Optional.of(Owner.connectedUser(1055L)), owner);
    }

    @Test
    void testEmbeddedWithNoConnectedUserResolvesToNoOwner() {
        when(integrationInstanceService.getIntegrationInstance(77L)).thenReturn(new IntegrationInstance());

        assertEquals(
            Optional.empty(), connectedUserOwnerResolver.resolveJobPrincipal(77L, PlatformType.EMBEDDED));
    }

    @Test
    void testAutomationResolvesThroughTheConnectedUserProject() {
        when(connectedUserProjectService.fetchConnectedUserId(88L)).thenReturn(Optional.of(1055L));

        Optional<Owner> owner = connectedUserOwnerResolver.resolveJobPrincipal(88L, PlatformType.AUTOMATION);

        assertEquals(Optional.of(Owner.connectedUser(1055L)), owner);
    }

    @Test
    void testAutomationWithNoConnectedUserProjectResolvesToNoOwner() {
        when(connectedUserProjectService.fetchConnectedUserId(88L)).thenReturn(Optional.empty());

        assertEquals(
            Optional.empty(), connectedUserOwnerResolver.resolveJobPrincipal(88L, PlatformType.AUTOMATION));
    }

    @Test
    void testCurrentPrincipalWithNoLoginResolvesToNoOwner() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.empty());

            assertEquals(Optional.empty(), connectedUserOwnerResolver.resolveCurrentPrincipal());
        }
    }

    @Test
    void testCurrentPrincipalWithNoEnvironmentResolvesToNoOwner() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class);
            MockedStatic<PrincipalEnvironment> principalEnvironment = mockStatic(PrincipalEnvironment.class)) {

            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.of("demo-account"));
            principalEnvironment.when(PrincipalEnvironment::fetchCurrentPrincipalEnvironmentId)
                .thenReturn(Optional.empty());

            assertEquals(Optional.empty(), connectedUserOwnerResolver.resolveCurrentPrincipal());
        }
    }

    @Test
    void testCurrentPrincipalResolvesThroughTheConnectedUser() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class);
            MockedStatic<PrincipalEnvironment> principalEnvironment = mockStatic(PrincipalEnvironment.class)) {

            securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
                .thenReturn(Optional.of("demo-account"));
            principalEnvironment.when(PrincipalEnvironment::fetchCurrentPrincipalEnvironmentId)
                .thenReturn(Optional.of(2L));

            ConnectedUser connectedUser = mock(ConnectedUser.class);

            when(connectedUser.getId()).thenReturn(1055L);
            when(connectedUserService.fetchConnectedUser("demo-account", 2L)).thenReturn(Optional.of(connectedUser));

            assertEquals(
                Optional.of(Owner.connectedUser(1055L)), connectedUserOwnerResolver.resolveCurrentPrincipal());
        }
    }
}
