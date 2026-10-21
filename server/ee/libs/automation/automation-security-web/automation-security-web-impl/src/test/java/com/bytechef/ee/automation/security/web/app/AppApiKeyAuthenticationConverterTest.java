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
import java.util.List;
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
    void testConvertCarriesTheUserTokenHeader() {
        MockHttpServletRequest request = appRequest("POST", "/api/app/v1/tools");

        request.addHeader("X-ByteChef-User-Token", "user-token");

        AppAuthenticationToken token = (AppAuthenticationToken) converter.convert(request);

        assertThat(token).isNotNull();
        assertThat(token.getUserToken()).isEqualTo("user-token");
        assertThat(token.isUserOptional()).isFalse();
        assertThat(token.toString()).doesNotContain("user-token");
    }

    @Test
    void testConvertWithoutUserTokenHeaderHasNoUserToken() {
        AppAuthenticationToken token = (AppAuthenticationToken) converter.convert(appRequest("GET", "/api/app/v1/me"));

        assertThat(token).isNotNull();
        assertThat(token.getUserToken()).isNull();
        assertThat(token.isUserOptional()).isFalse();
    }

    @Test
    void testOnlyGetContextAndTheDiscoveryListsAreUserOptional() {
        assertThat(isUserOptional(appRequest("GET", "/api/app/v1/context"))).isTrue();
        assertThat(isUserOptional(appRequest("GET", "/api/app/v2/context"))).isTrue();
        assertThat(isUserOptional(appRequest("POST", "/api/app/v1/context"))).isFalse();
        assertThat(isUserOptional(appRequest("GET", "/api/app/v1/context/extra"))).isFalse();
        assertThat(isUserOptional(appRequest("GET", "/api/app/v1/api/context"))).isFalse();

        for (String discoveryPath : List.of("connections", "actions", "tools", "workflows", "api-collections")) {
            assertThat(isUserOptional(appRequest("GET", "/api/app/v1/" + discoveryPath))).isTrue();
            assertThat(isUserOptional(appRequest("POST", "/api/app/v1/" + discoveryPath))).isFalse();
            assertThat(isUserOptional(appRequest("GET", "/api/app/v1/api/" + discoveryPath))).isFalse();
        }

        assertThat(isUserOptional(appRequest("GET", "/api/app/v1/me"))).isFalse();
        assertThat(isUserOptional(appRequest("GET", "/api/app/v1/workflow-runs/1"))).isFalse();
        assertThat(isUserOptional(appRequest("POST", "/api/app/v1/connections/crm/proxy"))).isFalse();
        assertThat(isUserOptional(appRequest("POST", "/api/app/v1/connections/crm/actions/getRecord"))).isFalse();
        assertThat(isUserOptional(appRequest("GET", "/api/app/v1/connections/crm"))).isFalse();
        assertThat(isUserOptional(appRequest("POST", "/api/app/v1/workflows/sync"))).isFalse();
    }

    @Test
    void testGetContextWithAQueryStringIsUserOptional() {
        MockHttpServletRequest request = appRequest("GET", "/api/app/v1/context");

        request.setQueryString("x=1");

        assertThat(isUserOptional(request)).isTrue();

        MockHttpServletRequest extraPathRequest = appRequest("GET", "/api/app/v1/context/extra");

        extraPathRequest.setQueryString("x=1");

        assertThat(isUserOptional(extraPathRequest)).isFalse();
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

    private boolean isUserOptional(MockHttpServletRequest request) {
        AppAuthenticationToken token = (AppAuthenticationToken) converter.convert(request);

        assertThat(token).isNotNull();

        return token.isUserOptional();
    }

    private static MockHttpServletRequest appRequest(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);

        request.setServletPath(path);
        request.addHeader("Authorization", "Bearer " + TenantKey.of("tenant9"));

        return request;
    }

    private void assertBadCredentials(String key) {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.addHeader("Authorization", "Bearer " + key);

        assertThatThrownBy(() -> converter.convert(request)).isInstanceOf(BadCredentialsException.class);
    }
}
