/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.ai.sensitivedata.PiiPatternCatalog;
import com.bytechef.platform.ai.sensitivedata.PiiPatternCatalog.PiiPattern;
import com.bytechef.platform.ai.sensitivedata.SensitiveDataDetector;
import com.bytechef.platform.ai.sensitivedata.SensitiveDataDetectors;
import com.bytechef.platform.ai.sensitivedata.SensitiveDataRedactor;
import com.bytechef.platform.ai.sensitivedata.SensitiveKind;
import com.bytechef.platform.ai.sensitivedata.SensitiveSpan;
import com.bytechef.platform.ai.sensitivedata.tokenization.PiiTokenSession;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class StreamingResponseRedactorTest {

    private static final String AWS_KEY = "AKIAIOSFODNN7EXAMPLE";
    private static final String OPENAI_KEY = "sk-abcdefghij1234567890ABCD";

    private static final String CLEAN_SENTENCE =
        "The system processed a routine batch of records without incident and every check completed cleanly "
            + "within the expected time for this environment. ";
    private static final String CLEAN_TAIL = CLEAN_SENTENCE.repeat(6)
        .strip();

    private final SensitiveDataRedactor builtInRedactor = new SensitiveDataRedactor(SensitiveDataDetectors.builtIn());

    @Test
    void testShortCleanInputHeldUntilFlush() {
        StreamingResponseRedactor redactor = new StreamingResponseRedactor(builtInRedactor, 64);

        assertThat(redactor.push("hello ")).isEmpty();
        assertThat(redactor.push("world")).isEmpty();
        assertThat(redactor.flush()).isEqualTo("hello world");
    }

    @Test
    void testExplicitMinConfidenceIsHonoredNotHardcodedToTheDefault() {
        double aboveEmailAddressScore = scoreOf("EMAIL_ADDRESS") + 0.05;
        StreamingResponseRedactor redactor = new StreamingResponseRedactor(
            builtInRedactor, 8, aboveEmailAddressScore, null, null, EnumSet.allOf(SensitiveKind.class));

        String emitted = redactor.push("mail bob@acme.io " + CLEAN_TAIL) + redactor.flush();

        assertThat(emitted).contains("bob@acme.io");
        assertThat(emitted).doesNotContain("[REDACTED_EMAIL_ADDRESS]");
    }

    @Test
    void testFlushIsEmptyAfterDraining() {
        StreamingResponseRedactor redactor = new StreamingResponseRedactor(builtInRedactor, 8);

        redactor.push("some text");
        redactor.flush();

        assertThat(redactor.flush()).isEmpty();
    }

    @Test
    void testSecretWellBeforeBoundaryIsRedactedAndEmitted() {
        StreamingResponseRedactor redactor = new StreamingResponseRedactor(builtInRedactor, 32);

        StringBuilder emitted = new StringBuilder();

        emitted.append(redactor.push("token " + AWS_KEY + " and a long tail of clean words to push past window"));
        emitted.append(redactor.flush());

        assertThat(emitted.toString()).contains("[REDACTED_SECRET]");
        assertThat(emitted.toString()).doesNotContain(AWS_KEY);
    }

    @Test
    void testSecretSplitAcrossPushesIsNeverLeaked() {
        StreamingResponseRedactor redactor = new StreamingResponseRedactor(builtInRedactor, 32);

        List<String> emissions = new ArrayList<>();

        emissions.add(redactor.push("prefix AKIAIOSF"));
        emissions.add(redactor.push("ODNN7EXAMPLE trailing context that is long enough to move past the window"));
        emissions.add(redactor.flush());

        String all = String.join("", emissions);

        assertThat(all).contains("[REDACTED_SECRET]");
        assertThat(all).doesNotContain(AWS_KEY);

        for (String emission : emissions) {
            assertThat(emission).doesNotContain("AKIAIOSF");
        }
    }

    @Test
    void testCharByCharFeedNeverLeaksAndMatchesWholeBufferRedaction() {
        String full = "intro AKIAIOSFODNN7EXAMPLE middle " + OPENAI_KEY + " and some closing words here.";

        for (int window : new int[] {
            32, 64, 128
        }) {
            StreamingResponseRedactor redactor = new StreamingResponseRedactor(builtInRedactor, window);

            List<String> emissions = new ArrayList<>();

            for (int i = 0; i < full.length(); i++) {
                emissions.add(redactor.push(String.valueOf(full.charAt(i))));
            }

            emissions.add(redactor.flush());

            String reassembled = String.join("", emissions);

            assertThat(reassembled)
                .as("window=%d reassembled equals whole-buffer redaction", window)
                .isEqualTo(builtInRedactor.redact(full, EnumSet.allOf(SensitiveKind.class), null));

            for (String emission : emissions) {
                assertThat(emission)
                    .as("window=%d no emission leaks a raw secret", window)
                    .doesNotContain(AWS_KEY)
                    .doesNotContain(OPENAI_KEY);
            }
        }
    }

    @Test
    void testCleanStreamReassemblesUnchanged() {
        String full = "The quarterly report is ready and the dashboard has been refreshed for the whole team today.";

        StreamingResponseRedactor redactor = new StreamingResponseRedactor(builtInRedactor, 16);

        StringBuilder reassembled = new StringBuilder();

        for (String word : full.split(" ")) {
            reassembled.append(redactor.push(word + " "));
        }

        reassembled.append(redactor.flush());

        assertThat(reassembled.toString()).isEqualTo(full + " ");
    }

    @Test
    void testNullAndEmptyPushesAreNoOps() {
        StreamingResponseRedactor redactor = new StreamingResponseRedactor(builtInRedactor, 8);

        assertThat(redactor.push(null)).isEmpty();
        assertThat(redactor.push("")).isEmpty();
        assertThat(redactor.flush()).isEmpty();
    }

    @Test
    void testIsRedactedTrueAfterMaskingSecret() {
        StreamingResponseRedactor redactor = new StreamingResponseRedactor(builtInRedactor, 32);

        redactor.push("token " + AWS_KEY + " and a long tail of clean words to push past the window");
        redactor.flush();

        assertThat(redactor.isRedacted()).isTrue();
    }

    @Test
    void testIsRedactedFalseForCleanStream() {
        StreamingResponseRedactor redactor = new StreamingResponseRedactor(builtInRedactor, 16);

        redactor.push("all clean text here with nothing sensitive at all across this stream of words");
        redactor.flush();

        assertThat(redactor.isRedacted()).isFalse();
    }

    @Test
    void testStreamedOutputEqualsWholeTextRedaction() {
        SensitiveDataRedactor sensitiveDataRedactor = new SensitiveDataRedactor(SensitiveDataDetectors.builtIn());

        List<String> corpus = List.of(
            "contact bob@example.com about AKIAIOSFODNN7EXAMPLE today " + CLEAN_TAIL,
            "token sk-proj-abcdefghijklmnopqrstuvwx and card 4111 1111 1111 1111 " + CLEAN_TAIL,
            "no sensitive content whatsoever in this sentence " + CLEAN_TAIL,
            "xoxb-1234567890123456-abcdef trailing text " + CLEAN_TAIL,
            "ip 10.0.0.5 phone 555-123-4567 ssn 123-45-6789 " + CLEAN_TAIL);

        for (String text : corpus) {
            String expected = sensitiveDataRedactor.redact(text, EnumSet.allOf(SensitiveKind.class), null);

            for (int window : new int[] {
                64, 128, 512
            }) {
                for (int chunkSize : new int[] {
                    1, 3, 7, 100
                }) {
                    StreamingResponseRedactor redactor =
                        new StreamingResponseRedactor(sensitiveDataRedactor, window);
                    StringBuilder emitted = new StringBuilder();
                    boolean pushEmittedSomething = false;

                    for (int index = 0; index < text.length(); index += chunkSize) {
                        String pushed =
                            redactor.push(text.substring(index, Math.min(index + chunkSize, text.length())));

                        if (!pushed.isEmpty()) {
                            pushEmittedSomething = true;
                        }

                        emitted.append(pushed);
                    }

                    emitted.append(redactor.flush());

                    assertThat(pushEmittedSomething)
                        .as(
                            "text=%s window=%d chunkSize=%d: no push() call emitted anything, so this combination "
                                + "never exercised the streaming cut logic",
                            text, window, chunkSize)
                        .isTrue();

                    assertThat(emitted.toString())
                        .as("text=%s window=%d chunkSize=%d", text, window, chunkSize)
                        .isEqualTo(expected);
                }
            }
        }
    }

    @Test
    void testValueLongerThanTheWindowMayHaveAPrefixEmitted() {
        String text = "contact bob@example.com about " + AWS_KEY + " today";

        StreamingResponseRedactor redactor = new StreamingResponseRedactor(builtInRedactor, 8);
        StringBuilder emitted = new StringBuilder();

        for (int index = 0; index < text.length(); index++) {
            emitted.append(redactor.push(String.valueOf(text.charAt(index))));
        }

        emitted.append(redactor.flush());

        String wholeTextRedaction = builtInRedactor.redact(text, EnumSet.allOf(SensitiveKind.class), null);

        assertThat(emitted.toString()).isNotEqualTo(wholeTextRedaction);
        assertThat(emitted.toString()).contains(AWS_KEY);
    }

    @Test
    void testNonStreamSafeDetectorDoesNotContributeToTheStream() {
        SensitiveDataDetector contextual = new SensitiveDataDetector() {

            @Override
            public String name() {
                return "contextual";
            }

            @Override
            public List<SensitiveSpan> detect(String text) {
                int index = text.indexOf("Ada");

                if (index < 0) {
                    return List.of();
                }

                return List.of(SensitiveSpan.of(SensitiveKind.PII, "PERSON", index, index + 3));
            }

            @Override
            public boolean streamSafe() {
                return false;
            }
        };

        SensitiveDataRedactor fullRedactor = new SensitiveDataRedactor(List.of(contextual));

        assertThat(fullRedactor.redact("Ada wrote it", EnumSet.allOf(SensitiveKind.class), null))
            .isEqualTo("[REDACTED_PERSON] wrote it");

        StreamingResponseRedactor redactor = new StreamingResponseRedactor(fullRedactor.streamSafeView(), 4);

        String emitted = redactor.push("Ada wrote it") + redactor.flush();

        assertThat(emitted).isEqualTo("Ada wrote it");
    }

    @Test
    void testDetectorFailureDuringStreamingIsRecorded() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AiGuardrailMetrics metrics = new AiGuardrailMetrics(meterRegistry, "ai_agent");

        SensitiveDataRedactor failingRedactor = new SensitiveDataRedactor(List.of(throwingDetector()));
        StreamingResponseRedactor redactor = new StreamingResponseRedactor(failingRedactor, 8, metrics);

        String emitted = redactor.push("contact bob@example.com about the outage today") + redactor.flush();

        assertThat(emitted).isEqualTo("contact bob@example.com about the outage today");
        assertThat(
            meterRegistry.counter(AiGuardrailMetrics.COUNTER_NAME, "event", "detector_failed", "surface", "ai_agent")
                .count()).isGreaterThan(0.0);
    }

    @Test
    void testATokenIsNeverSplitAcrossEmittedChunks() {
        SensitiveDataRedactor redactor = new SensitiveDataRedactor(SensitiveDataDetectors.builtIn());
        PiiTokenSession session = PiiTokenSession.create();

        String token = session.tokenFor("EMAIL", "bob@acme.io");
        String text = "please contact " + token + " about the outage as soon as you can today";

        StreamingResponseRedactor streamingRedactor = new StreamingResponseRedactor(redactor, 32, session);

        StringBuilder emitted = new StringBuilder();

        for (int index = 0; index < text.length(); index++) {
            emitted.append(streamingRedactor.push(text.substring(index, index + 1)));
        }

        emitted.append(streamingRedactor.flush());

        assertThat(emitted.toString()).isEqualTo(
            "please contact bob@acme.io about the outage as soon as you can today");
    }

    @Test
    void testRestorationRecordsPiiRestoredMetric() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AiGuardrailMetrics metrics = new AiGuardrailMetrics(meterRegistry, "ai_agent");

        SensitiveDataRedactor redactor = new SensitiveDataRedactor(SensitiveDataDetectors.builtIn());
        PiiTokenSession session = PiiTokenSession.create();
        String token = session.tokenFor("EMAIL", "bob@acme.io");

        StreamingResponseRedactor streamingRedactor = new StreamingResponseRedactor(redactor, metrics, session);

        String emitted = streamingRedactor.push("contact " + token + " today") + streamingRedactor.flush();

        assertThat(emitted).isEqualTo("contact bob@acme.io today");
        assertThat(
            meterRegistry.counter(AiGuardrailMetrics.COUNTER_NAME, "event", "pii_restored", "surface", "ai_agent")
                .count()).isGreaterThan(0.0);
        assertThat(
            meterRegistry.find(AiGuardrailMetrics.COUNTER_NAME)
                .tag("event", "token_unresolved")
                .counter())
                    .as("no token was unresolved, so this counter should never have been touched")
                    .isNull();
    }

    @Test
    void testRestorationRecordsTokenUnresolvedMetricForForeignToken() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AiGuardrailMetrics metrics = new AiGuardrailMetrics(meterRegistry, "ai_agent");

        SensitiveDataRedactor redactor = new SensitiveDataRedactor(SensitiveDataDetectors.builtIn());
        PiiTokenSession session = PiiTokenSession.create();

        session.tokenFor("EMAIL", "known@acme.io");

        PiiTokenSession foreignSession = PiiTokenSession.create();
        String foreignToken = foreignSession.tokenFor("EMAIL", "someone@else.example.com");

        StreamingResponseRedactor streamingRedactor = new StreamingResponseRedactor(redactor, metrics, session);

        String emitted = streamingRedactor.push("contact " + foreignToken + " today") + streamingRedactor.flush();

        assertThat(emitted).isEqualTo("contact " + foreignToken + " today");
        assertThat(
            meterRegistry.counter(AiGuardrailMetrics.COUNTER_NAME, "event", "token_unresolved", "surface", "ai_agent")
                .count()).isGreaterThan(0.0);
    }

    @Test
    void testTokenRestoredWithoutScanningAndNeverSplitAcrossChunks() {
        SensitiveDataRedactor redactor = new SensitiveDataRedactor(SensitiveDataDetectors.builtIn());
        PiiTokenSession session = PiiTokenSession.create();

        String token = session.tokenFor("EMAIL", "bob@acme.io");
        String text = "please contact " + token + " about jane.doe@example.com and the outage today";

        StreamingResponseRedactor streamingRedactor =
            new StreamingResponseRedactor(redactor, 32, null, session, EnumSet.noneOf(SensitiveKind.class));

        StringBuilder emitted = new StringBuilder();

        for (int index = 0; index < text.length(); index++) {
            emitted.append(streamingRedactor.push(text.substring(index, index + 1)));
        }

        emitted.append(streamingRedactor.flush());

        assertThat(emitted.toString()).contains("bob@acme.io");
        assertThat(emitted.toString()).doesNotContain(token);
        assertThat(emitted.toString()).contains("jane.doe@example.com");
        assertThat(emitted.toString()).doesNotContain("[REDACTED_EMAIL]");
    }

    private static double scoreOf(String type) {
        for (PiiPattern pattern : PiiPatternCatalog.ALL) {
            if (pattern.type()
                .equals(type)) {

                return pattern.score();
            }
        }

        throw new IllegalArgumentException("no catalog entry for " + type);
    }

    private static SensitiveDataDetector throwingDetector() {
        return new SensitiveDataDetector() {

            @Override
            public String name() {
                return "broken";
            }

            @Override
            public List<SensitiveSpan> detect(String text) {
                throw new IllegalStateException("detector is broken");
            }
        };
    }
}
