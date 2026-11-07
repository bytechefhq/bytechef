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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author Ivica Cardic
 */
public final class Pcm16Resampler {

    private Pcm16Resampler() {
    }

    public static byte[] resample(byte[] pcm16LittleEndian, int fromHz, int toHz) {
        if (fromHz == toHz) {
            return pcm16LittleEndian;
        }

        int inputSampleCount = pcm16LittleEndian.length / 2;

        if (inputSampleCount == 0) {
            return new byte[0];
        }

        short[] input = new short[inputSampleCount];

        ByteBuffer.wrap(pcm16LittleEndian, 0, inputSampleCount * 2)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
            .get(input);

        int outputSampleCount = (int) ((long) inputSampleCount * toHz / fromHz);
        ByteBuffer output = ByteBuffer.allocate(outputSampleCount * 2)
            .order(ByteOrder.LITTLE_ENDIAN);

        for (int outputIndex = 0; outputIndex < outputSampleCount; outputIndex++) {
            double position = (double) outputIndex * fromHz / toHz;
            int leftIndex = (int) position;
            int rightIndex = Math.min(leftIndex + 1, inputSampleCount - 1);
            double fraction = position - leftIndex;

            double sample = input[leftIndex] * (1.0 - fraction) + input[rightIndex] * fraction;

            output.putShort((short) Math.round(sample));
        }

        return output.array();
    }
}
