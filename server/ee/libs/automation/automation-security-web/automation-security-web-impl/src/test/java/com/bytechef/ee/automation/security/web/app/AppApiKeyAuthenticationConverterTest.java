/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.security.web.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.tenant.domain.TenantKey;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AppApiKeyAuthenticationConverterTest {

    private final AppApiKeySecurityConfigurer.AppApiKeyAuthenticationConverter converter =
        new AppApiKeySecurityConfigurer.AppApiKeyAuthenticationConverter(mock(EnvironmentService.class));

    @Test
    void testConvertReadsTenantFromKeyAndIgnoresEnvironmentHeader() {
        String key = String.valueOf(TenantKey.of("tenant9"));
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.addHeader("Authorization", "Bearer " + key);
        request.addHeader("X-ENVIRONMENT", "DEVELOPMENT");

        AppAuthenticationToken token = (AppAuthenticationToken) converter.convert(request);

        assertThat(token).isNotNull();
        assertThat(token.getTenantId()).isEqualTo("tenant9");
        assertThat(token.getSecretKey()).isEqualTo(key);
        assertThat(token.isAuthenticated()).isFalse();
    }

    @Test
    void testConvertIgnoresMalformedEnvironmentHeader() {
        String key = String.valueOf(TenantKey.of("tenant9"));
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.addHeader("Authorization", "Bearer " + key);
        request.addHeader("X-ENVIRONMENT", "NOT_AN_ENVIRONMENT");

        assertThat(converter.convert(request)).isNotNull();
    }

    @Test
    void testConvertMalformedKeyIsBadCredentials() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.addHeader("Authorization", "Bearer not-base64!!");

        assertThatThrownBy(() -> converter.convert(request)).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testConvertMissingHeaderIsBadCredentials() {
        assertThatThrownBy(() -> converter.convert(new MockHttpServletRequest()))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testConvertBase64WithPathTraversalTenantIsBadCredentials() {
        assertBadCredentials(EncodingUtils.base64EncodeToString("../../etc:x"));
    }

    @Test
    void testConvertBase64WithSpacesInTenantIsBadCredentials() {
        assertBadCredentials(EncodingUtils.base64EncodeToString("ten ant:x"));
    }

    @Test
    void testConvertBase64WithEmptyTenantIsBadCredentials() {
        assertBadCredentials(EncodingUtils.base64EncodeToString(":x"));
    }

    @Test
    void testConvertBase64OfOnlyDelimiterIsBadCredentials() {
        assertBadCredentials(EncodingUtils.base64EncodeToString(":"));
    }

    @Test
    void testConvertBase64OfEmptyStringIsBadCredentials() {
        assertBadCredentials("");
    }

    @Test
    void testConvertBase64WithInvalidUtf8TenantIsBadCredentials() {
        assertBadCredentials(EncodingUtils.base64EncodeToString(new byte[] {
            (byte) 0xC3, (byte) 0x28, ':', 'x'
        }));
    }

    private void assertBadCredentials(String key) {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.addHeader("Authorization", "Bearer " + key);

        assertThatThrownBy(() -> converter.convert(request)).isInstanceOf(BadCredentialsException.class);
    }
}
