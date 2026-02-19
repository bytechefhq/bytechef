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

import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.MultipleConnectionsOutputFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsStreamPerformFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.FlowAdapters;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;

/**
 * @author Ivica Cardic
 */
public class AiAgentStreamChatAction extends AbstractAiAgentChatAction {

    public static ActionDefinition of(ClusterElementDefinitionService clusterElementDefinitionService) {
        return new AiAgentStreamChatAction(clusterElementDefinitionService).build();
    }

    private AiAgentStreamChatAction(ClusterElementDefinitionService clusterElementDefinitionService) {
        super(clusterElementDefinitionService);
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
                            inputParameters, null, context)));
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

    protected SseEmitterHandler perform(
        Parameters inputParameters, Map<String, ComponentConnection> connectionParameters,
        Parameters extensions, ActionContext context) throws Exception {

        AtomicReference<SseEmitterHandler.SseEmitter> emitterReference = new AtomicReference<>();
        Queue<Map<String, Object>> bufferedEvents = new ConcurrentLinkedQueue<>();

        ToolExecutionListener toolExecutionListener = toolExecutionEvent -> {
            Map<String, Object> eventData = new LinkedHashMap<>();

            eventData.put("__eventType", "tool_execution");
            eventData.put("confidence", toolExecutionEvent.confidence());
            eventData.put("inputs", toolExecutionEvent.inputs());
            eventData.put("output", toolExecutionEvent.output());
            eventData.put("reasoning", toolExecutionEvent.reasoning());
            eventData.put("toolName", toolExecutionEvent.toolName());

            SseEmitterHandler.SseEmitter sseEmitter = emitterReference.get();

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

        var streamResponseSpec = chatClientRequestSpec.stream();

        Flow.Publisher<?> publisher;

        try {
            Flux<String> contentFlux = streamResponseSpec.content();

            publisher = FlowAdapters.toFlowPublisher(contentFlux);
        } catch (Throwable ignoredContent) {
            Flux<?> contentFlux = streamResponseSpec.chatResponse()
                .map(chatResponse -> {
                    Object payload = chatResponse;

                    Generation result = chatResponse.getResult();

                    if (result == null) {
                        return payload;
                    }

                    AssistantMessage output = result.getOutput();

                    String text = output.getText();

                    if (text != null) {
                        payload = text;
                    }

                    return payload;
                });

            publisher = FlowAdapters.toFlowPublisher(contentFlux);
        }

        Flow.Publisher<?> effectivePublisher = publisher;

        return emitter -> {
            emitterReference.set(emitter);

            Map<String, Object> bufferedEvent;

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

                    private Flow.Subscription subscription;

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
                            context.log(log -> log.trace(exception.getMessage(), exception));

                            subscription.cancel();
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
}
