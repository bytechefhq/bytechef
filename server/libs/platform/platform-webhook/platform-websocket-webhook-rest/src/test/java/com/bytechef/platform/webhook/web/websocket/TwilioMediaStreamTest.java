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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class TwilioMediaStreamTest {

    @Test
    void testEventTypeReadsDiscriminator() {
        assertThat(TwilioMediaStream.eventType(Map.of("event", "media"))).isEqualTo("media");
        assertThat(TwilioMediaStream.eventType(Map.of("action", "execute"))).isNull();
        assertThat(TwilioMediaStream.eventType(Map.of("event", 1))).isNull();
    }

    @Test
    void testExtractStreamSidFromTopLevel() {
        assertThat(TwilioMediaStream.extractStreamSid(Map.of("event", "media", "streamSid", "MZ123")))
            .isEqualTo("MZ123");
    }

    @Test
    void testExtractStreamSidFromStartObject() {
        Map<String, Object> startFrame = Map.of(
            "event", "start",
            "start", Map.of("streamSid", "MZ456", "callSid", "CA789"));

        assertThat(TwilioMediaStream.extractStreamSid(startFrame)).isEqualTo("MZ456");
    }

    @Test
    void testExtractStreamSidReturnsNullWhenAbsent() {
        assertThat(TwilioMediaStream.extractStreamSid(Map.of("event", "media"))).isNull();
        assertThat(TwilioMediaStream.extractStreamSid(Map.of("event", "start", "start", Map.of("callSid", "CA1"))))
            .isNull();
    }

    @Test
    void testDecodeMediaPayloadRoundTrip() {
        byte[] audio = "audio-frame".getBytes(StandardCharsets.UTF_8);
        String base64 = Base64.getEncoder()
            .encodeToString(audio);

        byte[] decoded = TwilioMediaStream.decodeMediaPayload(
            Map.of("event", "media", "media", Map.of("payload", base64)));

        assertThat(decoded).isEqualTo(audio);
    }

    @Test
    void testDecodeMediaPayloadReturnsNullWhenMissing() {
        assertThat(TwilioMediaStream.decodeMediaPayload(Map.of("event", "media"))).isNull();
        assertThat(TwilioMediaStream.decodeMediaPayload(Map.of("event", "media", "media", Map.of()))).isNull();
    }

    @Test
    void testMediaFrameEncodesAudioForStreamSid() {
        byte[] audio = {
            1, 2, 3, 4
        };

        Map<String, Object> frame = TwilioMediaStream.mediaFrame("MZ1", audio);

        assertThat(frame).containsEntry("event", "media")
            .containsEntry("streamSid", "MZ1");

        Object media = frame.get("media");

        assertThat(media).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        String payload = (String) ((Map<String, Object>) media).get("payload");

        assertThat(Base64.getDecoder()
            .decode(payload)).isEqualTo(audio);
    }

    @Test
    void testClearFrameCarriesStreamSid() {
        assertThat(TwilioMediaStream.clearFrame("MZ2"))
            .containsEntry("event", "clear")
            .containsEntry("streamSid", "MZ2");
    }
}
