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

package com.bytechef.platform.webhook.web.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class TwilioAudioCodecTest {

    @Test
    void testMuLawDecodeIsStableUnderReEncodeForEveryByte() {
        // Decoding to linear and re-encoding must land on a byte that decodes to the SAME linear value. This holds for
        // all 256 codes; exact byte equality does not, because mu-law has two codes for zero (0x7F and 0xFF) and encode
        // canonicalizes zero to 0xFF.
        for (int code = 0; code < 256; code++) {
            byte muLaw = (byte) code;

            short linear = TwilioAudioCodec.muLawToLinear(muLaw);
            byte reEncoded = TwilioAudioCodec.linearToMuLaw(linear);

            assertThat(TwilioAudioCodec.muLawToLinear(reEncoded)).isEqualTo(linear);

            if (code != 0x7F) {
                assertThat(reEncoded).isEqualTo(muLaw);
            }
        }
    }

    @Test
    void testMuLawDecodeIsMonotonicAcrossPositiveMagnitudes() {
        // Larger positive mu-law magnitudes decode to larger linear magnitudes.
        int previous = Integer.MIN_VALUE;

        for (int mantissaCode = 0; mantissaCode <= 0x0F; mantissaCode++) {
            // Positive samples: sign bit set in the (complemented) byte -> use codes 0xFF down to 0xF0.
            short linear = TwilioAudioCodec.muLawToLinear((byte) (0xFF - mantissaCode));

            assertThat(linear).isGreaterThanOrEqualTo((short) 0);
            assertThat((int) linear).isGreaterThan(previous);

            previous = linear;
        }
    }

    @Test
    void testPcm16MuLawRoundTripApproximatesLoudSamples() {
        // For a loud sample well above the mu-law quantization floor, encode->decode stays within a small relative
        // error.
        short original = 12000;

        byte muLaw = TwilioAudioCodec.linearToMuLaw(original);
        short decoded = TwilioAudioCodec.muLawToLinear(muLaw);

        assertThat(Math.abs(decoded - original)).isLessThan(400);
    }

    @Test
    void testMuLawToPcm16ProducesTwoBytesPerSample() {
        byte[] pcm = TwilioAudioCodec.muLawToPcm16(new byte[] {
            (byte) 0xFF, (byte) 0x00, 0x7F
        });

        assertThat(pcm).hasSize(6);
    }

    @Test
    void testPcm16ToMuLawProducesOneBytePerSample() {
        byte[] muLaw = TwilioAudioCodec.pcm16ToMuLaw(new byte[] {
            0x00, 0x00, 0x10, 0x20, 0x00, (byte) 0x80
        });

        assertThat(muLaw).hasSize(3);
    }

    @Test
    void testResampleReturnsSameArrayWhenRatesMatch() {
        byte[] pcm = {
            1, 2, 3, 4
        };

        assertThat(TwilioAudioCodec.resamplePcm16(pcm, 8000, 8000)).isSameAs(pcm);
    }

    @Test
    void testResampleUpsamplesSampleCountByRatio() {
        // 4 samples (8 bytes) at 8 kHz -> 8 samples (16 bytes) at 16 kHz.
        byte[] pcm = new byte[8];

        byte[] upsampled = TwilioAudioCodec.resamplePcm16(pcm, 8000, 16000);

        assertThat(upsampled).hasSize(16);
    }

    @Test
    void testResampleDownsamplesSampleCountByRatio() {
        // 6 samples (12 bytes) at 24 kHz -> 2 samples (4 bytes) at 8 kHz.
        byte[] pcm = new byte[12];

        byte[] downsampled = TwilioAudioCodec.resamplePcm16(pcm, 24000, 8000);

        assertThat(downsampled).hasSize(4);
    }

    @Test
    void testResampleReturnsEmptyForEmptyInput() {
        assertThat(TwilioAudioCodec.resamplePcm16(new byte[0], 8000, 16000)).isEmpty();
    }
}
