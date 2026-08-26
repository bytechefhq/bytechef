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

import static com.bytechef.component.ai.agent.constant.AiAgentConstants.STREAM_CHAT_PROPERTIES;
import static com.bytechef.component.definition.ActionDefinition.SseEmitterHandler.SseEmitter;
import static com.bytechef.component.definition.ComponentDsl.action;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.ai.agent.action.event.listener.ToolExecutionListener;
import com.bytechef.component.ai.agent.tool.AgentToolCallingManagers;
import com.bytechef.component.ai.llm.facade.AiAgentToolFacade;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.ai.constant.AiAgentSseEventType;
import com.bytechef.platform.ai.constant.AiAgentToolContextKey;
import com.bytechef.platform.ai.conversation.AgentConversationRecorder;
import com.bytechef.platform.ai.guardrails.AiGuardrailsAdvisorProvider;
import com.bytechef.platform.ai.workspaceprompt.WorkspaceSystemPromptAdvisorProvider;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.MultipleConnectionsOutputFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsResumePerformFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsStreamPerformFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.context.EnvironmentContextThreadLocalAccessor;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.tool.execution.ToolExecutionRecorder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.FlowAdapters;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Flux;

/**
 * @author Ivica Cardic
 */
public class AiAgentStreamChatAction extends AbstractAiAgentChatAction {

    @Override
    protected boolean isStreaming() {
        return true;
    }

    public static ActionDefinition of(
        AiAgentToolFacade aiAgentToolFacade, ClusterElementDefinitionService clusterElementDefinitionService,
        AgentToolCallingManagers agentToolCallingManagers,
        @Nullable ObjectProvider<ToolExecutionRecorder> toolExecutionRecorderObjectProvider,
        @Nullable ObjectProvider<AiGuardrailsAdvisorProvider> aiGuardrailsAdvisorProviderObjectProvider,
        @Nullable ObjectProvider<WorkspaceSystemPromptAdvisorProvider> workspaceSystemPromptAdvisorProviderObjectProvider,
        @Nullable ObjectProvider<AgentConversationRecorder> agentConversationRecorderObjectProvider) {

        return new AiAgentStreamChatAction(
            aiAgentToolFacade, clusterElementDefinitionService, agentToolCallingManagers,
            toolExecutionRecorderObjectProvider, aiGuardrailsAdvisorProviderObjectProvider,
            workspaceSystemPromptAdvisorProviderObjectProvider, agentConversationRecorderObjectProvider).build();
    }

    private AiAgentStreamChatAction(
        AiAgentToolFacade aiAgentToolFacade, ClusterElementDefinitionService clusterElementDefinitionService,
        AgentToolCallingManagers agentToolCallingManagers,
        @Nullable ObjectProvider<ToolExecutionRecorder> toolExecutionRecorderObjectProvider,
        @Nullable ObjectProvider<AiGuardrailsAdvisorProvider> aiGuardrailsAdvisorProviderObjectProvider,
        @Nullable ObjectProvider<WorkspaceSystemPromptAdvisorProvider> workspaceSystemPromptAdvisorProviderObjectProvider,
        @Nullable ObjectProvider<AgentConversationRecorder> agentConversationRecorderObjectProvider) {

        super(
            aiAgentToolFacade, clusterElementDefinitionService, agentToolCallingManagers,
            toolExecutionRecorderObjectProvider, aiGuardrailsAdvisorProviderObjectProvider,
            workspaceSystemPromptAdvisorProviderObjectProvider, agentConversationRecorderObjectProvider);
    }

    /**
     * Streaming chat never restores or clears crash checkpoints — restore lives only in
     * {@link AiAgentChatAction#perform} and nothing clears the row on stream completion. Writing a checkpoint after
     * every tool round would therefore be dead I/O that also leaves a stale conversation snapshot in {@code
     * data_storage} until job purge (plus a stale-restore hazard for a non-stream Chat node sharing the same
     * input-parameters fingerprint). Suppress checkpointing for the streaming path.
     */
    @Override
    protected @Nullable Consumer<List<Message>> createConversationCheckpointer(
        Parameters inputParameters, ActionContext context) {

        return null;
    }

    private ChatActionDefinitionWrapper build() {
        return new ChatActionDefinitionWrapper(
            action("streamChat")
                .title("Chat (stream)")
                .description("Chat with the AI agent and stream the response.")
                .properties(STREAM_CHAT_PROPERTIES)
                .output(
                    (MultipleConnectionsOutputFunction) (
                        inputParameters, componentConnections, extensions, context) -> ModelUtils.output(
                            inputParameters, null, context))
                .resumePerform((MultipleConnectionsResumePerformFunction) this::resumePerform));
    }

    public class ChatActionDefinitionWrapper extends AbstractActionDefinitionWrapper {

        @SuppressFBWarnings("EI_EXPOSE_REP2")
        public ChatActionDefinitionWrapper(ActionDefinition actionDefinition) {
            super(actionDefinition);
        }

        @Override
        public Optional<? extends BasePerformFunction> getPerform() {
            return Optional.of((MultipleConnectionsStreamPerformFunction) AiAgentStreamChatAction.this::perform);
        }
    }

    protected SseEmitterHandler resumePerform(
        Parameters inputParameters, Map<String, ComponentConnection> connectionParameters, Parameters extensions,
        Parameters continueParameters, Parameters data, ActionContext context) throws Exception {

        AtomicReference<@Nullable SseEmitter> emitterReference = new AtomicReference<>();
        Queue<Map<String, @Nullable Object>> bufferedEvents = new ConcurrentLinkedQueue<>();

        ChatClientRequestSpec chatClientRequestSpec = buildPatchedRequestSpec(
            inputParameters, connectionParameters, extensions, continueParameters, data, context);

        chatClientRequestSpec.toolContext(
            Map.of(
                AiAgentToolContextKey.ACTION_CONTEXT, context,
                AiAgentToolContextKey.SSE_EMITTER_REFERENCE, emitterReference,
                AiAgentToolContextKey.SSE_BUFFERED_EVENTS, bufferedEvents));

        Flux<Object> contentFlux = withEnvironmentContext(
            chatClientRequestSpec.stream()
                .chatResponse()
                .concatMap(chatResponse -> Flux.fromIterable(toSseEvents(chatResponse, context)))
                .doOnComplete(createAgentConversationTurnRecorder(extensions, context)));

        return createSseHandler(contentFlux, emitterReference, bufferedEvents, context);
    }

    protected SseEmitterHandler perform(
        Parameters inputParameters, Map<String, ComponentConnection> connectionParameters,
        Parameters extensions, ActionContext context) throws Exception {

        AtomicReference<@Nullable SseEmitter> emitterReference = new AtomicReference<>();
        Queue<Map<String, @Nullable Object>> bufferedEvents = new ConcurrentLinkedQueue<>();

        ToolExecutionListener toolExecutionListener = toolExecutionEvent -> {
            Map<String, @Nullable Object> toolExecutionLogEntry = new LinkedHashMap<>();

            toolExecutionLogEntry.put("confidence", toolExecutionEvent.confidence());
            toolExecutionLogEntry.put("inputs", toolExecutionEvent.inputs());
            toolExecutionLogEntry.put("reasoning", toolExecutionEvent.reasoning());
            toolExecutionLogEntry.put("toolName", toolExecutionEvent.toolName());

            context.log(log -> log.info(JsonUtils.write(toolExecutionLogEntry)));

            Map<String, @Nullable Object> eventData = new LinkedHashMap<>();

            eventData.put(AiAgentSseEventType.EVENT_TYPE, AiAgentSseEventType.TOOL_EXECUTION);
            eventData.put("confidence", toolExecutionEvent.confidence());
            eventData.put("inputs", toolExecutionEvent.inputs());
            eventData.put("output", toolExecutionEvent.output());
            eventData.put("reasoning", toolExecutionEvent.reasoning());
            eventData.put("toolName", toolExecutionEvent.toolName());

            SseEmitter sseEmitter = emitterReference.get();

            if (sseEmitter == null) {
                bufferedEvents.add(eventData);
            } else {
                try {
                    sseEmitter.send(eventData);
                } catch (Exception exception) {
                    context.log(log -> log.warn(
                        "Failed to send tool execution event: {}", exception.getMessage(), exception));
                }
            }
        };

        ChatClientRequestSpec chatClientRequestSpec = getChatClientRequestSpec(
            inputParameters, connectionParameters, extensions, toolExecutionListener, context);

        chatClientRequestSpec.toolContext(
            Map.of(
                AiAgentToolContextKey.ACTION_CONTEXT, context,
                AiAgentToolContextKey.SSE_EMITTER_REFERENCE, emitterReference,
                AiAgentToolContextKey.SSE_BUFFERED_EVENTS, bufferedEvents));

        Flux<Object> contentFlux = withEnvironmentContext(
            chatClientRequestSpec.stream()
                .chatResponse()
                .concatMap(chatResponse -> Flux.fromIterable(toSseEvents(chatResponse, context)))
                .doOnComplete(createAgentConversationTurnRecorder(extensions, context)));

        return createSseHandler(contentFlux, emitterReference, bufferedEvents, context);
    }

    static SseEmitterHandler createSseHandler(
        Flux<Object> contentFlux, AtomicReference<@Nullable SseEmitter> emitterReference,
        Queue<Map<String, @Nullable Object>> bufferedEvents, ActionContext context) {

        Flow.Publisher<?> effectivePublisher = FlowAdapters.toFlowPublisher(contentFlux);

        return emitter -> {
            emitterReference.set(emitter);

            Map<String, @Nullable Object> bufferedEvent;

            while ((bufferedEvent = bufferedEvents.poll()) != null) {
                try {
                    emitter.send(bufferedEvent);
                } catch (Exception exception) {
                    context.log(log -> log.warn(
                        "Failed to send buffered tool execution event: {}", exception.getMessage(), exception));
                }
            }

            effectivePublisher.subscribe(
                new Flow.Subscriber<Object>() {

                    private Flow.@Nullable Subscription subscription;

                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        this.subscription = subscription;

                        emitter.addTimeoutListener(subscription::cancel);

                        subscription.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(Object item) {
                        try {
                            emitter.send(item);
                        } catch (Exception exception) {
                            context.log(log -> log.warn(
                                "SSE send failed for stream item; cancelling subscription. {}",
                                exception.getMessage(), exception));

                            if (subscription != null) {
                                subscription.cancel();
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        emitter.error(throwable);
                    }

                    @Override
                    public void onComplete() {
                        emitter.complete();
                    }
                });
        };
    }

    private static Flux<Object> withEnvironmentContext(Flux<Object> flux) {
        Environment environment = EnvironmentContext.fetchCurrentEnvironment();

        if (environment == null) {
            return flux;
        }

        return flux.contextWrite(
            reactor.util.context.Context.of(EnvironmentContextThreadLocalAccessor.KEY, environment));
    }

    private static List<Object> toSseEvents(ChatResponse chatResponse, ActionContext context) {
        List<Object> events = new ArrayList<>();

        Generation result = chatResponse.getResult();

        if (result != null) {
            AssistantMessage output = result.getOutput();

            if (output != null) {
                String text = output.getText();

                if (text != null && !text.isEmpty()) {
                    events.add(text);
                }
            }
        }

        Map<String, Object> guardrailMetadata = ModelUtils.extractGuardrailMetadata(chatResponse);

        if (!guardrailMetadata.isEmpty()) {
            Map<String, @Nullable Object> guardrailEvent = new LinkedHashMap<>();

            guardrailEvent.put("__eventType", "guardrail");
            guardrailEvent.putAll(guardrailMetadata);

            events.add(guardrailEvent);

            context.log(log -> log.warn(
                "SSE guardrail metadata forwarded to subscriber: {}", guardrailMetadata.keySet()));
        }

        return events;
    }
}
