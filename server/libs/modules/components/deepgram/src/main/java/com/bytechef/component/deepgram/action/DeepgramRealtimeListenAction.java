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
import static com.bytechef.component.deepgram.constant.DeepgramConstants.LANGUAGE;
import static com.bytechef.component.deepgram.constant.DeepgramConstants.MODEL;
import static com.bytechef.component.deepgram.constant.DeepgramConstants.SAMPLE_RATE;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
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
 * Real-time speech-to-text action using Deepgram's streaming listen API.
 *
 * <p>
 * Receives audio data via {@link WebSocketEmitter}, forwards it to Deepgram for real-time transcription, and streams
 * transcription results back through the emitter.
 * </p>
 *
 * @author Ivica Cardic
 */
public class DeepgramRealtimeListenAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("realtimeListen")
        .title("Realtime Listen")
        .description(
            "Transcribe audio in real-time using Deepgram's streaming speech-to-text API. " +
                "Receives audio via WebSocket, forwards to Deepgram for transcription, " +
                "and streams transcription results back.")
        .properties(
            string(MODEL)
                .label("Model")
                .description("The AI model to use for transcription.")
                .options(
                    option("Nova 3", "nova-3"),
                    option("Nova 2", "nova-2"),
                    option("Nova 2 General", "nova-2-general"),
                    option("Nova 2 Meeting", "nova-2-meeting"),
                    option("Nova 2 Phonecall", "nova-2-phonecall"),
                    option("Enhanced", "enhanced"),
                    option("Base", "base"))
                .defaultValue("nova-3")
                .required(true),
            string(LANGUAGE)
                .label("Language")
                .description("The language code for transcription (e.g., 'en' for English).")
                .defaultValue("en")
                .required(false),
            string(ENCODING)
                .label("Encoding")
                .description("The encoding of the audio data.")
                .options(
                    option("Linear16", "linear16"),
                    option("FLAC", "flac"),
                    option("μ-law", "mulaw"),
                    option("A-law", "alaw"),
                    option("Opus", "opus"),
                    option("AMR NB", "amr-nb"),
                    option("AMR WB", "amr-wb"),
                    option("Speex", "speex"))
                .defaultValue("linear16")
                .required(false),
            string(SAMPLE_RATE)
                .label("Sample Rate")
                .description("The sample rate of the audio in Hz.")
                .defaultValue("16000")
                .required(false),
            bool("interimResults")
                .label("Interim Results")
                .description("Whether to receive interim (non-final) transcription results.")
                .defaultValue(true)
                .required(false),
            bool("punctuate")
                .label("Punctuate")
                .description("Add punctuation and capitalization to the transcript.")
                .defaultValue(true)
                .required(false),
            bool("smartFormat")
                .label("Smart Format")
                .description("Apply formatting to improve transcript readability.")
                .defaultValue(false)
                .required(false),
            bool("vadEvents")
                .label("VAD Events")
                .description("Receive voice activity detection events.")
                .defaultValue(false)
                .required(false))
        .perform(DeepgramRealtimeListenAction::perform);

    private DeepgramRealtimeListenAction() {
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
                        new DeepgramListenListener(webSocketEmitter, closed))
                    .join();

                webSocketEmitter.addBinaryMessageListener(
                    audioData -> sendAudio(deepgramWebSocket, audioData, closed));

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
        StringBuilder url = new StringBuilder("wss://api.deepgram.com/v1/listen");

        url.append("?model=")
            .append(inputParameters.getRequiredString(MODEL));

        String language = inputParameters.getString(LANGUAGE);

        if (language != null && !language.isBlank()) {
            url.append("&language=")
                .append(language);
        }

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

        if (inputParameters.getBoolean("interimResults", true)) {
            url.append("&interim_results=true");
        }

        if (inputParameters.getBoolean("punctuate", true)) {
            url.append("&punctuate=true");
        }

        if (inputParameters.getBoolean("smartFormat", false)) {
            url.append("&smart_format=true");
        }

        if (inputParameters.getBoolean("vadEvents", false)) {
            url.append("&vad_events=true");
        }

        return url.toString();
    }

    private static void sendAudio(WebSocket deepgramWebSocket, byte[] audioData, AtomicBoolean closed) {
        if (!closed.get() && !deepgramWebSocket.isOutputClosed()) {
            deepgramWebSocket.sendBinary(ByteBuffer.wrap(audioData), true);
        }
    }

    @SuppressWarnings("unchecked")
    private static void handleIncomingMessage(WebSocket deepgramWebSocket, Object message, AtomicBoolean closed) {
        if (closed.get() || deepgramWebSocket.isOutputClosed()) {
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
                        byte[] audioBytes = java.util.Base64.getDecoder()
                            .decode(audioPayload);

                        sendAudio(deepgramWebSocket, audioBytes, closed);
                    }
                }
            } else if ("stop".equals(event)) {
                closeDeepgramConnection(deepgramWebSocket, closed);
            }
        }
    }

    private static void closeDeepgramConnection(WebSocket deepgramWebSocket, AtomicBoolean closed) {
        if (closed.compareAndSet(false, true) && !deepgramWebSocket.isOutputClosed()) {
            deepgramWebSocket.sendText("{\"type\":\"CloseStream\"}", true)
                .thenRun(() -> deepgramWebSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done"));
        }
    }

    private static class DeepgramListenListener implements WebSocket.Listener {

        private final AtomicBoolean closed;
        private final StringBuilder messageBuffer = new StringBuilder();
        private final WebSocketEmitter webSocketEmitter;

        DeepgramListenListener(WebSocketEmitter webSocketEmitter, AtomicBoolean closed) {
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

                webSocketEmitter.send(Map.of("source", "deepgram_stt", "data", message));
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
