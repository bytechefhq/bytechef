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

package com.bytechef.component.ai.llm.openai.cluster;

import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.voice.Pcm16Resampler;
import com.bytechef.component.ai.llm.voice.ProviderWebSocketConnector;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.WebSocketHandler;
import com.bytechef.component.definition.ActionDefinition.WebSocketHandler.WebSocketEmitter;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.platform.component.definition.voice.VoiceAgentContext;
import com.bytechef.platform.component.definition.voice.VoiceAgentFunction;
import com.bytechef.platform.component.definition.voice.VoiceAgentToolset;
import com.bytechef.platform.component.definition.voice.VoiceToolDefinition;
import java.net.URI;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class OpenAiVoiceAgent {

    private static final Logger log = LoggerFactory.getLogger(OpenAiVoiceAgent.class);

    private static final String GREETING = "greeting";
    private static final String INPUT_AUDIO_TRANSCRIPTION = "inputAudioTranscription";
    private static final String INPUT_SAMPLE_RATE = "inputSampleRate";
    private static final String INSTRUCTIONS = "instructions";
    private static final String MODEL = "model";
    private static final String OUTPUT_SAMPLE_RATE = "outputSampleRate";
    private static final String VOICE = "voice";

    private static final int OPENAI_AUDIO_SAMPLE_RATE = 24000;
    private static final String TRANSCRIPTION_MODEL = "whisper-1";

    public static final ClusterElementDefinition<VoiceAgentFunction> CLUSTER_ELEMENT_DEFINITION =
        of(ProviderWebSocketConnector.jdk());

    private OpenAiVoiceAgent() {
    }

    public static ClusterElementDefinition<VoiceAgentFunction> of(ProviderWebSocketConnector connector) {
        return ComponentDsl.<VoiceAgentFunction>clusterElement("voiceAgent")
            .title("OpenAI Voice Agent")
            .description(
                "Start a real-time voice agent conversation using OpenAI's Realtime API. Handles end-to-end " +
                    "voice interaction with integrated speech-to-text, LLM reasoning, and text-to-speech in a " +
                    "single WebSocket connection.")
            .type(VoiceAgentFunction.VOICE_AGENT)
            .properties(
                string(MODEL)
                    .label("Model")
                    .description("The OpenAI Realtime model used for the voice agent conversation.")
                    .options(
                        option("gpt-realtime", "gpt-realtime"),
                        option("gpt-realtime-1.5", "gpt-realtime-1.5"),
                        option("gpt-realtime-2", "gpt-realtime-2"),
                        option("gpt-realtime-2.1", "gpt-realtime-2.1"),
                        option("gpt-realtime-mini", "gpt-realtime-mini"),
                        option("gpt-realtime-2.1-mini", "gpt-realtime-2.1-mini"),
                        option("gpt-4o-realtime-preview", "gpt-4o-realtime-preview"),
                        option("gpt-4o-mini-realtime-preview", "gpt-4o-mini-realtime-preview"))
                    .defaultValue("gpt-realtime")
                    .required(false),
                string(INSTRUCTIONS)
                    .label("Instructions")
                    .description("The system instructions that define the agent's behavior and personality.")
                    .controlType(ControlType.TEXT_AREA)
                    .required(false),
                string(VOICE)
                    .label("Voice")
                    .description("The voice OpenAI uses for the agent's speech output.")
                    .defaultValue("alloy")
                    .required(false),
                string(GREETING)
                    .label("Greeting")
                    .description("The initial greeting the agent speaks when the conversation starts.")
                    .required(false),
                bool(INPUT_AUDIO_TRANSCRIPTION)
                    .label("Transcribe Caller Audio")
                    .description("Whether OpenAI should transcribe the caller's audio into a text transcript.")
                    .defaultValue(true)
                    .required(false),
                integer(INPUT_SAMPLE_RATE)
                    .label("Input Sample Rate")
                    .description("The sample rate of the inbound browser audio in Hz, before it is resampled to " +
                        "the 24 kHz OpenAI's Realtime API requires.")
                    .defaultValue(16000)
                    .required(false),
                integer(OUTPUT_SAMPLE_RATE)
                    .label("Output Sample Rate")
                    .description("The sample rate of the audio the browser should expect in Hz. OpenAI's " +
                        "Realtime API always emits 24 kHz PCM16 audio; this value does not change what OpenAI " +
                        "sends.")
                    .defaultValue(OPENAI_AUDIO_SAMPLE_RATE)
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
        String model = inputParameters.getString(MODEL, "gpt-realtime");
        int inputSampleRate = inputParameters.getInteger(INPUT_SAMPLE_RATE, 16000);
        String greeting = inputParameters.getString(GREETING);
        String sessionUpdateEventId = "session-update-" + UUID.randomUUID();
        String sessionUpdateMessage = buildSessionUpdateMessage(
            inputParameters, actionContext, toolset.definitions(), sessionUpdateEventId);

        return webSocketEmitter -> {
            AtomicBoolean closed = new AtomicBoolean(false);

            try {
                WebSocket openAiWebSocket = connector.connect(
                    URI.create("wss://api.openai.com/v1/realtime?model=" + model),
                    Map.of("Authorization", "Bearer " + apiKey),
                    new OpenAiRealtimeListener(webSocketEmitter, actionContext, toolset, closed, sessionUpdateEventId));

                openAiWebSocket.sendText(sessionUpdateMessage, true)
                    .join();

                if (greeting != null && !greeting.isBlank()) {
                    openAiWebSocket.sendText(buildGreetingMessage(actionContext, greeting), true);
                }

                webSocketEmitter.addBinaryMessageListener(
                    audioData -> sendAudio(openAiWebSocket, audioData, closed, actionContext, inputSampleRate));

                webSocketEmitter.addCloseListener(
                    () -> closeOpenAiConnection(openAiWebSocket, closed));

                webSocketEmitter.addTimeoutListener(
                    () -> closeOpenAiConnection(openAiWebSocket, closed));
            } catch (Exception exception) {
                webSocketEmitter.error(exception);
            }
        };
    }

    private static String buildSessionUpdateMessage(
        Parameters inputParameters, ActionContext actionContext, List<VoiceToolDefinition> toolDefinitions,
        String eventId) {

        String instructions = inputParameters.getString(INSTRUCTIONS);
        String voice = inputParameters.getString(VOICE, "alloy");
        boolean inputAudioTranscription = inputParameters.getBoolean(INPUT_AUDIO_TRANSCRIPTION, true);

        Map<String, Object> audioInput = new LinkedHashMap<>();

        audioInput.put("format", Map.of("type", "audio/pcm", "rate", OPENAI_AUDIO_SAMPLE_RATE));

        if (inputAudioTranscription) {
            audioInput.put("transcription", Map.of("model", TRANSCRIPTION_MODEL));
        }

        Map<String, Object> audioOutput = new LinkedHashMap<>();

        audioOutput.put("format", Map.of("type", "audio/pcm", "rate", OPENAI_AUDIO_SAMPLE_RATE));
        audioOutput.put("voice", voice);

        Map<String, Object> session = new LinkedHashMap<>();

        session.put("type", "realtime");

        if (instructions != null && !instructions.isBlank()) {
            session.put("instructions", instructions);
        }

        session.put("output_modalities", List.of("audio"));
        session.put("audio", Map.of("input", audioInput, "output", audioOutput));

        if (!toolDefinitions.isEmpty()) {
            session.put("tools", toFunctionDefinitions(actionContext, toolDefinitions));
        }

        Map<String, Object> sessionUpdate = new LinkedHashMap<>();

        sessionUpdate.put("event_id", eventId);
        sessionUpdate.put("type", "session.update");
        sessionUpdate.put("session", session);

        return actionContext.json(json -> json.write(sessionUpdate));
    }

    private static String buildGreetingMessage(ActionContext actionContext, String greeting) {
        Map<String, Object> response = new LinkedHashMap<>();

        response.put("instructions", "Greet the user by saying exactly: " + greeting);

        Map<String, Object> responseCreate = new LinkedHashMap<>();

        responseCreate.put("type", "response.create");
        responseCreate.put("response", response);

        return actionContext.json(json -> json.write(responseCreate));
    }

    private static List<Map<String, Object>> toFunctionDefinitions(
        ActionContext actionContext, List<VoiceToolDefinition> toolDefinitions) {

        List<Map<String, Object>> functions = new ArrayList<>();

        for (VoiceToolDefinition toolDefinition : toolDefinitions) {
            Map<String, Object> function = new LinkedHashMap<>();

            function.put("type", "function");
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

    private static void sendAudio(
        WebSocket openAiWebSocket, byte[] audioData, AtomicBoolean closed, ActionContext actionContext,
        int inputSampleRate) {

        if (closed.get() || openAiWebSocket.isOutputClosed()) {
            return;
        }

        byte[] resampled = Pcm16Resampler.resample(audioData, inputSampleRate, OPENAI_AUDIO_SAMPLE_RATE);
        String base64Audio = Base64.getEncoder()
            .encodeToString(resampled);

        Map<String, Object> append = new LinkedHashMap<>();

        append.put("type", "input_audio_buffer.append");
        append.put("audio", base64Audio);

        openAiWebSocket.sendText(actionContext.json(json -> json.write(append)), true);
    }

    private static void closeOpenAiConnection(WebSocket openAiWebSocket, AtomicBoolean closed) {
        if (closed.compareAndSet(false, true) && !openAiWebSocket.isOutputClosed()) {
            openAiWebSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
        }
    }

    static @Nullable Map<String, Object> toVoiceEvent(Map<String, Object> openAiMessage) {
        if (!(openAiMessage.get("type") instanceof String type)) {
            return null;
        }

        return switch (type) {
            case "input_audio_buffer.speech_started" -> Map.of("type", "speech_start");
            case "conversation.item.input_audio_transcription.completed" ->
                Map.of("type", "transcript_final", "text", transcriptOf(openAiMessage));
            case "response.output_audio_transcript.done" ->
                Map.of("type", "assistant_text", "text", transcriptOf(openAiMessage));
            default -> null;
        };
    }

    private static String transcriptOf(Map<String, Object> openAiMessage) {
        Object transcript = openAiMessage.get("transcript");

        return transcript == null ? "" : String.valueOf(transcript);
    }

    private static final Set<String> RECOVERABLE_ERROR_CODES = Set.of(
        "conversation_already_has_active_response", "invalid_value");

    private static class OpenAiRealtimeListener implements WebSocket.Listener {

        private final ActionContext actionContext;
        private final AtomicBoolean closed;
        private final AtomicBoolean errored = new AtomicBoolean(false);
        private final String sessionUpdateEventId;
        private final AtomicBoolean sessionUpdated = new AtomicBoolean(false);

        private final Map<String, PendingResponse> pendingResponses = new HashMap<>();
        private final StringBuilder textBuffer = new StringBuilder();
        private final VoiceAgentToolset toolset;
        private final WebSocketEmitter webSocketEmitter;

        OpenAiRealtimeListener(
            WebSocketEmitter webSocketEmitter, ActionContext actionContext, VoiceAgentToolset toolset,
            AtomicBoolean closed, String sessionUpdateEventId) {

            this.actionContext = actionContext;
            this.closed = closed;
            this.sessionUpdateEventId = sessionUpdateEventId;
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

                handleMessage(webSocket, message);
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
            if (errored.compareAndSet(false, true)) {
                webSocketEmitter.error(error);
            }

            closeOpenAiConnection(webSocket, closed);
        }

        @SuppressWarnings("unchecked")
        private void handleMessage(WebSocket webSocket, String message) {
            Map<String, Object> openAiMessage;

            try {
                openAiMessage = actionContext.json(json -> json.read(message, Map.class));
            } catch (Exception exception) {
                log.debug("Ignoring an unparseable OpenAI Realtime frame", exception);

                webSocketEmitter.send(Map.of("source", "openai_realtime", "data", message));

                return;
            }

            String type = openAiMessage.get("type") instanceof String typeValue ? typeValue : null;

            if ("response.output_audio.delta".equals(type)) {
                handleAudioDelta(openAiMessage);
            } else if ("response.function_call_arguments.done".equals(type)) {
                handleFunctionCallArgumentsDone(webSocket, openAiMessage);
            } else if ("error".equals(type)) {
                handleError(webSocket, openAiMessage);
            } else {
                if ("response.done".equals(type)) {
                    handleResponseDone(webSocket, openAiMessage);
                } else if ("session.updated".equals(type)) {
                    sessionUpdated.set(true);
                }

                Map<String, Object> voiceEvent = toVoiceEvent(openAiMessage);

                webSocketEmitter.send(voiceEvent != null ? voiceEvent : Map.of(
                    "source", "openai_realtime", "data", message));
            }
        }

        private void handleAudioDelta(Map<String, Object> openAiMessage) {
            if (!(openAiMessage.get("delta") instanceof String delta) || delta.isEmpty()) {
                return;
            }

            webSocketEmitter.sendBinary(Base64.getDecoder()
                .decode(delta));
        }

        private void handleError(WebSocket webSocket, Map<String, Object> openAiMessage) {
            String message = errorMessageOf(openAiMessage);
            String code = errorCodeOf(openAiMessage);
            boolean rejectsSessionUpdate = !sessionUpdated.get() ||
                sessionUpdateEventId.equals(errorEventIdOf(openAiMessage));

            if (!rejectsSessionUpdate && code != null && RECOVERABLE_ERROR_CODES.contains(code)) {
                log.warn("Recoverable OpenAI Realtime error, the session continues: code={}, message={}", code,
                    message);

                return;
            }

            if (errored.compareAndSet(false, true)) {
                webSocketEmitter.error(new IllegalStateException(message));
            }

            closeOpenAiConnection(webSocket, closed);
        }

        private static @Nullable String errorEventIdOf(Map<String, Object> openAiMessage) {
            if (openAiMessage.get("error") instanceof Map<?, ?> errorDetails
                && errorDetails.get("event_id") instanceof String eventId && !eventId.isBlank()) {

                return eventId;
            }

            return null;
        }

        private static @Nullable String errorCodeOf(Map<String, Object> openAiMessage) {
            if (openAiMessage.get("error") instanceof Map<?, ?> errorDetails
                && errorDetails.get("code") instanceof String code && !code.isBlank()) {

                return code;
            }

            return null;
        }

        private static String errorMessageOf(Map<String, Object> openAiMessage) {
            if (openAiMessage.get("error") instanceof Map<?, ?> errorDetails
                && errorDetails.get("message") instanceof String message && !message.isBlank()) {

                return message;
            }

            return "OpenAI Realtime error";
        }

        private void handleFunctionCallArgumentsDone(WebSocket webSocket, Map<String, Object> openAiMessage) {
            String callId = openAiMessage.get("call_id") instanceof String value ? value : null;
            String name = openAiMessage.get("name") instanceof String value ? value : null;
            String argumentsJson = openAiMessage.get("arguments") instanceof String value ? value : "{}";

            if (callId == null || name == null) {
                return;
            }

            String responseId = openAiMessage.get("response_id") instanceof String value ? value : null;

            if (responseId != null) {
                synchronized (pendingResponses) {
                    PendingResponse pendingResponse = pendingResponses.computeIfAbsent(
                        responseId, key -> new PendingResponse());

                    pendingResponse.pendingCallIds.add(callId);
                }
            }

            Thread.startVirtualThread(() -> callTool(webSocket, callId, responseId, name, argumentsJson));
        }

        private void handleResponseDone(WebSocket webSocket, Map<String, Object> openAiMessage) {
            if (!(openAiMessage.get("response") instanceof Map<?, ?> response)
                || !(response.get("id") instanceof String responseId)) {

                return;
            }

            boolean startNextResponse;

            synchronized (pendingResponses) {
                PendingResponse pendingResponse = pendingResponses.get(responseId);

                if (pendingResponse == null) {
                    return;
                }

                pendingResponse.done = true;
                startNextResponse = pendingResponse.pendingCallIds.isEmpty();

                if (startNextResponse) {
                    pendingResponses.remove(responseId);
                }
            }

            if (startNextResponse) {
                sendResponseCreate(webSocket);
            }
        }

        private boolean outputSentStartsNextResponse(@Nullable String responseId, String callId) {
            if (responseId == null) {
                return true;
            }

            synchronized (pendingResponses) {
                PendingResponse pendingResponse = pendingResponses.get(responseId);

                if (pendingResponse == null) {
                    return false;
                }

                pendingResponse.pendingCallIds.remove(callId);

                if (pendingResponse.done && pendingResponse.pendingCallIds.isEmpty()) {
                    pendingResponses.remove(responseId);

                    return true;
                }

                return false;
            }
        }

        private void sendResponseCreate(WebSocket webSocket) {
            Map<String, Object> responseCreate = Map.of("type", "response.create");

            try {
                if (!closed.get() && !webSocket.isOutputClosed()) {
                    webSocket.sendText(actionContext.json(json -> json.write(responseCreate)), true);
                }
            } catch (Exception exception) {
                log.warn("Failed to start the response after a voice tool reply", exception);
            }
        }

        private void callTool(
            WebSocket webSocket, String callId, @Nullable String responseId, String name, String argumentsJson) {

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

            Map<String, Object> item = new LinkedHashMap<>();

            item.put("type", "function_call_output");
            item.put("call_id", callId);
            item.put("output", result);

            Map<String, Object> itemCreate = new LinkedHashMap<>();

            itemCreate.put("type", "conversation.item.create");
            itemCreate.put("item", item);

            try {
                if (!closed.get() && !webSocket.isOutputClosed()) {
                    webSocket.sendText(actionContext.json(json -> json.write(itemCreate)), true)
                        .join();

                    if (outputSentStartsNextResponse(responseId, callId)) {
                        sendResponseCreate(webSocket);
                    }
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

    private static final class PendingResponse {

        private final Set<String> pendingCallIds = new HashSet<>();
        private boolean done;
    }
}
