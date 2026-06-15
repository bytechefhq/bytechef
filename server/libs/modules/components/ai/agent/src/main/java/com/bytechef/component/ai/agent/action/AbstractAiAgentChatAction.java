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

import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_SCHEMA;
import static com.bytechef.platform.ai.constant.AiAgentSimulationConstants.RESPONSE_PROMPT;
import static com.bytechef.platform.ai.constant.AiAgentSimulationConstants.SIMULATION_MODEL;
import static com.bytechef.platform.ai.constant.AiAgentSimulationConstants.TOOL_SIMULATIONS;
import static com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction.CHAT_MEMORY;
import static com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction.GUARDRAILS;
import static com.bytechef.platform.component.definition.ai.agent.ModelFunction.MODEL;
import static com.bytechef.platform.component.definition.ai.agent.RagFunction.RAG;
import static com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction.CHECK_FOR_VIOLATIONS;
import static com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction.SANITIZE_TEXT;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.ai.agent.action.event.ToolExecutionEvent;
import com.bytechef.component.ai.agent.action.event.listener.ToolExecutionListener;
import com.bytechef.component.ai.agent.facade.AiAgentToolFacade;
import com.bytechef.component.ai.llm.ChatModel.ResponseFormat;
import com.bytechef.component.ai.llm.advisor.ContextLoggerAdvisor;
import com.bytechef.component.ai.llm.converter.JsonSchemaStructuredOutputConverter;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolCallbackProviderFunction;
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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.augment.AugmentedToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractAiAgentChatAction {

    private static final Logger log = LoggerFactory.getLogger(AbstractAiAgentChatAction.class);
    private static final JsonMapper JSON_MAPPER = new JsonMapper();

    private static final String TOOL_SIMULATION_UNAVAILABLE = "[tool simulation unavailable]";

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final AiAgentToolFacade aiAgentToolFacade;
    private final ToolCallingManager toolCallingManager;

    protected AbstractAiAgentChatAction(
        AiAgentToolFacade aiAgentToolFacade, ClusterElementDefinitionService clusterElementDefinitionService,
        ToolCallingManager toolCallingManager) {

        this.aiAgentToolFacade = aiAgentToolFacade;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.toolCallingManager = toolCallingManager;
    }

    protected ChatClient.ChatClientRequestSpec getChatClientRequestSpec(
        Parameters inputParameters, Map<String, ComponentConnection> connectionParameters, Parameters extensions,
        @Nullable ToolExecutionListener toolExecutionListener, ActionContext context) throws Exception {

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        ClusterElement modelClusterElement = clusterElementMap.getClusterElement(MODEL);

        ModelFunction modelFunction = clusterElementDefinitionService.getClusterElement(
            modelClusterElement.getComponentName(), modelClusterElement.getComponentVersion(),
            modelClusterElement.getClusterElementName());

        ComponentConnection modelConnection = connectionParameters.get(modelClusterElement.getWorkflowNodeName());

        Map<String, Object> concatenatedInputParameters = MapUtils.concat(
            new HashMap<>(inputParameters.toMap()), new HashMap<>(modelClusterElement.getParameters()));

        ChatModel chatModel = (ChatModel) modelFunction.apply(
            ParametersFactory.create(concatenatedInputParameters),
            ParametersFactory.create(modelConnection.getParameters()), true);

        String conversationId = clusterElementMap.fetchClusterElement(CHAT_MEMORY)
            .map(clusterElement -> {
                Parameters chatMemoryParameters = ParametersFactory.create(clusterElement.getParameters());

                String id = chatMemoryParameters.getString("conversationId");

                if (id != null) {
                    return id;
                }

                UUID uuid = UUID.randomUUID();

                return uuid.toString();
            })
            .orElse(null);

        @SuppressWarnings("unchecked")
        Map<String, Map<String, String>> toolSimulations =
            (Map<String, Map<String, String>>) inputParameters.get(TOOL_SIMULATIONS);

        ChatClient chatClient = ChatClient.builder(chatModel)
            .build();

        return createPrompt(chatClient, inputParameters, context)
            .advisors(getAdvisors(clusterElementMap, connectionParameters, context))
            .advisors(getConversationAdvisor(conversationId))
            .messages(ModelUtils.getMessages(inputParameters, context))
            .tools(
                getToolCallbacks(
                    clusterElementMap.getClusterElements(BaseToolFunction.TOOLS), connectionParameters,
                    context.isEditorEnvironment(), toolExecutionListener, toolSimulations, chatModel, context)
                        .toArray());
    }

    private ChatMemoryFunction.Result buildChatMemoryResult(
        Map<String, ComponentConnection> componentConnections, ClusterElement clusterElement,
        ActionContext context) {

        ChatMemoryFunction chatMemoryFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        try {
            return chatMemoryFunction.apply(
                ParametersFactory.create(clusterElement.getParameters()),
                getConnectionParameters(componentConnections, clusterElement),
                ParametersFactory.create(clusterElement.getExtensions()), componentConnections);
        } catch (Exception e) {
            throw clusterElementInitializationException(clusterElement, "chat memory", e, context);
        }
    }

    private static RuntimeException clusterElementInitializationException(
        ClusterElement clusterElement, String kind, Throwable cause, ActionContext context) {

        Class<? extends Throwable> causeClass = cause.getClass();

        String message = String.format(
            "Failed to initialize %s advisor for cluster element '%s' (component=%s v%d): %s",
            kind, clusterElement.getClusterElementName(), clusterElement.getComponentName(),
            clusterElement.getComponentVersion(),
            cause.getMessage() == null ? causeClass.getSimpleName() : cause.getMessage());

        context.log(log -> log.error(message, cause));

        return new IllegalStateException(message, cause);
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
                log.debug("Tool '{}' request: {}", toolDefinition.name(), toolInput);

                Map<String, Object> inputs;

                try {
                    inputs = JSON_MAPPER.readValue(toolInput, new TypeReference<>() {});
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
                        log -> log.warn(
                            "Tool execution listener failed for '{}'", toolDefinition.name(), exception));
                }

                return result;
            }
        };
    }

    private static ChatClient.ChatClientRequestSpec createPrompt(
        ChatClient chatClient, Parameters inputParameters, ActionContext context) {

        ResponseFormat responseFormat = inputParameters.getFromPath(
            RESPONSE + "." + RESPONSE_FORMAT, ResponseFormat.class, ResponseFormat.TEXT);

        if (responseFormat == ResponseFormat.TEXT) {
            return chatClient.prompt();
        }

        JsonSchemaStructuredOutputConverter converter = new JsonSchemaStructuredOutputConverter(
            inputParameters.getFromPath(RESPONSE + "." + RESPONSE_SCHEMA, String.class), context);

        return chatClient.prompt(converter.getFormat());
    }

    protected static void applyStructuredOutputValidation(
        ChatClient.ChatClientRequestSpec chatClientRequestSpec, Parameters inputParameters, ActionContext context) {

        ResponseFormat responseFormat = inputParameters.getFromPath(
            RESPONSE + "." + RESPONSE_FORMAT, ResponseFormat.class, ResponseFormat.TEXT);

        if (responseFormat == ResponseFormat.TEXT) {
            return;
        }

        JsonSchemaStructuredOutputConverter converter = new JsonSchemaStructuredOutputConverter(
            inputParameters.getFromPath(RESPONSE + "." + RESPONSE_SCHEMA, String.class), context);

        chatClientRequestSpec.advisors(
            StructuredOutputValidationAdvisor.builder()
                .outputJsonSchema(converter.getJsonSchema())
                .build());
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

    List<Advisor> getAdvisors(
        ClusterElementMap clusterElementMap, Map<String, ComponentConnection> connectionParameters,
        ActionContext context) {

        List<Advisor> advisors = new ArrayList<>();

        List<ClusterElement> guardrailClusterElements = clusterElementMap.getClusterElements(GUARDRAILS);

        long checkForViolationsCount = guardrailClusterElements.stream()
            .filter(clusterElement -> Objects.equals(CHECK_FOR_VIOLATIONS.key(), clusterElement.getComponentName()))
            .count();

        if (checkForViolationsCount > 1) {
            throw new IllegalStateException(
                "Multiple CheckForViolations parent cluster elements configured — advisor order collides at " +
                    "HIGHEST_PRECEDENCE and Spring AI ordering becomes undefined. Configure at most one.");
        }

        long sanitizeTextCount = guardrailClusterElements.stream()
            .filter(clusterElement -> Objects.equals(SANITIZE_TEXT.key(), clusterElement.getComponentName()))
            .count();

        if (sanitizeTextCount > 1) {
            throw new IllegalStateException(
                "Multiple SanitizeText parent cluster elements configured — advisor order collides at " +
                    "DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER - 1 and Spring AI ordering becomes undefined. " +
                    "Configure at most one.");
        }

        Optional<ChatMemoryFunction.Result> chatMemoryResult =
            clusterElementMap.fetchClusterElement(CHAT_MEMORY)
                .map(clusterElement -> buildChatMemoryResult(connectionParameters, clusterElement, context));

        if (!guardrailClusterElements.isEmpty()) {
            List<Message> conversationHistory = chatMemoryResult
                .map(result -> loadConversationHistory(result.chatMemory(), clusterElementMap))
                .orElse(List.of());

            for (ClusterElement clusterElement : guardrailClusterElements) {
                advisors.add(getGuardrailsAdvisor(connectionParameters, clusterElement, context, conversationHistory));
            }
        }

        // memory

        chatMemoryResult
            .map(ChatMemoryFunction.Result::advisor)
            .ifPresent(advisors::add);

        // tool call
        //
        // Spring AI 2.0.0-RC1 orders ChatMemoryAdvisor (DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER, MIN+200) upstream
        // of ToolCallingAdvisor (DEFAULT_ORDER, MIN+300), so memory wraps the whole tool-execution loop:
        // persisted history is applied once before the loop and only the final assistant message is written back.
        // ToolCallingAdvisor retains the intermediate (assistant-with-tool_calls, tool-result) pairs in its own
        // in-loop history (conversationHistoryEnabled defaults to true), so the standard composition already emits
        // well-formed tool-call sequences. This replaces the former custom ToolHistoryToolCallAdvisor +
        // disableInternalConversationHistory() workaround, which was only required under the M-series ordering
        // where ChatMemoryAdvisor sat downstream of the tool loop and therefore could not rehydrate it.
        advisors.add(
            ToolCallingAdvisor.builder()
                .toolCallingManager(toolCallingManager)
                .build());

        clusterElementMap.fetchClusterElement(RAG)
            .map(clusterElement -> getRagAdvisor(connectionParameters, clusterElement, context))
            .ifPresent(advisors::add);

        advisors.add(new ContextLoggerAdvisor(context));

        return advisors;
    }

    private static Parameters getConnectionParameters(
        Map<String, ComponentConnection> componentConnections, ClusterElement clusterElement) {

        ComponentConnection componentConnection = componentConnections.get(clusterElement.getWorkflowNodeName());

        return ParametersFactory.create(componentConnection);
    }

    private static Consumer<ChatClient.AdvisorSpec> getConversationAdvisor(@Nullable String conversationId) {
        return advisor -> {
            if (conversationId != null) {
                advisor.param(ChatMemory.CONVERSATION_ID, conversationId);
            }
        };
    }

    private Advisor getGuardrailsAdvisor(
        Map<String, ComponentConnection> componentConnections, ClusterElement clusterElement, ActionContext context,
        List<Message> conversationHistory) {

        GuardrailsFunction guardrailsFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        try {
            return guardrailsFunction.apply(
                ParametersFactory.create(clusterElement.getParameters()),
                getConnectionParameters(componentConnections, clusterElement),
                ParametersFactory.create(clusterElement.getExtensions()), componentConnections, context,
                conversationHistory);
        } catch (Exception e) {
            throw clusterElementInitializationException(clusterElement, "guardrails", e, context);
        }
    }

    private Advisor getRagAdvisor(
        Map<String, ComponentConnection> componentConnections, ClusterElement clusterElement,
        ActionContext context) {

        RagFunction ragFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        try {
            return ragFunction.apply(
                ParametersFactory.create(clusterElement.getParameters()),
                getConnectionParameters(componentConnections, clusterElement),
                ParametersFactory.create(clusterElement.getExtensions()), componentConnections);
        } catch (Exception e) {
            throw clusterElementInitializationException(clusterElement, "RAG", e, context);
        }
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
                        .model(simulationModel));
            }

            String generatedResponse = requestSpec.call()
                .content();

            return generatedResponse != null ? generatedResponse : TOOL_SIMULATION_UNAVAILABLE;
        } catch (Exception exception) {
            context.log(log -> log.warn("Failed to generate simulated response", exception));

            return TOOL_SIMULATION_UNAVAILABLE;
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

            if (clusterElementFunction instanceof MultipleConnectionsToolCallbackProviderFunction multipleConnectionsToolCallbackProviderFunction) {
                try {
                    ToolCallback[] providerCallbacks = multipleConnectionsToolCallbackProviderFunction
                        .apply(
                            ParametersFactory.create(clusterElement.getParameters()),
                            getConnectionParameters(connectionParameters, clusterElement),
                            ParametersFactory.create(clusterElement.getExtensions()),
                            connectionParameters, context)
                        .getToolCallbacks();

                    toolCallbacks.addAll(Arrays.asList(providerCallbacks));
                } catch (Exception exception) {
                    throw clusterElementInitializationException(clusterElement, "tool callback", exception, context);
                }
            } else if (clusterElementFunction instanceof ToolCallbackProviderFunction toolCallbackProviderFunction) {
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
                    throw clusterElementInitializationException(clusterElement, "tool callback", exception, context);
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

    private List<Message> loadConversationHistory(
        @Nullable ChatMemory chatMemory, ClusterElementMap clusterElementMap) {

        if (chatMemory == null) {
            return List.of();
        }

        return clusterElementMap.fetchClusterElement(CHAT_MEMORY)
            .map(clusterElement -> {
                Parameters chatMemoryParameters = ParametersFactory.create(clusterElement.getParameters());
                String conversationId = chatMemoryParameters.getString("conversationId");

                if (conversationId == null) {
                    return List.<Message>of();
                }

                try {
                    List<Message> all = chatMemory.get(conversationId);

                    if (all.isEmpty()) {
                        return List.<Message>of();
                    }

                    int size = all.size();

                    return size <= 3 ? List.copyOf(all) : List.copyOf(all.subList(size - 3, size));
                } catch (Exception exception) {
                    return List.<Message>of();
                }
            })
            .orElse(List.of());
    }
}
