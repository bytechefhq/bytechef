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

package com.bytechef.platform.component.runner.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class BoundedOutputCaptureTest {

    @Test
    void testShortOutputIsReturnedVerbatim() {
        BoundedOutputCapture capture = new BoundedOutputCapture();

        append(capture, "hello world");

        assertThat(capture.get()).isEqualTo("hello world");
        assertThat(capture.getTotalLength()).isEqualTo(11);
    }

    @Test
    void testOutputExactlyAtTheLimitIsReturnedVerbatim() {
        BoundedOutputCapture capture = new BoundedOutputCapture();

        String text = "a".repeat(64 * 1024);

        append(capture, text);

        assertThat(capture.get()).isEqualTo(text);
    }

    @Test
    void testLongOutputKeepsBothEndsAndElidesTheMiddle() {
        BoundedOutputCapture capture = new BoundedOutputCapture();

        append(capture, "HEAD" + "x".repeat(200 * 1024) + "TAIL");

        String captured = capture.get();

        assertThat(captured).startsWith("HEAD");
        assertThat(captured).endsWith("TAIL");
        assertThat(captured).contains("elided");
        assertThat(captured.length()).isLessThan(70 * 1024);
        assertThat(capture.getTotalLength()).isEqualTo(200 * 1024 + 8);
    }

    @Test
    void testManySmallAppendsBehaveLikeOneLargeOne() {
        BoundedOutputCapture chunked = new BoundedOutputCapture();
        BoundedOutputCapture whole = new BoundedOutputCapture();

        String text = "HEAD" + "y".repeat(200 * 1024) + "TAIL";

        for (int index = 0; index < text.length(); index += 997) {
            append(chunked, text.substring(index, Math.min(index + 997, text.length())));
        }

        append(whole, text);

        assertThat(chunked.get()).isEqualTo(whole.get());
    }

    @Test
    void testASizedCaptureKeepsHalfAtEachEnd() {
        BoundedOutputCapture capture = new BoundedOutputCapture(8);

        byte[] bytes = "0123456789ABCDEF".getBytes(StandardCharsets.UTF_8);

        capture.append(bytes, bytes.length);

        String text = capture.get();

        assertThat(text).startsWith("0123");
        assertThat(text).endsWith("CDEF");
        assertThat(text).contains("8 bytes elided");
    }

    @Test
    void testACeilingBelowTwoBytesIsRejected() {
        assertThatThrownBy(() -> new BoundedOutputCapture(1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testEmptyCaptureIsAnEmptyString() {
        assertThat(new BoundedOutputCapture().get()).isEmpty();
    }

    @Test
    void testMultiByteCharacterAtTheSeamIsNotCorrupted() {
        BoundedOutputCapture capture = new BoundedOutputCapture();

        String text = "a".repeat(32 * 1024 - 2) + "€" + "TAIL";

        append(capture, text);

        assertThat(capture.get()).isEqualTo(text);
    }

    @Test
    void testPartialTailFillPreservesOrder() {
        BoundedOutputCapture capture = new BoundedOutputCapture();

        String text = "HEAD" + "x".repeat(32 * 1024 + 100 - 8) + "TAIL";

        append(capture, text);

        assertThat(capture.get()).isEqualTo(text);
    }

    private static void append(BoundedOutputCapture capture, String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        capture.append(bytes, bytes.length);
    }
}
