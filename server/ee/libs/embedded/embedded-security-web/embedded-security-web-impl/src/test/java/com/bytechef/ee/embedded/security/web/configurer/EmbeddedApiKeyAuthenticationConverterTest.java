/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.configurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.security.service.SigningKeyService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedApiKeyAuthenticationConverterTest {

    private EmbeddedApiKeyAuthenticationConverter converter;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        SigningKeyService signingKeyService = mock(SigningKeyService.class);

        converter = new EmbeddedApiKeyAuthenticationConverter(signingKeyService);
        request = mock(HttpServletRequest.class);
    }

    @Test
    void testConvertWithNullAuthorizationHeaderReturnsNull() {
        when(request.getHeader("Authorization")).thenReturn(null);

        Authentication result = converter.convert(request);

        assertThat(result).isNull();
    }

    @Test
    void testConvertWithEmptyBearerTokenReturnsNull() {
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        Authentication result = converter.convert(request);

        assertThat(result).isNull();
    }

    @Test
    void testConvertWithBlankBearerTokenReturnsNull() {
        when(request.getHeader("Authorization")).thenReturn("Bearer    ");

        Authentication result = converter.convert(request);

        assertThat(result).isNull();
    }

    @Test
    void testConvertWithNonJwtTokenAndInternalUrlThrowsIllegalArgumentException() {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(request.getRequestURI()).thenReturn("/api/platform/internal/some-endpoint");

        assertThatThrownBy(() -> converter.convert(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("externalUserId parameter is required");
    }
}
