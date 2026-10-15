/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.ConnectedUserAuthentication;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.OrderUtils;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserProjectJobPrincipalAuthenticationResolverTest {

    private static final long CONNECTED_USER_ID = 3L;
    private static final long PROJECT_DEPLOYMENT_ID = 12L;

    private final ConnectedUserProjectService connectedUserProjectService = mock(ConnectedUserProjectService.class);
    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);

    private final ConnectedUserProjectJobPrincipalAuthenticationResolver resolver =
        new ConnectedUserProjectJobPrincipalAuthenticationResolver(connectedUserProjectService, connectedUserService);

    @Test
    void testResolverTakesPrecedenceOverOtherAutomationResolvers() {
        assertThat(resolver.getType()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(OrderUtils.getOrder(ConnectedUserProjectJobPrincipalAuthenticationResolver.class))
            .isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }

    @Test
    void testIsApplicableOnlyToConnectedUserProjectDeployments() {
        when(connectedUserProjectService.containsProjectDeployment(PROJECT_DEPLOYMENT_ID)).thenReturn(true);
        when(connectedUserProjectService.containsProjectDeployment(13L)).thenReturn(false);

        assertThat(resolver.isApplicable(PROJECT_DEPLOYMENT_ID)).isTrue();
        assertThat(resolver.isApplicable(13L)).isFalse();
    }

    @Test
    void testFetchAuthenticationReturnsTheConnectedUserOwningTheProjectDeployment() {
        when(connectedUserProjectService.fetchConnectedUserId(PROJECT_DEPLOYMENT_ID))
            .thenReturn(Optional.of(CONNECTED_USER_ID));

        ConnectedUser connectedUser = new ConnectedUser(
            Map.of(), null, true, "external-user", CONNECTED_USER_ID, null, 0);

        connectedUser.setEnvironment(Environment.DEVELOPMENT);

        when(connectedUserService.fetchConnectedUser(CONNECTED_USER_ID)).thenReturn(Optional.of(connectedUser));

        Authentication authentication = resolver.fetchAuthentication(PROJECT_DEPLOYMENT_ID)
            .orElseThrow();

        assertThat(authentication).isInstanceOf(ConnectedUserAuthentication.class);
        assertThat(authentication.getName()).isEqualTo("external-user");
        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @Test
    void testFetchAuthenticationIsEmptyWithoutAConnectedUser() {
        when(connectedUserProjectService.fetchConnectedUserId(PROJECT_DEPLOYMENT_ID)).thenReturn(Optional.empty());

        assertThat(resolver.fetchAuthentication(PROJECT_DEPLOYMENT_ID)).isEmpty();
    }
}
