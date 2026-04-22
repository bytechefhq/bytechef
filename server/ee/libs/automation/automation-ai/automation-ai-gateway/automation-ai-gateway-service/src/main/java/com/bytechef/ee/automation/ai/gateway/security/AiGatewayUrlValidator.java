/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.security;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

/**
 * Validates that a URL points to a public, non-internal host.
 *
 * <p>
 * Guards against SSRF by rejecting loopback, private, link-local, multicast, any-local, CGNAT (100.64.0.0/10) and IPv6
 * unique local (fc00::/7) addresses. Every A/AAAA record is checked so a multi-record hostname with a mixed
 * public/private result cannot slip a private address through.
 *
 * <p>
 * <b>DNS rebinding (accepted residual risk):</b> because the JDK {@link java.net.http.HttpClient} does not expose a DNS
 * resolver hook, the connect-time resolution happens after this validator returns. An attacker-controlled authoritative
 * DNS can therefore return a public IP during validation and a private IP microseconds later at socket connect. Callers
 * that need hardened guarantees SHOULD either: (a) re-validate per attempt (webhook delivery already does this on every
 * retry), or (b) migrate the call site to an HTTP client that supports pinning the resolved IP to the socket (OkHttp's
 * custom {@code Dns} is the intended long-term path, tracked for a follow-up).
 *
 * <p>
 * An earlier revision rewrote the host to the resolved IP literal, which defeated rebinding but broke HTTPS
 * certificate-hostname verification and SNI for legitimate subscribers. That approach was reverted in favor of
 * per-attempt validation.
 *
 * @version ee
 */
public final class AiGatewayUrlValidator {

    private static final Set<String> ALLOWED_URL_SCHEMES = Set.of("http", "https");

    private AiGatewayUrlValidator() {
    }

    public static void validateExternalUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be null or blank");
        }

        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();

            if (scheme == null || !ALLOWED_URL_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException(
                    "URL scheme '" + scheme + "' is not allowed. Only HTTP and HTTPS are permitted.");
            }

            String host = uri.getHost();

            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("URL must contain a valid host");
            }

            InetAddress[] addresses = InetAddress.getAllByName(host);

            if (addresses.length == 0) {
                throw new IllegalArgumentException("Cannot resolve URL host: " + url);
            }

            for (InetAddress address : addresses) {
                rejectPrivateAddress(host, address);
            }
        } catch (IllegalArgumentException illegalArgumentException) {
            throw illegalArgumentException;
        } catch (UnknownHostException unknownHostException) {
            throw new IllegalArgumentException("Cannot resolve URL host: " + url, unknownHostException);
        }
    }

    private static void rejectPrivateAddress(String host, InetAddress address) {
        if (address.isLinkLocalAddress()) {
            throw new IllegalArgumentException(
                "URL host '" + host + "' resolves to a link-local address, which is not allowed");
        }

        if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isAnyLocalAddress()
            || address.isMulticastAddress()) {

            throw new IllegalArgumentException(
                "URL host '" + host + "' resolves to a private or loopback address, which is not allowed");
        }

        if (address instanceof Inet4Address) {
            byte[] bytes = address.getAddress();
            int first = bytes[0] & 0xFF;
            int second = bytes[1] & 0xFF;

            if (first == 100 && second >= 64 && second <= 127) {
                throw new IllegalArgumentException(
                    "URL host '" + host + "' resolves to a CGNAT address, which is not allowed");
            }
        } else if (address instanceof Inet6Address) {
            byte[] bytes = address.getAddress();

            if ((bytes[0] & 0xFE) == 0xFC) {
                throw new IllegalArgumentException(
                    "URL host '" + host + "' resolves to an IPv6 unique local address, which is not allowed");
            }
        }
    }
}
