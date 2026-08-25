/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.detector;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class SensitiveDataRedactorTest {

    private static final Set<SensitiveKind> BOTH = EnumSet.allOf(SensitiveKind.class);

    @Test
    void testAppliesNonOverlappingSpansLeftToRightInResult() {
        SensitiveDataRedactor redactor = redactor(
            fixed("pii", true,
                SensitiveSpan.of(SensitiveKind.PII, "EMAIL", 0, 3),
                SensitiveSpan.of(SensitiveKind.PII, "IP", 4, 7)));

        assertThat(redactor.redact("abc def", BOTH, null)).isEqualTo("[REDACTED_EMAIL] [REDACTED_IP]");
    }

    @Test
    void testSecretBeatsOverlappingPii() {
        SensitiveDataRedactor redactor = redactor(
            fixed("pii", true, SensitiveSpan.of(SensitiveKind.PII, "CC", 5, 21)),
            fixed("secret", true, SensitiveSpan.of(SensitiveKind.SECRET, "SECRET", 0, 28)));

        assertThat(redactor.redact("xoxb-1234567890123456-abcdef", BOTH, null)).isEqualTo("[REDACTED_SECRET]");
    }

    @Test
    void testLongerSpanBeatsNestedSpanOfTheSameKind() {
        SensitiveDataRedactor redactor = redactor(
            fixed("outer", true, SensitiveSpan.of(SensitiveKind.SECRET, "SECRET", 0, 10)),
            fixed("inner", true, SensitiveSpan.of(SensitiveKind.SECRET, "SECRET", 3, 6)));

        assertThat(redactor.redact("0123456789", BOTH, null)).isEqualTo("[REDACTED_SECRET]");
    }

    @Test
    void testLongerSpanBeatsPartiallyOverlappingShorterSpanOfTheSameKind() {
        SensitiveDataRedactor redactor = redactor(
            fixed("early", true, SensitiveSpan.of(SensitiveKind.SECRET, "SECRET", 0, 10)),
            fixed("longer", true, SensitiveSpan.of(SensitiveKind.SECRET, "SECRET", 5, 20)));

        // Length wins over position: the later-but-longer span is accepted, the earlier one dropped.
        assertThat(redactor.redact("0123456789abcdefghij", BOTH, null)).isEqualTo("01234[REDACTED_SECRET]");
    }

    @Test
    void testEarlierStartWinsWhenKindAndLengthTieAndSpansOverlap() {
        SensitiveDataRedactor redactor = redactor(
            fixed("later", true, SensitiveSpan.of(SensitiveKind.SECRET, "SECRET", 5, 15)),
            fixed("earlier", true, SensitiveSpan.of(SensitiveKind.SECRET, "SECRET", 0, 10)));

        // Same kind, same length (10), overlapping: position wins and the earlier-starting span is accepted.
        assertThat(redactor.redact("0123456789abcde", BOTH, null)).isEqualTo("[REDACTED_SECRET]abcde");
    }

    @Test
    void testEarlierCategoryWinsWhenKindLengthAndStartAllTie() {
        SensitiveDataRedactor redactor = redactor(
            fixed("beta", true, SensitiveSpan.of(SensitiveKind.SECRET, "BETA", 0, 5)),
            fixed("alpha", true, SensitiveSpan.of(SensitiveKind.SECRET, "ALPHA", 0, 5)));

        // Same kind, same length, same start -- the two spans overlap exactly, so only one can be accepted.
        // The alphabetically earlier category wins.
        assertThat(redactor.redact("01234", BOTH, null)).isEqualTo("[REDACTED_ALPHA]");
    }

    @Test
    void testTouchingSpansBothSurvive() {
        SensitiveDataRedactor redactor = redactor(
            fixed("pii", true,
                SensitiveSpan.of(SensitiveKind.PII, "IP", 0, 5),
                SensitiveSpan.of(SensitiveKind.PII, "IP", 5, 10)));

        assertThat(redactor.redact("0123456789", BOTH, null)).isEqualTo("[REDACTED_IP][REDACTED_IP]");
    }

    @Test
    void testResultIsIndependentOfDetectorOrder() {
        SensitiveDataDetector piiDetector = fixed(
            "pii", true, SensitiveSpan.of(SensitiveKind.PII, "CC", 5, 21));
        SensitiveDataDetector secretDetector = fixed(
            "secret", true, SensitiveSpan.of(SensitiveKind.SECRET, "SECRET", 0, 28));

        String text = "xoxb-1234567890123456-abcdef";

        List<SensitiveDataDetector> detectors = new ArrayList<>(List.of(piiDetector, secretDetector));
        String expected = new SensitiveDataRedactor(detectors).redact(text, BOTH, null);

        for (int attempt = 0; attempt < 20; attempt++) {
            Collections.shuffle(detectors);

            assertThat(new SensitiveDataRedactor(detectors).redact(text, BOTH, null)).isEqualTo(expected);
        }
    }

    @Test
    void testKindFilterIsAppliedBeforeResolutionSoAPiiOnlyCallStillRedacts() {
        // The SECRET span would win the overlap, but a PII-only caller never sees it -- filtering candidates before
        // resolution is what keeps single-toggle output identical to the pre-SPI engine. Filtering after resolution
        // would leave this text untouched, which is a redaction regression.
        SensitiveDataRedactor redactor = redactor(
            fixed("pii", true, SensitiveSpan.of(SensitiveKind.PII, "CC", 5, 21)),
            fixed("secret", true, SensitiveSpan.of(SensitiveKind.SECRET, "SECRET", 0, 28)));

        assertThat(redactor.redact("xoxb-1234567890123456-abcdef", EnumSet.of(SensitiveKind.PII), null))
            .isEqualTo("xoxb-[REDACTED_CC]-abcdef");
    }

    @Test
    void testFailingDetectorIsSkippedAndOthersStillApply() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AiGuardrailMetrics metrics = new AiGuardrailMetrics(meterRegistry, "ai_hub");

        SensitiveDataRedactor redactor = redactor(
            throwing("broken"),
            fixed("pii", true, SensitiveSpan.of(SensitiveKind.PII, "EMAIL", 0, 3)));

        assertThat(redactor.redact("abc def", BOTH, metrics)).isEqualTo("[REDACTED_EMAIL] def");

        Counter counter = meterRegistry.find(AiGuardrailMetrics.COUNTER_NAME)
            .tag("event", "detector_failed")
            .tag("surface", "ai_hub")
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void testFailingDetectorWithoutMetricsDoesNotThrow() {
        SensitiveDataRedactor redactor = redactor(throwing("broken"));

        assertThat(redactor.redact("abc def", BOTH, null)).isEqualTo("abc def");
    }

    @Test
    void testSpanBeyondTextIsTreatedAsDetectorFailure() {
        SensitiveDataRedactor redactor = redactor(
            fixed("rogue", true, SensitiveSpan.of(SensitiveKind.PII, "EMAIL", 0, 500)),
            fixed("pii", true, SensitiveSpan.of(SensitiveKind.PII, "IP", 4, 7)));

        assertThat(redactor.redact("abc def", BOTH, null)).isEqualTo("abc [REDACTED_IP]");
    }

    @Test
    void testDetectorReportingOneOutOfBoundsSpanDiscardsAllOfItsSpans() {
        SensitiveDataRedactor redactor = redactor(
            fixed("mixed", true,
                SensitiveSpan.of(SensitiveKind.PII, "EMAIL", 0, 3),
                SensitiveSpan.of(SensitiveKind.PII, "IP", 0, 500)),
            fixed("valid", true, SensitiveSpan.of(SensitiveKind.PII, "SSN", 4, 7)));

        // The "mixed" detector reports one valid span (EMAIL) alongside one out-of-bounds span (IP). collectSpans
        // validates a detector's whole batch before adding any of it, so BOTH of "mixed"'s spans are discarded --
        // including the otherwise-valid EMAIL one. The "valid" detector's span is a separate detector and still
        // applies.
        assertThat(redactor.redact("abc def", BOTH, null)).isEqualTo("abc [REDACTED_SSN]");
    }

    @Test
    void testStreamSafeViewExcludesNonStreamSafeDetectors() {
        SensitiveDataRedactor redactor = redactor(
            fixed("local", true, SensitiveSpan.of(SensitiveKind.PII, "IP", 4, 7)),
            fixed("contextual", false, SensitiveSpan.of(SensitiveKind.PII, "PERSON", 0, 3)));

        assertThat(redactor.redact("abc def", BOTH, null)).isEqualTo("[REDACTED_PERSON] [REDACTED_IP]");
        assertThat(redactor.streamSafeView()
            .redact("abc def", BOTH, null)).isEqualTo("abc [REDACTED_IP]");
    }

    @Test
    void testEmptyTextIsReturnedUnchanged() {
        SensitiveDataRedactor redactor = redactor(
            fixed("pii", true, SensitiveSpan.of(SensitiveKind.PII, "EMAIL", 0, 3)));

        assertThat(redactor.redact("", BOTH, null)).isEmpty();
    }

    @Test
    void testEmptyKindSetRedactsNothing() {
        SensitiveDataRedactor redactor = redactor(
            fixed("pii", true, SensitiveSpan.of(SensitiveKind.PII, "EMAIL", 0, 3)));

        assertThat(redactor.redact("abc def", EnumSet.noneOf(SensitiveKind.class), null)).isEqualTo("abc def");
    }

    private static SensitiveDataRedactor redactor(SensitiveDataDetector... detectors) {
        return new SensitiveDataRedactor(List.of(detectors));
    }

    private static SensitiveDataDetector fixed(String name, boolean streamSafe, SensitiveSpan... spans) {
        return new SensitiveDataDetector() {

            @Override
            public String name() {
                return name;
            }

            @Override
            public List<SensitiveSpan> detect(String text) {
                return List.of(spans);
            }

            @Override
            public boolean streamSafe() {
                return streamSafe;
            }
        };
    }

    private static SensitiveDataDetector throwing(String name) {
        return new SensitiveDataDetector() {

            @Override
            public String name() {
                return name;
            }

            @Override
            public List<SensitiveSpan> detect(String text) {
                throw new IllegalStateException("detector is broken");
            }
        };
    }
}
