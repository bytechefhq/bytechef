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
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;

import com.bytechef.component.definition.TriggerDefinition.TriggerType;

/**
 * Trigger that fires when a browser opens a voice session (WebSocket upgrade) against this workflow's webhook.
 *
 * <p>
 * Type {@link TriggerType#WEBSOCKET}: the engine registers no HTTP webhook controller for this trigger. The public
 * surface is the existing WebSocket upgrade path at {@code /webhooks/(webhookId)/wss}, plus the companion single-use
 * session-token mint endpoint at {@code POST /webhooks/(webhookId)/voice-session-token}.
 *
 * <p>
 * The sub-workflow specified via {@code subWorkflow} runs for the duration of the voice session. Audio frames flow
 * bidirectionally between the browser and the running sub-workflow's WebSocketEmitter — typically the
 * {@code deepgram/v1/voiceAgent} action which terminates the loop with STT, LLM, and TTS in one provider WS.
 *
 * @author Ivica Cardic
 */
public class BrowserVoiceSessionTrigger {

    public static final String SUB_WORKFLOW = "subWorkflow";
    public static final String SAMPLE_RATE = "sampleRate";
    public static final String ECHO_CANCELLATION = "echoCancellation";
    public static final String NOISE_SUPPRESSION = "noiseSuppression";
    public static final String SESSION_LIMIT_SECONDS = "sessionLimitSeconds";

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("voiceSession")
        .title("Browser Voice Session")
        .description(
            "Triggers when a browser opens a voice session against this workflow. The sub-workflow runs for the " +
                "duration of the session against a bidirectional audio stream.")
        .type(TriggerType.WEBSOCKET)
        .properties(
            string(SUB_WORKFLOW)
                .label("Real-Time Workflow")
                .description("The workflow ID to execute for the duration of the voice session.")
                .required(true),
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
                .description("Maximum duration of a voice session. 0 means no limit.")
                .defaultValue(150)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("sessionId").description("ByteChef-issued session id (UUID)"),
                        string("startedAt").description("ISO-8601 session start timestamp"))));

    private BrowserVoiceSessionTrigger() {
    }
}
