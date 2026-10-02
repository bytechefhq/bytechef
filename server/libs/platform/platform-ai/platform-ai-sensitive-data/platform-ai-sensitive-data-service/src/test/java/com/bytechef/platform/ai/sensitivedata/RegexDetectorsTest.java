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

package com.bytechef.platform.ai.sensitivedata;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.ai.sensitivedata.tokenization.PiiTokenSession;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RegexDetectorsTest {

    private static final Set<SensitiveKind> BOTH = EnumSet.allOf(SensitiveKind.class);

    private final SensitiveDataRedactor redactor = new SensitiveDataRedactor(SensitiveDataDetectors.builtIn());

    @Test
    void testDetectsEveryPiiCategory() {
        String redacted = redactor.redact(
            "Email me at jane.doe@example.com or call 415-555-0132. SSN 123-45-6789, card 4111 1111 1111 1111, " +
                "host 192.168.1.20.",
            BOTH, null);

        assertThat(redacted).contains("[REDACTED_EMAIL_ADDRESS]");
        assertThat(redacted).contains("[REDACTED_US_SSN]");
        assertThat(redacted).contains("[REDACTED_CREDIT_CARD]");
        assertThat(redacted).contains("[REDACTED_PHONE_NUMBER]");
        assertThat(redacted).contains("[REDACTED_IP_ADDRESS]");
        assertThat(redacted).doesNotContain("jane.doe@example.com");
        assertThat(redacted).doesNotContain("123-45-6789");
    }

    @Test
    void testDetectsKnownSecretShapes() {
        String redacted = redactor.redact(
            "aws AKIAIOSFODNN7EXAMPLE gh ghp_1234567890abcdefghij1234567890abcdef openai " +
                "sk-abcdefghij1234567890ABCD jwt eyJhbGciOiJIUzI.eyJzdWIiOiIxMjM0.SflKxwRJSMeKKF2QT4 done",
            BOTH, null);

        assertThat(redacted).contains("[REDACTED_SECRET]");
        assertThat(redacted).doesNotContain("AKIAIOSFODNN7EXAMPLE");
        assertThat(redacted).doesNotContain("ghp_1234567890abcdefghij1234567890abcdef");
        assertThat(redacted).doesNotContain("sk-abcdefghij1234567890ABCD");
        assertThat(redacted).doesNotContain("eyJhbGciOiJIUzI");
    }

    @Test
    void testRedactsPemPrivateKeyBlockWhole() {
        String redacted = redactor.redact(
            "key:\n-----BEGIN RSA PRIVATE KEY-----\nMIIBOgIBAAJBAKj34Gkx...\n-----END RSA PRIVATE KEY-----\ntail",
            BOTH, null);

        assertThat(redacted).contains("[REDACTED_SECRET]");
        assertThat(redacted).doesNotContain("BEGIN RSA PRIVATE KEY");
        assertThat(redacted).contains("tail");
    }

    @Test
    void testDetectsAwsSecretKeyPattern() {
        assertThat(redactor.redact(
            "awsSecretKey=\"wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY\" done", BOTH, null))
                .isEqualTo("[REDACTED_SECRET] done");
    }

    @Test
    void testDoesNotRedactStripePublishableKey() {
        String text = "key pk_test_AAAAAAAAAAAAAAAAAAAA done";

        assertThat(redactor.redact(text, BOTH, null)).isEqualTo(text);
    }

    @Test
    void testDetectsStripeSecretTestKey() {
        assertThat(redactor.redact("key sk_test_AAAAAAAAAAAAAAAAAAAA done", BOTH, null))
            .isEqualTo("key [REDACTED_SECRET] done");
    }

    @Test
    void testDetectsStripeRestrictedLiveKey() {
        assertThat(redactor.redact("key rk_" + "live_" + "B".repeat(24) + " done", BOTH, null))
            .isEqualTo("key [REDACTED_SECRET] done");
    }

    @Test
    void testLeavesCleanTextUnchanged() {
        String content = "Summarize the quarterly revenue report. The deployment succeeded.";

        assertThat(redactor.redact(content, BOTH, null)).isEqualTo(content);
    }

    @Test
    void testSecretContainingDigitRunIsRedactedWholeNotPartially() {
        assertThat(redactor.redact("sk-proj-1234567890123456", BOTH, null)).isEqualTo("[REDACTED_SECRET]");
        assertThat(redactor.redact("xoxb-1234567890123456-abcdef", BOTH, null)).isEqualTo("[REDACTED_SECRET]");
    }

    @Test
    void testNonOverlappingInputsMatchThePreSpiOutput() {
        assertThat(redactor.redact("card 4111 1111 1111 1111 ok", BOTH, null))
            .isEqualTo("card [REDACTED_CREDIT_CARD] ok");
        assertThat(redactor.redact("mail me at bob@example.com please", BOTH, null))
            .isEqualTo("mail me at [REDACTED_EMAIL_ADDRESS] please");
        assertThat(redactor.redact("contact sk-proj-abcdefghijklmnopqrstuvwx now", BOTH, null))
            .isEqualTo("contact [REDACTED_SECRET] now");
        assertThat(redactor.redact("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.abc123", BOTH, null))
            .isEqualTo("[REDACTED_SECRET]");
        assertThat(redactor.redact("bob@example.com called from 10.0.0.5 with 555-123-4567", BOTH, null))
            .isEqualTo("[REDACTED_EMAIL_ADDRESS] called from [REDACTED_IP_ADDRESS] with [REDACTED_PHONE_NUMBER]");
    }

    @Test
    void testOrdinaryBusinessIdentifiersProduceNoSpansAtTheDefaultThreshold() {
        assertThat(redactor.redact("Order 20260825 shipped; invoice 4500123987 total 1234.56", BOTH, null))
            .isEqualTo("Order 20260825 shipped; invoice 4500123987 total 1234.56");
        assertThat(redactor.redact("SKU AB123456 qty 12345678", BOTH, null))
            .isEqualTo("SKU AB123456 qty 12345678");
        assertThat(redactor.redact("Ticket 987654321 escalated", BOTH, null))
            .isEqualTo("Ticket 987654321 escalated");
    }

    @Test
    void testGeneratedBusinessIdentifierCorpusProducesNoSpansAtTheDefaultThreshold() {
        List<String> corpus = new ArrayList<>();

        for (int length = 8; length <= 17; length++) {
            corpus.add("Reference " + "1".repeat(length) + " confirmed.");
            corpus.add("Reference " + "9".repeat(length) + " confirmed.");
        }

        for (int prefixLength = 1; prefixLength <= 3; prefixLength++) {
            String prefix = "ABC".substring(0, prefixLength);

            for (int digitLength = 3; digitLength <= 10; digitLength++) {
                corpus.add("SKU " + prefix + "1".repeat(digitLength) + " qty 5");
            }
        }

        for (int digitLength = 6; digitLength <= 12; digitLength++) {
            String digits = "1".repeat(digitLength);

            corpus.add("Order " + digits + " shipped");
            corpus.add("Invoice " + digits + " total 1234.56");
            corpus.add("Ticket " + digits + " escalated");
            corpus.add("PO-" + digits + " approved");
        }

        corpus.add("Order 1234567890123456 shipped");

        List<String> unexpectedlyRedacted = new ArrayList<>();

        for (String text : corpus) {
            String redacted = redactor.redact(text, BOTH, null);

            if (!redacted.equals(text)) {
                unexpectedlyRedacted.add(text + " -> " + redacted);
            }
        }

        assertThat(unexpectedlyRedacted).isEmpty();
    }

    @Test
    void testUnformattedCreditCardRedactsOnlyWhenLuhnValid() {
        assertThat(redactor.redact("card 4532015112830366 ok", BOTH, null))
            .isEqualTo("card [REDACTED_CREDIT_CARD] ok");
        assertThat(redactor.redact("Order 1234567890123456 shipped", BOTH, null))
            .isEqualTo("Order 1234567890123456 shipped");
    }

    @Test
    void testStrongPatternsStillDetectAtTheDefaultThreshold() {
        assertThat(redactor.redact("mail bob@acme.io", BOTH, null)).isEqualTo("mail [REDACTED_EMAIL_ADDRESS]");
        assertThat(redactor.redact("key AKIAIOSFODNN7EXAMPLE", BOTH, null)).isEqualTo("key [REDACTED_SECRET]");
    }

    @Test
    void testBothDetectorsAreStreamSafe() {
        List<SensitiveDataDetector> detectors = SensitiveDataDetectors.builtIn();

        assertThat(detectors).hasSize(2);
        assertThat(detectors).allMatch(SensitiveDataDetector::streamSafe);
    }

    @Test
    void testKindsAreAssignedCorrectly() {
        assertThat(new RegexPiiDetector().detect("bob@example.com"))
            .allMatch(span -> span.kind() == SensitiveKind.PII);
        assertThat(new RegexSecretDetector().detect("AKIAIOSFODNN7EXAMPLE"))
            .allMatch(span -> span.kind() == SensitiveKind.SECRET);
    }

    @Test
    void testSecretSpanCarriesItsPatternScoreRatherThanFullConfidence() {
        List<SensitiveSpan> spans = new RegexSecretDetector().detect("aws AKIAIOSFODNN7EXAMPLE done");

        assertThat(spans).hasSize(1);

        double expected = SecretPatternCatalog.ALL.stream()
            .filter(secretPattern -> "AWS_ACCESS_KEY".equals(secretPattern.type()))
            .findFirst()
            .orElseThrow()
            .score();

        assertThat(spans.get(0)
            .confidence()).isEqualTo(expected);
    }

    @Test
    void testTokenizesPiiAndLeavesSecretsRedacted() {
        PiiTokenSession session = PiiTokenSession.create();

        String text = "mail bob@example.com about AKIAIOSFODNN7EXAMPLE";

        String tokenized = redactor.tokenizeWithSpans(text, BOTH, session, null)
            .text();

        assertThat(tokenized).contains("[PII_EMAIL_ADDRESS_1_" + session.sessionId() + "]");
        assertThat(tokenized).contains("[REDACTED_SECRET]");
        assertThat(tokenized).doesNotContain("bob@example.com");
        assertThat(tokenized).doesNotContain("AKIAIOSFODNN7EXAMPLE");
    }

    @Test
    void testTwoDifferentValuesBecomeTwoDifferentTokens() {
        PiiTokenSession session = PiiTokenSession.create();

        String tokenized = redactor
            .tokenizeWithSpans("forward bob@acme.io's note to alice@acme.io", BOTH, session, null)
            .text();

        assertThat(tokenized).contains("[PII_EMAIL_ADDRESS_1_" + session.sessionId() + "]");
        assertThat(tokenized).contains("[PII_EMAIL_ADDRESS_2_" + session.sessionId() + "]");
    }

    @Test
    void testTheSameValueTwiceBecomesTheSameToken() {
        PiiTokenSession session = PiiTokenSession.create();

        String tokenized = redactor
            .tokenizeWithSpans("bob@acme.io told bob@acme.io", BOTH, session, null)
            .text();

        assertThat(tokenized).isEqualTo(
            "[PII_EMAIL_ADDRESS_1_" + session.sessionId() + "] told [PII_EMAIL_ADDRESS_1_" + session.sessionId()
                + "]");
    }

    @Test
    void testTokenizingThenRestoringIsTheIdentityForPii() {
        PiiTokenSession session = PiiTokenSession.create();

        String text = "forward bob@acme.io's note to alice@acme.io";

        String restored = session.restore(
            redactor.tokenizeWithSpans(text, BOTH, session, null)
                .text());

        assertThat(restored).isEqualTo(text);
    }

    @Test
    void testSecretsDoNotSurviveTheRoundTrip() {
        PiiTokenSession session = PiiTokenSession.create();

        String text = "token AKIAIOSFODNN7EXAMPLE";

        String restored = session.restore(
            redactor.tokenizeWithSpans(text, BOTH, session, null)
                .text());

        assertThat(restored).isEqualTo("token [REDACTED_SECRET]");
        assertThat(restored).doesNotContain("AKIAIOSFODNN7EXAMPLE");
    }

    @Test
    void testBuiltInDetectorsRedactUnderTheEntityTypeName() {
        String redacted = redactor.redact("mail bob@acme.io", BOTH, null);

        assertThat(redacted).isEqualTo("mail [REDACTED_EMAIL_ADDRESS]");
    }

    @Test
    void testBuiltInDetectorsCoverInternationalIdentifiers() {
        String redacted = redactor.redact("ID S1234567D", BOTH, null);

        assertThat(redacted).contains("[REDACTED_SG_NRIC_FIN]");
    }
}
