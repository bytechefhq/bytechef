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

/**
 * Audio transcoding between Twilio's telephony format (G.711 &micro;-law, 8&nbsp;kHz, mono) and signed 16-bit
 * little-endian linear PCM used by the realtime voice components. Lets a linear-PCM pipeline (e.g. the browser
 * {@code deepgram/v1/voiceAgent} example configured for {@code linear16}) serve Twilio phone calls without the
 * component having to be reconfigured for &micro;-law.
 *
 * <p>
 * Pure and side-effect-free so the &micro;-law math and resampling can be unit-tested without a live call.
 *
 * @author Ivica Cardic
 */
public final class TwilioAudioCodec {

    private static final int BIAS = 0x84;
    private static final int CLIP = 32635;
    private static final int SIGN_BIT = 0x80;
    private static final int[] DECODE_EXPONENT = {
        0, 132, 396, 924, 1980, 4092, 8316, 16764
    };

    private TwilioAudioCodec() {
    }

    /**
     * Decodes a G.711 &micro;-law byte into a signed 16-bit linear PCM sample.
     */
    static short muLawToLinear(byte muLawByte) {
        int value = ~muLawByte & 0xFF;
        int sign = value & SIGN_BIT;
        int exponent = (value >> 4) & 0x07;
        int mantissa = value & 0x0F;
        int sample = DECODE_EXPONENT[exponent] + (mantissa << (exponent + 3));

        return (short) (sign != 0 ? -sample : sample);
    }

    /**
     * Encodes a signed 16-bit linear PCM sample into a G.711 &micro;-law byte.
     */
    static byte linearToMuLaw(short pcm) {
        int sample = pcm;
        int sign = (sample >> 8) & SIGN_BIT;

        if (sign != 0) {
            sample = -sample;
        }

        if (sample > CLIP) {
            sample = CLIP;
        }

        sample += BIAS;

        int exponent = 7;
        int mask = 0x4000;

        while (exponent > 0 && (sample & mask) == 0) {
            exponent--;
            mask >>= 1;
        }

        int mantissa = (sample >> (exponent + 3)) & 0x0F;

        return (byte) ~(sign | (exponent << 4) | mantissa);
    }

    /**
     * Decodes a &micro;-law byte stream into signed 16-bit little-endian PCM bytes (2 bytes per sample).
     */
    static byte[] muLawToPcm16(byte[] muLaw) {
        byte[] pcm = new byte[muLaw.length * 2];

        for (int i = 0; i < muLaw.length; i++) {
            short sample = muLawToLinear(muLaw[i]);

            pcm[i * 2] = (byte) (sample & 0xFF);
            pcm[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }

        return pcm;
    }

    /**
     * Encodes signed 16-bit little-endian PCM bytes into a &micro;-law byte stream (1 byte per sample). A trailing odd
     * byte, if any, is ignored.
     */
    static byte[] pcm16ToMuLaw(byte[] pcm16LittleEndian) {
        int sampleCount = pcm16LittleEndian.length / 2;
        byte[] muLaw = new byte[sampleCount];

        for (int i = 0; i < sampleCount; i++) {
            short sample = (short) ((pcm16LittleEndian[i * 2] & 0xFF) | (pcm16LittleEndian[i * 2 + 1] << 8));

            muLaw[i] = linearToMuLaw(sample);
        }

        return muLaw;
    }

    /**
     * Resamples signed 16-bit little-endian PCM from {@code fromHz} to {@code toHz} using linear interpolation, which
     * is adequate for narrowband voice. Returns the input unchanged when the rates match.
     */
    static byte[] resamplePcm16(byte[] pcm16LittleEndian, int fromHz, int toHz) {
        if (fromHz == toHz) {
            return pcm16LittleEndian;
        }

        int inSampleCount = pcm16LittleEndian.length / 2;

        if (inSampleCount == 0) {
            return new byte[0];
        }

        short[] in = new short[inSampleCount];

        for (int i = 0; i < inSampleCount; i++) {
            in[i] = (short) ((pcm16LittleEndian[i * 2] & 0xFF) | (pcm16LittleEndian[i * 2 + 1] << 8));
        }

        int outSampleCount = (int) ((long) inSampleCount * toHz / fromHz);

        if (outSampleCount <= 0) {
            return new byte[0];
        }

        byte[] out = new byte[outSampleCount * 2];
        double ratio = outSampleCount == 1 ? 0 : (double) (inSampleCount - 1) / (outSampleCount - 1);

        for (int i = 0; i < outSampleCount; i++) {
            double position = i * ratio;
            int index = (int) position;
            double fraction = position - index;

            short current = in[index];
            short next = index + 1 < inSampleCount ? in[index + 1] : current;
            int sample = (int) Math.round(current + (next - current) * fraction);

            out[i * 2] = (byte) (sample & 0xFF);
            out[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }

        return out;
    }
}
