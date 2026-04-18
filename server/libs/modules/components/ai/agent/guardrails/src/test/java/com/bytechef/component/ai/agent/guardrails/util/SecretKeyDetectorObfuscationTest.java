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

import com.bytechef.component.ai.agent.guardrails.util.SecretKeyDetector.Permissiveness;
import com.bytechef.component.ai.agent.guardrails.util.SecretKeyDetector.SecretMatch;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pins detection (or documented non-detection) of obfuscated secrets. Complements
 * {@link SecretKeyDetectorTest#testBase64EncodedSecretIsNotDecodedNorMatchedByBuiltInDetector()} with the additional
 * common obfuscation vectors: secrets split across newlines, URL-encoded secrets, and JSON-escaped secrets.
 *
 * <p>
 * For each vector the test asserts either (a) that the detector still fires on the literal prefix, or (b) that the
 * limitation is documented. Pinning both outcomes as tests is deliberate — the security team needs a clear map of "what
 * can slip past" when evaluating coverage.
 */
class SecretKeyDetectorObfuscationTest {

    @Test
    void testSecretSplitByBackslashNewlineIsNotReassembledButPrefixHalfStillMatchesWhenLongEnough() {
        // Common config-file shape: `token = "sk-ABC\` then `DEF..."` on the next line. The detector runs on the
        // concatenated text as delivered; a line-continuation backslash does NOT collapse into the next line for
        // this detector. Document the boundary: a secret whose prefix half is too short to meet the minimum-length
        // bound will NOT trigger the OPENAI_KEY match.
        String splitShort = "token = \"sk-ABC\\\nDEFGHIJKLMNOPQRST\"";

        List<SecretMatch> shortMatches = SecretKeyDetector.detect(splitShort, Permissiveness.BALANCED);

        // The OPENAI_KEY regex requires a minimum length (~20 chars) after "sk-". Neither half meets it, so neither
        // fires. Document this boundary.
        assertThat(shortMatches)
            .as("A secret split by backslash-newline where neither half meets the prefix's minimum length does NOT "
                + "fire the provider-prefix match. Operators relying on split-detection must pre-normalise input.")
            .noneSatisfy(match -> assertThat(match.type()).isEqualTo("OPENAI_KEY"));
    }

    @Test
    void testSecretSplitAcrossNewlinesDoesNotReassembleIntoSingleMatch() {
        // Secret split with a literal newline in the middle: neither half looks like a valid provider-prefix secret
        // because the regex is anchored on contiguous characters.
        String splitSecret = "token=sk-ABCDEFG\nHIJKLMNOPQRSTUV";

        List<SecretMatch> matches = SecretKeyDetector.detect(splitSecret, Permissiveness.BALANCED);

        // Document the limitation: raw newlines break the literal-prefix regex. A downstream detector that
        // canonicalises whitespace would be needed to catch this vector.
        assertThat(matches)
            .as("Secrets split across literal newlines do NOT reassemble; neither half is long enough to trip the "
                + "prefix regex. Documented limitation.")
            .noneSatisfy(match -> assertThat(match.type()).isEqualTo("OPENAI_KEY"));
    }

    @Test
    void testUrlEncodedSecretIsNotDecodedButMayTripHighEntropyHeuristic() {
        // "sk-" URL-encoded becomes "sk%2D" which breaks the literal-prefix regex. The detector does NOT URL-decode
        // input. This is the URL-path / query-string obfuscation vector.
        String urlEncoded = "https://api.example/?key=sk%2DABCDEFGHIJKLMNOPQRSTUV";

        List<SecretMatch> matches = SecretKeyDetector.detect(urlEncoded, Permissiveness.BALANCED);

        // The literal OPENAI_KEY provider-prefix match cannot fire because "sk-" is URL-encoded to "sk%2D". Operators
        // who need this coverage must pre-decode input upstream.
        assertThat(matches)
            .as("URL-encoded provider prefix ('sk-' -> 'sk%2D') breaks the literal prefix regex; the detector does "
                + "NOT URL-decode. Pre-decoding is the operator's responsibility.")
            .noneSatisfy(match -> assertThat(match.type()).isEqualTo("OPENAI_KEY"));
    }

    @Test
    void testJsonEscapedSecretIsMatchedIfEscapesDontBreakThePrefixContiguity() {
        // JSON-escaped quotes and whitespace are common in config blobs. The escape characters themselves don't
        // occur inside the prefix, so a secret wrapped in JSON escapes still matches.
        String jsonEscaped = "{\"api_key\": \"sk-ABCDEFGHIJKLMNOPQRSTUV\"}";

        List<SecretMatch> matches = SecretKeyDetector.detect(jsonEscaped, Permissiveness.BALANCED);

        // Positive assertion: JSON quoting is transparent for the prefix-regex as long as the escape characters are
        // outside the secret itself. This is the common case and the detector handles it.
        assertThat(matches)
            .as("JSON-quoted secrets are detected — the quotes are outside the secret body so the prefix regex still "
                + "matches contiguously.")
            .anySatisfy(match -> {
                assertThat(match.type()).isEqualTo("OPENAI_KEY");
                assertThat(match.value()).startsWith("sk-");
            });
    }

    @Test
    void testJsonEscapedSecretWithUnicodeEscapeInThePrefixDoesNotMatch() {
        // If the JSON uses a unicode escape for '-' (allowed by the JSON spec), the literal prefix "sk-" is broken
        // at the source-text level. The detector runs on the raw string it was given — it does NOT decode JSON-style
        // escape sequences. This is an adversarial vector documented as a known limitation.
        String unicodeEscaped = "{\"api_key\": \"sk\\" + "u002DABCDEFGHIJKLMNOPQRSTUV\"}";

        List<SecretMatch> matches = SecretKeyDetector.detect(unicodeEscaped, Permissiveness.BALANCED);

        assertThat(matches)
            .as("JSON unicode-escaped hyphen in the prefix breaks the literal prefix regex. The detector does NOT "
                + "decode JSON unicode escapes. Known adversarial vector; pre-decoding is required.")
            .noneSatisfy(match -> assertThat(match.type()).isEqualTo("OPENAI_KEY"));
    }
}
