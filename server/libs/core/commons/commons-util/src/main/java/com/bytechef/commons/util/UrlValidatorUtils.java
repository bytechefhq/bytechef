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
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * @author Ivica Cardic
 */
public final class UrlValidatorUtils {

    private static final Set<String> ALLOWED_URL_SCHEMES = Set.of("http", "https");

    private UrlValidatorUtils() {
    }

    public static boolean isValid(String url, Set<String> allowedHosts) {
        try {
            validate(url, allowedHosts);

            return true;
        } catch (UrlValidationException urlValidationException) {
            return false;
        }
    }

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

    public static List<InetAddress> resolveValidatedAddresses(String url, Set<String> allowedHosts) {
        return resolveValidatedAddresses(url, allowedHosts, InetAddress::getAllByName);
    }

    public static List<InetAddress> resolveValidatedAddresses(
        String url, Set<String> allowedHosts, HostResolver hostResolver) {

        URI uri = parseUri(url);

        String host = uri.getHost();

        List<InetAddress> addresses = resolve(url, host, hostResolver);

        if (!isAllowlisted(host, allowedHosts)) {
            for (InetAddress address : addresses) {
                rejectPrivateAddress(host, address);
            }
        }

        return addresses;
    }

    public static void validate(String url, Set<String> allowedHosts) {
        URI uri = parseUri(url);

        String host = uri.getHost();

        if (isAllowlisted(host, allowedHosts)) {
            return;
        }

        for (InetAddress address : resolve(url, host, InetAddress::getAllByName)) {
            rejectPrivateAddress(host, address);
        }
    }

    private static URI parseUri(String url) {
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

        return uri;
    }

    private static List<InetAddress> resolve(String url, String host, HostResolver hostResolver) {
        InetAddress[] addresses;

        try {
            addresses = hostResolver.resolve(host);
        } catch (UnknownHostException unknownHostException) {
            throw new UrlValidationException("Cannot resolve URL host: " + url, unknownHostException);
        }

        if (addresses == null || addresses.length == 0) {
            throw new UrlValidationException("Cannot resolve URL host: " + url);
        }

        return List.of(addresses);
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

    /**
     * Resolves a host name to its addresses.
     */
    @FunctionalInterface
    public interface HostResolver {

        InetAddress[] resolve(String host) throws UnknownHostException;
    }
}
