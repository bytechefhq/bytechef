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
import static com.bytechef.component.ai.agent.constant.AiAgentConstants.CONVERSATION_ID;
import static com.bytechef.component.ai.llm.constant.LLMConstants.ATTACHMENTS_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FORMAT_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MESSAGES_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PROMPT_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SYSTEM_PROMPT_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction.CHAT_MEMORY;
import static com.bytechef.platform.component.definition.ai.agent.ModelFunction.MODEL;
import static com.bytechef.platform.component.definition.ai.agent.RagFunction.RAG;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.ai.agent.ToolFunction;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.MultipleConnectionsOutputFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.definition.ai.agent.RagFunction;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.util.JsonSchemaGeneratorUtils;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

/**
 * @author Ivica Cardic
 */
public class AiAgentChatAction {

    public final ChatActionDefinition actionDefinition;
    private final ClusterElementDefinitionService clusterElementDefinitionService;

    public AiAgentChatAction(ClusterElementDefinitionService clusterElementDefinitionService) {

        actionDefinition = new ChatActionDefinition(
            action(CHAT)
                .title("Chat")
                .description("Chat with the AI agent.")
                .properties(
                    FORMAT_PROPERTY,
                    PROMPT_PROPERTY,
                    SYSTEM_PROMPT_PROPERTY,
                    ATTACHMENTS_PROPERTY,
                    MESSAGES_PROPERTY,
                    RESPONSE_PROPERTY,
                    string(CONVERSATION_ID)
                        .description("The conversation id used in conjunction with memory."))
                .output(
                    (MultipleConnectionsOutputFunction) (
                        inputParameters, componentConnections, extensions, context) -> ModelUtils.output(
                            inputParameters, null, context)));

        this.clusterElementDefinitionService = clusterElementDefinitionService;
    }

    public class ChatActionDefinition extends AbstractActionDefinitionWrapper {

        public ChatActionDefinition(ActionDefinition actionDefinition) {
            super(actionDefinition);
        }

        @Override
        public Optional<BasePerformFunction> getPerform() {
            return Optional.of((MultipleConnectionsPerformFunction) this::perform);
        }

        protected Object perform(
            Parameters inputParameters, Map<String, ComponentConnection> connectionParameters,
            Parameters extensions, ActionContext actionContext) throws Exception {

            ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

            ClusterElement clusterElement = clusterElementMap.getClusterElement(MODEL);

            ModelFunction modelFunction = clusterElementDefinitionService.getClusterElement(
                clusterElement.getComponentName(), clusterElement.getComponentVersion(),
                clusterElement.getClusterElementName());

            ComponentConnection componentConnection = connectionParameters.get(clusterElement.getWorkflowNodeName());

            ChatModel chatModel = (ChatModel) modelFunction.apply(
                ParametersFactory.createParameters(
                    MapUtils.concat(Map.copyOf(inputParameters.toMap()), Map.copyOf(clusterElement.getParameters()))),
                ParametersFactory.createParameters(componentConnection.getParameters()), true);

            ChatClient chatClient = ChatClient.builder(chatModel)
                .build();

            ChatClient.CallResponseSpec call = chatClient.prompt()
                .advisors(getAdvisors(clusterElementMap, connectionParameters))
                .advisors(getConversationAdvisor(inputParameters))
                .messages(ModelUtils.getMessages(inputParameters, actionContext))
                .toolCallbacks(
                    getToolCallbacks(
                        clusterElementMap.getClusterElements(ToolFunction.TOOLS), connectionParameters,
                        actionContext.isEditorEnvironment()))
                .call();

            return ModelUtils.getChatResponse(call, inputParameters, actionContext);
        }
    }

    private List<Advisor> getAdvisors(
        ClusterElementMap clusterElementMap, Map<String, ComponentConnection> connectionParameters) {

        List<Advisor> advisors = new ArrayList<>();

        // memory

        clusterElementMap.fetchClusterElement(CHAT_MEMORY)
            .map(clusterElement -> getChatMemoryAdvisor(connectionParameters, clusterElement))
            .ifPresent(advisors::add);

        // RAG

        clusterElementMap.fetchClusterElement(RAG)
            .map(clusterElement -> getRagAdvisor(connectionParameters, clusterElement))
            .ifPresent(advisors::add);

        // logger

        advisors.add(new SimpleLoggerAdvisor());

        return advisors;
    }

    private Advisor getChatMemoryAdvisor(
        Map<String, ComponentConnection> componentConnections, ClusterElement clusterElement) {

        ChatMemoryFunction chatMemoryFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        try {
            return chatMemoryFunction.apply(
                ParametersFactory.createParameters(clusterElement.getParameters()),
                getConnectionParameters(componentConnections, clusterElement),
                ParametersFactory.createParameters(clusterElement.getExtensions()), componentConnections);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Parameters getConnectionParameters(
        Map<String, ComponentConnection> componentConnections, ClusterElement clusterElement) {

        ComponentConnection componentConnection = componentConnections.get(clusterElement.getWorkflowNodeName());

        return ParametersFactory.createParameters(
            componentConnection == null ? Map.of() : componentConnection.getParameters());
    }

    private static Consumer<ChatClient.AdvisorSpec> getConversationAdvisor(Parameters inputParameters) {
        return advisor -> {
            String conversationId = inputParameters.getString(CONVERSATION_ID);

            if (conversationId != null) {
                advisor.param(ChatMemory.CONVERSATION_ID, conversationId);
            }
        };
    }

    private Advisor getRagAdvisor(
        Map<String, ComponentConnection> componentConnections, ClusterElement clusterElement) {

        RagFunction ragFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        try {
            return ragFunction.apply(
                ParametersFactory.createParameters(clusterElement.getParameters()),
                getConnectionParameters(componentConnections, clusterElement),
                ParametersFactory.createParameters(clusterElement.getExtensions()), componentConnections);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressFBWarnings("NP")
    private List<ToolCallback> getToolCallbacks(
        List<ClusterElement> toolClusterElements, Map<String, ComponentConnection> connectionParameters,
        boolean editorEnvironment) {

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (ClusterElement clusterElement : toolClusterElements) {
            ClusterElementDefinition clusterElementDefinition =
                clusterElementDefinitionService.getClusterElementDefinition(
                    clusterElement.getComponentName(), clusterElement.getComponentVersion(),
                    clusterElement.getClusterElementName());

            ComponentConnection componentConnection = connectionParameters.get(clusterElement.getWorkflowNodeName());

            FunctionToolCallback.Builder<Map<String, Object>, Object> builder = FunctionToolCallback.builder(
                clusterElementDefinition.getName(),
                getToolCallbackFunction(
                    clusterElement.getComponentName(), clusterElement.getComponentVersion(),
                    clusterElementDefinition.getName(), clusterElement.getParameters(), componentConnection,
                    editorEnvironment))
                .inputType(Map.class)
                .inputSchema(JsonSchemaGeneratorUtils.generateInputSchema(clusterElementDefinition.getProperties()));

            if (clusterElementDefinition.getDescription() != null) {
                builder.description(clusterElementDefinition.getDescription());
            }

            toolCallbacks.add(builder.build());
        }

        return toolCallbacks;
    }

    private Function<Map<String, Object>, Object> getToolCallbackFunction(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> parameters,
        ComponentConnection componentConnection, boolean editorEnvironment) {

        return request -> clusterElementDefinitionService.executeTool(
            componentName, componentVersion, clusterElementName, MapUtils.concat(request, new HashMap<>(parameters)),
            componentConnection, editorEnvironment);
    }
}
