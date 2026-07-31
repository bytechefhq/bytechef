/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
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

    @Test
    void testShortCleanInputHeldUntilFlush() {
        StreamingResponseRedactor redactor = new StreamingResponseRedactor(64);

        assertThat(redactor.push("hello ")).isEmpty();
        assertThat(redactor.push("world")).isEmpty();
        assertThat(redactor.flush()).isEqualTo("hello world");
    }

    @Test
    void testFlushIsEmptyAfterDraining() {
        StreamingResponseRedactor redactor = new StreamingResponseRedactor(8);

        redactor.push("some text");
        redactor.flush();

        assertThat(redactor.flush()).isEmpty();
    }

    @Test
    void testSecretWellBeforeBoundaryIsRedactedAndEmitted() {
        StreamingResponseRedactor redactor = new StreamingResponseRedactor(32);

        StringBuilder emitted = new StringBuilder();

        emitted.append(redactor.push("token " + AWS_KEY + " and a long tail of clean words to push past window"));
        emitted.append(redactor.flush());

        assertThat(emitted.toString()).contains("[REDACTED_SECRET]");
        assertThat(emitted.toString()).doesNotContain(AWS_KEY);
    }

    @Test
    void testSecretSplitAcrossPushesIsNeverLeaked() {
        StreamingResponseRedactor redactor = new StreamingResponseRedactor(32);

        List<String> emissions = new ArrayList<>();

        // Feed the AWS key split down the middle across two pushes, then trailing context.
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

        // The window must exceed the longest token (OpenAI key, 27 chars) for the no-leak guarantee to hold.
        for (int window : new int[] {
            32, 64, 128
        }) {
            StreamingResponseRedactor redactor = new StreamingResponseRedactor(window);

            List<String> emissions = new ArrayList<>();

            for (int i = 0; i < full.length(); i++) {
                emissions.add(redactor.push(String.valueOf(full.charAt(i))));
            }

            emissions.add(redactor.flush());

            String reassembled = String.join("", emissions);

            assertThat(reassembled)
                .as("window=%d reassembled equals whole-buffer redaction", window)
                .isEqualTo(AiGuardrails.redactAll(full));

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

        StreamingResponseRedactor redactor = new StreamingResponseRedactor(16);

        StringBuilder reassembled = new StringBuilder();

        for (String word : full.split(" ")) {
            reassembled.append(redactor.push(word + " "));
        }

        reassembled.append(redactor.flush());

        assertThat(reassembled.toString()).isEqualTo(full + " ");
    }

    @Test
    void testNullAndEmptyPushesAreNoOps() {
        StreamingResponseRedactor redactor = new StreamingResponseRedactor(8);

        assertThat(redactor.push(null)).isEmpty();
        assertThat(redactor.push("")).isEmpty();
        assertThat(redactor.flush()).isEmpty();
    }

    @Test
    void testIsRedactedTrueAfterMaskingSecret() {
        StreamingResponseRedactor redactor = new StreamingResponseRedactor(32);

        redactor.push("token " + AWS_KEY + " and a long tail of clean words to push past the window");
        redactor.flush();

        assertThat(redactor.isRedacted()).isTrue();
    }

    @Test
    void testIsRedactedFalseForCleanStream() {
        StreamingResponseRedactor redactor = new StreamingResponseRedactor(16);

        redactor.push("all clean text here with nothing sensitive at all across this stream of words");
        redactor.flush();

        assertThat(redactor.isRedacted()).isFalse();
    }
}
