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
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import org.jspecify.annotations.Nullable;

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

            return first == 0 || (first == 100 && second >= 64 && second <= 127);
        }

        if (address instanceof Inet6Address) {
            byte[] bytes = address.getAddress();

            InetAddress embeddedIpv4Address = toEmbeddedIpv4Address(bytes);

            if (embeddedIpv4Address != null) {
                return isPrivateAddress(embeddedIpv4Address);
            }

            return (bytes[0] & 0xFE) == 0xFC;
        }

        return false;
    }

    private static boolean isNat64Address(byte[] bytes) {
        if ((bytes[0] & 0xFF) != 0x00 || (bytes[1] & 0xFF) != 0x64 || (bytes[2] & 0xFF) != 0xFF ||
            (bytes[3] & 0xFF) != 0x9B) {

            return false;
        }

        return isZero(bytes, 4, 12);
    }

    private static boolean is6to4Address(byte[] bytes) {
        return (bytes[0] & 0xFF) == 0x20 && (bytes[1] & 0xFF) == 0x02;
    }

    private static boolean isIpv4CompatibleAddress(byte[] bytes) {
        return isZero(bytes, 0, 12);
    }

    private static boolean isIpv4MappedAddress(byte[] bytes) {
        return isZero(bytes, 0, 10) && (bytes[10] & 0xFF) == 0xFF && (bytes[11] & 0xFF) == 0xFF;
    }

    private static boolean isZero(byte[] bytes, int fromIndex, int toIndex) {
        for (int index = fromIndex; index < toIndex; index++) {
            if (bytes[index] != 0) {
                return false;
            }
        }

        return true;
    }

    private static @Nullable InetAddress toEmbeddedIpv4Address(byte[] bytes) {
        if (isNat64Address(bytes) || isIpv4CompatibleAddress(bytes) || isIpv4MappedAddress(bytes)) {
            return toInet4Address(Arrays.copyOfRange(bytes, 12, 16));
        }

        if (is6to4Address(bytes)) {
            return toInet4Address(Arrays.copyOfRange(bytes, 2, 6));
        }

        return null;
    }

    private static InetAddress toInet4Address(byte[] ipv4Bytes) {
        try {
            return InetAddress.getByAddress(ipv4Bytes);
        } catch (UnknownHostException unknownHostException) {
            throw new IllegalStateException(unknownHostException);
        }
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

            if (first == 0) {
                throw new UrlValidationException(
                    "URL host '" + host + "' resolves to an address in 0.0.0.0/8, which is not allowed");
            }

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

            InetAddress embeddedIpv4Address = toEmbeddedIpv4Address(bytes);

            if (embeddedIpv4Address != null) {
                rejectPrivateAddress(host, embeddedIpv4Address);
            }
        }
    }
}
