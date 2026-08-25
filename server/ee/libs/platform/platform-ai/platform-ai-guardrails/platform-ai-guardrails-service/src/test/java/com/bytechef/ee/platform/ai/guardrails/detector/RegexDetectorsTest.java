/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.detector;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class RegexDetectorsTest {

    private static final Set<SensitiveKind> BOTH = EnumSet.allOf(SensitiveKind.class);

    private final SensitiveDataRedactor redactor = new SensitiveDataRedactor(SensitiveDataDetectors.builtIn());

    @Test
    void testDetectsEveryPiiCategory() {
        String redacted = redactor.redact(
            "Email me at jane.doe@example.com or call 415-555-0132. SSN 123-45-6789, card 4111 1111 1111 1111, " +
                "host 192.168.1.20.",
            BOTH, null);

        assertThat(redacted).contains("[REDACTED_EMAIL]");
        assertThat(redacted).contains("[REDACTED_SSN]");
        assertThat(redacted).contains("[REDACTED_CC]");
        assertThat(redacted).contains("[REDACTED_PHONE]");
        assertThat(redacted).contains("[REDACTED_IP]");
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
    void testLeavesCleanTextUnchanged() {
        String content = "Summarize the quarterly revenue report. The deployment succeeded.";

        assertThat(redactor.redact(content, BOTH, null)).isEqualTo(content);
    }

    /**
     * The bug this SPI was built to fix. The old sequential chain ran CREDIT_CARD before the secret patterns, so it
     * rewrote the digits the secret pattern needed and emitted "sk-proj-[REDACTED_CC]" -- disclosing that an OpenAI key
     * was present and leaking its prefix. Resolving spans against the original text redacts the whole secret.
     */
    @Test
    void testSecretContainingDigitRunIsRedactedWholeNotPartially() {
        assertThat(redactor.redact("sk-proj-1234567890123456", BOTH, null)).isEqualTo("[REDACTED_SECRET]");
        assertThat(redactor.redact("xoxb-1234567890123456-abcdef", BOTH, null)).isEqualTo("[REDACTED_SECRET]");
    }

    /**
     * Control cases: inputs with no PII/secret overlap must be byte-identical to what the old chain produced.
     */
    @Test
    void testNonOverlappingInputsMatchThePreSpiOutput() {
        assertThat(redactor.redact("card 4111 1111 1111 1111 ok", BOTH, null)).isEqualTo("card [REDACTED_CC] ok");
        assertThat(redactor.redact("mail me at bob@example.com please", BOTH, null))
            .isEqualTo("mail me at [REDACTED_EMAIL] please");
        assertThat(redactor.redact("contact sk-proj-abcdefghijklmnopqrstuvwx now", BOTH, null))
            .isEqualTo("contact [REDACTED_SECRET] now");
        assertThat(redactor.redact("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.abc123", BOTH, null))
            .isEqualTo("[REDACTED_SECRET]");
        assertThat(redactor.redact("bob@example.com called from 10.0.0.5 with 555-123-4567", BOTH, null))
            .isEqualTo("[REDACTED_EMAIL] called from [REDACTED_IP] with [REDACTED_PHONE]");
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
}
