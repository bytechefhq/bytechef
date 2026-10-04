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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.ai.llm.ChatModel.Format;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.ai.constant.AiAgentToolContextKey;
import com.bytechef.platform.ai.tool.ToolSuspension;
import com.bytechef.platform.ai.tool.ToolSuspensionException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.resolution.DelegatingToolCallbackResolver;
import reactor.core.publisher.Flux;

/**
 * @author Ivica Cardic
 */
final class AiAgentResumeTestSupport {

    static final String CONVERSATION_ID = "conversation_1";
    static final String FAILING_TOOL_NAME = "failingApproval";
    static final String LOOK_UP_TOOL_NAME = "lookUp";
    static final String LOOK_UP_TOOL_RESULT = "looked up";
    static final String SUSPEND_THEN_FAIL_TOOL_NAME = "suspendThenFail";
    static final String SUSPENDING_TOOL_NAME = "requestApproval";
    static final String USER_PROMPT = "please get approval";

    private AiAgentResumeTestSupport() {
    }

    static ToolCallingManager createToolCallingManager() {
        return DefaultToolCallingManager.builder()
            .toolCallbackResolver(new DelegatingToolCallbackResolver(List.of()))
            .build();
    }

    static ChatModel createChatModel(List<Prompt> prompts, Function<Integer, ChatResponse> responseFunction) {
        return new ChatModel() {

            @Override
            public ChatResponse call(Prompt prompt) {
                prompts.add(prompt);

                return responseFunction.apply(prompts.size());
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                prompts.add(prompt);

                return Flux.just(responseFunction.apply(prompts.size()));
            }

            @Override
            public ToolCallingChatOptions getOptions() {
                return ToolCallingChatOptions.builder()
                    .build();
            }
        };
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    static ClusterElementDefinitionService createClusterElementDefinitionService(
        ChatModel chatModel, ChatMemoryFunction.@Nullable Result chatMemoryResult) throws Exception {

        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);

        ModelFunction modelFunction = mock(ModelFunction.class);

        when(clusterElementDefinitionService.<ModelFunction>getClusterElement(
            eq("testComponent"), eq(1), eq("testModel"))).thenReturn(modelFunction);
        when(modelFunction.apply(any(), any(), anyBoolean())).thenAnswer(invocation -> chatModel);

        ToolCallbackProvider toolCallbackProvider = createToolCallbackProvider();

        ToolCallbackProviderFunction toolCallbackProviderFunction =
            (inputParameters, connectionParameters, context) -> toolCallbackProvider;

        when(clusterElementDefinitionService.<ToolCallbackProviderFunction>getClusterElement(
            eq("testComponent"), eq(1), eq("testTools"))).thenReturn(toolCallbackProviderFunction);

        if (chatMemoryResult != null) {
            ChatMemoryFunction chatMemoryFunction =
                (inputParameters, connectionParameters, extensions, componentConnections) -> chatMemoryResult;

            when(clusterElementDefinitionService.<ChatMemoryFunction>getClusterElement(
                eq("testComponent"), eq(1), eq("testChatMemory"))).thenReturn(chatMemoryFunction);
        }

        return clusterElementDefinitionService;
    }

    static Parameters createInputParameters(String responseFormat) {
        Map<String, Object> response = new HashMap<>();

        response.put("responseFormat", responseFormat);

        if ("JSON".equals(responseFormat)) {
            response.put("responseSchema", "{\"type\":\"object\",\"properties\":{\"answer\":{\"type\":\"string\"}}}");
        }

        return MockParametersFactory.create(
            Map.of(
                "format", Format.SIMPLE.name(),
                "userPrompt", USER_PROMPT,
                "response", response));
    }

    static Parameters createExtensions(boolean withChatMemory) {
        Map<String, Object> modelElementMap = new HashMap<>();

        modelElementMap.put("name", "model_1");
        modelElementMap.put("type", "testComponent/v1/testModel");
        modelElementMap.put("parameters", Map.of("model", "gpt-4o"));

        Map<String, Object> toolElementMap = new HashMap<>();

        toolElementMap.put("name", "tool_1");
        toolElementMap.put("type", "testComponent/v1/testTools");
        toolElementMap.put("parameters", Map.of());

        Map<String, Object> clusterElements = new HashMap<>();

        clusterElements.put("model", modelElementMap);
        clusterElements.put("tools", List.of(toolElementMap));

        if (withChatMemory) {
            Map<String, Object> chatMemoryElementMap = new HashMap<>();

            chatMemoryElementMap.put("name", "chatMemory_1");
            chatMemoryElementMap.put("type", "testComponent/v1/testChatMemory");
            chatMemoryElementMap.put("parameters", Map.of("conversationId", CONVERSATION_ID));

            clusterElements.put("chatMemory", chatMemoryElementMap);
        }

        return MockParametersFactory.create(Map.of("clusterElements", clusterElements));
    }

    static Map<String, ComponentConnection> createConnectionParameters() {
        Map<String, ComponentConnection> connectionParameters = new HashMap<>();

        connectionParameters.put("model_1", new ComponentConnection("testComponent", 1, 1L, Map.of(), null));
        connectionParameters.put("tool_1", new ComponentConnection("testComponent", 1, 2L, Map.of(), null));
        connectionParameters.put("chatMemory_1", new ComponentConnection("testComponent", 1, 3L, Map.of(), null));

        return connectionParameters;
    }

    static ActionContextAware createActionContext(AtomicReference<ActionContext.Suspend> suspendReference) {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        doAnswer(invocation -> {
            suspendReference.set(invocation.getArgument(0));

            return null;
        }).when(actionContext)
            .suspend(any());

        when(actionContext.getSuspend()).thenAnswer(invocation -> suspendReference.get());

        return actionContext;
    }

    @SuppressWarnings("unchecked")
    static void mockJson(ActionContext actionContext) {
        Context.Json json = mock(Context.Json.class);

        when(json.readMap(anyString(), eq(Object.class))).thenAnswer(
            invocation -> new HashMap<>(JsonUtils.readMap((String) invocation.getArgument(0), Object.class)));
        when(json.read(anyString(), any(TypeReference.class))).thenAnswer(
            invocation -> JsonUtils.read((String) invocation.getArgument(0)));
        when(json.write(any())).thenAnswer(invocation -> JsonUtils.write(invocation.getArgument(0)));
        when(actionContext.json(any())).thenAnswer(invocation -> {
            Context.ContextFunction<Context.Json, Object> contextFunction = invocation.getArgument(0);

            return contextFunction.apply(json);
        });
    }

    static Parameters toPersistedContinueParameters(ActionContext.Suspend suspend) {
        Map<String, ?> persistedContinueParameters = JsonUtils.readMap(JsonUtils.write(suspend.continueParameters()));

        return ParametersFactory.create(persistedContinueParameters);
    }

    static ChatResponse toolCallResponse(String toolName, String... toolCallIds) {
        List<AssistantMessage.ToolCall> toolCalls = Arrays.stream(toolCallIds)
            .map(toolCallId -> new AssistantMessage.ToolCall(toolCallId, "function", toolName, "{}"))
            .toList();

        AssistantMessage assistantMessage = AssistantMessage.builder()
            .content("")
            .toolCalls(toolCalls)
            .build();

        return ChatResponse.builder()
            .generations(List.of(new Generation(assistantMessage)))
            .build();
    }

    static ChatResponse toolCallResponse(AssistantMessage.ToolCall... toolCalls) {
        AssistantMessage assistantMessage = AssistantMessage.builder()
            .content("")
            .toolCalls(List.of(toolCalls))
            .build();

        return ChatResponse.builder()
            .generations(List.of(new Generation(assistantMessage)))
            .build();
    }

    static AssistantMessage.ToolCall toolCall(String toolName, String toolCallId) {
        return new AssistantMessage.ToolCall(toolCallId, "function", toolName, "{}");
    }

    static ChatResponse textResponse(String text) {
        return ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(text))))
            .build();
    }

    static @Nullable ToolResponseMessage findLastToolResponseMessage(List<Message> messages) {
        ToolResponseMessage lastToolResponseMessage = null;

        for (Message message : messages) {
            if (message instanceof ToolResponseMessage toolResponseMessage) {
                lastToolResponseMessage = toolResponseMessage;
            }
        }

        return lastToolResponseMessage;
    }

    static String suspendedToolResult() {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getSuspend()).thenReturn(new ActionContext.Suspend(Map.of(), Instant.now()));

        return ToolSuspension.suspendedToolResult(actionContext);
    }

    private static ToolCallbackProvider createToolCallbackProvider() {
        ToolDefinition suspendingToolDefinition = createToolDefinition(SUSPENDING_TOOL_NAME);

        ToolCallback suspendingToolCallback = new ToolCallback() {

            @Override
            public ToolDefinition getToolDefinition() {
                return suspendingToolDefinition;
            }

            @Override
            public String call(String toolInput) {
                throw new UnsupportedOperationException("must be called with ToolContext");
            }

            @Override
            public String call(String toolInput, @Nullable ToolContext toolContext) {
                if (toolContext == null) {
                    throw new UnsupportedOperationException("must be called with ToolContext");
                }

                Map<String, Object> toolContextMap = toolContext.getContext();

                ActionContext actionContext = (ActionContext) toolContextMap.get(AiAgentToolContextKey.ACTION_CONTEXT);

                actionContext.suspend(
                    new ActionContext.Suspend(
                        new HashMap<>(), Instant.now()
                            .plusSeconds(60)));

                return ToolSuspension.suspendedToolResult(actionContext);
            }
        };

        ToolDefinition lookUpToolDefinition = createToolDefinition(LOOK_UP_TOOL_NAME);

        ToolCallback lookUpToolCallback = new ToolCallback() {

            @Override
            public ToolDefinition getToolDefinition() {
                return lookUpToolDefinition;
            }

            @Override
            public String call(String toolInput) {
                return LOOK_UP_TOOL_RESULT;
            }
        };

        ToolDefinition failingToolDefinition = createToolDefinition(FAILING_TOOL_NAME);

        ToolCallback failingToolCallback = new ToolCallback() {

            @Override
            public ToolDefinition getToolDefinition() {
                return failingToolDefinition;
            }

            @Override
            public String call(String toolInput) {
                throw new ToolExecutionException(
                    failingToolDefinition,
                    new ToolSuspensionException(
                        "The approval request could not be sent", new IllegalStateException("mail server down")));
            }
        };

        ToolDefinition suspendThenFailToolDefinition = createToolDefinition(SUSPEND_THEN_FAIL_TOOL_NAME);

        ToolCallback suspendThenFailToolCallback = new ToolCallback() {

            @Override
            public ToolDefinition getToolDefinition() {
                return suspendThenFailToolDefinition;
            }

            @Override
            public String call(String toolInput) {
                throw new UnsupportedOperationException("must be called with ToolContext");
            }

            @Override
            public String call(String toolInput, @Nullable ToolContext toolContext) {
                if (toolContext == null) {
                    throw new UnsupportedOperationException("must be called with ToolContext");
                }

                Map<String, Object> toolContextMap = toolContext.getContext();

                ActionContext actionContext = (ActionContext) toolContextMap.get(AiAgentToolContextKey.ACTION_CONTEXT);

                actionContext.suspend(
                    new ActionContext.Suspend(
                        new HashMap<>(), Instant.now()
                            .plusSeconds(60)));

                throw new IllegalStateException("The notification could not be sent");
            }
        };

        return () -> new ToolCallback[] {
            suspendingToolCallback, lookUpToolCallback, failingToolCallback, suspendThenFailToolCallback
        };
    }

    private static ToolDefinition createToolDefinition(String name) {
        return DefaultToolDefinition.builder()
            .name(name)
            .description(name + " tool")
            .inputSchema("{\"type\":\"object\",\"properties\":{}}")
            .build();
    }
}
