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

package com.bytechef.component.ai.agent.guardrails.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.ai.agent.guardrails.util.UrlDetector.UrlMatch;
import com.bytechef.component.ai.agent.guardrails.util.UrlDetector.UrlPolicy;
import java.util.List;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
class UrlDetectorTest {

    private static final UrlPolicy DEFAULT_POLICY = new UrlPolicy(
        List.of(), List.of("http", "https"), true, true);

    @Test
    void testDetectBareUrl() {
        List<UrlMatch> matches = UrlDetector.detectViolations(
            "visit https://example.com/page now", DEFAULT_POLICY);

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst()
            .url()).isEqualTo("https://example.com/page");
    }

    @Test
    void testAllowedUrlNotFlagged() {
        UrlPolicy policy = new UrlPolicy(
            List.of("example.com"), List.of("http", "https"), true, true);
        List<UrlMatch> matches = UrlDetector.detectViolations(
            "visit https://example.com/page", policy);

        assertThat(matches).isEmpty();
    }

    @Test
    void testSubdomainAllowedViaFlag() {
        UrlPolicy policy = new UrlPolicy(
            List.of("example.com"), List.of("http", "https"), true, true);
        List<UrlMatch> matches = UrlDetector.detectViolations(
            "visit https://sub.example.com/p", policy);

        assertThat(matches).isEmpty();
    }

    @Test
    void testSubdomainBlockedWhenFlagOff() {
        UrlPolicy policy = new UrlPolicy(
            List.of("example.com"), List.of("http", "https"), true, false);
        List<UrlMatch> matches = UrlDetector.detectViolations(
            "visit https://sub.example.com/p", policy);

        assertThat(matches).hasSize(1);
    }

    @Test
    void testSchemeNotAllowed() {
        UrlPolicy policy = new UrlPolicy(
            List.of("example.com"), List.of("https"), true, true);
        List<UrlMatch> matches = UrlDetector.detectViolations(
            "visit ftp://example.com/p", policy);

        assertThat(matches).hasSize(1);
    }

    @Test
    void testUserinfoBlocked() {
        List<UrlMatch> matches = UrlDetector.detectViolations(
            "login https://user:pass@example.com/", DEFAULT_POLICY);

        assertThat(matches).hasSize(1);
    }

    @Test
    void testMask() {
        String masked = UrlDetector.mask(
            "visit https://evil.com/page then",
            List.of(new UrlMatch("https://evil.com/page", 6, 27, "SCHEME_OR_HOST_NOT_ALLOWED")));

        assertThat(masked).isEqualTo("visit <URL> then");
    }

    @Test
    void testDetectsBareDomainWithoutScheme() {
        UrlPolicy policy = new UrlPolicy(
            List.of("example.com"), List.of("https"), false, false);

        List<UrlMatch> violations = UrlDetector.detectViolations("visit evil.com today", policy);

        assertThat(violations).extracting(UrlMatch::url)
            .contains("evil.com");
    }

    @Test
    void testDetectsIpv4Address() {
        UrlPolicy policy = new UrlPolicy(List.of(), List.of("https"), false, false);

        List<UrlMatch> violations = UrlDetector.detectViolations("ping 10.0.0.1 and 203.0.113.5", policy);

        assertThat(violations).extracting(UrlMatch::url)
            .contains("10.0.0.1", "203.0.113.5");
    }

    @Test
    void testSchemeUrlDoesNotAlsoMatchBareDomainSubstring() {
        UrlPolicy policy = new UrlPolicy(List.of(), List.of("https"), false, false);

        List<UrlMatch> violations = UrlDetector.detectViolations("see https://evil.com/path", policy);

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0)
            .url()).startsWith("https://");
    }

    @Test
    void testCidrAllowlistEntryPermitsAddressInRange() {
        UrlPolicy policy = new UrlPolicy(List.of("10.0.0.0/24"), List.of("https"), false, false);

        List<UrlMatch> violations = UrlDetector.detectViolations("https://10.0.0.5/api", policy);

        assertThat(violations).isEmpty();
    }

    @Test
    void testCidrAllowlistEntryBlocksOutOfRangeAddress() {
        UrlPolicy policy = new UrlPolicy(List.of("10.0.0.0/24"), List.of("https"), false, false);

        List<UrlMatch> violations = UrlDetector.detectViolations("https://10.0.1.5/api", policy);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void testPathPrefixAllowlistMatchesPrefixOnly() {
        UrlPolicy policy = new UrlPolicy(
            List.of("api.example.com/v2/"), List.of("https"), false, false);

        assertThat(UrlDetector.detectViolations(
            "https://api.example.com/v2/users", policy)).isEmpty();
        assertThat(UrlDetector.detectViolations(
            "https://api.example.com/v1/users", policy)).isNotEmpty();
    }

    @Test
    void testPathPrefixAllowlistWithoutTrailingSlashDoesNotLeakToSiblingPaths() {
        // Entry "example.com/admin" must not allowlist "example.com/administrator" — the prefix
        // check must respect segment boundaries even when the allowlist entry omits the trailing slash.
        UrlPolicy policy = new UrlPolicy(
            List.of("example.com/admin"), List.of("https"), false, false);

        assertThat(UrlDetector.detectViolations("https://example.com/admin", policy)).isEmpty();
        assertThat(UrlDetector.detectViolations("https://example.com/admin/users", policy)).isEmpty();
        assertThat(UrlDetector.detectViolations("https://example.com/administrator", policy)).isNotEmpty();
        assertThat(UrlDetector.detectViolations("https://example.com/adminpanel", policy)).isNotEmpty();
    }

    @Test
    void testUserinfoAllowedWhenBlockUserinfoIsFalse() {
        UrlPolicy policy = new UrlPolicy(
            List.of("example.com"), List.of("https"), false, false);

        List<UrlMatch> matches = UrlDetector.detectViolations(
            "login https://user:pass@example.com/", policy);

        assertThat(matches).isEmpty();
    }

    @Test
    void testCidrAllowlistEntryWithPrefixOutOfRangeDoesNotMatch() {
        UrlPolicy policy = new UrlPolicy(List.of("10.0.0.0/99"), List.of("https"), false, false);

        List<UrlMatch> violations = UrlDetector.detectViolations("https://10.0.0.5/api", policy);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void testCidrAllowlistEntryWithMalformedOctetDoesNotMatch() {
        UrlPolicy policy = new UrlPolicy(List.of("10.0/24"), List.of("https"), false, false);

        List<UrlMatch> violations = UrlDetector.detectViolations("https://10.0.0.5/api", policy);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void testIpv6UrlIsFlaggedAsSchemeSpecificUrlViolation() {
        // Pins the current behaviour with IPv6 URLs: the scheme regex matches the whole URL-shape, then the URI
        // parser extracts the bracketed host. The allowlist doesn't carry IPv6 entries so the URL surfaces as a
        // HOST_NOT_ALLOWED or SCHEME_NOT_ALLOWED violation rather than slipping through. Pinned to catch any
        // regression that silently accepts bare IPv6 addresses as if they were allowlisted hosts.
        UrlPolicy policy = new UrlPolicy(
            List.of("example.com"), List.of("http", "https"), true, true);

        List<UrlMatch> violations = UrlDetector.detectViolations(
            "connect to http://[2001:db8::1]:8080/api now", policy);

        assertThat(violations)
            .as("IPv6 scheme-prefixed URL must produce a violation rather than silently slip through")
            .isNotEmpty();
    }

    @Test
    void testPunycodeIdnHostIsFlaggedWhenNotInAllowlist() {
        // Homograph / IDN-bypass defense: an attacker registers xn--exmple-cua.com (looks like "exámple.com")
        // and embeds it in a URL hoping the operator's allowlist of "example.com" lets it through. The detector
        // compares the raw punycode host to allowlist entries byte-for-byte, so xn--exmple-cua.com is NOT
        // considered a subdomain/match of example.com and must surface as a violation. Pin the behaviour so a
        // future "normalize punycode before comparing" change that accidentally matches IDN variants produces a
        // visible test failure.
        UrlPolicy policy = new UrlPolicy(
            List.of("example.com"), List.of("http", "https"), true, true);

        List<UrlMatch> violations = UrlDetector.detectViolations(
            "visit https://xn--exmple-cua.com/login", policy);

        assertThat(violations)
            .as("punycode IDN host must not pass as a match for the Latin-letter allowlist entry")
            .isNotEmpty();
    }
}
