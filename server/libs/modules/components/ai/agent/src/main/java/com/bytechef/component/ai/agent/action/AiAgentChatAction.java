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

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.ai.agent.action.event.ToolExecutionEvent;
import com.bytechef.component.ai.agent.action.event.listener.ToolExecutionListener;
import com.bytechef.component.ai.llm.facade.AiAgentToolFacade;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.ai.tool.AiAgentToolContext;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.MultipleConnectionsOutputFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsResumePerformFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.model.tool.ToolCallingManager;

/**
 * @author Ivica Cardic
 */
public class AiAgentChatAction extends AbstractAiAgentChatAction {

    public static ChatActionDefinitionWrapper of(
        AiAgentToolFacade aiAgentToolFacade, ClusterElementDefinitionService clusterElementDefinitionService,
        ToolCallingManager toolCallingManager) {

        return new AiAgentChatAction(aiAgentToolFacade, clusterElementDefinitionService, toolCallingManager).build();
    }

    private AiAgentChatAction(
        AiAgentToolFacade aiAgentToolFacade, ClusterElementDefinitionService clusterElementDefinitionService,
        ToolCallingManager toolCallingManager) {

        super(aiAgentToolFacade, clusterElementDefinitionService, toolCallingManager);
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
                            inputParameters, null, context))
                .help("", "https://docs.bytechef.io/reference/components/ai-agent_v1#chat")
                .resumePerform(
                    (MultipleConnectionsResumePerformFunction) this::resumePerform));
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

    protected @Nullable Object resumePerform(
        Parameters inputParameters, Map<String, ComponentConnection> connectionParameters, Parameters extensions,
        Parameters continueParameters, Parameters data, ActionContext context) throws Exception {

        List<ToolExecutionEvent> toolExecutionEvents = new ArrayList<>();

        ModelUtils.ChatActionResult chatActionResult = resumeChat(
            inputParameters, connectionParameters, extensions, continueParameters, data,
            createToolExecutionListener(toolExecutionEvents, context), context);

        return toOutput(chatActionResult, toolExecutionEvents, context);
    }

    @Nullable
    protected Object perform(
        Parameters inputParameters, Map<String, ComponentConnection> connectionParameters, Parameters extensions,
        ActionContext context) throws Exception {

        List<ToolExecutionEvent> toolExecutionEvents = new ArrayList<>();

        ChatClientRequestSpec chatClientRequestSpec = getChatClientRequestSpec(
            inputParameters, connectionParameters, extensions,
            createToolExecutionListener(toolExecutionEvents, context), context);

        applyStructuredOutputValidation(chatClientRequestSpec, inputParameters, context);

        chatClientRequestSpec.toolContext(new AiAgentToolContext(context).toMap());

        ChatClient.CallResponseSpec call = chatClientRequestSpec.call();

        ModelUtils.ChatActionResult chatActionResult = ModelUtils.getChatActionResult(
            call, inputParameters, context);

        return toOutput(chatActionResult, toolExecutionEvents, context);
    }

    private static ToolExecutionListener createToolExecutionListener(
        List<ToolExecutionEvent> toolExecutionEvents, ActionContext context) {

        return toolExecutionEvent -> {
            Map<String, @Nullable Object> toolExecutionLogEntry = new LinkedHashMap<>();

            toolExecutionLogEntry.put("confidence", toolExecutionEvent.confidence());
            toolExecutionLogEntry.put("inputs", toolExecutionEvent.inputs());
            toolExecutionLogEntry.put("output", toolExecutionEvent.output());
            toolExecutionLogEntry.put("reasoning", toolExecutionEvent.reasoning());
            toolExecutionLogEntry.put("toolName", toolExecutionEvent.toolName());

            context.log(log -> log.info(JsonUtils.write(toolExecutionLogEntry)));

            if (context.isEditorEnvironment() &&
                (!(context instanceof ActionContextAware actionContextAware) ||
                    actionContextAware.getJobId() == null)) {

                toolExecutionEvents.add(toolExecutionEvent);
            }
        };
    }

    private static @Nullable Object toOutput(
        ModelUtils.ChatActionResult chatActionResult, List<ToolExecutionEvent> toolExecutionEvents,
        ActionContext context) {

        Object chatResponse = chatActionResult.response();

        boolean editorWithToolEvents = context.isEditorEnvironment() && !toolExecutionEvents.isEmpty();
        boolean hasGuardrailMetadata = chatActionResult.hasGuardrailMetadata();

        if (!editorWithToolEvents && !hasGuardrailMetadata) {
            return chatResponse;
        }

        Map<String, @Nullable Object> response = new LinkedHashMap<>();

        response.put("response", chatResponse);

        if (editorWithToolEvents) {
            List<Map<String, @Nullable Object>> toolExecutions = toolExecutionEvents.stream()
                .map(toolExecutionEvent -> {
                    Map<String, @Nullable Object> eventData = new LinkedHashMap<>();

                    eventData.put("confidence", toolExecutionEvent.confidence());
                    eventData.put("inputs", toolExecutionEvent.inputs());
                    eventData.put("output", toolExecutionEvent.output());
                    eventData.put("reasoning", toolExecutionEvent.reasoning());
                    eventData.put("toolName", toolExecutionEvent.toolName());

                    return eventData;
                })
                .toList();

            response.put("toolExecutions", toolExecutions);
        }

        if (hasGuardrailMetadata) {
            response.put("guardrail", chatActionResult.guardrailMetadata());
        }

        return response;
    }
}
