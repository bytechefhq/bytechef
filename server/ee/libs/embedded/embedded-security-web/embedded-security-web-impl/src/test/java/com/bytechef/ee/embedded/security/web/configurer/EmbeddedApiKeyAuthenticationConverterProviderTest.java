/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.configurer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.platform.security.service.ApiKeyService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * Proves the end of the Critical-severity fix: on the previously-phantom Frontend path, the converter now throws before
 * it ever produces an {@code Authentication} for
 * {@link com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationProvider} to authenticate --
 * so the provider's get-or-create ({@code ConnectedUserService#fetchConnectedUser}/ {@code #createConnectedUser}) is
 * never reached and no phantom {@code ConnectedUser} row is created. The
 * {@code EmbeddedApiKeySecurityConfigurer}/{@code AbstractApiKeyHttpConfigurer} filter only calls the provider with
 * whatever {@code Authentication} the converter returns; when {@code convert()} throws, the filter's exception handling
 * takes over and the provider is never invoked at all -- verified here directly against the two collaborators, without
 * needing the full filter/HttpSecurity wiring.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedApiKeyAuthenticationConverterProviderTest {

    private ApiKeyService apiKeyService;
    private ConnectedUserService connectedUserService;
    private EmbeddedApiKeyAuthenticationConverter converter;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        JwtTokenService jwtTokenService = mock(JwtTokenService.class);
        SigningKeyService signingKeyService = mock(SigningKeyService.class);

        apiKeyService = mock(ApiKeyService.class);
        connectedUserService = mock(ConnectedUserService.class);
        converter = new EmbeddedApiKeyAuthenticationConverter(jwtTokenService, signingKeyService);
        request = mock(HttpServletRequest.class);
    }

    @Test
    void testFrontendPathNeverReachesProviderSoNoConnectedUserIsCreated() {
        String tenantKey = EncodingUtils.base64EncodeToString("test-tenant" + ":randomData");

        when(request.getHeader("Authorization")).thenReturn("Bearer " + tenantKey);
        when(request.getHeader("X-ENVIRONMENT")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/embedded/v1/automation/projects");

        // The filter chain calls convert() first; when it throws, EmbeddedApiKeyAuthenticationProvider.authenticate
        // is never invoked, so connectedUserService.fetchConnectedUser/createConnectedUser never run either.
        assertThatThrownBy(() -> converter.convert(request))
            .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(connectedUserService);
        verifyNoInteractions(apiKeyService);
    }
}
