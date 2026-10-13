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

import java.util.Set;
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
    void testZeroNetworkBlocked() {
        assertThat(UrlValidatorUtils.isValid("http://0.0.0.0", Set.of())).isFalse();
        assertThat(UrlValidatorUtils.isValid("http://0.1.2.3", Set.of())).isFalse();
        assertThat(UrlValidatorUtils.resolvesToPrivateAddress("0.1.2.3")).isTrue();
    }

    @Test
    void testNat64WithPrivateIpv4Blocked() {
        assertThat(UrlValidatorUtils.isValid("http://[64:ff9b::a9fe:a9fe]", Set.of())).isFalse();
        assertThat(UrlValidatorUtils.isValid("http://[64:ff9b::a00:1]", Set.of())).isFalse();
        assertThat(UrlValidatorUtils.isValid("http://[64:ff9b::7f00:1]", Set.of())).isFalse();
        assertThat(UrlValidatorUtils.isValid("http://[64:ff9b::c0a8:101]", Set.of())).isFalse();
        assertThat(UrlValidatorUtils.isValid("http://[64:ff9b::6440:1]", Set.of())).isFalse();
        assertThat(UrlValidatorUtils.isValid("http://[64:ff9b::]", Set.of())).isFalse();
        assertThat(UrlValidatorUtils.resolvesToPrivateAddress("64:ff9b::a9fe:a9fe")).isTrue();
    }

    @Test
    void test6to4WithPrivateIpv4Blocked() {
        assertThat(UrlValidatorUtils.isValid("http://[2002:a9fe:a9fe::]", Set.of())).isFalse();
        assertThat(UrlValidatorUtils.isValid("http://[2002:a00:1::1]", Set.of())).isFalse();
        assertThat(UrlValidatorUtils.isValid("http://[2002:7f00:1::]", Set.of())).isFalse();
        assertThat(UrlValidatorUtils.isValid("http://[2002:c0a8:101::]", Set.of())).isFalse();
        assertThat(UrlValidatorUtils.isValid("http://[2002:6440:1::]", Set.of())).isFalse();
        assertThat(UrlValidatorUtils.isValid("http://[2002::]", Set.of())).isFalse();
        assertThat(UrlValidatorUtils.resolvesToPrivateAddress("2002:a9fe:a9fe::")).isTrue();
    }

    @Test
    void test6to4WithPublicIpv4Allowed() {
        assertThat(UrlValidatorUtils.isValid("https://[2002:808:808::1]", Set.of())).isTrue();
        assertThat(UrlValidatorUtils.resolvesToPrivateAddress("2002:808:808::1")).isFalse();
    }

    @Test
    void testIpv4CompatibleWithPrivateIpv4Blocked() {
        assertThat(UrlValidatorUtils.isValid("http://[::a9fe:a9fe]", Set.of())).isFalse();
        assertThat(UrlValidatorUtils.isValid("http://[::a00:1]", Set.of())).isFalse();
        assertThat(UrlValidatorUtils.isValid("http://[::7f00:1]", Set.of())).isFalse();
        assertThat(UrlValidatorUtils.isValid("http://[::6440:1]", Set.of())).isFalse();
        assertThat(UrlValidatorUtils.resolvesToPrivateAddress("::a9fe:a9fe")).isTrue();
    }

    @Test
    void testIpv4MappedWithPrivateIpv4Blocked() {
        assertThat(UrlValidatorUtils.isValid("http://[::ffff:a9fe:a9fe]", Set.of())).isFalse();
        assertThat(UrlValidatorUtils.isValid("http://[::ffff:10.0.0.1]", Set.of())).isFalse();
        assertThat(UrlValidatorUtils.isValid("http://[::ffff:127.0.0.1]", Set.of())).isFalse();
        assertThat(UrlValidatorUtils.isValid("http://[::ffff:100.64.0.1]", Set.of())).isFalse();
        assertThat(UrlValidatorUtils.isValid("https://[::ffff:8.8.8.8]", Set.of())).isTrue();
    }

    @Test
    void testNat64WithPublicIpv4Allowed() {
        assertThat(UrlValidatorUtils.isValid("https://[64:ff9b::101:101]", Set.of())).isTrue();
        assertThat(UrlValidatorUtils.isValid("https://[64:ff9b::808:808]/path", Set.of())).isTrue();
        assertThat(UrlValidatorUtils.resolvesToPrivateAddress("64:ff9b::808:808")).isFalse();
    }

    @Test
    void testIpv6UniqueLocalAndLinkLocalBlocked() {
        assertThat(UrlValidatorUtils.isValid("http://[fd00::1]", Set.of())).isFalse();
        assertThat(UrlValidatorUtils.isValid("http://[fe80::1]", Set.of())).isFalse();
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
}
