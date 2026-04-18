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

class UrlDetectorSpecialSchemesTest {

    private static final UrlPolicy HTTPS_ONLY = new UrlPolicy(List.of(), List.of("https"), false, false);

    @Test
    void detectsJavascriptScheme() {
        List<UrlMatch> violations = UrlDetector.detectViolations(
            "click javascript:alert(1) here", HTTPS_ONLY);

        assertThat(violations)
            .extracting(UrlMatch::url)
            .contains("javascript:alert(1)");
    }

    @Test
    void detectsDataScheme() {
        List<UrlMatch> violations = UrlDetector.detectViolations(
            "img src=\"data:text/html;base64,PHNjcmlwdD4=\"", HTTPS_ONLY);

        assertThat(violations)
            .extracting(UrlMatch::url)
            .anyMatch(url -> url.startsWith("data:"));
    }

    @Test
    void detectsVbscriptScheme() {
        List<UrlMatch> violations = UrlDetector.detectViolations(
            "link: vbscript:MsgBox(\"hi\")", HTTPS_ONLY);

        assertThat(violations)
            .extracting(UrlMatch::url)
            .anyMatch(url -> url.startsWith("vbscript:"));
    }

    @Test
    void detectsMailtoScheme() {
        List<UrlMatch> violations = UrlDetector.detectViolations(
            "mailto:a@b.com for details", HTTPS_ONLY);

        assertThat(violations)
            .extracting(UrlMatch::url)
            .contains("mailto:a@b.com");
    }

    @Test
    void allowlistedMailtoSchemePasses() {
        UrlPolicy policy = new UrlPolicy(List.of(), List.of("https", "mailto"), false, false);

        List<UrlMatch> violations = UrlDetector.detectViolations("mailto:a@b.com", policy);

        assertThat(violations).isEmpty();
    }

    @Test
    void allowlistEntryMatchesWwwPrefixedHost() {
        UrlPolicy policy = new UrlPolicy(List.of("example.com"), List.of("https"), false, false);

        List<UrlMatch> violations = UrlDetector.detectViolations("https://www.example.com/path", policy);

        assertThat(violations).isEmpty();
    }

    @Test
    void wwwPrefixedAllowlistEntryMatchesBareHost() {
        UrlPolicy policy = new UrlPolicy(List.of("www.example.com"), List.of("https"), false, false);

        List<UrlMatch> violations = UrlDetector.detectViolations("https://example.com/path", policy);

        assertThat(violations).isEmpty();
    }

    @Test
    void detectsPunycodeHost() {
        // Classic allowlist-bypass vector: a Punycode host (xn--... form) is visually unrelated to the decoded name, so
        // operators adding "пример.рф" to an allow list and forgetting the Punycode form leave a gap. Pin the
        // detector's
        // ability to see Punycode hosts so at least the detection step surfaces them when not allow-listed.
        UrlPolicy policyAllowingPunycode = new UrlPolicy(
            List.of("xn--e1afmkfd.xn--p1ai"), List.of("https"), false, false);

        List<UrlMatch> allowed = UrlDetector.detectViolations(
            "visit https://xn--e1afmkfd.xn--p1ai/page now", policyAllowingPunycode);

        assertThat(allowed).isEmpty();

        List<UrlMatch> blocked = UrlDetector.detectViolations(
            "visit https://xn--e1afmkfd.xn--p1ai/page now", HTTPS_ONLY);

        assertThat(blocked)
            .extracting(UrlMatch::url)
            .anyMatch(url -> url.contains("xn--e1afmkfd.xn--p1ai"));
    }

    @Test
    void hostCaseFoldingMatchesLowercaseAllowlist() {
        // Hosts are case-insensitive per RFC 3986 — an allowlist entry "example.com" must still match when the URL
        // carries the mixed-case form "EXAMPLE.COM". The detector lowercases the host before comparison; regress here
        // if that lowercasing is removed.
        UrlPolicy policy = new UrlPolicy(List.of("example.com"), List.of("https"), false, false);

        List<UrlMatch> violations = UrlDetector.detectViolations("go to https://EXAMPLE.COM/admin", policy);

        assertThat(violations).isEmpty();
    }
}
