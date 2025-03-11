/*
 * Copyright 2023-present ByteChef Inc.
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
import static com.bytechef.component.ai.agent.constant.AiAgentConstants.CONVERSATION_ID;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MESSAGES_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.definition.aiagent.MemoryFunction.MEMORY;
import static com.bytechef.platform.component.definition.aiagent.ModelFunction.MODEL;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.ai.agent.util.JsonSchemaGenerator;
import com.bytechef.component.ai.llm.util.LLMUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.aiagent.ToolFunction;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ContextFactory;
import com.bytechef.platform.component.definition.MultipleConnectionsOutputFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.aiagent.MemoryFunction;
import com.bytechef.platform.component.definition.aiagent.ModelFunction;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElements;
import com.bytechef.platform.configuration.domain.ClusterElements.ClusterElement;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

/**
 * @author Ivica Cardic
 */
public class AiAgentChatAction {

    public final ChatActionDefinition actionDefinition;

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ContextFactory contextFactory;

    public AiAgentChatAction(
        ClusterElementDefinitionService clusterElementDefinitionService, ContextFactory contextFactory) {
        actionDefinition = new ChatActionDefinition(
            action(CHAT)
                .title("Chat")
                .description("Chat with the AI agent.")
                .properties(
                    MESSAGES_PROPERTY,
                    RESPONSE_PROPERTY,
                    string(CONVERSATION_ID)
                        .description("The conversation id used in conjunction with memory."))
                .output(
                    (MultipleConnectionsOutputFunction) (
                        inputParameters, componentConnections, extensions, context) -> LLMUtils.output(
                            inputParameters, null, context)));

        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.contextFactory = contextFactory;
    }

    public class ChatActionDefinition extends AbstractActionDefinitionWrapper {

        public ChatActionDefinition(ActionDefinition actionDefinition) {
            super(actionDefinition);
        }

        @Override
        public Optional<PerformFunction> getPerform() {
            return Optional.of((MultipleConnectionsPerformFunction) this::perform);
        }

        protected Object perform(
            Parameters inputParameters, Map<String, ComponentConnection> connectionParameters,
            Parameters extensions, ActionContext actionContext) throws Exception {

            ClusterElements clusterElements = ClusterElements.of(extensions);

            ClusterElement modelClusterElement = clusterElements.getFirst(MODEL.name());

            ComponentConnection componentConnection = connectionParameters.get(modelClusterElement.getComponentName());

            ModelFunction modelFunction = clusterElementDefinitionService.getClusterElementObject(
                modelClusterElement.getComponentName(), modelClusterElement.getComponentVersion(), MODEL);

            ActionContextAware actionContextAware = (ActionContextAware) actionContext;

            ChatModel chatModel = modelFunction.apply(
                ParametersFactory.createParameters(modelClusterElement.getParameters()),
                ParametersFactory.createParameters(componentConnection.getParameters()),
                contextFactory.createContext(
                    modelClusterElement.getComponentName(), componentConnection,
                    actionContextAware.isEditorEnvironment()));

            ChatClient chatClient = ChatClient.builder(chatModel)
                .build();

            ChatClient.CallResponseSpec call = chatClient.prompt()
                .advisors(getAdvisors(clusterElements, connectionParameters))
                .advisors(advisor -> {
                    String conversationId = inputParameters.getString(CONVERSATION_ID);

                    if (conversationId != null) {
                        advisor.param(CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId);
                    }
                })
                .messages(LLMUtils.getMessages(inputParameters, actionContext))
                .tools(
                    getTools(
                        clusterElements.get(ToolFunction.TOOL.name()), connectionParameters,
                        actionContextAware.isEditorEnvironment()))
                .call();

            return LLMUtils.getChatResponse(call, inputParameters, actionContext);
        }
    }

    private List<Advisor> getAdvisors(
        ClusterElements clusterElements, Map<String, ComponentConnection> connectionParameters) {

        List<Advisor> advisors = new ArrayList<>();

        clusterElements.fetchFirst(MEMORY.name())
            .map(clusterElement -> {
                MemoryFunction memoryFunction = clusterElementDefinitionService.getClusterElementObject(
                    clusterElement.getComponentName(), clusterElement.getComponentVersion(), MEMORY);

                ComponentConnection componentConnection = connectionParameters.get(clusterElement.getComponentName());

                try {
                    return memoryFunction.apply(
                        ParametersFactory.createParameters(clusterElement.getParameters()),
                        componentConnection == null
                            ? null : ParametersFactory.createParameters(componentConnection.getParameters()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .ifPresent(advisors::add);

        advisors.add(new SimpleLoggerAdvisor());

        return advisors;
    }

    @SuppressFBWarnings("NP")
    private List<ToolCallback> getTools(
        List<ClusterElement> toolClusterElements, Map<String, ComponentConnection> connectionParameters,
        boolean editorEnvironment) {

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (ClusterElement clusterElement : toolClusterElements) {
            ClusterElementDefinition clusterElementDefinition =
                clusterElementDefinitionService.getClusterElementDefinition(
                    clusterElement.getComponentName(), clusterElement.getComponentVersion(), ToolFunction.TOOL,
                    clusterElement.getComponentOperation());

            ToolFunction toolFunction = clusterElementDefinitionService.getClusterElementObject(
                clusterElementDefinition.getComponentName(), clusterElementDefinition.getComponentVersion(),
                ToolFunction.TOOL, clusterElementDefinition.getName());

            ComponentConnection componentConnection = connectionParameters.get(clusterElement.getComponentName());

            FunctionToolCallback.Builder<Map<String, Object>, Object> builder = FunctionToolCallback.builder(
                clusterElementDefinition.getName(),
                new ToolCallbackFunction(
                    toolFunction, clusterElement.getName(), clusterElement.getParameters(), componentConnection,
                    editorEnvironment, contextFactory))
                .inputType(Map.class)
                .inputSchema(JsonSchemaGenerator.generateInputSchema(clusterElementDefinition.getProperties()));

            if (clusterElementDefinition.getDescription() != null) {
                builder.description(clusterElementDefinition.getDescription());
            }

            toolCallbacks.add(builder.build());
        }

        return toolCallbacks;
    }

    private record ToolCallbackFunction(
        ToolFunction toolFunction, String componentName, Map<String, ?> parameters,
        ComponentConnection componentConnection, boolean editorEnvironment, ContextFactory contextFactory)
        implements Function<Map<String, Object>, Object> {

        public Object apply(Map<String, Object> request) {
            try {
                return toolFunction.apply(
                    ParametersFactory.createParameters(MapUtils.concat(request, new HashMap<>(parameters))),
                    componentConnection == null
                        ? null : ParametersFactory.createParameters(componentConnection.getParameters()),
                    contextFactory.createContext(componentName, componentConnection, editorEnvironment));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
