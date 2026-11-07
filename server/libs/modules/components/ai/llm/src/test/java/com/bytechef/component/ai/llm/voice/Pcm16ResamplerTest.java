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

package com.bytechef.component.ai.llm.voice;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

class Pcm16ResamplerTest {

    @Test
    void testSameRateReturnsInputUnchanged() {
        byte[] input = pcm(1000, -1000, 500);

        assertThat(Pcm16Resampler.resample(input, 16000, 16000)).isSameAs(input);
    }

    @Test
    void testUpsamplingDoublesSampleCount() {
        byte[] input = pcm(0, 1000, 2000, 3000);

        byte[] output = Pcm16Resampler.resample(input, 8000, 16000);

        assertThat(output).hasSize(input.length * 2);
        assertThat(samples(output)).startsWith((short) 0, (short) 500, (short) 1000, (short) 1500);
    }

    @Test
    void testDownsamplingHalvesSampleCount() {
        byte[] input = pcm(0, 1000, 2000, 3000, 4000, 5000);

        byte[] output = Pcm16Resampler.resample(input, 16000, 8000);

        assertThat(output).hasSize(input.length / 2);
        assertThat(samples(output)).containsExactly((short) 0, (short) 2000, (short) 4000);
    }

    @Test
    void testInputWithoutAWholeSampleResamplesToNothing() {
        assertThat(Pcm16Resampler.resample(new byte[0], 16000, 24000)).isEmpty();
        assertThat(Pcm16Resampler.resample(new byte[] {
            7
        }, 16000, 24000)).isEmpty();
    }

    @Test
    void testOddByteCountIsTruncatedToWholeSamples() {
        byte[] output = Pcm16Resampler.resample(new byte[] {
            1, 2, 3
        }, 8000, 16000);

        assertThat(output.length % 2).isZero();
    }

    private static byte[] pcm(int... values) {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * 2)
            .order(ByteOrder.LITTLE_ENDIAN);

        for (int value : values) {
            buffer.putShort((short) value);
        }

        return buffer.array();
    }

    private static short[] samples(byte[] pcm16) {
        short[] samples = new short[pcm16.length / 2];

        ByteBuffer.wrap(pcm16)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
            .get(samples);

        return samples;
    }
}
