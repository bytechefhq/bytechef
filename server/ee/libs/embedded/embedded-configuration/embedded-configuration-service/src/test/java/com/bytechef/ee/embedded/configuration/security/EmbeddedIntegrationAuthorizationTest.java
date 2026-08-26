/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.util.SecurityUtils;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * The @PreAuthorize predicate guarding the by-id integration read. Uses a real {@link EmbeddedPermissionEvaluator} so
 * the permission expressions are genuinely evaluated rather than stubbed.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedIntegrationAuthorizationTest {

    private static final String EXTERNAL_ID = "connected-user-1";
    private static final long INTEGRATION_ID = 55L;

    private ConnectedUserService connectedUserService;
    private EmbeddedIntegrationAuthorization embeddedIntegrationAuthorization;
    private IntegrationService integrationService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        connectedUserService = mock(ConnectedUserService.class);
        integrationService = mock(IntegrationService.class);

        embeddedIntegrationAuthorization = new EmbeddedIntegrationAuthorization(
            connectedUserService, new EmbeddedPermissionEvaluator(SpelEvaluator.create()), integrationService);

        securityUtilsMock = mockStatic(SecurityUtils.class);

        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.of(EXTERNAL_ID));
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Test
    void testConnectedUserOwningTheIntegrationIsAllowed() {
        givenConnectedUserWithPlan("pro");
        givenIntegrationWithPermissionExpression("metadata['plan'] == 'pro'");

        assertThat(embeddedIntegrationAuthorization.canAccessIntegration(INTEGRATION_ID, Environment.PRODUCTION))
            .isTrue();
    }

    @Test
    void testConnectedUserNotOwningTheIntegrationIsDenied() {
        // The one that matters. A perfectly valid connected user, authenticated in the right environment, asking for
        // an integration whose permission expression excludes them. If the predicate ever degrades to "is a connected
        // user", this is the test that catches it.
        givenConnectedUserWithPlan("free");
        givenIntegrationWithPermissionExpression("metadata['plan'] == 'pro'");

        assertThat(embeddedIntegrationAuthorization.canAccessIntegration(INTEGRATION_ID, Environment.PRODUCTION))
            .isFalse();
    }

    @Test
    void testIntegrationWithoutPermissionExpressionIsVisibleToAnyConnectedUser() {
        // The product's own visibility model: a blank expression means visible to all connected users. The predicate
        // is exactly as strong as that model -- the same answer the list path's per-row filter gives.
        givenConnectedUserWithPlan("free");
        givenIntegrationWithPermissionExpression(null);

        assertThat(embeddedIntegrationAuthorization.canAccessIntegration(INTEGRATION_ID, Environment.PRODUCTION))
            .isTrue();
    }

    @Test
    void testPrincipalThatIsNotAConnectedUserInThatEnvironmentIsDenied() {
        when(connectedUserService.fetchConnectedUser(EXTERNAL_ID, Environment.PRODUCTION))
            .thenReturn(Optional.empty());

        assertThat(embeddedIntegrationAuthorization.canAccessIntegration(INTEGRATION_ID, Environment.PRODUCTION))
            .isFalse();

        // Denied before the integration is ever loaded.
        verify(integrationService, never()).getIntegration(anyLong());
    }

    @Test
    void testNoSecurityContextIsDenied() {
        securityUtilsMock.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.empty());

        assertThat(embeddedIntegrationAuthorization.canAccessIntegration(INTEGRATION_ID, Environment.PRODUCTION))
            .isFalse();
    }

    @Test
    void testNullEnvironmentIsDenied() {
        assertThat(embeddedIntegrationAuthorization.canAccessIntegration(INTEGRATION_ID, null)).isFalse();
    }

    @Test
    void testUnknownIntegrationIsDenied() {
        givenConnectedUserWithPlan("pro");

        when(integrationService.getIntegration(INTEGRATION_ID)).thenThrow(new IllegalArgumentException("missing"));

        assertThat(embeddedIntegrationAuthorization.canAccessIntegration(INTEGRATION_ID, Environment.PRODUCTION))
            .isFalse();
    }

    @Test
    void testCallerIsTakenFromTheSecurityContextNotFromAnArgument() {
        // The predicate has no externalUserId parameter at all, so it cannot be satisfied by naming somebody else.
        // This pins that the connected user actually looked up is the authenticated one.
        givenConnectedUserWithPlan("pro");
        givenIntegrationWithPermissionExpression("metadata['plan'] == 'pro'");

        embeddedIntegrationAuthorization.canAccessIntegration(INTEGRATION_ID, Environment.PRODUCTION);

        verify(connectedUserService).fetchConnectedUser(EXTERNAL_ID, Environment.PRODUCTION);
    }

    private void givenConnectedUserWithPlan(String plan) {
        ConnectedUser connectedUser = new ConnectedUser();

        connectedUser.setExternalId(EXTERNAL_ID);
        connectedUser.setEnvironment(Environment.PRODUCTION);
        connectedUser.setMetadata(Map.of("plan", plan));

        lenient().when(connectedUserService.fetchConnectedUser(EXTERNAL_ID, Environment.PRODUCTION))
            .thenReturn(Optional.of(connectedUser));
    }

    private void givenIntegrationWithPermissionExpression(String permissionExpression) {
        Integration integration = mock(Integration.class);

        lenient().when(integration.getPermissionExpression())
            .thenReturn(permissionExpression);
        lenient().when(integrationService.getIntegration(INTEGRATION_ID))
            .thenReturn(integration);
    }
}
