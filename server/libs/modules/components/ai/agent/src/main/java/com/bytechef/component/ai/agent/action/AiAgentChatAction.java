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

import static com.bytechef.component.ai.agent.constant.AiAgentConstants.CHAT;
import static com.bytechef.component.ai.agent.constant.AiAgentConstants.CHAT_PROPERTIES;
import static com.bytechef.component.definition.ComponentDsl.action;

import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.MultipleConnectionsOutputFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;

/**
 * @author Ivica Cardic
 */
public class AiAgentChatAction extends AbstractAiAgentChatAction {

    public static ChatActionDefinitionWrapper of(ClusterElementDefinitionService clusterElementDefinitionService) {
        return new AiAgentChatAction(clusterElementDefinitionService).build();
    }

    private AiAgentChatAction(ClusterElementDefinitionService clusterElementDefinitionService) {
        super(clusterElementDefinitionService);
    }

    private ChatActionDefinitionWrapper build() {
        return new ChatActionDefinitionWrapper(
            action(CHAT)
                .title("Chat")
                .description("Chat with the AI agent.")
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
            return Optional.of((MultipleConnectionsPerformFunction) AiAgentChatAction.this::perform);
        }
    }

    protected Object perform(
        Parameters inputParameters, Map<String, ComponentConnection> connectionParameters, Parameters extensions,
        ActionContext context) throws Exception {

        List<ToolExecutionEvent> toolExecutionEvents = new ArrayList<>();

        ToolExecutionListener toolExecutionListener = toolExecutionEvent -> {
            context.log(
                log -> log.info(
                    "Tool execution: {} | Reasoning: {} | Confidence: {} | Inputs: {} | Output: {}",
                    toolExecutionEvent.toolName(), toolExecutionEvent.reasoning(), toolExecutionEvent.confidence(),
                    toolExecutionEvent.inputs(), toolExecutionEvent.output()));

            if (context.isEditorEnvironment()) {
                toolExecutionEvents.add(toolExecutionEvent);
            }
        };

        ChatClientRequestSpec chatClientRequestSpec = getChatClientRequestSpec(
            inputParameters, connectionParameters, extensions, toolExecutionListener, context);

        ChatClient.CallResponseSpec call = chatClientRequestSpec.call();

        Object chatResponse = ModelUtils.getChatResponse(call, inputParameters, context);

        if (context.isEditorEnvironment() && !toolExecutionEvents.isEmpty()) {
            Map<String, Object> response = new LinkedHashMap<>();

            response.put("response", chatResponse);

            List<Map<String, Object>> toolExecutions = toolExecutionEvents.stream()
                .map(toolExecutionEvent -> {
                    Map<String, Object> eventData = new LinkedHashMap<>();

                    eventData.put("confidence", toolExecutionEvent.confidence());
                    eventData.put("inputs", toolExecutionEvent.inputs());
                    eventData.put("output", toolExecutionEvent.output());
                    eventData.put("reasoning", toolExecutionEvent.reasoning());
                    eventData.put("toolName", toolExecutionEvent.toolName());

                    return eventData;
                })
                .toList();

            response.put("toolExecutions", toolExecutions);

            return response;
        }

        return chatResponse;
    }
}
