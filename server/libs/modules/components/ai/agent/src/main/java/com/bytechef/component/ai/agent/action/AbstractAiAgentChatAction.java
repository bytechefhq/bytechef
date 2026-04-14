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

import static com.bytechef.component.ai.agent.constant.AiAgentConstants.CONVERSATION_ID;
import static com.bytechef.platform.ai.constant.AiAgentSimulationConstants.RESPONSE_PROMPT;
import static com.bytechef.platform.ai.constant.AiAgentSimulationConstants.SIMULATION_MODEL;
import static com.bytechef.platform.ai.constant.AiAgentSimulationConstants.TOOL_SIMULATIONS;
import static com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction.CHAT_MEMORY;
import static com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction.GUARDRAILS;
import static com.bytechef.platform.component.definition.ai.agent.ModelFunction.MODEL;
import static com.bytechef.platform.component.definition.ai.agent.RagFunction.RAG;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.ai.agent.action.event.ToolExecutionEvent;
import com.bytechef.component.ai.agent.action.event.listener.ToolExecutionListener;
import com.bytechef.component.ai.agent.facade.AiAgentToolFacade;
import com.bytechef.component.ai.llm.advisor.ContextLoggerAdvisor;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolFunction;
import com.bytechef.platform.component.definition.ai.agent.RagFunction;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.augment.AugmentedToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.util.json.JsonParser;
import tools.jackson.core.type.TypeReference;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractAiAgentChatAction {

    private static final Logger logger = LoggerFactory.getLogger(AbstractAiAgentChatAction.class);

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final AiAgentToolFacade aiAgentToolFacade;

    protected AbstractAiAgentChatAction(
        ClusterElementDefinitionService clusterElementDefinitionService, AiAgentToolFacade aiAgentToolFacade) {

        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.aiAgentToolFacade = aiAgentToolFacade;
    }

    protected ChatClient.ChatClientRequestSpec getChatClientRequestSpec(
        Parameters inputParameters, Map<String, ComponentConnection> connectionParameters, Parameters extensions,
        @Nullable ToolExecutionListener toolExecutionListener, ActionContext context) throws Exception {

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        ClusterElement clusterElement = clusterElementMap.getClusterElement(MODEL);

        ModelFunction modelFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        ComponentConnection componentConnection = connectionParameters.get(clusterElement.getWorkflowNodeName());

        ChatModel chatModel = (ChatModel) modelFunction.apply(
            ParametersFactory.create(
                MapUtils.concat(new HashMap<>(inputParameters.toMap()), new HashMap<>(clusterElement.getParameters()))),
            ParametersFactory.create(componentConnection.getParameters()), true);

        @SuppressWarnings("unchecked")
        Map<String, Map<String, String>> toolSimulations =
            (Map<String, Map<String, String>>) inputParameters.get(TOOL_SIMULATIONS);

        ChatClient chatClient = ChatClient.builder(chatModel)
            .build();

        return chatClient.prompt()
            .advisors(getAdvisors(clusterElementMap, connectionParameters, context))
            .advisors(getConversationAdvisor(inputParameters))
            .messages(ModelUtils.getMessages(inputParameters, context))
            .toolCallbacks(
                getToolCallbacks(
                    clusterElementMap.getClusterElements(BaseToolFunction.TOOLS), connectionParameters,
                    context.isEditorEnvironment(), toolExecutionListener, toolSimulations, chatModel, context));
    }

    private List<Advisor> getAdvisors(
        ClusterElementMap clusterElementMap, Map<String, ComponentConnection> connectionParameters,
        ActionContext context) {

        List<Advisor> advisors = new ArrayList<>();

        // guardrails (first to block early)

        clusterElementMap.fetchClusterElement(GUARDRAILS)
            .map(clusterElement -> getGuardrailsAdvisor(connectionParameters, clusterElement))
            .ifPresent(advisors::add);

        // memory

        clusterElementMap.fetchClusterElement(CHAT_MEMORY)
            .map(clusterElement -> getChatMemoryAdvisor(connectionParameters, clusterElement))
            .ifPresent(advisors::add);

        // RAG

        clusterElementMap.fetchClusterElement(RAG)
            .map(clusterElement -> getRagAdvisor(connectionParameters, clusterElement))
            .ifPresent(advisors::add);

        // logger

        advisors.add(new ContextLoggerAdvisor(context));

        return advisors;
    }

    private Advisor getChatMemoryAdvisor(
        Map<String, ComponentConnection> componentConnections, ClusterElement clusterElement) {

        ChatMemoryFunction chatMemoryFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        try {
            return chatMemoryFunction.apply(
                ParametersFactory.create(clusterElement.getParameters()),
                getConnectionParameters(componentConnections, clusterElement),
                ParametersFactory.create(clusterElement.getExtensions()), componentConnections);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Parameters getConnectionParameters(
        Map<String, ComponentConnection> componentConnections, ClusterElement clusterElement) {

        ComponentConnection componentConnection = componentConnections.get(clusterElement.getWorkflowNodeName());

        return ParametersFactory.create(
            componentConnection == null ? Map.of() : componentConnection.getParameters());
    }

    private Advisor getGuardrailsAdvisor(
        Map<String, ComponentConnection> componentConnections, ClusterElement clusterElement) {

        GuardrailsFunction guardrailsFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        try {
            return guardrailsFunction.apply(
                ParametersFactory.create(clusterElement.getParameters()),
                getConnectionParameters(componentConnections, clusterElement),
                ParametersFactory.create(clusterElement.getExtensions()), componentConnections);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
                ParametersFactory.create(clusterElement.getParameters()),
                getConnectionParameters(componentConnections, clusterElement),
                ParametersFactory.create(clusterElement.getExtensions()), componentConnections);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<ToolCallback> getToolCallbacks(
        List<ClusterElement> toolClusterElements, Map<String, ComponentConnection> connectionParameters,
        boolean editorEnvironment, @Nullable ToolExecutionListener toolExecutionListener,
        @Nullable Map<String, Map<String, String>> toolSimulations, ChatModel chatModel, ActionContext context) {

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (ClusterElement clusterElement : toolClusterElements) {
            Object clusterElementFunction = clusterElementDefinitionService.getClusterElement(
                clusterElement.getComponentName(), clusterElement.getComponentVersion(),
                clusterElement.getClusterElementName());

            if (clusterElementFunction instanceof ToolCallbackProviderFunction toolCallbackProviderFunction) {
                try {
                    ComponentConnection componentConnection = connectionParameters.get(
                        clusterElement.getWorkflowNodeName());

                    ToolCallback[] providerCallbacks = toolCallbackProviderFunction
                        .apply(
                            ParametersFactory.create(clusterElement.getParameters()),
                            ParametersFactory.create(componentConnection), context)
                        .getToolCallbacks();

                    toolCallbacks.addAll(Arrays.asList(providerCallbacks));
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            } else if (clusterElementFunction instanceof MultipleConnectionsToolFunction) {
                toolCallbacks.add(
                    aiAgentToolFacade.getFunctionToolCallback(clusterElement, connectionParameters, editorEnvironment));
            } else {
                ComponentConnection componentConnection = connectionParameters.get(
                    clusterElement.getWorkflowNodeName());

                toolCallbacks.add(
                    aiAgentToolFacade.getFunctionToolCallback(clusterElement, componentConnection, editorEnvironment));
            }
        }

        if (toolSimulations != null && !toolSimulations.isEmpty()) {
            List<ToolCallback> simulatedCallbacks = new ArrayList<>();

            for (ToolCallback toolCallback : toolCallbacks) {
                simulatedCallbacks.add(
                    createSimulationAwareToolCallback(toolCallback, toolSimulations, chatModel, context));
            }

            toolCallbacks = simulatedCallbacks;
        }

        if (toolExecutionListener == null) {
            return toolCallbacks;
        }

        AtomicReference<@Nullable AgentThinking> thinkingReference = new AtomicReference<>();

        List<ToolCallback> observableToolCallbacks = toolCallbacks.stream()
            .map(
                toolCallback -> createObservableToolCallback(
                    toolCallback, thinkingReference, toolExecutionListener, context))
            .toList();

        AugmentedToolCallbackProvider<AgentThinking> augmentedToolCallbackProvider =
            AugmentedToolCallbackProvider.<AgentThinking>builder()
                .delegate(() -> observableToolCallbacks.toArray(ToolCallback[]::new))
                .argumentType(AgentThinking.class)
                .argumentConsumer(event -> thinkingReference.set(event.arguments()))
                .removeExtraArgumentsAfterProcessing(true)
                .build();

        return Arrays.asList(augmentedToolCallbackProvider.getToolCallbacks());
    }

    private static ToolCallback createSimulationAwareToolCallback(
        ToolCallback delegate, Map<String, Map<String, String>> toolSimulations, ChatModel chatModel,
        ActionContext context) {

        String toolName = delegate.getToolDefinition()
            .name();

        Map<String, String> simulation = toolSimulations.get(toolName);

        if (simulation == null) {
            return delegate;
        }

        return new ToolCallback() {

            private final ToolDefinition toolDefinition = delegate.getToolDefinition();

            @Override
            public ToolDefinition getToolDefinition() {
                return toolDefinition;
            }

            @Override
            public String call(String toolInput) {
                return getSimulatedResult(toolInput, simulation, chatModel, context);
            }

            @Override
            public String call(String toolInput, @Nullable ToolContext toolContext) {
                return getSimulatedResult(toolInput, simulation, chatModel, context);
            }
        };
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private static String getSimulatedResult(
        String toolInput, Map<String, String> simulation, ChatModel chatModel, ActionContext context) {

        String responsePrompt = simulation.get(RESPONSE_PROMPT);
        String simulationModel = simulation.get(SIMULATION_MODEL);

        try {
            ChatClient simulationClient = ChatClient.builder(chatModel)
                .build();

            String prompt =
                "Given this tool call input: %s\n\nGenerate a realistic response following these instructions: %s"
                    .formatted(toolInput, responsePrompt);

            ChatClient.ChatClientRequestSpec requestSpec = simulationClient.prompt()
                .user(prompt);

            if (simulationModel != null && !simulationModel.isEmpty()) {
                requestSpec = requestSpec.options(
                    ChatOptions.builder()
                        .model(simulationModel)
                        .build());
            }

            String generatedResponse = requestSpec.call()
                .content();

            return generatedResponse != null ? generatedResponse : responsePrompt;
        } catch (Exception exception) {
            context.log(
                log -> log.warn(
                    "Failed to generate simulated response, falling back to verbatim: {}", exception.getMessage()));

            return responsePrompt;
        }
    }

    private static ToolCallback createObservableToolCallback(
        ToolCallback delegate, AtomicReference<@Nullable AgentThinking> thinkingReference,
        ToolExecutionListener toolExecutionListener, ActionContext context) {

        return new ToolCallback() {

            private final ToolDefinition toolDefinition = delegate.getToolDefinition();

            @Override
            public ToolDefinition getToolDefinition() {
                return toolDefinition;
            }

            @Override
            public String call(String toolInput) {
                return observeAndCall(toolInput, () -> delegate.call(toolInput));
            }

            @Override
            public String call(String toolInput, @Nullable ToolContext toolContext) {
                return observeAndCall(toolInput, () -> delegate.call(toolInput, toolContext));
            }

            private String observeAndCall(String toolInput, Supplier<String> execution) {
                AbstractAiAgentChatAction.logger.debug("Tool '{}' request: {}", toolDefinition.name(), toolInput);

                Map<String, Object> inputs;

                try {
                    inputs = JsonParser.fromJson(toolInput, new TypeReference<>() {});
                } catch (Exception exception) {
                    context.log(
                        log -> log.debug(
                            "Failed to parse tool input as JSON for '{}': {}", toolDefinition.name(),
                            exception.getMessage()));

                    inputs = Map.of("rawInput", toolInput);
                }

                String result = execution.get();

                AgentThinking agentThinking = thinkingReference.getAndSet(null);

                try {
                    toolExecutionListener.onToolExecution(
                        new ToolExecutionEvent(
                            toolDefinition.name(), inputs, result,
                            agentThinking != null ? agentThinking.reasoning() : null,
                            agentThinking != null ? agentThinking.confidence() : null));
                } catch (Exception exception) {
                    context.log(
                        log -> log.debug(
                            "Tool execution listener failed for '{}': {}", toolDefinition.name(),
                            exception.getMessage(), exception));
                }

                return result;
            }
        };
    }
}
