/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.commons.util;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

/**
 * Validates that a URL points to a public, non-internal host, to guard against Server-Side Request Forgery (SSRF).
 *
 * <p>
 * Rejects non-http(s) schemes and any host that resolves to a loopback, private (RFC 1918), link-local, any-local,
 * multicast, CGNAT (100.64.0.0/10) or IPv6 unique-local (fc00::/7) address. Every A/AAAA record is checked so a
 * multi-record hostname with a mixed public/private result cannot slip a private address through. A host whose literal
 * value is in {@code allowedHosts} (case-insensitive) bypasses the check, providing an escape-hatch for legitimate
 * internal targets.
 *
 * <p>
 * <b>DNS rebinding (accepted residual risk):</b> the JDK {@link java.net.http.HttpClient} does not expose a DNS
 * resolver hook, so connect-time resolution happens after this validator returns. Callers that need stronger guarantees
 * should re-validate per delivery attempt.
 *
 * @author Ivica Cardic
 */
public final class UrlValidator {

    private static final Set<String> ALLOWED_URL_SCHEMES = Set.of("http", "https");

    private UrlValidator() {
    }

    public static boolean isValid(String url, Set<String> allowedHosts) {
        try {
            validate(url, allowedHosts);

            return true;
        } catch (UrlValidationException urlValidationException) {
            return false;
        }
    }

    /**
     * Returns {@code true} only when {@code host} resolves and at least one of its addresses is private/loopback/
     * link-local/any-local/multicast/CGNAT/IPv6-ULA. A host that cannot be resolved, or that resolves to all-public
     * addresses, returns {@code false}. Intended for callers (e.g. redirect validation) that must block internal
     * targets without rejecting merely-unresolvable ones.
     */
    public static boolean resolvesToPrivateAddress(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);

            for (InetAddress address : addresses) {
                if (isPrivateAddress(address)) {
                    return true;
                }
            }

            return false;
        } catch (UnknownHostException unknownHostException) {
            return false;
        }
    }

    public static void validate(String url, Set<String> allowedHosts) {
        if (url == null || url.isBlank()) {
            throw new UrlValidationException("URL must not be null or blank");
        }

        URI uri;

        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new UrlValidationException("Malformed URL: " + url, illegalArgumentException);
        }

        String scheme = uri.getScheme();

        if (scheme == null || !ALLOWED_URL_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
            throw new UrlValidationException(
                "URL scheme '" + scheme + "' is not allowed. Only HTTP and HTTPS are permitted.");
        }

        String host = uri.getHost();

        if (host == null || host.isBlank()) {
            throw new UrlValidationException("URL must contain a valid host");
        }

        if (isAllowlisted(host, allowedHosts)) {
            return;
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);

            if (addresses.length == 0) {
                throw new UrlValidationException("Cannot resolve URL host: " + url);
            }

            for (InetAddress address : addresses) {
                rejectPrivateAddress(host, address);
            }
        } catch (UnknownHostException unknownHostException) {
            throw new UrlValidationException("Cannot resolve URL host: " + url, unknownHostException);
        }
    }

    private static boolean isAllowlisted(String host, Set<String> allowedHosts) {
        for (String allowedHost : allowedHosts) {
            if (allowedHost.equalsIgnoreCase(host)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isPrivateAddress(InetAddress address) {
        if (address.isLinkLocalAddress() || address.isLoopbackAddress() || address.isSiteLocalAddress()
            || address.isAnyLocalAddress() || address.isMulticastAddress()) {

            return true;
        }

        if (address instanceof Inet4Address) {
            byte[] bytes = address.getAddress();
            int first = bytes[0] & 0xFF;
            int second = bytes[1] & 0xFF;

            return first == 100 && second >= 64 && second <= 127;
        }

        if (address instanceof Inet6Address) {
            byte[] bytes = address.getAddress();

            return (bytes[0] & 0xFE) == 0xFC;
        }

        return false;
    }

    private static void rejectPrivateAddress(String host, InetAddress address) {
        if (address.isLinkLocalAddress()) {
            throw new UrlValidationException(
                "URL host '" + host + "' resolves to a link-local address, which is not allowed");
        }

        if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isAnyLocalAddress()
            || address.isMulticastAddress()) {

            throw new UrlValidationException(
                "URL host '" + host + "' resolves to a private or loopback address, which is not allowed");
        }

        if (address instanceof Inet4Address) {
            byte[] bytes = address.getAddress();
            int first = bytes[0] & 0xFF;
            int second = bytes[1] & 0xFF;

            if (first == 100 && second >= 64 && second <= 127) {
                throw new UrlValidationException(
                    "URL host '" + host + "' resolves to a CGNAT address, which is not allowed");
            }
        } else if (address instanceof Inet6Address) {
            byte[] bytes = address.getAddress();

            if ((bytes[0] & 0xFE) == 0xFC) {
                throw new UrlValidationException(
                    "URL host '" + host + "' resolves to an IPv6 unique local address, which is not allowed");
            }
        }
    }
}
