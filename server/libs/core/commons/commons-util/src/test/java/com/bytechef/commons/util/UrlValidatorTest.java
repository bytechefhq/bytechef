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
@SuppressWarnings("PMD.AvoidUsingHardCodedIP") // literal IPs are intentional: deterministic, network-free SSRF tests
class UrlValidatorTest {

    @Test
    void testPublicLiteralIpPasses() {
        // 1.1.1.1 is a public literal IP; getAllByName parses it without DNS.
        assertThat(UrlValidator.isValid("https://1.1.1.1/path", Set.of())).isTrue();
    }

    @Test
    void testLoopbackBlocked() {
        assertThat(UrlValidator.isValid("http://127.0.0.1", Set.of())).isFalse();
        assertThat(UrlValidator.isValid("http://[::1]", Set.of())).isFalse();
    }

    @Test
    void testPrivateRangesBlocked() {
        assertThat(UrlValidator.isValid("http://10.0.0.1", Set.of())).isFalse();
        assertThat(UrlValidator.isValid("http://192.168.1.1", Set.of())).isFalse();
    }

    @Test
    void testLinkLocalAndCgnatBlocked() {
        assertThat(UrlValidator.isValid("http://169.254.169.254", Set.of())).isFalse();
        assertThat(UrlValidator.isValid("http://100.64.0.1", Set.of())).isFalse();
    }

    @Test
    void testNonHttpSchemeBlocked() {
        assertThat(UrlValidator.isValid("ftp://1.1.1.1", Set.of())).isFalse();
    }

    @Test
    void testMissingSchemeOrHostBlocked() {
        assertThat(UrlValidator.isValid("1.1.1.1", Set.of())).isFalse();
        assertThat(UrlValidator.isValid("http://", Set.of())).isFalse();
    }

    @Test
    void testAllowlistedPrivateHostPasses() {
        assertThat(UrlValidator.isValid("http://10.0.0.1", Set.of("10.0.0.1"))).isTrue();
    }

    @Test
    void testValidateThrowsWithMessage() {
        assertThatThrownBy(() -> UrlValidator.validate("http://127.0.0.1", Set.of()))
            .isInstanceOf(UrlValidationException.class);
    }

    @Test
    void testResolvesToPrivateAddress() {
        assertThat(UrlValidator.resolvesToPrivateAddress("10.0.0.1")).isTrue();
        assertThat(UrlValidator.resolvesToPrivateAddress("127.0.0.1")).isTrue();
        assertThat(UrlValidator.resolvesToPrivateAddress("1.1.1.1")).isFalse();
        // Unresolvable host is treated as not-private (callers must not reject merely-unresolvable targets).
        assertThat(UrlValidator.resolvesToPrivateAddress("nonexistent.invalid")).isFalse();
    }
}
