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
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.elevenlabs.constant.ElevenLabsConstants.MODEL_ID;
import static com.bytechef.component.elevenlabs.constant.ElevenLabsConstants.VOICE_ID;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ActionDefinition.WebSocketHandler;
import com.bytechef.component.definition.ActionDefinition.WebSocketHandler.WebSocketEmitter;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.elevenlabs.util.ElevenLabsUtils;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Real-time text-to-speech action that bridges text input to ElevenLabs' WebSocket-based TTS streaming API.
 *
 * <p>
 * This action uses the {@link WebSocketHandler} pattern: it receives text messages from a connected WebSocket client
 * via the {@link WebSocketEmitter}, forwards them to ElevenLabs for real-time speech synthesis, and sends the generated
 * audio chunks back through the emitter as base64-encoded data.
 * </p>
 *
 * @author Ivica Cardic
 */
public class ElevenLabsCreateRealtimeSpeechAction {

    private static final String OUTPUT_FORMAT = "outputFormat";
    private static final String SIMILARITY_BOOST = "similarityBoost";
    private static final String STABILITY = "stability";

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createRealtimeSpeech")
        .title("Create Realtime Speech")
        .description(
            "Generate speech in real-time using ElevenLabs WebSocket-based text-to-speech API. " +
                "Receives text via WebSocket, forwards to ElevenLabs for speech synthesis, " +
                "and streams generated audio chunks back.")
        .properties(
            string(VOICE_ID)
                .label("Voice")
                .description("Voice to use for converting the text into speech.")
                .options((OptionsFunction<String>) ElevenLabsUtils::getVoiceOptions)
                .required(true),
            string(MODEL_ID)
                .label("Model")
                .description("The model to use for text-to-speech generation.")
                .options(
                    option("Eleven Flash v2.5 (Fastest)", "eleven_flash_v2_5"),
                    option("Eleven Multilingual v2", "eleven_multilingual_v2"),
                    option("Eleven Turbo v2.5", "eleven_turbo_v2_5"),
                    option("Eleven Turbo v2", "eleven_turbo_v2"),
                    option("Eleven Monolingual v1", "eleven_monolingual_v1"),
                    option("Eleven Multilingual v1", "eleven_multilingual_v1"))
                .defaultValue("eleven_flash_v2_5")
                .required(true),
            number(STABILITY)
                .label("Stability")
                .description(
                    "Voice stability (0.0 to 1.0). Lower values produce more variation, " +
                        "higher values produce more consistent speech.")
                .defaultValue(0.5)
                .minValue(0.0)
                .maxValue(1.0)
                .required(false),
            number(SIMILARITY_BOOST)
                .label("Similarity Boost")
                .description(
                    "Voice similarity boost (0.0 to 1.0). Higher values make the voice " +
                        "more closely match the original voice.")
                .defaultValue(0.8)
                .minValue(0.0)
                .maxValue(1.0)
                .required(false),
            string(OUTPUT_FORMAT)
                .label("Output Format")
                .description("The output audio format.")
                .options(
                    option("MP3 44.1kHz 128kbps", "mp3_44100_128"),
                    option("MP3 44.1kHz 64kbps", "mp3_44100_64"),
                    option("MP3 44.1kHz 32kbps", "mp3_44100_32"),
                    option("PCM 16kHz", "pcm_16000"),
                    option("PCM 22.05kHz", "pcm_22050"),
                    option("PCM 24kHz", "pcm_24000"),
                    option("PCM 44.1kHz", "pcm_44100"),
                    option("μ-law 8kHz", "ulaw_8000"))
                .defaultValue("mp3_44100_128")
                .required(false))
        .perform(ElevenLabsCreateRealtimeSpeechAction::perform);

    private ElevenLabsCreateRealtimeSpeechAction() {
    }

    protected static WebSocketHandler perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        String apiKey = connectionParameters.getRequiredString(VALUE);
        String voiceId = inputParameters.getRequiredString(VOICE_ID);
        String modelId = inputParameters.getRequiredString(MODEL_ID);
        double stability = inputParameters.getDouble(STABILITY, 0.5);
        double similarityBoost = inputParameters.getDouble(SIMILARITY_BOOST, 0.8);
        String outputFormat = inputParameters.getString(OUTPUT_FORMAT, "mp3_44100_128");

        String websocketUrl = buildWebSocketUrl(voiceId, modelId, outputFormat);

        return webSocketEmitter -> {
            AtomicBoolean closed = new AtomicBoolean(false);

            try {
                HttpClient httpClient = HttpClient.newHttpClient();

                WebSocket elevenLabsWebSocket = httpClient.newWebSocketBuilder()
                    .buildAsync(
                        URI.create(websocketUrl),
                        new ElevenLabsTtsListener(webSocketEmitter, closed))
                    .join();

                String bosMessage = buildBosMessage(apiKey, stability, similarityBoost);

                elevenLabsWebSocket.sendText(bosMessage, true)
                    .join();

                webSocketEmitter.addMessageListener(
                    message -> handleIncomingMessage(elevenLabsWebSocket, message, closed));

                webSocketEmitter.addCloseListener(
                    () -> closeElevenLabsConnection(elevenLabsWebSocket, closed));

                webSocketEmitter.addTimeoutListener(
                    () -> closeElevenLabsConnection(elevenLabsWebSocket, closed));
            } catch (Exception exception) {
                webSocketEmitter.error(exception);
            }
        };
    }

    private static String buildWebSocketUrl(String voiceId, String modelId, String outputFormat) {
        return "wss://api.elevenlabs.io/v1/text-to-speech/" + voiceId +
            "/stream-input?model_id=" + modelId +
            "&output_format=" + outputFormat;
    }

    private static String buildBosMessage(String apiKey, double stability, double similarityBoost) {
        return "{\"text\":\" \"," +
            "\"voice_settings\":{" +
            "\"stability\":" + stability + "," +
            "\"similarity_boost\":" + similarityBoost + "}," +
            "\"generation_config\":{\"chunk_length_schedule\":[120,160,250,290]}," +
            "\"xi_api_key\":\"" + apiKey + "\"}";
    }

    @SuppressWarnings("unchecked")
    private static void handleIncomingMessage(
        WebSocket elevenLabsWebSocket, Object message, AtomicBoolean closed) {

        if (closed.get() || elevenLabsWebSocket.isOutputClosed()) {
            return;
        }

        String text = null;

        if (message instanceof String messageString) {
            text = messageString;
        } else if (message instanceof Map) {
            Map<String, Object> messageMap = (Map<String, Object>) message;

            text = (String) messageMap.get("text");

            if (text == null) {
                String event = (String) messageMap.get("event");

                if ("stop".equals(event) || "close".equals(event)) {
                    sendEos(elevenLabsWebSocket, closed);

                    return;
                }
            }
        }

        if (text != null && !text.isEmpty()) {
            String textMessage = "{\"text\":\"" + escapeJson(text) + "\"}";

            elevenLabsWebSocket.sendText(textMessage, true);
        }
    }

    private static void sendEos(WebSocket elevenLabsWebSocket, AtomicBoolean closed) {
        if (!closed.get() && !elevenLabsWebSocket.isOutputClosed()) {
            elevenLabsWebSocket.sendText("{\"text\":\"\"}", true);
        }
    }

    private static void closeElevenLabsConnection(WebSocket elevenLabsWebSocket, AtomicBoolean closed) {
        if (closed.compareAndSet(false, true) && !elevenLabsWebSocket.isOutputClosed()) {
            elevenLabsWebSocket.sendText("{\"text\":\"\"}", true)
                .thenRun(() -> elevenLabsWebSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done"));
        }
    }

    private static String escapeJson(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    /**
     * WebSocket listener that receives audio chunks from ElevenLabs and forwards them through the
     * {@link WebSocketEmitter} to the connected client.
     */
    private static class ElevenLabsTtsListener implements WebSocket.Listener {

        private final AtomicBoolean closed;
        private final StringBuilder messageBuffer = new StringBuilder();
        private final WebSocketEmitter webSocketEmitter;

        ElevenLabsTtsListener(WebSocketEmitter webSocketEmitter, AtomicBoolean closed) {
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

                processResponse(message);
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

        private void processResponse(String messageText) {
            if (messageText.contains("\"audio\"") && messageText.contains("\"isFinal\"") &&
                (messageText.contains("\"isFinal\":true") || messageText.contains("\"isFinal\": true"))) {

                return;
            }

            if (messageText.contains("\"audio\":null") || messageText.contains("\"audio\": null")) {
                return;
            }

            if (messageText.contains("\"audio\"")) {
                String audioBase64 = extractAudioField(messageText);

                if (audioBase64 != null && !audioBase64.isEmpty()) {
                    byte[] audioBytes = Base64.getDecoder()
                        .decode(audioBase64);

                    webSocketEmitter.sendBinary(audioBytes);
                }
            }
        }

        private static String extractAudioField(String json) {
            int audioKeyIndex = json.indexOf("\"audio\"");

            if (audioKeyIndex < 0) {
                return null;
            }

            int colonIndex = json.indexOf(':', audioKeyIndex + 7);

            if (colonIndex < 0) {
                return null;
            }

            int startQuote = json.indexOf('"', colonIndex + 1);

            if (startQuote < 0) {
                return null;
            }

            int endQuote = json.indexOf('"', startQuote + 1);

            if (endQuote < 0) {
                return null;
            }

            return json.substring(startQuote + 1, endQuote);
        }
    }
}
