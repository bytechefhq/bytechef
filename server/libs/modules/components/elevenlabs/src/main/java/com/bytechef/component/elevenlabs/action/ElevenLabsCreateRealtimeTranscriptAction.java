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

package com.bytechef.component.elevenlabs.action;

import static com.bytechef.component.definition.Authorization.VALUE;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.elevenlabs.constant.ElevenLabsConstants.MODEL_ID;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.WebSocketHandler;
import com.bytechef.component.definition.ActionDefinition.WebSocketHandler.WebSocketEmitter;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Real-time speech-to-text action that bridges incoming WebSocket audio to ElevenLabs' real-time transcription API.
 *
 * <p>
 * This action uses the {@link WebSocketHandler} pattern: it receives audio data from a connected WebSocket client
 * (e.g., Twilio media stream) via the {@link WebSocketEmitter}, forwards it to ElevenLabs for real-time transcription,
 * and sends transcription results back through the emitter.
 * </p>
 *
 * @author Ivica Cardic
 */
public class ElevenLabsCreateRealtimeTranscriptAction {

    private static final String AUDIO_FORMAT = "audioFormat";
    private static final String LANGUAGE_CODE = "languageCode";
    private static final String SAMPLE_RATE = "sampleRate";

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createRealtimeTranscript")
        .title("Create Realtime Transcript")
        .description(
            "Transcribe audio in real-time using ElevenLabs WebSocket-based speech-to-text API. " +
                "Receives audio via WebSocket, forwards to ElevenLabs for transcription, " +
                "and streams transcription results back.")
        .properties(
            string(MODEL_ID)
                .label("Model")
                .description("The model to use for real-time transcription.")
                .defaultValue("scribe_v2_realtime")
                .required(true),
            string(LANGUAGE_CODE)
                .label("Language Code")
                .description(
                    "The language code for transcription (e.g., 'en' for English). " +
                        "If not specified, the language is auto-detected.")
                .required(false),
            integer(SAMPLE_RATE)
                .label("Sample Rate")
                .description("The sample rate of the audio in Hz.")
                .defaultValue(16000)
                .required(false),
            string(AUDIO_FORMAT)
                .label("Audio Format")
                .description("The format of the audio data.")
                .options(
                    option("PCM 16-bit signed, 16kHz", "pcm_16000"),
                    option("PCM 16-bit signed, 22.05kHz", "pcm_22050"),
                    option("PCM 16-bit signed, 44.1kHz", "pcm_44100"),
                    option("PCM μ-law, 8kHz", "ulaw_8000"))
                .defaultValue("pcm_16000")
                .required(false),
            bool("includeTimestamps")
                .label("Include Timestamps")
                .description("Whether to include word-level timestamps in the transcription.")
                .defaultValue(false)
                .required(false))
        .perform(ElevenLabsCreateRealtimeTranscriptAction::perform);

    private ElevenLabsCreateRealtimeTranscriptAction() {
    }

    protected static WebSocketHandler perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        String apiKey = connectionParameters.getRequiredString(VALUE);
        String modelId = inputParameters.getRequiredString(MODEL_ID);
        String languageCode = inputParameters.getString(LANGUAGE_CODE);
        int sampleRate = inputParameters.getInteger(SAMPLE_RATE, 16000);
        String audioFormat = inputParameters.getString(AUDIO_FORMAT, "pcm_16000");
        boolean includeTimestamps = inputParameters.getBoolean("includeTimestamps", false);

        String websocketUrl = buildWebSocketUrl(modelId, languageCode, sampleRate, audioFormat, includeTimestamps);

        return webSocketEmitter -> {
            AtomicBoolean closed = new AtomicBoolean(false);

            try {
                HttpClient httpClient = HttpClient.newHttpClient();

                WebSocket elevenLabsWebSocket = httpClient.newWebSocketBuilder()
                    .header("xi-api-key", apiKey)
                    .buildAsync(
                        URI.create(websocketUrl),
                        new ElevenLabsListener(webSocketEmitter, closed))
                    .join();

                webSocketEmitter.addMessageListener(
                    message -> handleIncomingMessage(elevenLabsWebSocket, message, sampleRate, closed));

                webSocketEmitter.addBinaryMessageListener(
                    audioData -> handleIncomingAudio(elevenLabsWebSocket, audioData, sampleRate, closed));

                webSocketEmitter.addCloseListener(() -> closeElevenLabsConnection(elevenLabsWebSocket, closed));

                webSocketEmitter.addTimeoutListener(() -> closeElevenLabsConnection(elevenLabsWebSocket, closed));
            } catch (Exception exception) {
                webSocketEmitter.error(exception);
            }
        };
    }

    private static String buildWebSocketUrl(
        String modelId, String languageCode, int sampleRate, String audioFormat, boolean includeTimestamps) {

        StringBuilder url = new StringBuilder("wss://api.elevenlabs.io/v1/speech-to-text/realtime");

        url.append("?model_id=")
            .append(modelId);
        url.append("&sample_rate=")
            .append(sampleRate);
        url.append("&audio_format=")
            .append(audioFormat);
        url.append("&include_timestamps=")
            .append(includeTimestamps);

        if (languageCode != null && !languageCode.isBlank()) {
            url.append("&language_code=")
                .append(languageCode);
        }

        return url.toString();
    }

    @SuppressWarnings("unchecked")
    private static void handleIncomingMessage(
        WebSocket elevenLabsWebSocket, Object message, int sampleRate, AtomicBoolean closed) {

        if (closed.get() || elevenLabsWebSocket.isOutputClosed()) {
            return;
        }

        if (message instanceof Map) {
            Map<String, Object> messageMap = (Map<String, Object>) message;
            String event = (String) messageMap.get("event");

            if ("media".equals(event)) {
                Map<String, Object> media = (Map<String, Object>) messageMap.get("media");

                if (media != null) {
                    String audioPayload = (String) media.get("payload");

                    if (audioPayload != null) {
                        sendAudioToElevenLabs(elevenLabsWebSocket, audioPayload, sampleRate);
                    }
                }
            } else if ("stop".equals(event)) {
                flushAndClose(elevenLabsWebSocket, closed);
            }
        } else if (message instanceof String messageString) {
            sendAudioToElevenLabs(elevenLabsWebSocket, messageString, sampleRate);
        }
    }

    private static void handleIncomingAudio(
        WebSocket elevenLabsWebSocket, byte[] audioData, int sampleRate, AtomicBoolean closed) {

        if (closed.get() || elevenLabsWebSocket.isOutputClosed()) {
            return;
        }

        String base64Audio = Base64.getEncoder()
            .encodeToString(audioData);

        sendAudioToElevenLabs(elevenLabsWebSocket, base64Audio, sampleRate);
    }

    private static void sendAudioToElevenLabs(WebSocket webSocket, String base64Audio, int sampleRate) {
        String message = "{\"message_type\":\"input_audio_chunk\",\"audio_base_64\":\"" +
            base64Audio + "\",\"sample_rate\":" + sampleRate + "}";

        webSocket.sendText(message, true);
    }

    private static void flushAndClose(WebSocket elevenLabsWebSocket, AtomicBoolean closed) {
        if (closed.compareAndSet(false, true)) {
            elevenLabsWebSocket.sendText("{\"message_type\":\"flush\"}", true)
                .thenRun(() -> elevenLabsWebSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done"));
        }
    }

    private static void closeElevenLabsConnection(WebSocket elevenLabsWebSocket, AtomicBoolean closed) {
        if (closed.compareAndSet(false, true) && !elevenLabsWebSocket.isOutputClosed()) {
            elevenLabsWebSocket.sendText("{\"message_type\":\"flush\"}", true)
                .thenRun(() -> elevenLabsWebSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done"));
        }
    }

    /**
     * WebSocket listener that receives transcription results from ElevenLabs and forwards them through the
     * {@link WebSocketEmitter} to the connected client.
     */
    private static class ElevenLabsListener implements WebSocket.Listener {

        private final AtomicBoolean closed;
        private final StringBuilder messageBuffer = new StringBuilder();
        private final WebSocketEmitter webSocketEmitter;

        ElevenLabsListener(WebSocketEmitter webSocketEmitter, AtomicBoolean closed) {
            this.closed = closed;
            this.webSocketEmitter = webSocketEmitter;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            messageBuffer.append(data);

            if (last) {
                String message = messageBuffer.toString();

                messageBuffer.setLength(0);

                webSocketEmitter.send(Map.of("source", "elevenlabs_stt", "data", message));
            }

            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            if (closed.compareAndSet(false, true)) {
                webSocketEmitter.complete();
            }

            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            if (closed.compareAndSet(false, true)) {
                webSocketEmitter.error(error);
            }
        }
    }
}
