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

import com.bytechef.component.ai.agent.guardrails.util.SecretKeyDetectorUtils.Permissiveness;
import com.bytechef.component.ai.agent.guardrails.util.SecretKeyDetectorUtils.SecretMatch;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SecretKeyDetectorUtilsTest {

    @Test
    void testDetectAwsAccessKey() {
        List<SecretMatch> matches = SecretKeyDetectorUtils.detect(
            "use AKIAIOSFODNN7EXAMPLE to sign", Permissiveness.PERMISSIVE);

        assertThat(matches)
            .extracting(SecretMatch::type)
            .contains("AWS_ACCESS_KEY", "PREFIXED_SECRET");
        assertThat(matches)
            .allMatch(match -> match.value()
                .equals("AKIAIOSFODNN7EXAMPLE"));
    }

    @Test
    void testDetectGithubPat() {
        List<SecretMatch> matches = SecretKeyDetectorUtils.detect(
            "token ghp_abcdefghijklmnopqrstuvwxyz0123456789 here", Permissiveness.PERMISSIVE);

        assertThat(matches)
            .extracting(SecretMatch::type)
            .contains("GITHUB_PAT", "PREFIXED_SECRET");
    }

    @Test
    void testBalancedCatchesHighEntropyBelowPermissiveMinLength() {
        // 24 chars — above BALANCED's 10-char minLength, below PERMISSIVE's 30.
        String text = "secret a1B2c3D4e5F6g7H8i9J0kLmN";

        List<SecretMatch> permissive = SecretKeyDetectorUtils.detect(text, Permissiveness.PERMISSIVE);
        List<SecretMatch> balanced = SecretKeyDetectorUtils.detect(text, Permissiveness.BALANCED);

        assertThat(permissive)
            .extracting(SecretMatch::type)
            .doesNotContain("HIGH_ENTROPY_TOKEN");
        assertThat(balanced)
            .extracting(SecretMatch::type)
            .contains("HIGH_ENTROPY_TOKEN");
    }

    @Test
    void testMask() {
        String masked = SecretKeyDetectorUtils.mask(
            "use AKIAIOSFODNN7EXAMPLE to sign",
            List.of(new SecretMatch("AKIAIOSFODNN7EXAMPLE", 4, 24, "AWS_ACCESS_KEY")));

        assertThat(masked).isEqualTo("use <AWS_ACCESS_KEY> to sign");
    }

    @Test
    void testBalancedLevelDetectsHighEntropyTokenByMeasurement() {
        String content = "token: pLk7qQm9Wx2aBcDeFg8HiJkLmNpQrStUvWxYz01";

        List<SecretMatch> matches = SecretKeyDetectorUtils.detect(content, Permissiveness.BALANCED);

        assertThat(matches).extracting(SecretMatch::type)
            .contains("HIGH_ENTROPY_TOKEN");
    }

    @Test
    void testLowEntropyLongStringIsIgnored() {
        String content = "log: " + "a".repeat(50);

        List<SecretMatch> matches = SecretKeyDetectorUtils.detect(content, Permissiveness.BALANCED);

        assertThat(matches).extracting(SecretMatch::type)
            .doesNotContain("HIGH_ENTROPY_TOKEN");
    }

    @Test
    void testShortTokenBelowMinLengthIsIgnored() {
        // 9 chars — below BALANCED's minLength of 10.
        String content = "val aB1cD2eF";

        List<SecretMatch> matches = SecretKeyDetectorUtils.detect(content, Permissiveness.BALANCED);

        assertThat(matches).extracting(SecretMatch::type)
            .doesNotContain("HIGH_ENTROPY_TOKEN");
    }

    @Test
    void testLowDiversityLongTokenIsIgnored() {
        // 30 chars of lowercase only — char-class diversity is 1, below BALANCED's minCharDiversity of 3.
        String content = "key: abcdabcdabcdabcdabcdabcdabcdab";

        List<SecretMatch> matches = SecretKeyDetectorUtils.detect(content, Permissiveness.BALANCED);

        assertThat(matches).extracting(SecretMatch::type)
            .doesNotContain("HIGH_ENTROPY_TOKEN");
    }

    @Test
    void testBalancedRejectsTwoCharClassTokenAcceptsThreeCharClass() {
        // Mixed lowercase+digit only — 2 char classes, below BALANCED's minCharDiversity of 3.
        String twoClassToken = "key: abcdef0123456789abcdef0123456789";
        // Lowercase + uppercase + digit — 3 char classes, meets BALANCED's threshold.
        String threeClassToken = "key: aBc0dEf1gHi2jKl3mNo4pQr5sTu6";

        List<SecretMatch> twoClassMatches = SecretKeyDetectorUtils.detect(twoClassToken, Permissiveness.BALANCED);
        List<SecretMatch> threeClassMatches = SecretKeyDetectorUtils.detect(threeClassToken, Permissiveness.BALANCED);

        assertThat(twoClassMatches).extracting(SecretMatch::type)
            .as("2-char-class token should not fire HIGH_ENTROPY_TOKEN under BALANCED (minCharDiversity=3)")
            .doesNotContain("HIGH_ENTROPY_TOKEN");
        assertThat(threeClassMatches).extracting(SecretMatch::type)
            .as("3-char-class token should fire HIGH_ENTROPY_TOKEN under BALANCED")
            .contains("HIGH_ENTROPY_TOKEN");
    }

    @Test
    void testPermissiveLevelUsesHigherMinLength() {
        // 24-char high-entropy token — above BALANCED's 10-char minLength but below PERMISSIVE's 30.
        String content = "token: pLk7qQm9Wx2aBcDeFg8HiJkLm";

        List<SecretMatch> matches = SecretKeyDetectorUtils.detect(content, Permissiveness.PERMISSIVE);

        assertThat(matches)
            .extracting(SecretMatch::type)
            .doesNotContain("HIGH_ENTROPY_TOKEN");
    }

    @Test
    void testDetectEmptyContentReturnsEmpty() {
        assertThat(SecretKeyDetectorUtils.detect("", Permissiveness.BALANCED)).isEmpty();
    }

    @Test
    void testDetectNullContentReturnsEmpty() {
        assertThat(SecretKeyDetectorUtils.detect(null, Permissiveness.BALANCED)).isEmpty();
    }

    @Test
    void testMaskNullContentReturnsNull() {
        assertThat(SecretKeyDetectorUtils.mask(null, List.of())).isNull();
    }

    @Test
    void testMaskDeduplicatesOverlappingSecretMatches() {
        // A GitHub PAT matches both the GITHUB_PAT named pattern and the generic PREFIXED_SECRET shape detector at
        // the same span. deduplicateOverlaps must ensure the mask produces a valid placeholder rather than a
        // fragmented "<GITHUB_PAT><PREFIXED_SECRET>" collision.
        String content = "use ghp_abcdefghijklmnopqrstuvwxyz0123456789 today";

        List<SecretMatch> matches = SecretKeyDetectorUtils.detect(content, Permissiveness.PERMISSIVE);

        assertThat(matches).hasSizeGreaterThan(1); // both types fired

        String masked = SecretKeyDetectorUtils.mask(content, matches);

        // Exactly one placeholder — the deduplication collapses the two overlapping matches to a single masked span.
        assertThat(masked)
            .doesNotContain("ghp_abcdefghijklmnopqrstuvwxyz0123456789")
            .startsWith("use ")
            .endsWith(" today");
        assertThat(masked.chars()
            .filter(character -> character == '<')
            .count()).isEqualTo(1);
    }

    @Test
    void testStrictFiresOnTwoCharClassTokenThatBalancedRejects() {
        // Pin the asymmetry between STRICT and BALANCED via char-class diversity: STRICT requires only 2
        // classes whereas BALANCED requires 3. A long alphanumeric (lowercase + digit only) token has
        // diversity 2, so it fires on STRICT but is rejected by BALANCED's higher diversity gate. Without
        // this pin a refactor that swapped the thresholds would invert the security posture — operators
        // selecting BALANCED for low-FP would suddenly receive the higher-detection rule.
        String content = "key: abcdef0123456789abcdef0123456789";

        List<SecretMatch> strict = SecretKeyDetectorUtils.detect(content, Permissiveness.STRICT);
        List<SecretMatch> balanced = SecretKeyDetectorUtils.detect(content, Permissiveness.BALANCED);

        assertThat(strict)
            .as("STRICT (charDiversity=2) must flag the 2-class alphanumeric token")
            .extracting(SecretMatch::type)
            .contains("HIGH_ENTROPY_TOKEN");
        assertThat(balanced)
            .as("BALANCED (charDiversity=3) must NOT flag a 2-class token")
            .extracting(SecretMatch::type)
            .doesNotContain("HIGH_ENTROPY_TOKEN");
    }

    @Test
    void testPermissiveRequiresLongerTokenThanBalanced() {
        // 24 chars high-entropy fires on BALANCED (>=10) but not PERMISSIVE (>=30). Pin the asymmetry so a
        // refactor that aligns the minLengths would surface as a test failure.
        String content = "token: pLk7qQm9Wx2aBcDeFg8HiJkLm";

        List<SecretMatch> balanced = SecretKeyDetectorUtils.detect(content, Permissiveness.BALANCED);
        List<SecretMatch> permissive = SecretKeyDetectorUtils.detect(content, Permissiveness.PERMISSIVE);

        assertThat(balanced).extracting(SecretMatch::type)
            .contains("HIGH_ENTROPY_TOKEN");
        assertThat(permissive).extracting(SecretMatch::type)
            .doesNotContain("HIGH_ENTROPY_TOKEN");
    }

    @Test
    void testOversizedContentRejectedByBoundedWrap() {
        // Built-in scans must honour the DoS bound enforced by RegexParser.bounded — without it, a very long input
        // paired with a pathological provider regex could run unbounded. Feed a content just above MAX_INPUT_LENGTH
        // and expect the bounded wrap to reject before any pattern matching runs.
        String oversized = "a".repeat(RegexParserUtils.MAX_INPUT_LENGTH + 1);

        assertThatThrownBy(() -> SecretKeyDetectorUtils.detect(oversized, Permissiveness.BALANCED))
            .isInstanceOf(RegexParserUtils.RegexExecutionLimitException.class);
    }

    @Test
    void testKeyEqualsValuePatternIgnoresSubstringKeyNouns() {
        // Regression: the prior pattern matched 'notapikey=...' / 'mySecretValue=...' because the key noun group
        // (api[_-]?key|secret|token|password|auth) had no left-side anchor. The added (?<![A-Za-z]) lookbehind
        // restricts matches to identifiers where the key noun starts at a non-letter boundary, eliminating the
        // false-positive class while still flagging real assignments like 'apiKey=...' / 'X-Api-Key: ...'.
        List<SecretKeyDetectorUtils.SecretMatch> noisy = SecretKeyDetectorUtils.detect(
            "let mySecretValue = abcdefghijklmnopqrstuv", Permissiveness.STRICT);

        assertThat(noisy)
            .as("identifier ending in 'secret' must not be flagged as a generic key=value secret")
            .noneSatisfy(match -> assertThat(match.type()).isEqualTo("GENERIC_SECRET"));

        List<SecretKeyDetectorUtils.SecretMatch> real = SecretKeyDetectorUtils.detect(
            "api_key=abcdefghijklmnopqrstuv", Permissiveness.STRICT);

        assertThat(real)
            .as("real key=value assignment must still be flagged")
            .anySatisfy(match -> assertThat(match.type()).isEqualTo("GENERIC_SECRET"));
    }

    @Test
    void testUuidIsNotFlaggedAsHighEntropyTokenAtBalancedLevel() {
        // UUIDs are an extremely common false-positive source for entropy-based secret detection: 32 hex characters
        // interrupted by dashes have high character diversity and realistic entropy. Operators surface UUIDs
        // constantly (request IDs, entity keys, X-Request-Id headers) and a detector that flags them would cascade
        // into "blocked every request" noise. Pin that BALANCED does not flag a standard UUID as HIGH_ENTROPY_TOKEN.
        String text = "request id 550e8400-e29b-41d4-a716-446655440000";

        List<SecretMatch> matches = SecretKeyDetectorUtils.detect(text, Permissiveness.BALANCED);

        assertThat(matches)
            .as("standard UUIDs must not trip the BALANCED high-entropy scan")
            .noneSatisfy(match -> assertThat(match.type()).isEqualTo("HIGH_ENTROPY_TOKEN"));
    }

    @Test
    void testSha256HexDigestIsNotFlaggedAsHighEntropyTokenAtBalancedLevel() {
        // SHA-256 hex digests appear in realistic logs (commit hashes, ETags, HMAC outputs). The 64-char
        // hex-only shape has limited character-class diversity (just 0-9a-f), which the entropy heuristic
        // should recognize as structured rather than secret. Operators who deliberately want to flag exposed
        // hashes can add a custom regex — the default must not flag them.
        String text = "sha=a3e60c6b8e4e5e3d8e5e5e3d8e5e5e3d8e5e5e3d8e5e5e3d8e5e5e3d8e5e5e3d";

        List<SecretMatch> matches = SecretKeyDetectorUtils.detect(text, Permissiveness.BALANCED);

        assertThat(matches)
            .as("plain lowercase hex digests must not trip the BALANCED high-entropy scan")
            .noneSatisfy(match -> assertThat(match.type()).isEqualTo("HIGH_ENTROPY_TOKEN"));
    }

    @Test
    void testBase64EncodedSecretIsNotDecodedNorMatchedByBuiltInDetector() {
        // Documents the current behaviour: base64-wrapped secrets are NOT decoded by the built-in detector. A
        // secret like 'sk-ABCDEFGHIJKLMNOPQRSTUV' base64-encoded becomes a high-entropy string that the
        // entropy-based scan (BALANCED+) WILL flag, but the original 'sk-' provider-prefix detection cannot fire
        // because the literal prefix is no longer present. Operators who need to scan base64 payloads must either
        // pre-decode upstream or add a custom-regex guardrail. Pinning the limitation here so a future "why didn't
        // it catch this?" ticket has a clear test pointing at the design boundary.
        String base64Wrapped = "Authorization: Basic c2stQUJDREVGR0hJSktMTU5PUFFSU1RVVg==";

        List<SecretKeyDetectorUtils.SecretMatch> matches = SecretKeyDetectorUtils.detect(
            base64Wrapped, Permissiveness.BALANCED);

        // The base64 token may be flagged by the high-entropy heuristic, but never as the literal sk- provider type.
        assertThat(matches)
            .noneSatisfy(match -> assertThat(match.type()).isEqualTo("OPENAI_KEY"));
    }

    @Test
    void testDetectRejectsPathologicallyLargeInputViaRegexParserBound() {
        String oversizedInput = "a".repeat(RegexParserUtils.MAX_INPUT_LENGTH + 1);

        assertThatThrownBy(() -> SecretKeyDetectorUtils.detect(oversizedInput, Permissiveness.BALANCED))
            .as("input above RegexParserUtils.MAX_INPUT_LENGTH must abort via RegexExecutionLimitException to "
                + "prevent a pathological pattern scan from holding a worker thread")
            .isInstanceOf(RegexParserUtils.RegexExecutionLimitException.class);
    }
}
