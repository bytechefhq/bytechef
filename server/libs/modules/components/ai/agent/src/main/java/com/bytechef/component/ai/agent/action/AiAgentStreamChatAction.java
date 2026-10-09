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

import static com.bytechef.component.ai.agent.constant.AiAgentConstants.CHAT_PROPERTIES;
import static com.bytechef.component.definition.ComponentDsl.action;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.ai.agent.action.event.listener.ToolExecutionListener;
import com.bytechef.component.ai.llm.facade.AiAgentToolFacade;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.ai.constant.AiAgentSseEventType;
import com.bytechef.platform.ai.tool.AiAgentToolContext;
import com.bytechef.platform.ai.tool.AiAgentToolContext.SseTransport;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.MultipleConnectionsOutputFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsResumePerformFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsStreamPerformFunction;
import com.bytechef.platform.component.definition.SuspendAwareSseEmitterHandler;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.context.EnvironmentContextThreadLocalAccessor;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.TenantContextThreadLocalAccessor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.FlowAdapters;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.model.tool.ToolCallingManager;
import reactor.core.publisher.Flux;

/**
 * @author Ivica Cardic
 */
public class AiAgentStreamChatAction extends AbstractAiAgentChatAction {

    public static ActionDefinition of(
        AiAgentToolFacade aiAgentToolFacade, ClusterElementDefinitionService clusterElementDefinitionService,
        ToolCallingManager toolCallingManager) {

        return new AiAgentStreamChatAction(aiAgentToolFacade, clusterElementDefinitionService, toolCallingManager)
            .build();
    }

    private AiAgentStreamChatAction(
        AiAgentToolFacade aiAgentToolFacade, ClusterElementDefinitionService clusterElementDefinitionService,
        ToolCallingManager toolCallingManager) {

        super(aiAgentToolFacade, clusterElementDefinitionService, toolCallingManager);
    }

    private ChatActionDefinitionWrapper build() {
        return new ChatActionDefinitionWrapper(
            action("streamChat")
                .title("Chat (stream)")
                .description("Chat with the AI agent and stream the response.")
                .properties(CHAT_PROPERTIES)
                .output(
                    (MultipleConnectionsOutputFunction) (
                        inputParameters, componentConnections, extensions, context) -> ModelUtils.output(
                            inputParameters, null, context))
                .help("", "https://docs.bytechef.io/reference/components/ai-agent_v1#chat-stream")
                .resumePerform((MultipleConnectionsResumePerformFunction) this::resumePerform));
    }

    public class ChatActionDefinitionWrapper extends AbstractActionDefinitionWrapper {

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

        SseTransport sseTransport = new SseTransport();
        TurnTextSeparator turnTextSeparator = new TurnTextSeparator();

        ChatClientRequestSpec chatClientRequestSpec = buildResumedRequestSpec(
            inputParameters, connectionParameters, extensions, continueParameters, data,
            createToolExecutionListener(sseTransport, turnTextSeparator, context), context);

        chatClientRequestSpec.toolContext(new AiAgentToolContext(context, sseTransport).toMap());

        Flux<Object> contentFlux = withExecutionContext(
            chatClientRequestSpec.stream()
                .chatResponse()
                .concatMap(chatResponse -> Flux.fromIterable(toSseEvents(chatResponse, turnTextSeparator, context))));

        return createSseHandler(contentFlux, sseTransport, context);
    }

    protected SseEmitterHandler perform(
        Parameters inputParameters, Map<String, ComponentConnection> connectionParameters,
        Parameters extensions, ActionContext context) throws Exception {

        SseTransport sseTransport = new SseTransport();
        TurnTextSeparator turnTextSeparator = new TurnTextSeparator();

        ChatClientRequestSpec chatClientRequestSpec = getChatClientRequestSpec(
            inputParameters, connectionParameters, extensions,
            createToolExecutionListener(sseTransport, turnTextSeparator, context), context);

        chatClientRequestSpec.toolContext(new AiAgentToolContext(context, sseTransport).toMap());

        Flux<Object> contentFlux = withExecutionContext(
            chatClientRequestSpec.stream()
                .chatResponse()
                .concatMap(chatResponse -> Flux.fromIterable(toSseEvents(chatResponse, turnTextSeparator, context))));

        return createSseHandler(contentFlux, sseTransport, context);
    }

    static ToolExecutionListener createToolExecutionListener(
        SseTransport sseTransport, TurnTextSeparator turnTextSeparator, ActionContext context) {

        return toolExecutionEvent -> {
            turnTextSeparator.markToolExecuted();

            Map<String, @Nullable Object> toolExecutionLogEntry = new LinkedHashMap<>();

            toolExecutionLogEntry.put("confidence", toolExecutionEvent.confidence());
            toolExecutionLogEntry.put("inputs", toolExecutionEvent.inputs());
            toolExecutionLogEntry.put("output", toolExecutionEvent.output());
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

            try {
                sseTransport.send(eventData);
            } catch (Exception exception) {
                context.log(log -> log.warn(
                    "Failed to send tool execution event: {}", exception.getMessage(), exception));
            }
        };
    }

    static SseEmitterHandler createSseHandler(
        Flux<Object> contentFlux, SseTransport sseTransport, ActionContext context) {

        Flow.Publisher<?> effectivePublisher = FlowAdapters.toFlowPublisher(contentFlux);

        SseEmitterHandler sseEmitterHandler = emitter -> {
            sseTransport.attach(
                emitter,
                exception -> context.log(log -> log.warn(
                    "Failed to send buffered agent event: {}", exception.getMessage(), exception)));

            effectivePublisher.subscribe(
                new Flow.Subscriber<Object>() {

                    private final StringBuilder responseTextBuilder = new StringBuilder();
                    private Flow.@Nullable Subscription subscription;

                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        this.subscription = subscription;

                        emitter.addTimeoutListener(subscription::cancel);

                        subscription.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(Object item) {
                        if (item instanceof String text) {
                            responseTextBuilder.append(text);
                        }

                        try {
                            emitter.send(item);
                        } catch (Exception exception) {
                            context.log(log -> log.warn(
                                "SSE send failed for stream item; cancelling subscription. {}",
                                exception.getMessage(), exception));

                            if (subscription != null) {
                                subscription.cancel();
                            }

                            emitter.error(exception);
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        context.log(log -> log.error("AI agent stream failed", throwable));

                        emitter.error(throwable);
                    }

                    @Override
                    public void onComplete() {
                        if (!responseTextBuilder.isEmpty()) {
                            String responseText = responseTextBuilder.toString();

                            context.log(log -> log.info("AI agent response: {}", responseText));
                        }

                        emitter.complete();
                    }
                });
        };

        if (context instanceof ActionContextAware actionContextAware) {
            return new SuspendAwareSseEmitterHandler(sseEmitterHandler, actionContextAware);
        }

        return sseEmitterHandler;
    }

    static Flux<Object> withExecutionContext(Flux<Object> flux) {
        reactor.util.context.Context reactorContext = reactor.util.context.Context.of(
            TenantContextThreadLocalAccessor.KEY, TenantContext.getCurrentTenantId());

        Environment environment = EnvironmentContext.fetchCurrentEnvironment();

        if (environment != null) {
            reactorContext = reactorContext.put(EnvironmentContextThreadLocalAccessor.KEY, environment);
        }

        return flux.contextWrite(reactorContext);
    }

    static List<Object> toSseEvents(
        ChatResponse chatResponse, TurnTextSeparator turnTextSeparator, ActionContext context) {

        List<Object> events = new ArrayList<>();

        Generation result = chatResponse.getResult();

        if (result != null) {
            AssistantMessage output = result.getOutput();

            if (output != null) {
                String text = output.getText();

                if (text != null && !text.isEmpty()) {
                    events.add(turnTextSeparator.apply(text));
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

    static final class TurnTextSeparator {

        private static final String SEPARATOR = "\n\n";

        private final AtomicBoolean textEmitted = new AtomicBoolean();
        private final AtomicBoolean toolExecuted = new AtomicBoolean();

        void markToolExecuted() {
            toolExecuted.set(true);
        }

        String apply(String text) {
            boolean separate = toolExecuted.getAndSet(false) && textEmitted.get();

            textEmitted.set(true);

            return separate ? SEPARATOR + text : text;
        }
    }
}
