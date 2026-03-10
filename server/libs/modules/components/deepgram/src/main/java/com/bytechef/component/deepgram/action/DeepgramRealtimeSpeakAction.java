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

package com.bytechef.component.deepgram.action;

import static com.bytechef.component.deepgram.constant.DeepgramConstants.ENCODING;
import static com.bytechef.component.deepgram.constant.DeepgramConstants.MODEL;
import static com.bytechef.component.deepgram.constant.DeepgramConstants.SAMPLE_RATE;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.WebSocketHandler;
import com.bytechef.component.definition.ActionDefinition.WebSocketHandler.WebSocketEmitter;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Real-time text-to-speech action using Deepgram's streaming speak API.
 *
 * <p>
 * Receives text via {@link WebSocketEmitter}, forwards it to Deepgram for real-time speech synthesis, and sends
 * generated audio chunks back through the emitter as binary data.
 * </p>
 *
 * @author Ivica Cardic
 */
public class DeepgramRealtimeSpeakAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("realtimeSpeak")
        .title("Realtime Speak")
        .description(
            "Generate speech in real-time using Deepgram's streaming text-to-speech API. " +
                "Receives text via WebSocket, forwards to Deepgram for speech synthesis, " +
                "and streams generated audio chunks back.")
        .properties(
            string(MODEL)
                .label("Voice Model")
                .description("The voice model to use for speech synthesis.")
                .options(
                    option("Aura Asteria (English)", "aura-asteria-en"),
                    option("Aura Luna (English)", "aura-luna-en"),
                    option("Aura Stella (English)", "aura-stella-en"),
                    option("Aura Athena (English)", "aura-athena-en"),
                    option("Aura Hera (English)", "aura-hera-en"),
                    option("Aura Orion (English)", "aura-orion-en"),
                    option("Aura Arcas (English)", "aura-arcas-en"),
                    option("Aura Perseus (English)", "aura-perseus-en"),
                    option("Aura Angus (English)", "aura-angus-en"),
                    option("Aura Orpheus (English)", "aura-orpheus-en"),
                    option("Aura Helios (English)", "aura-helios-en"),
                    option("Aura Zeus (English)", "aura-zeus-en"))
                .defaultValue("aura-asteria-en")
                .required(true),
            string(ENCODING)
                .label("Encoding")
                .description("The encoding of the output audio.")
                .options(
                    option("Linear16", "linear16"),
                    option("μ-law", "mulaw"),
                    option("A-law", "alaw"))
                .defaultValue("linear16")
                .required(false),
            string(SAMPLE_RATE)
                .label("Sample Rate")
                .description("The sample rate of the output audio in Hz.")
                .options(
                    option("8000 Hz", "8000"),
                    option("16000 Hz", "16000"),
                    option("24000 Hz", "24000"),
                    option("32000 Hz", "32000"),
                    option("48000 Hz", "48000"))
                .defaultValue("24000")
                .required(false))
        .perform(DeepgramRealtimeSpeakAction::perform);

    private DeepgramRealtimeSpeakAction() {
    }

    protected static WebSocketHandler perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        String apiKey = connectionParameters.getRequiredString(TOKEN);
        String websocketUrl = buildWebSocketUrl(inputParameters);

        return webSocketEmitter -> {
            AtomicBoolean closed = new AtomicBoolean(false);

            try {
                HttpClient httpClient = HttpClient.newHttpClient();

                WebSocket deepgramWebSocket = httpClient.newWebSocketBuilder()
                    .header("Authorization", "Token " + apiKey)
                    .buildAsync(
                        URI.create(websocketUrl),
                        new DeepgramSpeakListener(webSocketEmitter, closed))
                    .join();

                webSocketEmitter.addMessageListener(
                    message -> handleIncomingMessage(deepgramWebSocket, message, closed));

                webSocketEmitter.addCloseListener(
                    () -> closeDeepgramConnection(deepgramWebSocket, closed));

                webSocketEmitter.addTimeoutListener(
                    () -> closeDeepgramConnection(deepgramWebSocket, closed));
            } catch (Exception exception) {
                webSocketEmitter.error(exception);
            }
        };
    }

    private static String buildWebSocketUrl(Parameters inputParameters) {
        StringBuilder url = new StringBuilder("wss://api.deepgram.com/v1/speak");

        url.append("?model=")
            .append(inputParameters.getRequiredString(MODEL));

        String encoding = inputParameters.getString(ENCODING);

        if (encoding != null) {
            url.append("&encoding=")
                .append(encoding);
        }

        String sampleRate = inputParameters.getString(SAMPLE_RATE);

        if (sampleRate != null) {
            url.append("&sample_rate=")
                .append(sampleRate);
        }

        return url.toString();
    }

    @SuppressWarnings("unchecked")
    private static void handleIncomingMessage(WebSocket deepgramWebSocket, Object message, AtomicBoolean closed) {
        if (closed.get() || deepgramWebSocket.isOutputClosed()) {
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
                    closeDeepgramConnection(deepgramWebSocket, closed);

                    return;
                }
            }
        }

        if (text != null && !text.isEmpty()) {
            String speakMessage = "{\"type\":\"Speak\",\"text\":\"" + escapeJson(text) + "\"}";

            deepgramWebSocket.sendText(speakMessage, true);
        }
    }

    private static void closeDeepgramConnection(WebSocket deepgramWebSocket, AtomicBoolean closed) {
        if (closed.compareAndSet(false, true) && !deepgramWebSocket.isOutputClosed()) {
            deepgramWebSocket.sendText("{\"type\":\"Flush\"}", true)
                .thenCompose(webSocket -> webSocket.sendText("{\"type\":\"Close\"}", true))
                .thenRun(() -> deepgramWebSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done"));
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

    private static class DeepgramSpeakListener implements WebSocket.Listener {

        private final AtomicBoolean closed;
        private final StringBuilder textBuffer = new StringBuilder();
        private final WebSocketEmitter webSocketEmitter;

        DeepgramSpeakListener(WebSocketEmitter webSocketEmitter, AtomicBoolean closed) {
            this.closed = closed;
            this.webSocketEmitter = webSocketEmitter;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            textBuffer.append(data);

            if (last) {
                String message = textBuffer.toString();

                textBuffer.setLength(0);

                webSocketEmitter.send(Map.of("source", "deepgram_tts", "data", message));
            }

            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            byte[] audioBytes = new byte[data.remaining()];

            data.get(audioBytes);

            webSocketEmitter.sendBinary(audioBytes);

            return WebSocket.Listener.super.onBinary(webSocket, data, last);
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
