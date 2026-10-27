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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
class UrlValidatorUtilsTest {

    @Test
    void testPublicLiteralIpPasses() {
        assertThat(UrlValidatorUtils.isValid("https://1.1.1.1/path", Set.of())).isTrue();
    }

    @Test
    void testLoopbackBlocked() {
        assertThat(UrlValidatorUtils.isValid("http://127.0.0.1", Set.of())).isFalse();
        assertThat(UrlValidatorUtils.isValid("http://[::1]", Set.of())).isFalse();
    }

    @Test
    void testPrivateRangesBlocked() {
        assertThat(UrlValidatorUtils.isValid("http://10.0.0.1", Set.of())).isFalse();
        assertThat(UrlValidatorUtils.isValid("http://192.168.1.1", Set.of())).isFalse();
    }

    @Test
    void testLinkLocalAndCgnatBlocked() {
        assertThat(UrlValidatorUtils.isValid("http://169.254.169.254", Set.of())).isFalse();
        assertThat(UrlValidatorUtils.isValid("http://100.64.0.1", Set.of())).isFalse();
    }

    @Test
    void testNonHttpSchemeBlocked() {
        assertThat(UrlValidatorUtils.isValid("ftp://1.1.1.1", Set.of())).isFalse();
    }

    @Test
    void testMissingSchemeOrHostBlocked() {
        assertThat(UrlValidatorUtils.isValid("1.1.1.1", Set.of())).isFalse();
        assertThat(UrlValidatorUtils.isValid("http://", Set.of())).isFalse();
    }

    @Test
    void testAllowlistedPrivateHostPasses() {
        assertThat(UrlValidatorUtils.isValid("http://10.0.0.1", Set.of("10.0.0.1"))).isTrue();
    }

    @Test
    void testValidateThrowsWithMessage() {
        assertThatThrownBy(() -> UrlValidatorUtils.validate("http://127.0.0.1", Set.of()))
            .isInstanceOf(UrlValidationException.class);
    }

    @Test
    void testResolvesToPrivateAddress() {
        assertThat(UrlValidatorUtils.resolvesToPrivateAddress("10.0.0.1")).isTrue();
        assertThat(UrlValidatorUtils.resolvesToPrivateAddress("127.0.0.1")).isTrue();
        assertThat(UrlValidatorUtils.resolvesToPrivateAddress("1.1.1.1")).isFalse();
        assertThat(UrlValidatorUtils.resolvesToPrivateAddress("nonexistent.invalid")).isFalse();
    }

    @Test
    void testResolveValidatedAddressesReturnsTheResolvedPublicAddresses() throws UnknownHostException {
        InetAddress publicAddress = InetAddress.getByAddress("docs.example.com", new byte[] {
            93, (byte) 184, (byte) 216, 34
        });

        List<InetAddress> addresses = UrlValidatorUtils.resolveValidatedAddresses(
            "https://docs.example.com/api", Set.of(), host -> new InetAddress[] {
                publicAddress
            });

        assertThat(addresses).containsExactly(publicAddress);
    }

    @Test
    void testResolveValidatedAddressesRejectsWhenAnyResolvedAddressIsPrivate() throws UnknownHostException {
        InetAddress publicAddress = InetAddress.getByAddress("docs.example.com", new byte[] {
            93, (byte) 184, (byte) 216, 34
        });
        InetAddress privateAddress = InetAddress.getByAddress("docs.example.com", new byte[] {
            10, 0, 0, 1
        });

        assertThatThrownBy(() -> UrlValidatorUtils.resolveValidatedAddresses(
            "https://docs.example.com/api", Set.of(), host -> new InetAddress[] {
                publicAddress, privateAddress
            }))
                .isInstanceOf(UrlValidationException.class)
                .hasMessageContaining("private or loopback");
    }

    @Test
    void testResolveValidatedAddressesResolvesTheHostExactlyOnce() throws UnknownHostException {
        InetAddress publicAddress = InetAddress.getByAddress("docs.example.com", new byte[] {
            93, (byte) 184, (byte) 216, 34
        });
        AtomicInteger lookupCount = new AtomicInteger();

        UrlValidatorUtils.resolveValidatedAddresses("https://docs.example.com/api", Set.of(), host -> {
            lookupCount.incrementAndGet();

            return new InetAddress[] {
                publicAddress
            };
        });

        assertThat(lookupCount.get()).isEqualTo(1);
    }

    @Test
    void testResolveValidatedAddressesReturnsPrivateAddressesOfAllowlistedHost() throws UnknownHostException {
        InetAddress privateAddress = InetAddress.getByAddress("intranet.local", new byte[] {
            10, 0, 0, 1
        });

        List<InetAddress> addresses = UrlValidatorUtils.resolveValidatedAddresses(
            "http://intranet.local/api", Set.of("intranet.local"), host -> new InetAddress[] {
                privateAddress
            });

        assertThat(addresses).containsExactly(privateAddress);
    }

    @Test
    void testResolveValidatedAddressesRejectsUnresolvableHost() {
        assertThatThrownBy(() -> UrlValidatorUtils.resolveValidatedAddresses(
            "https://docs.example.com/api", Set.of(), host -> {
                throw new UnknownHostException(host);
            }))
                .isInstanceOf(UrlValidationException.class)
                .hasMessageContaining("Cannot resolve URL host");
    }

    @Test
    void testResolveValidatedAddressesRejectsNonHttpScheme() {
        assertThatThrownBy(() -> UrlValidatorUtils.resolveValidatedAddresses("file:///etc/passwd", Set.of()))
            .isInstanceOf(UrlValidationException.class)
            .hasMessageContaining("Only HTTP and HTTPS are permitted");
    }
}
