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

import java.util.Base64;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Codec for the <a href="https://www.twilio.com/docs/voice/media-streams/websocket-messages">Twilio Media Streams</a>
 * WebSocket protocol. Twilio sends and expects call audio as JSON <b>text</b> frames (base64 &micro;-law/8&nbsp;kHz
 * payloads), not raw binary frames, so the WebSocket bridge must translate between those envelopes and the raw audio
 * bytes exchanged with the sub-workflow's realtime component.
 *
 * <p>
 * This class is a pure, side-effect-free codec so it can be unit-tested without a live socket.
 *
 * @author Ivica Cardic
 */
public final class TwilioMediaStream {

    static final String EVENT_CONNECTED = "connected";
    static final String EVENT_START = "start";
    static final String EVENT_MEDIA = "media";
    static final String EVENT_STOP = "stop";

    private TwilioMediaStream() {
    }

    /**
     * Returns the Twilio Media Streams event discriminator ({@code connected}/{@code start}/{@code media}/{@code stop})
     * of an inbound text frame, or {@code null} when the frame is not a Twilio media-stream frame (e.g. a browser-voice
     * control message).
     */
    static @Nullable String eventType(Map<String, Object> frame) {
        return frame.get("event") instanceof String event ? event : null;
    }

    /**
     * Extracts the {@code streamSid} from a Twilio {@code start} frame. The streamSid identifies the media stream and
     * is required on every outbound {@code media}/{@code clear} frame sent back to the caller.
     *
     * @return the streamSid, or {@code null} if absent
     */
    static @Nullable String extractStreamSid(Map<String, Object> frame) {
        if (frame.get("streamSid") instanceof String streamSid && !streamSid.isBlank()) {
            return streamSid;
        }

        if (frame.get("start") instanceof Map<?, ?> start && start.get("streamSid") instanceof String streamSid &&
            !streamSid.isBlank()) {

            return streamSid;
        }

        return null;
    }

    /**
     * Base64-decodes the audio payload of an inbound Twilio {@code media} frame into raw audio bytes (&micro;-law when
     * the stream is configured for {@code audio/x-mulaw}).
     *
     * @return the decoded audio bytes, or {@code null} if the frame carries no payload
     */
    static byte @Nullable [] decodeMediaPayload(Map<String, Object> frame) {
        if (frame.get("media") instanceof Map<?, ?> media && media.get("payload") instanceof String payload &&
            !payload.isBlank()) {

            return Base64.getDecoder()
                .decode(payload);
        }

        return null;
    }

    /**
     * Builds an outbound Twilio {@code media} frame that streams {@code audio} back to the caller on the given
     * {@code streamSid}.
     */
    static Map<String, Object> mediaFrame(String streamSid, byte[] audio) {
        return Map.of(
            "event", EVENT_MEDIA,
            "streamSid", streamSid,
            "media", Map.of(
                "payload", Base64.getEncoder()
                    .encodeToString(audio)));
    }

    /**
     * Builds an outbound Twilio {@code clear} frame that flushes any audio Twilio has buffered but not yet played —
     * used to stop the current agent utterance when the caller interrupts (barge-in).
     */
    static Map<String, Object> clearFrame(String streamSid) {
        return Map.of(
            "event", "clear",
            "streamSid", streamSid);
    }
}
