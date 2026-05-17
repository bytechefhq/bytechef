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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.component.ai.agent.guardrails.util.UrlDetectorUtils.UrlMatch;
import com.bytechef.component.ai.agent.guardrails.util.UrlDetectorUtils.UrlPolicy;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
class UrlDetectorUtilsTest {

    private static final UrlPolicy DEFAULT_POLICY = new UrlPolicy(
        List.of(), List.of("http", "https"), true, true);

    @Test
    void testDetectBareUrl() {
        List<UrlMatch> matches = UrlDetectorUtils.detectViolations(
            "visit https://example.com/page now", DEFAULT_POLICY);

        assertThat(matches).hasSize(1);
        assertThat(
            matches.getFirst()
                .url()).isEqualTo("https://example.com/page");
    }

    @Test
    void testAllowedUrlNotFlagged() {
        UrlPolicy policy = new UrlPolicy(
            List.of("example.com"), List.of("http", "https"), true, true);
        List<UrlMatch> matches = UrlDetectorUtils.detectViolations(
            "visit https://example.com/page", policy);

        assertThat(matches).isEmpty();
    }

    @Test
    void testSubdomainAllowedViaFlag() {
        UrlPolicy policy = new UrlPolicy(
            List.of("example.com"), List.of("http", "https"), true, true);
        List<UrlMatch> matches = UrlDetectorUtils.detectViolations(
            "visit https://sub.example.com/p", policy);

        assertThat(matches).isEmpty();
    }

    @Test
    void testSubdomainBlockedWhenFlagOff() {
        UrlPolicy policy = new UrlPolicy(
            List.of("example.com"), List.of("http", "https"), true, false);
        List<UrlMatch> matches = UrlDetectorUtils.detectViolations(
            "visit https://sub.example.com/p", policy);

        assertThat(matches).hasSize(1);
    }

    @Test
    void testSchemeNotAllowed() {
        UrlPolicy policy = new UrlPolicy(
            List.of("example.com"), List.of("https"), true, true);
        List<UrlMatch> matches = UrlDetectorUtils.detectViolations(
            "visit ftp://example.com/p", policy);

        assertThat(matches).hasSize(1);
    }

    @Test
    void testUserinfoBlocked() {
        List<UrlMatch> matches = UrlDetectorUtils.detectViolations(
            "login https://user:pass@example.com/", DEFAULT_POLICY);

        assertThat(matches).hasSize(1);
    }

    @Test
    void testMask() {
        String masked = UrlDetectorUtils.mask(
            "visit https://evil.com/page then",
            List.of(new UrlMatch("https://evil.com/page", 6, 27, "SCHEME_OR_HOST_NOT_ALLOWED")));

        assertThat(masked).isEqualTo("visit <URL> then");
    }

    @Test
    void testSchemeUrlDoesNotAlsoMatchBareDomainSubstring() {
        UrlPolicy policy = new UrlPolicy(List.of(), List.of("https"), false, false);

        List<UrlMatch> violations = UrlDetectorUtils.detectViolations("see https://evil.com/path", policy);

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0)
            .url()).startsWith("https://");
    }

    @Test
    void testCidrAllowlistEntryPermitsAddressInRange() {
        UrlPolicy policy = new UrlPolicy(List.of("10.0.0.0/24"), List.of("https"), false, false);

        List<UrlMatch> violations = UrlDetectorUtils.detectViolations("https://10.0.0.5/api", policy);

        assertThat(violations).isEmpty();
    }

    @Test
    void testCidrAllowlistEntryBlocksOutOfRangeAddress() {
        UrlPolicy policy = new UrlPolicy(List.of("10.0.0.0/24"), List.of("https"), false, false);

        List<UrlMatch> violations = UrlDetectorUtils.detectViolations("https://10.0.1.5/api", policy);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void testPathPrefixAllowlistMatchesPrefixOnly() {
        UrlPolicy policy = new UrlPolicy(
            List.of("api.example.com/v2/"), List.of("https"), false, false);

        assertThat(UrlDetectorUtils.detectViolations(
            "https://api.example.com/v2/users", policy)).isEmpty();
        assertThat(UrlDetectorUtils.detectViolations(
            "https://api.example.com/v1/users", policy)).isNotEmpty();
    }

    @Test
    void testPathPrefixAllowlistWithoutTrailingSlashDoesNotLeakToSiblingPaths() {
        // Entry "example.com/admin" must not allowlist "example.com/administrator" — the prefix
        // check must respect segment boundaries even when the allowlist entry omits the trailing slash.
        UrlPolicy policy = new UrlPolicy(
            List.of("example.com/admin"), List.of("https"), false, false);

        assertThat(UrlDetectorUtils.detectViolations("https://example.com/admin", policy)).isEmpty();
        assertThat(UrlDetectorUtils.detectViolations("https://example.com/admin/users", policy)).isEmpty();
        assertThat(UrlDetectorUtils.detectViolations("https://example.com/administrator", policy)).isNotEmpty();
        assertThat(UrlDetectorUtils.detectViolations("https://example.com/adminpanel", policy)).isNotEmpty();
    }

    @Test
    void testUserinfoAllowedWhenBlockUserinfoIsFalse() {
        UrlPolicy policy = new UrlPolicy(
            List.of("example.com"), List.of("https"), false, false);

        List<UrlMatch> matches = UrlDetectorUtils.detectViolations(
            "login https://user:pass@example.com/", policy);

        assertThat(matches).isEmpty();
    }

    @Test
    void testCidrAllowlistEntryWithPrefixOutOfRangeDoesNotMatch() {
        UrlPolicy policy = new UrlPolicy(List.of("10.0.0.0/99"), List.of("https"), false, false);

        List<UrlMatch> violations = UrlDetectorUtils.detectViolations("https://10.0.0.5/api", policy);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void testCidrAllowlistEntryWithMalformedOctetDoesNotMatch() {
        UrlPolicy policy = new UrlPolicy(List.of("10.0/24"), List.of("https"), false, false);

        List<UrlMatch> violations = UrlDetectorUtils.detectViolations("https://10.0.0.5/api", policy);

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

        List<UrlMatch> violations = UrlDetectorUtils.detectViolations(
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

        List<UrlMatch> violations = UrlDetectorUtils.detectViolations(
            "visit https://xn--exmple-cua.com/login", policy);

        assertThat(violations)
            .as("punycode IDN host must not pass as a match for the Latin-letter allowlist entry")
            .isNotEmpty();
    }

    @Test
    void testSchemePrefixedAllowlistEntryMatchesHostAndPort() {
        // Reviewer scenario from PR #4915: allowlist 'http://localhost:5173' must allow
        // 'http://localhost:5173/automation/projects/...'. Before the fix, the entry was split on the first '/'
        // (the '/' inside 'http://') and entryHost became 'http:' — every URL was silently rejected.
        UrlPolicy policy = new UrlPolicy(
            List.of("http://localhost:5173"), List.of("http", "https"), true, true);

        List<UrlMatch> violations = UrlDetectorUtils.detectViolations(
            "open http://localhost:5173/automation/projects/1052/project-workflows/1064 in browser", policy);

        assertThat(violations).isEmpty();
    }

    @Test
    void testSchemePrefixedAllowlistEntryRejectsDifferentPort() {
        // Entry 'http://localhost:5173' must not allow URLs on a different port. Without per-port matching, the
        // user has no way to lock the allowlist to a specific dev server.
        UrlPolicy policy = new UrlPolicy(
            List.of("http://localhost:5173"), List.of("http", "https"), true, true);

        List<UrlMatch> violations = UrlDetectorUtils.detectViolations(
            "open http://localhost:9999/foo in browser", policy);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void testSchemePrefixedAllowlistEntryWithoutPortMatchesAnyPort() {
        // Symmetric case: when the allowlist entry omits the port, any port on the host is allowed. Without this,
        // operators who write 'https://example.com' would inadvertently lock themselves out of port-redirected URLs.
        UrlPolicy policy = new UrlPolicy(
            List.of("https://example.com"), List.of("http", "https"), true, true);

        assertThat(UrlDetectorUtils.detectViolations(
            "see https://example.com/p", policy)).isEmpty();
        assertThat(UrlDetectorUtils.detectViolations(
            "see https://example.com:8443/p", policy)).isEmpty();
    }

    @Test
    void testSchemePrefixedAllowlistEntryWithPathMatchesPathPrefix() {
        // Combine all three: scheme, host, and path-prefix. Pins parity with the bare-host path-prefix branch
        // (testPathPrefixAllowlistMatchesPrefixOnly) so users get the same semantics whether they write
        // 'api.example.com/v2/' or 'https://api.example.com/v2/'.
        UrlPolicy policy = new UrlPolicy(
            List.of("https://api.example.com/v2/"), List.of("https"), true, true);

        assertThat(UrlDetectorUtils.detectViolations(
            "fetch https://api.example.com/v2/users", policy)).isEmpty();
        assertThat(UrlDetectorUtils.detectViolations(
            "fetch https://api.example.com/v1/users", policy)).isNotEmpty();
    }

    @Test
    void testSchemePrefixedAllowlistEntryHonorsAllowSubdomainFlag() {
        UrlPolicy allowed = new UrlPolicy(
            List.of("https://example.com"), List.of("https"), true, true);
        UrlPolicy disallowed = new UrlPolicy(
            List.of("https://example.com"), List.of("https"), true, false);

        assertThat(UrlDetectorUtils.detectViolations("visit https://api.example.com/p", allowed)).isEmpty();
        assertThat(UrlDetectorUtils.detectViolations("visit https://api.example.com/p", disallowed)).isNotEmpty();
    }

    @Test
    void testProseAndBareDomainsAreNotFlagged() {
        // Regression for commit 6707fca47cc: bare-domain / IPv4 detection was removed because it
        // false-positived on common prose. If a future change re-introduces scheme-less detection,
        // these assertions catch it before the bug ships.
        assertThat(UrlDetectorUtils.detectViolations("see e.g. the documentation", DEFAULT_POLICY))
            .as("'e.g.' prose abbreviation must not be flagged as a URL")
            .isEmpty();
        assertThat(
            UrlDetectorUtils.detectViolations("i.e. the assistant only accepts cooking questions", DEFAULT_POLICY))
                .as("'i.e.' prose abbreviation must not be flagged as a URL")
                .isEmpty();
        assertThat(UrlDetectorUtils.detectViolations("contact support at example.com for help", DEFAULT_POLICY))
            .as("bare host without a scheme must not be flagged as a URL")
            .isEmpty();
        assertThat(UrlDetectorUtils.detectViolations("our server runs at 192.168.1.1 on port 8080", DEFAULT_POLICY))
            .as("bare IPv4 address without a scheme must not be flagged as a URL")
            .isEmpty();
    }

    @Test
    void testOversizedInputRejectedByRegexParserBudget() {
        // Pins that URL detection routes through RegexParserUtils.bounded(): an input one character past
        // MAX_INPUT_LENGTH must surface RegexExecutionLimitException rather than spending unbounded scanner
        // budget. Without this test, a future refactor that pulled the scheme matcher off the bounded()
        // wrapper would let a pathological prose payload lock a worker thread.
        String oversized = "https://example.com/" + "a".repeat(RegexParserUtils.MAX_INPUT_LENGTH);

        assertThatThrownBy(() -> UrlDetectorUtils.detectViolations(oversized, DEFAULT_POLICY))
            .isInstanceOf(RegexParserUtils.RegexExecutionLimitException.class);
    }
}
