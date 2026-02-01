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
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.MultipleConnectionsOutputFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsStreamPerformFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Flow;
import org.reactivestreams.FlowAdapters;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import reactor.core.publisher.Flux;

/**
 * @author Ivica Cardic
 */
public class AiAgentStreamChatAction extends AbstractAiAgentChatAction {

    public static ChatActionDefinitionWrapper of(ClusterElementDefinitionService clusterElementDefinitionService) {
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

    protected Flow.Publisher<?> perform(
        Parameters inputParameters, Map<String, ComponentConnection> connectionParameters,
        Parameters extensions, ActionContext context) throws Exception {

        ChatClient.ChatClientRequestSpec chatClientRequestSpec = getChatClientRequestSpec(
            inputParameters, connectionParameters, extensions, context);

        SimpleLoggerAdvisor simpleLoggerAdvisor = SimpleLoggerAdvisor.builder()
            .build();

        var streamResponseSpec = chatClientRequestSpec
            .advisors(simpleLoggerAdvisor)
            .stream();

        try {
            Flux<String> contentFlux = streamResponseSpec.content();

            return FlowAdapters.toFlowPublisher(contentFlux);
        } catch (Throwable ignoredContent) {
            Flux<?> contentFlux = streamResponseSpec.chatResponse()
                .map(chatResponse -> {
                    Object payload = chatResponse;

                    String text = chatResponse.getResult()
                        .getOutput()
                        .getText();

                    if (text != null) {
                        payload = text;
                    }

                    return payload;
                });

            return FlowAdapters.toFlowPublisher(contentFlux);
        }
    }
}
