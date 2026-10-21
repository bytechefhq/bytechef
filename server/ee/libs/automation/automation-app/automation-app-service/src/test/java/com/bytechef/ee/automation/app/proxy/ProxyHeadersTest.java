/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProxyHeadersTest {

    @Test
    void stripsForbiddenRequestHeadersCaseInsensitively() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("authorization", List.of("Bearer evil"));
        headers.put("PROXY-Authorization", List.of("x"));
        headers.put("Cookie", List.of("a=b"));
        headers.put("host", List.of("evil"));
        headers.put("Connection", List.of("keep-alive"));
        headers.put("Transfer-Encoding", List.of("chunked"));
        headers.put("Accept", List.of("application/json"));
        headers.put("X-Custom", List.of("ok"));

        assertThat(ProxyHeaders.sanitizeRequest(headers)).containsExactlyInAnyOrderEntriesOf(
            Map.of("Accept", List.of("application/json"), "X-Custom", List.of("ok")));
    }

    @Test
    void dropsSetCookieFromResponse() {
        assertThat(ProxyHeaders.sanitizeResponse(Map.of("set-cookie", List.of("s=1"), "Content-Type", List.of("a"))))
            .containsOnlyKeys("Content-Type");
    }

    @Test
    void dropsCrlfInRequestHeaderName() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("X-Custom\r\nInjection", List.of("value"));
        headers.put("X-Good", List.of("ok"));

        assertThat(ProxyHeaders.sanitizeRequest(headers)).containsOnlyKeys("X-Good");
    }

    @Test
    void dropsCrlfInRequestHeaderValue() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("X-Header", List.of("value\ninjection"));
        headers.put("X-Good", List.of("ok"));

        assertThat(ProxyHeaders.sanitizeRequest(headers)).containsOnlyKeys("X-Good");
    }

    @Test
    void dropsRequestHeaderWhenOneValueContainsCrLf() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("X-Header", Arrays.asList("value1", "value2\rinjection", "value3"));
        headers.put("X-Good", List.of("ok"));

        assertThat(ProxyHeaders.sanitizeRequest(headers)).containsOnlyKeys("X-Good");
    }

    @Test
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    void dropsRequestProxyMetadataHeadersWithVariedCasing() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("X-Forwarded-For", List.of("1.2.3.4"));
        headers.put("x-forwarded-host", List.of("evil"));
        headers.put("X-FORWARDED-PROTO", List.of("https"));
        headers.put("x-real-ip", List.of("1.1.1.1"));
        headers.put("Forwarded", List.of("for=1.2.3.4"));
        headers.put("Accept", List.of("application/json"));

        assertThat(ProxyHeaders.sanitizeRequest(headers)).containsOnlyKeys("Accept");
    }

    @Test
    void dropsRequestXForwardedPrefixHeaders() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("X-Forwarded-Port", List.of("8080"));
        headers.put("x-forwarded-prefix", List.of("/app"));
        headers.put("X-Forwarded-Server", List.of("server"));
        headers.put("Accept", List.of("application/json"));

        assertThat(ProxyHeaders.sanitizeRequest(headers)).containsOnlyKeys("Accept");
    }

    @Test
    void dropsRequestOriginalUrlHeader() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("X-Original-URL", List.of("http://evil.com"));
        headers.put("Accept", List.of("application/json"));

        assertThat(ProxyHeaders.sanitizeRequest(headers)).containsOnlyKeys("Accept");
    }

    @Test
    void dropsRequestAuthorizationHeaderWithVariedCasing() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Authorization", List.of("Bearer token"));
        headers.put("aUtHoRiZaTiOn", List.of("Bearer evil"));
        headers.put("AUTHORIZATION", List.of("Bearer evil2"));
        headers.put("Accept", List.of("application/json"));

        assertThat(ProxyHeaders.sanitizeRequest(headers)).containsOnlyKeys("Accept");
    }

    @Test
    void nullRequestMapReturnsEmptyMap() {
        assertThat(ProxyHeaders.sanitizeRequest(null)).isEmpty();
    }

    @Test
    void nullRequestHeaderNameIsDropped() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put(null, List.of("value"));
        headers.put("Accept", List.of("ok"));

        assertThat(ProxyHeaders.sanitizeRequest(headers)).containsOnlyKeys("Accept");
    }

    @Test
    void nullValueListDropsRequestHeader() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("X-Custom", null);
        headers.put("Accept", List.of("application/json"));

        assertThat(ProxyHeaders.sanitizeRequest(headers)).containsOnlyKeys("Accept");
    }

    @Test
    void dropsRequestRemainingForbiddenHeaders() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Content-Length", List.of("100"));
        headers.put("Keep-Alive", List.of("timeout=5"));
        headers.put("Proxy-Authenticate", List.of("Basic"));
        headers.put("TE", List.of("trailers"));
        headers.put("Trailer", List.of("Server-Timing"));
        headers.put("Upgrade", List.of("websocket"));
        headers.put("Accept", List.of("application/json"));

        assertThat(ProxyHeaders.sanitizeRequest(headers)).containsOnlyKeys("Accept");
    }

    @Test
    void dropsRequestExpectHeaderWithVariedCasing() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Expect", List.of("100-continue"));
        headers.put("EXPECT", List.of("100-continue"));
        headers.put("Accept", List.of("application/json"));

        assertThat(ProxyHeaders.sanitizeRequest(headers)).containsOnlyKeys("Accept");
    }

    @Test
    void dropsResponseSetCookieVariations() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Set-Cookie", List.of("a=b"));
        headers.put("set-cookie2", List.of("c=d"));
        headers.put("Content-Type", List.of("application/json"));

        assertThat(ProxyHeaders.sanitizeResponse(headers)).containsOnlyKeys("Content-Type");
    }

    @Test
    void dropsResponseHopByHopHeaders() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Connection", List.of("close"));
        headers.put("Keep-Alive", List.of("timeout=5"));
        headers.put("Transfer-Encoding", List.of("chunked"));
        headers.put("Trailer", List.of("Server-Timing"));
        headers.put("Upgrade", List.of("websocket"));
        headers.put("Content-Type", List.of("application/json"));

        assertThat(ProxyHeaders.sanitizeResponse(headers)).containsOnlyKeys("Content-Type");
    }

    @Test
    void dropsCrlfInResponseHeaderName() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("X-Custom\r\nInjection", List.of("value"));
        headers.put("Content-Type", List.of("text/plain"));

        assertThat(ProxyHeaders.sanitizeResponse(headers)).containsOnlyKeys("Content-Type");
    }

    @Test
    void dropsCrlfInResponseHeaderValue() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("X-Header", List.of("value\ninjection"));
        headers.put("Content-Type", List.of("text/plain"));

        assertThat(ProxyHeaders.sanitizeResponse(headers)).containsOnlyKeys("Content-Type");
    }

    @Test
    void nullResponseMapReturnsEmptyMap() {
        assertThat(ProxyHeaders.sanitizeResponse(null)).isEmpty();
    }

    @Test
    void nullResponseHeaderNameIsDropped() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put(null, List.of("value"));
        headers.put("Content-Type", List.of("text/plain"));

        assertThat(ProxyHeaders.sanitizeResponse(headers)).containsOnlyKeys("Content-Type");
    }

    @Test
    void nullValueListDropsResponseHeader() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("X-Custom", null);
        headers.put("Content-Type", List.of("text/plain"));

        assertThat(ProxyHeaders.sanitizeResponse(headers)).containsOnlyKeys("Content-Type");
    }

    @Test
    void preservesOrderAndOriginalCasingInRequest() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Z-Last", List.of("z"));
        headers.put("Accept", List.of("a"));
        headers.put("X-Custom", List.of("x"));
        headers.put("Content-Type", List.of("c"));

        assertThat(ProxyHeaders.sanitizeRequest(headers)).containsExactly(
            Map.entry("Z-Last", List.of("z")),
            Map.entry("Accept", List.of("a")),
            Map.entry("X-Custom", List.of("x")),
            Map.entry("Content-Type", List.of("c")));
    }

    @Test
    void preservesOrderAndOriginalCasingInResponse() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Z-Last", List.of("z"));
        headers.put("Content-Type", List.of("c"));
        headers.put("X-Custom", List.of("x"));
        headers.put("Server", List.of("s"));

        assertThat(ProxyHeaders.sanitizeResponse(headers)).containsExactly(
            Map.entry("Z-Last", List.of("z")),
            Map.entry("Content-Type", List.of("c")),
            Map.entry("X-Custom", List.of("x")),
            Map.entry("Server", List.of("s")));
    }
}
