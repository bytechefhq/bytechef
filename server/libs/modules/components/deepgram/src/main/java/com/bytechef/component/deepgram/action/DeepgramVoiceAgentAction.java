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
 * Voice agent action using Deepgram's conversational AI WebSocket API.
 *
 * <p>
 * Bridges audio between a connected WebSocket client and Deepgram's voice agent API, which provides end-to-end
 * conversational AI with integrated STT, LLM, and TTS capabilities.
 * </p>
 *
 * @author Ivica Cardic
 */
public class DeepgramVoiceAgentAction {

    private static final String AUDIO_INPUT_ENCODING = "audioInputEncoding";
    private static final String AUDIO_INPUT_SAMPLE_RATE = "audioInputSampleRate";
    private static final String AUDIO_OUTPUT_ENCODING = "audioOutputEncoding";
    private static final String AUDIO_OUTPUT_SAMPLE_RATE = "audioOutputSampleRate";
    private static final String GREETING = "greeting";
    private static final String LANGUAGE = "language";
    private static final String LLM_MODEL = "llmModel";
    private static final String LLM_PROVIDER = "llmProvider";
    private static final String PROMPT = "prompt";
    private static final String TTS_MODEL = "ttsModel";
    private static final String TTS_PROVIDER = "ttsProvider";

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("voiceAgent")
        .title("Voice Agent")
        .description(
            "Start a real-time voice agent conversation using Deepgram's conversational AI API. " +
                "Handles end-to-end voice interaction with integrated speech-to-text, LLM reasoning, " +
                "and text-to-speech in a single WebSocket connection.")
        .properties(
            string(LANGUAGE)
                .label("Language")
                .description("The language for the voice agent conversation.")
                .defaultValue("en")
                .required(false),
            string(PROMPT)
                .label("System Prompt")
                .description("The system prompt that defines the agent's behavior and personality.")
                .required(false),
            string(GREETING)
                .label("Greeting")
                .description("The initial greeting the agent speaks when the conversation starts.")
                .required(false),
            string(LLM_PROVIDER)
                .label("LLM Provider")
                .description("The LLM provider for the agent's reasoning.")
                .options(
                    option("OpenAI", "open_ai"),
                    option("Anthropic", "anthropic"),
                    option("Google", "google"),
                    option("Groq", "groq"),
                    option("AWS Bedrock", "aws_bedrock"))
                .defaultValue("open_ai")
                .required(false),
            string(LLM_MODEL)
                .label("LLM Model")
                .description("The LLM model to use for reasoning.")
                .defaultValue("gpt-4o-mini")
                .required(false),
            string(TTS_PROVIDER)
                .label("TTS Provider")
                .description("The text-to-speech provider for the agent's voice.")
                .options(
                    option("Deepgram", "deepgram"),
                    option("ElevenLabs", "eleven_labs"),
                    option("OpenAI", "open_ai"),
                    option("Cartesia", "cartesia"),
                    option("AWS Polly", "aws_polly"))
                .defaultValue("deepgram")
                .required(false),
            string(TTS_MODEL)
                .label("TTS Voice")
                .description("The voice model for text-to-speech.")
                .defaultValue("aura-asteria-en")
                .required(false),
            string(AUDIO_INPUT_ENCODING)
                .label("Audio Input Encoding")
                .description("The encoding of the input audio.")
                .options(
                    option("Linear16", "linear16"),
                    option("μ-law", "mulaw"),
                    option("A-law", "alaw"))
                .defaultValue("linear16")
                .required(false),
            string(AUDIO_INPUT_SAMPLE_RATE)
                .label("Audio Input Sample Rate")
                .description("The sample rate of the input audio in Hz.")
                .defaultValue("16000")
                .required(false),
            string(AUDIO_OUTPUT_ENCODING)
                .label("Audio Output Encoding")
                .description("The encoding of the output audio.")
                .options(
                    option("Linear16", "linear16"),
                    option("μ-law", "mulaw"),
                    option("A-law", "alaw"))
                .defaultValue("linear16")
                .required(false),
            string(AUDIO_OUTPUT_SAMPLE_RATE)
                .label("Audio Output Sample Rate")
                .description("The sample rate of the output audio in Hz.")
                .defaultValue("24000")
                .required(false))
        .perform(DeepgramVoiceAgentAction::perform);

    private DeepgramVoiceAgentAction() {
    }

    protected static WebSocketHandler perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        String apiKey = connectionParameters.getRequiredString(TOKEN);
        String settingsMessage = buildSettingsMessage(inputParameters);

        return webSocketEmitter -> {
            AtomicBoolean closed = new AtomicBoolean(false);

            try {
                HttpClient httpClient = HttpClient.newHttpClient();

                WebSocket deepgramWebSocket = httpClient.newWebSocketBuilder()
                    .header("Authorization", "Token " + apiKey)
                    .buildAsync(
                        URI.create("wss://agent.deepgram.com/v1/agent/converse"),
                        new DeepgramAgentListener(webSocketEmitter, closed))
                    .join();

                deepgramWebSocket.sendText(settingsMessage, true)
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

    private static String buildSettingsMessage(Parameters inputParameters) {
        String language = inputParameters.getString(LANGUAGE, "en");
        String llmProvider = inputParameters.getString(LLM_PROVIDER, "open_ai");
        String llmModel = inputParameters.getString(LLM_MODEL, "gpt-4o-mini");
        String ttsProvider = inputParameters.getString(TTS_PROVIDER, "deepgram");
        String ttsModel = inputParameters.getString(TTS_MODEL, "aura-asteria-en");
        String inputEncoding = inputParameters.getString(AUDIO_INPUT_ENCODING, "linear16");
        String inputSampleRate = inputParameters.getString(AUDIO_INPUT_SAMPLE_RATE, "16000");
        String outputEncoding = inputParameters.getString(AUDIO_OUTPUT_ENCODING, "linear16");
        String outputSampleRate = inputParameters.getString(AUDIO_OUTPUT_SAMPLE_RATE, "24000");
        String prompt = inputParameters.getString(PROMPT);
        String greeting = inputParameters.getString(GREETING);

        StringBuilder settings = new StringBuilder();

        settings.append("{\"type\":\"Settings\",");
        settings.append("\"audio\":{");
        settings.append("\"input\":{\"encoding\":\"")
            .append(inputEncoding)
            .append("\",\"sample_rate\":")
            .append(inputSampleRate)
            .append("},");
        settings.append("\"output\":{\"encoding\":\"")
            .append(outputEncoding)
            .append("\",\"sample_rate\":")
            .append(outputSampleRate)
            .append("}},");
        settings.append("\"agent\":{");
        settings.append("\"language\":\"")
            .append(language)
            .append("\",");
        settings.append("\"listen\":{\"provider\":{\"type\":\"deepgram\",\"model\":\"nova-3\"}},");
        settings.append("\"think\":{\"provider\":{\"type\":\"")
            .append(llmProvider)
            .append("\",\"model\":\"")
            .append(llmModel)
            .append("\"}");

        if (prompt != null && !prompt.isBlank()) {
            settings.append(",\"instructions\":\"")
                .append(escapeJson(prompt))
                .append("\"");
        }

        settings.append("},");
        settings.append("\"speak\":{\"provider\":{\"type\":\"")
            .append(ttsProvider)
            .append("\",\"model\":\"")
            .append(ttsModel)
            .append("\"}}");

        if (greeting != null && !greeting.isBlank()) {
            settings.append(",\"greeting\":\"")
                .append(escapeJson(greeting))
                .append("\"");
        }

        settings.append("}}");

        return settings.toString();
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
            deepgramWebSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
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

    private static class DeepgramAgentListener implements WebSocket.Listener {

        private final AtomicBoolean closed;
        private final StringBuilder textBuffer = new StringBuilder();
        private final WebSocketEmitter webSocketEmitter;

        DeepgramAgentListener(WebSocketEmitter webSocketEmitter, AtomicBoolean closed) {
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

                webSocketEmitter.send(Map.of("source", "deepgram_agent", "data", message));
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
