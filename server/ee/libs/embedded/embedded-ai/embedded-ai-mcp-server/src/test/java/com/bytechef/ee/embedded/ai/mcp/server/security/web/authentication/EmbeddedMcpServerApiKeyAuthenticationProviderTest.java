/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.security.web.authentication.ConnectedUserAuthentication;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedMcpServerApiKeyAuthenticationProviderTest {

    private ConnectedUserService connectedUserService;
    private EmbeddedMcpServerApiKeyAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        connectedUserService = mock(ConnectedUserService.class);

        provider = new EmbeddedMcpServerApiKeyAuthenticationProvider(connectedUserService);
    }

    @Test
    void testAuthenticatedTokenCarriesConnectedUserAndEnvironment() {
        ConnectedUser connectedUser = new ConnectedUser(Map.of(), null, true, "ext-1", 42L, null, 0);

        when(connectedUserService.fetchConnectedUser("ext-1", 2L)).thenReturn(Optional.of(connectedUser));

        Authentication authentication = provider.authenticate(
            new EmbeddedMcpServerApiKeyAuthenticationToken(2L, "ext-1", "tenant"));

        assertThat(authentication).isInstanceOf(ConnectedUserAuthentication.class);

        ConnectedUserAuthentication connectedUserAuthentication = (ConnectedUserAuthentication) authentication;

        assertThat(connectedUserAuthentication.connectedUserId()).isEqualTo(42L);
        assertThat(connectedUserAuthentication.externalUserId()).isEqualTo("ext-1");
        assertThat(connectedUserAuthentication.environmentId()).isEqualTo(2L);
    }
}
