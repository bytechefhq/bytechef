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

package com.bytechef.component.ai.agent.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.ai.agent.tool.AgentToolCallingManagers;
import com.bytechef.component.ai.llm.facade.AiAgentToolFacade;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionDefinition.WebSocketHandler;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.ai.guardrails.AiGuardrailsAdvisorProvider;
import com.bytechef.platform.ai.workspaceprompt.WorkspaceSystemPromptAdvisorProvider;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.MultipleConnectionsWebSocketPerformFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.tool.execution.ToolExecutionRecorder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Subscription;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Flux;

/**
 * Streaming voice-loop agent action. Unlike {@link AiAgentStreamChatAction} (which streams ONE response per call as
 * SSE), this action runs for the duration of a WebSocket session, processing every inbound {@code user_turn} event by
 * calling the LLM with streaming and emitting {@code assistant_token} events back through the WebSocketEmitter.
 *
 * <p>
 * Event protocol on the emitter:
 *
 * <pre>{@code
 * inbound:  {"type":"user_turn",       "text":"...", "turnId":"..."}
 * outbound: {"type":"assistant_token", "text":"...", "turnId":"...", "done":false}
 * outbound: {"type":"assistant_token", "text":"",    "turnId":"...", "done":true}
 * outbound: {"type":"error",           "message":"...", "turnId":"..."}
 * }</pre>
 *
 * <p>
 * Designed to sit between an STT task (emits {@code user_turn}) and a TTS task (consumes {@code assistant_token}) in an
 * array-ordered {@code websocketTasks} pipeline. Single-task workflows can also use it standalone if the trigger
 * delivers user_turn events directly.
 *
 * <p>
 * <b>Tier 1 scope:</b> uses the same model + memory cluster-element resolution as the regular chat action
 * ({@link AbstractAiAgentChatAction#getChatClientRequestSpec}). Guardrails, RAG, tools, and simulation are inherited
 * from the parent but rebuilding the {@link ChatClientRequestSpec} per turn means per-turn LLM invocations get the
 * fresh advisor chain — same behavior as multi-turn chat. Barge-in is wired via {@code emitter.addTurnCancelListener}:
 * when the platform fires {@code cancelTurn(turnId)} (typically from an upstream STT detecting speech-start), this
 * action aborts the in-flight Flux subscription, marks the turn interrupted, and stops emitting tokens for it.
 *
 * @author Ivica Cardic
 */
public class AiAgentRealtimeChatAction extends AbstractAiAgentChatAction {

    private static final String MAX_TOKENS = "maxTokens";
    private static final String MODEL = "model";
    private static final String SYSTEM_PROMPT = "systemPrompt";
    private static final String TEMPERATURE = "temperature";
    private static final String THREAD_ID = "threadId";
    private static final String USER_TURN_TYPE = "user_turn";
    private static final String ASSISTANT_TOKEN_TYPE = "assistant_token";
    private static final String ERROR_TYPE = "error";

    @Override
    protected boolean isStreaming() {
        return true;
    }

    public static ActionDefinition of(
        AiAgentToolFacade aiAgentToolFacade, ClusterElementDefinitionService clusterElementDefinitionService,
        AgentToolCallingManagers agentToolCallingManagers,
        @Nullable ObjectProvider<ToolExecutionRecorder> toolExecutionRecorderObjectProvider,
        @Nullable ObjectProvider<AiGuardrailsAdvisorProvider> aiGuardrailsAdvisorProviderObjectProvider,
        @Nullable ObjectProvider<WorkspaceSystemPromptAdvisorProvider> workspaceSystemPromptAdvisorProviderObjectProvider) {

        return new AiAgentRealtimeChatAction(
            aiAgentToolFacade, clusterElementDefinitionService, agentToolCallingManagers,
            toolExecutionRecorderObjectProvider, aiGuardrailsAdvisorProviderObjectProvider,
            workspaceSystemPromptAdvisorProviderObjectProvider).build();
    }

    private AiAgentRealtimeChatAction(
        AiAgentToolFacade aiAgentToolFacade, ClusterElementDefinitionService clusterElementDefinitionService,
        AgentToolCallingManagers agentToolCallingManagers,
        @Nullable ObjectProvider<ToolExecutionRecorder> toolExecutionRecorderObjectProvider,
        @Nullable ObjectProvider<AiGuardrailsAdvisorProvider> aiGuardrailsAdvisorProviderObjectProvider,
        @Nullable ObjectProvider<WorkspaceSystemPromptAdvisorProvider> workspaceSystemPromptAdvisorProviderObjectProvider) {

        super(
            aiAgentToolFacade, clusterElementDefinitionService, agentToolCallingManagers,
            toolExecutionRecorderObjectProvider, aiGuardrailsAdvisorProviderObjectProvider,
            workspaceSystemPromptAdvisorProviderObjectProvider);
    }

    private RealtimeChatActionDefinitionWrapper build() {
        return new RealtimeChatActionDefinitionWrapper(
            action("realtimeChat")
                .title("Realtime Chat")
                .description(
                    "Streaming LLM turn handler. Consumes finalized user turns from the inbound WebSocket and emits " +
                        "assistant tokens to the outbound channel, supporting tool calls and conversation memory. " +
                        "Designed for voice pipelines where this action sits between an STT and a TTS task.")
                .properties(
                    string(MODEL)
                        .label("Model")
                        .description(
                            "Model name override for the cluster element. If omitted, the model cluster element's " +
                                "default is used.")
                        .required(false),
                    string(SYSTEM_PROMPT)
                        .label("System Prompt")
                        .description("The system prompt that defines the agent's behavior and personality.")
                        .required(false),
                    string(THREAD_ID)
                        .label("Thread ID")
                        .description(
                            "Chat memory thread id. Defaults to the call's ${callSid} when omitted (referenced via " +
                                "the embedded sub-workflow's inputs).")
                        .required(false),
                    integer(MAX_TOKENS)
                        .label("Max Tokens")
                        .description("Maximum tokens per assistant response.")
                        .defaultValue(512)
                        .required(false),
                    number(TEMPERATURE)
                        .label("Temperature")
                        .description("Sampling temperature for the assistant response.")
                        .defaultValue(0.7)
                        .required(false)));
    }

    private class RealtimeChatActionDefinitionWrapper extends AbstractActionDefinitionWrapper {

        RealtimeChatActionDefinitionWrapper(ActionDefinition actionDefinition) {
            super(actionDefinition);
        }

        @Override
        public Optional<? extends BasePerformFunction> getPerform() {
            return Optional.of(
                (MultipleConnectionsWebSocketPerformFunction) AiAgentRealtimeChatAction.this::perform);
        }
    }

    protected WebSocketHandler perform(
        Parameters inputParameters, Map<String, ComponentConnection> connectionParameters,
        Parameters extensions, ActionContext context) {

        return emitter -> {
            // Per-turn state: track active subscription so cancelTurn can abort it. AtomicReference covers the
            // race between an in-flight stream and a barge-in arriving on a different thread.
            AtomicReference<@Nullable Subscription> activeSubscriptionRef = new AtomicReference<>();
            AtomicReference<@Nullable String> activeTurnIdRef = new AtomicReference<>();

            emitter.addTurnCancelListener(turnId -> {
                if (turnId == null) {
                    return;
                }

                String activeTurnId = activeTurnIdRef.get();

                if (!turnId.equals(activeTurnId)) {
                    return;
                }

                Subscription subscription = activeSubscriptionRef.getAndSet(null);

                if (subscription != null) {
                    subscription.cancel();
                }

                activeTurnIdRef.set(null);
            });

            emitter.addCloseListener(() -> {
                Subscription subscription = activeSubscriptionRef.getAndSet(null);

                if (subscription != null) {
                    subscription.cancel();
                }
            });

            emitter.addMessageListener(rawMessage -> {
                Map<String, @Nullable Object> event = parseUserTurnEvent(rawMessage);

                if (event == null) {
                    return;
                }

                String text = (String) event.get("text");
                String turnId = (String) event.get("turnId");

                if (text == null || text.isBlank() || turnId == null) {
                    return;
                }

                // Abort any in-flight turn before starting a new one — if the caller didn't fire cancelTurn
                // explicitly, getting a new user_turn implies the previous turn ended.
                Subscription previous = activeSubscriptionRef.getAndSet(null);

                if (previous != null) {
                    previous.cancel();
                }

                activeTurnIdRef.set(turnId);

                try {
                    ChatClientRequestSpec spec = getChatClientRequestSpec(
                        inputParameters, connectionParameters, extensions, null, context);

                    Flux<String> contentFlux = spec.user(text)
                        .stream()
                        .content();

                    contentFlux.subscribe(new TokenSubscriber(emitter, activeSubscriptionRef, activeTurnIdRef, turnId));
                } catch (Exception exception) {
                    activeTurnIdRef.set(null);
                    emitError(emitter, turnId, exception.getMessage());
                }
            });
        };
    }

    private static @Nullable Map<String, @Nullable Object> parseUserTurnEvent(Object rawMessage) {
        String json;

        if (rawMessage instanceof String stringMessage) {
            json = stringMessage;
        } else if (rawMessage == null) {
            return null;
        } else {
            json = JsonUtils.write(rawMessage);
        }

        Map<String, @Nullable Object> event;

        try {
            @SuppressWarnings("unchecked")
            Map<String, @Nullable Object> parsed = (Map<String, @Nullable Object>) JsonUtils.read(json, Map.class);

            event = parsed;
        } catch (Exception exception) {
            return null;
        }

        if (!USER_TURN_TYPE.equals(event.get("type"))) {
            return null;
        }

        return event;
    }

    private static void emitToken(
        WebSocketHandler.WebSocketEmitter emitter, String turnId, String text, boolean done) {

        Map<String, @Nullable Object> event = new LinkedHashMap<>();

        event.put("type", ASSISTANT_TOKEN_TYPE);
        event.put("text", text);
        event.put("turnId", turnId);
        event.put("done", done);

        emitter.send(JsonUtils.write(event));
    }

    private static void emitError(WebSocketHandler.WebSocketEmitter emitter, String turnId, @Nullable String message) {
        Map<String, @Nullable Object> event = new LinkedHashMap<>();

        event.put("type", ERROR_TYPE);
        event.put("turnId", turnId);
        event.put("message", message);

        emitter.send(JsonUtils.write(event));
    }

    /**
     * Reactor {@link reactor.core.publisher.BaseSubscriber}-style subscriber that emits each token as it arrives and
     * the terminal {@code done:true} marker on completion. Wired into the AtomicReference so cancelTurn can call
     * {@link Subscription#cancel} from any thread.
     */
    private static final class TokenSubscriber implements org.reactivestreams.Subscriber<String> {

        private final WebSocketHandler.WebSocketEmitter emitter;
        private final AtomicReference<@Nullable Subscription> activeSubscriptionRef;
        private final AtomicReference<@Nullable String> activeTurnIdRef;
        private final String turnId;

        TokenSubscriber(
            WebSocketHandler.WebSocketEmitter emitter,
            AtomicReference<@Nullable Subscription> activeSubscriptionRef,
            AtomicReference<@Nullable String> activeTurnIdRef, String turnId) {

            this.emitter = emitter;
            this.activeSubscriptionRef = activeSubscriptionRef;
            this.activeTurnIdRef = activeTurnIdRef;
            this.turnId = turnId;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            activeSubscriptionRef.set(subscription);

            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(String token) {
            // Skip emits if a cancelTurn raced us between subscription chunks.
            if (!turnId.equals(activeTurnIdRef.get())) {
                return;
            }

            emitToken(emitter, turnId, token, false);
        }

        @Override
        public void onError(Throwable throwable) {
            if (turnId.equals(activeTurnIdRef.get())) {
                emitError(emitter, turnId, throwable.getMessage());

                activeTurnIdRef.set(null);
            }

            activeSubscriptionRef.compareAndSet(null, null);
        }

        @Override
        public void onComplete() {
            if (turnId.equals(activeTurnIdRef.get())) {
                emitToken(emitter, turnId, "", true);

                activeTurnIdRef.set(null);
            }

            activeSubscriptionRef.compareAndSet(null, null);
        }
    }
}
