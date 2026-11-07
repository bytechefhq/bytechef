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

package com.bytechef.component.deepgram.cluster;

import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.voice.ProviderWebSocketConnector;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.WebSocketHandler;
import com.bytechef.component.definition.ActionDefinition.WebSocketHandler.WebSocketEmitter;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.voice.VoiceAgentContext;
import com.bytechef.platform.component.definition.voice.VoiceAgentFunction;
import com.bytechef.platform.component.definition.voice.VoiceAgentToolset;
import com.bytechef.platform.component.definition.voice.VoiceToolDefinition;
import java.net.URI;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class DeepgramVoiceAgent {

    private static final Logger log = LoggerFactory.getLogger(DeepgramVoiceAgent.class);

    private static final String AUDIO_INPUT_ENCODING = "audioInputEncoding";
    private static final String AUDIO_INPUT_SAMPLE_RATE = "audioInputSampleRate";
    private static final String AUDIO_OUTPUT_ENCODING = "audioOutputEncoding";
    private static final String GREETING = "greeting";
    private static final String LANGUAGE = "language";
    private static final String LLM_MODEL = "llmModel";
    private static final String LLM_PROVIDER = "llmProvider";
    private static final String OUTPUT_SAMPLE_RATE = "outputSampleRate";
    private static final String PROMPT = "prompt";
    private static final String TTS_MODEL = "ttsModel";
    private static final String TTS_PROVIDER = "ttsProvider";

    public static final ClusterElementDefinition<VoiceAgentFunction> CLUSTER_ELEMENT_DEFINITION =
        of(ProviderWebSocketConnector.jdk());

    private DeepgramVoiceAgent() {
    }

    public static ClusterElementDefinition<VoiceAgentFunction> of(ProviderWebSocketConnector connector) {
        return ComponentDsl.<VoiceAgentFunction>clusterElement("voiceAgent")
            .title("Deepgram Voice Agent")
            .description(
                "Start a real-time voice agent conversation using Deepgram's conversational AI API. " +
                    "Handles end-to-end voice interaction with integrated speech-to-text, LLM reasoning, " +
                    "and text-to-speech in a single WebSocket connection.")
            .type(VoiceAgentFunction.VOICE_AGENT)
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
                integer(AUDIO_INPUT_SAMPLE_RATE)
                    .label("Audio Input Sample Rate")
                    .description("The sample rate of the input audio in Hz.")
                    .defaultValue(16000)
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
                integer(OUTPUT_SAMPLE_RATE)
                    .label("Audio Output Sample Rate")
                    .description("The sample rate of the output audio in Hz.")
                    .defaultValue(24000)
                    .required(false))
            .object(() -> (inputParameters, connectionParameters, context) -> perform(
                inputParameters, connectionParameters, context, connector));
    }

    protected static WebSocketHandler perform(
        Parameters inputParameters, Parameters connectionParameters, VoiceAgentContext context,
        ProviderWebSocketConnector connector) {

        String apiKey = connectionParameters.getRequiredString(TOKEN);
        ActionContext actionContext = context.actionContext();
        VoiceAgentToolset toolset = context.toolset();
        String settingsMessage = buildSettingsMessage(inputParameters, actionContext, toolset.definitions());

        return webSocketEmitter -> {
            AtomicBoolean closed = new AtomicBoolean(false);

            try {
                WebSocket deepgramWebSocket = connector.connect(
                    URI.create("wss://agent.deepgram.com/v1/agent/converse"),
                    Map.of("Authorization", "Token " + apiKey),
                    new DeepgramAgentListener(webSocketEmitter, actionContext, toolset, closed));

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

    private static String buildSettingsMessage(
        Parameters inputParameters, ActionContext actionContext, List<VoiceToolDefinition> toolDefinitions) {

        String language = inputParameters.getString(LANGUAGE, "en");
        String llmProvider = inputParameters.getString(LLM_PROVIDER, "open_ai");
        String llmModel = inputParameters.getString(LLM_MODEL, "gpt-4o-mini");
        String ttsProvider = inputParameters.getString(TTS_PROVIDER, "deepgram");
        String ttsModel = inputParameters.getString(TTS_MODEL, "aura-asteria-en");
        String inputEncoding = inputParameters.getString(AUDIO_INPUT_ENCODING, "linear16");
        int inputSampleRate = inputParameters.getInteger(AUDIO_INPUT_SAMPLE_RATE, 16000);
        String outputEncoding = inputParameters.getString(AUDIO_OUTPUT_ENCODING, "linear16");
        int outputSampleRate = inputParameters.getInteger(OUTPUT_SAMPLE_RATE, 24000);
        String prompt = inputParameters.getString(PROMPT);
        String greeting = inputParameters.getString(GREETING);

        Map<String, Object> think = new LinkedHashMap<>();

        think.put("provider", Map.of("type", llmProvider, "model", llmModel));

        if (prompt != null && !prompt.isBlank()) {
            think.put("instructions", prompt);
        }

        if (!toolDefinitions.isEmpty()) {
            think.put("functions", toFunctionDefinitions(actionContext, toolDefinitions));
        }

        Map<String, Object> agent = new LinkedHashMap<>();

        agent.put("language", language);
        agent.put("listen", Map.of("provider", Map.of("type", "deepgram", "model", "nova-3")));
        agent.put("think", think);
        agent.put("speak", Map.of("provider", Map.of("type", ttsProvider, "model", ttsModel)));

        if (greeting != null && !greeting.isBlank()) {
            agent.put("greeting", greeting);
        }

        Map<String, Object> settings = new LinkedHashMap<>();

        settings.put("type", "Settings");
        settings.put(
            "audio",
            Map.of(
                "input", Map.of("encoding", inputEncoding, "sample_rate", inputSampleRate),
                "output", Map.of("encoding", outputEncoding, "sample_rate", outputSampleRate)));
        settings.put("agent", agent);

        return actionContext.json(json -> json.write(settings));
    }

    private static List<Map<String, Object>> toFunctionDefinitions(
        ActionContext actionContext, List<VoiceToolDefinition> toolDefinitions) {

        List<Map<String, Object>> functions = new ArrayList<>();

        for (VoiceToolDefinition toolDefinition : toolDefinitions) {
            Map<String, Object> function = new LinkedHashMap<>();

            function.put("name", toolDefinition.name());
            function.put("description", toolDefinition.description());
            function.put("parameters", parseJsonSchema(actionContext, toolDefinition.inputSchema()));

            functions.add(function);
        }

        return functions;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJsonSchema(ActionContext actionContext, String schema) {
        return actionContext.json(json -> json.read(schema, Map.class));
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

        if (!(message instanceof Map)) {
            return;
        }

        Map<String, Object> messageMap = (Map<String, Object>) message;

        if ("control".equals(messageMap.get("type")) && "end".equals(messageMap.get("action"))) {
            closeDeepgramConnection(deepgramWebSocket, closed);
        }
    }

    private static void closeDeepgramConnection(WebSocket deepgramWebSocket, AtomicBoolean closed) {
        if (closed.compareAndSet(false, true) && !deepgramWebSocket.isOutputClosed()) {
            deepgramWebSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> toBrowserVoiceEvent(ActionContext actionContext, String message) {
        Map<String, Object> passthrough = Map.of("source", "deepgram_agent", "data", message);

        try {
            Map<String, Object> deepgramMessage = actionContext.json(json -> json.read(message, Map.class));

            Map<String, Object> voiceEvent = toVoiceEvent(deepgramMessage);

            return voiceEvent != null ? voiceEvent : passthrough;
        } catch (Exception exception) {
            return passthrough;
        }
    }

    static @Nullable Map<String, Object> toVoiceEvent(Map<String, Object> deepgramMessage) {
        if (!(deepgramMessage.get("type") instanceof String type)) {
            return null;
        }

        return switch (type) {
            case "ConversationText" -> {
                Object content = deepgramMessage.get("content");
                String text = content == null ? "" : String.valueOf(content);

                yield "assistant".equals(deepgramMessage.get("role"))
                    ? Map.of("type", "assistant_text", "text", text)
                    : Map.of("type", "transcript_final", "text", text);
            }
            case "UserStartedSpeaking" -> Map.of("type", "speech_start");
            default -> null;
        };
    }

    private static class DeepgramAgentListener implements WebSocket.Listener {

        private final ActionContext actionContext;
        private final AtomicBoolean closed;
        private final AtomicBoolean errored = new AtomicBoolean(false);
        private final StringBuilder textBuffer = new StringBuilder();
        private final VoiceAgentToolset toolset;
        private final WebSocketEmitter webSocketEmitter;

        DeepgramAgentListener(
            WebSocketEmitter webSocketEmitter, ActionContext actionContext, VoiceAgentToolset toolset,
            AtomicBoolean closed) {

            this.actionContext = actionContext;
            this.closed = closed;
            this.toolset = toolset;
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

                String errorDescription = errorDescriptionOf(message);

                if (errorDescription == null) {
                    webSocketEmitter.send(toBrowserVoiceEvent(actionContext, message));

                    handleFunctionCallRequest(webSocket, message);
                } else {
                    if (errored.compareAndSet(false, true)) {
                        webSocketEmitter.error(new IllegalStateException(errorDescription));
                    }

                    closeDeepgramConnection(webSocket, closed);
                }
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
            if (errored.compareAndSet(false, true)) {
                webSocketEmitter.error(error);
            }

            closeDeepgramConnection(webSocket, closed);
        }

        @SuppressWarnings("unchecked")
        private @Nullable String errorDescriptionOf(String message) {
            Map<String, Object> deepgramMessage;

            try {
                deepgramMessage = actionContext.json(json -> json.read(message, Map.class));
            } catch (Exception exception) {
                return null;
            }

            if (!"Error".equals(deepgramMessage.get("type"))) {
                return null;
            }

            return deepgramMessage.get("description") instanceof String description && !description.isBlank()
                ? description : "The Deepgram voice agent reported an error";
        }

        @SuppressWarnings("unchecked")
        private void handleFunctionCallRequest(WebSocket webSocket, String message) {
            Map<String, Object> deepgramMessage;

            try {
                deepgramMessage = actionContext.json(json -> json.read(message, Map.class));
            } catch (Exception exception) {
                log.debug("Ignoring an unparseable Deepgram voice-agent frame", exception);

                return;
            }

            if (!"FunctionCallRequest".equals(deepgramMessage.get("type"))) {
                return;
            }

            Object functionsValue = deepgramMessage.get("functions");

            if (!(functionsValue instanceof List<?> functions)) {
                return;
            }

            for (Object functionObject : functions) {
                if (!(functionObject instanceof Map<?, ?> function)) {
                    continue;
                }

                if (!Boolean.TRUE.equals(function.get("client_side"))) {
                    continue;
                }

                String id = (String) function.get("id");
                String name = (String) function.get("name");
                String argumentsJson = (String) function.get("arguments");

                Thread.startVirtualThread(() -> callTool(webSocket, id, name, argumentsJson));
            }
        }

        private void callTool(WebSocket webSocket, String id, String name, String argumentsJson) {
            Map<String, Object> parsedArguments = parseArguments(argumentsJson);
            String result;
            boolean ok;

            try {
                result = toolset.call(name, argumentsJson);
                ok = true;
            } catch (Exception exception) {
                log.warn("Voice tool '{}' failed", name, exception);

                result = errorMessage(exception);
                ok = false;
            }

            webSocketEmitter.send(Map.of("type", "tool_call", "name", name, "arguments", parsedArguments));
            webSocketEmitter.send(
                Map.of("type", "tool_result", "name", name, "ok", ok, "arguments", parsedArguments, "result", result));

            Map<String, Object> response = new LinkedHashMap<>();

            response.put("type", "FunctionCallResponse");
            response.put("id", id);
            response.put("name", name);
            response.put("content", result);

            try {
                if (!closed.get() && !webSocket.isOutputClosed()) {
                    webSocket.sendText(actionContext.json(json -> json.write(response)), true);
                }
            } catch (Exception exception) {
                log.warn("Failed to send the voice tool reply for '{}'", name, exception);
            }
        }

        private static String errorMessage(Exception exception) {
            String message = exception.getMessage();

            return message != null && !message.isBlank() ? message : exception.getClass()
                .getSimpleName();
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> parseArguments(String argumentsJson) {
            try {
                return actionContext.json(json -> json.read(argumentsJson, Map.class));
            } catch (Exception exception) {
                log.debug("Ignoring unparseable tool-call arguments for display: {}", argumentsJson, exception);

                return Map.of();
            }
        }
    }
}
