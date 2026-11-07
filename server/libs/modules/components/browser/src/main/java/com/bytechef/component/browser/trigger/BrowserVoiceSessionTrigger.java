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

package com.bytechef.component.browser.trigger;

import static com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;

import com.bytechef.component.definition.TriggerDefinition.TriggerType;

/**
 * @author Ivica Cardic
 */
public class BrowserVoiceSessionTrigger {

    public static final String SAMPLE_RATE = "sampleRate";
    public static final String ECHO_CANCELLATION = "echoCancellation";
    public static final String NOISE_SUPPRESSION = "noiseSuppression";
    public static final String SESSION_LIMIT_SECONDS = "sessionLimitSeconds";
    public static final String SILENCE_TIMEOUT_SECONDS = "silenceTimeoutSeconds";

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("voiceSession")
        .title("Browser Voice Session")
        .description(
            "Triggers when a browser opens a voice session against this workflow. The Voice Agent cluster " +
                "element answers the caller for the length of the session; the workflow's tasks run afterwards " +
                "with the session transcript as the trigger output.")
        .type(TriggerType.WEBSOCKET)
        .properties(
            string(SAMPLE_RATE)
                .label("Audio Sample Rate")
                .description("PCM16 sample rate for both inbound mic audio and outbound TTS playback.")
                .options(option("16 kHz", "16000"), option("24 kHz", "24000"))
                .defaultValue("16000")
                .required(false),
            bool(ECHO_CANCELLATION)
                .label("Echo Cancellation")
                .description("Request the browser apply built-in echo cancellation to the mic stream.")
                .defaultValue(true)
                .required(false),
            bool(NOISE_SUPPRESSION)
                .label("Noise Suppression")
                .description("Request the browser apply built-in noise suppression to the mic stream.")
                .defaultValue(true)
                .required(false),
            integer(SESSION_LIMIT_SECONDS)
                .label("Session limit (seconds)")
                .description(
                    "Maximum duration of a voice session. 0 uses the server's maximum session duration (30 minutes).")
                .defaultValue(150)
                .required(false),
            integer(SILENCE_TIMEOUT_SECONDS)
                .label("Silence timeout (seconds)")
                .description(
                    "End the session after this many seconds without audio from the caller. 0 disables the timeout.")
                .defaultValue(120)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("sessionId").description("ByteChef-issued session id"),
                        string("startedAt").description("ISO-8601 session start timestamp"),
                        integer("durationSeconds").description("Session length in seconds"),
                        string("endReason")
                            .description(
                                "client_closed, session_limit, silence_timeout, provider_error or server_shutdown"),
                        array("transcript")
                            .items(
                                object().properties(
                                    string("role").description("user or assistant"),
                                    string("text"),
                                    string("at").description("ISO-8601 timestamp"))),
                        array("toolCalls")
                            .items(
                                object().properties(
                                    string("name"),
                                    object("arguments"),
                                    string("result"),
                                    string("at").description("ISO-8601 timestamp"))))));

    private BrowserVoiceSessionTrigger() {
    }
}
