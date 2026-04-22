/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class AiGatewayUrlValidationTest {

    @Test
    void testValidHttpUrlPasses() {
        assertDoesNotThrow(() -> AiGatewayFacade.validateExternalUrl("http://example.com/path"));
    }

    @Test
    void testValidHttpsUrlPasses() {
        assertDoesNotThrow(() -> AiGatewayFacade.validateExternalUrl("https://example.com/path"));
    }

    @Test
    void testValidatedUrlIsReturnedUnmodified() {
        String originalUrl = "https://example.com/path?q=1#frag";
        String returnedUrl = AiGatewayFacade.validateExternalUrl(originalUrl);

        assertEquals(originalUrl, returnedUrl, "Validated URL must be returned unmodified to preserve TLS SNI");
    }

    @Test
    void testValidatedUrlPreservesPort() {
        String originalUrl = "https://example.com:8443/path";
        String returnedUrl = AiGatewayFacade.validateExternalUrl(originalUrl);

        assertEquals(originalUrl, returnedUrl, "Validated URL must be returned unmodified");
    }

    @Test
    void testFtpUrlIsRejected() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> AiGatewayFacade.validateExternalUrl("ftp://example.com/file"));

        assertTrue(exception.getMessage()
            .contains("not allowed"));
    }

    @Test
    void testLoopbackAddressIsRejected() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> AiGatewayFacade.validateExternalUrl("http://127.0.0.1/path"));

        assertTrue(exception.getMessage()
            .contains("private or loopback"));
    }

    @Test
    void testLocalhostIsRejected() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> AiGatewayFacade.validateExternalUrl("http://localhost/path"));

        assertTrue(exception.getMessage()
            .contains("private or loopback"));
    }

    @Test
    void testLinkLocalAddressIsRejected() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> AiGatewayFacade.validateExternalUrl("http://169.254.1.1/path"));

        assertTrue(exception.getMessage()
            .contains("link-local address"));
    }

    @Test
    void testPrivateAddress10IsRejected() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> AiGatewayFacade.validateExternalUrl("http://10.0.0.1/path"));

        assertTrue(exception.getMessage()
            .contains("private or loopback"));
    }

    @Test
    void testPrivateAddress192168IsRejected() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> AiGatewayFacade.validateExternalUrl("http://192.168.1.1/path"));

        assertTrue(exception.getMessage()
            .contains("private or loopback"));
    }

    @Test
    void testUrlWithNoHostIsRejected() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> AiGatewayFacade.validateExternalUrl("http:///path"));

        assertTrue(exception.getMessage()
            .contains("valid host"));
    }

    @Test
    void testNonExistentDomainThrows() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> AiGatewayFacade.validateExternalUrl(
                "https://this-domain-does-not-exist-xyz123abc.com/path"));

        assertTrue(exception.getMessage()
            .contains("Cannot resolve URL host"));
    }

    @Test
    void testNullSchemeUrlIsRejected() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> AiGatewayFacade.validateExternalUrl("noscheme-host/path"));

        assertTrue(exception.getMessage()
            .contains("not allowed"));
    }

    @Test
    void testNullUrlIsRejected() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> AiGatewayFacade.validateExternalUrl(null));

        assertTrue(
            exception.getMessage()
                .contains("must not be null or blank"),
            "Null URL must throw IllegalArgumentException, not NPE");
    }

    @Test
    void testBlankUrlIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> AiGatewayFacade.validateExternalUrl("   "));
    }

    @Test
    void testCgnatAddressIsRejected() {
        // 100.64.0.0/10 is reserved for carrier-grade NAT — not a private address per InetAddress.isSiteLocalAddress,
        // so the general private-address check misses it. The dedicated guard in AiGatewayUrlValidator catches it.
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> AiGatewayFacade.validateExternalUrl("http://100.64.0.1/path"));

        assertTrue(
            exception.getMessage()
                .contains("CGNAT"),
            "Expected CGNAT rejection, got: " + exception.getMessage());
    }

    @Test
    void testCgnatBoundaryAddressesAreRejected() {
        // 100.64.0.0 – 100.127.255.255 inclusive. Check both ends of the range.
        assertThrows(
            IllegalArgumentException.class,
            () -> AiGatewayFacade.validateExternalUrl("http://100.64.0.0/path"),
            "100.64.0.0 (low end of CGNAT range) should be rejected");
        assertThrows(
            IllegalArgumentException.class,
            () -> AiGatewayFacade.validateExternalUrl("http://100.127.255.255/path"),
            "100.127.255.255 (high end of CGNAT range) should be rejected");
    }

    @Test
    void testNonCgnatAddressInAdjacent100BlockPasses() {
        // 100.63.x.x and 100.128.x.x are OUTSIDE the CGNAT /10; they're normal public addresses. The guard must
        // not over-reject — otherwise we block legitimate providers.
        assertDoesNotThrow(() -> AiGatewayFacade.validateExternalUrl("http://100.63.0.1/path"));
    }

    @Test
    void testIpv6UniqueLocalAddressIsRejected() {
        // fc00::/7 covers fc00 – fdff. These are IPv6 unique local addresses — the IPv6 equivalent of 10.0.0.0/8
        // and not caught by InetAddress.isSiteLocalAddress (which is IPv4-only). Dedicated guard must reject.
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> AiGatewayFacade.validateExternalUrl("http://[fc00::1]/path"));

        assertTrue(
            exception.getMessage()
                .contains("IPv6 unique local"),
            "Expected IPv6 ULA rejection, got: " + exception.getMessage());
    }

    @Test
    void testIpv6UniqueLocalFdBoundaryIsRejected() {
        // fd00::/8 is the most common ULA prefix — must also be rejected.
        assertThrows(
            IllegalArgumentException.class,
            () -> AiGatewayFacade.validateExternalUrl("http://[fd12:3456:789a::1]/path"));
    }

    @Test
    void testIpv6LoopbackIsRejected() {
        // ::1 is the IPv6 loopback. Covered by isLoopbackAddress, but worth a regression guard.
        assertThrows(
            IllegalArgumentException.class,
            () -> AiGatewayFacade.validateExternalUrl("http://[::1]/path"));
    }

    @Test
    void testMixedCaseSchemeIsAccepted() {
        // Locale.ROOT on scheme toLowerCase — guards against a Turkish-locale 'I' bypass.
        assertDoesNotThrow(() -> AiGatewayFacade.validateExternalUrl("HTTPS://example.com/path"));
        assertDoesNotThrow(() -> AiGatewayFacade.validateExternalUrl("HtTp://example.com/path"));
    }
}
