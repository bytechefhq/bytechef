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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.agent.tool.AgentToolCallingManagers;
import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.facade.AiAgentToolFacade;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.ResumePerformFunction.ResumeResponse;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.ai.constant.AiAgentToolContextKey;
import com.bytechef.platform.ai.constant.ToolSuspendConstants;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
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
import org.springframework.ai.tool.resolution.DelegatingToolCallbackResolver;

/**
 * End-to-end integration test for the AI agent suspend/resume flow. Exercises the real
 * {@link SuspendableToolCallingManager} inside the {@link AiAgentChatAction} advisor chain: a stub
 * {@link org.springframework.ai.chat.model.ChatModel} whose first response is a tool call triggers a suspend; the test
 * then calls {@link AiAgentChatAction#resumePerform} with the captured continuation parameters and verifies the second
 * model call receives the patched tool response.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AiAgentChatActionResumeIntTest {

    private static final String TOOL_NAME = "requestApproval";
    private static final String TOOL_CALL_ID = "call_1";
    private static final String FINAL_ANSWER = "final answer";

    @Test
    void testSuspendOnFirstCallAndResumeWithPatchedToolResponse() throws Exception {
        Parameters inputParameters = buildInputParameters();
        Parameters extensions = buildExtensions();
        Map<String, ComponentConnection> connectionParameters = buildConnectionParameters();

        AtomicReference<ActionContext.Suspend> capturedSuspend = new AtomicReference<>();
        ActionContextAware actionContext = buildActionContext(capturedSuspend);

        AtomicInteger callCount = new AtomicInteger(0);
        AtomicReference<Prompt> secondCallPrompt = new AtomicReference<>();

        org.springframework.ai.chat.model.ChatModel chatModel = buildChatModel(callCount, secondCallPrompt);
        ToolCallbackProvider toolCallbackProvider = buildSuspendingToolCallbackProvider();
        ClusterElementDefinitionService clusterElementDefinitionService =
            buildClusterElementDefinitionService(chatModel, toolCallbackProvider);

        ToolCallingManager toolCallingManager = DefaultToolCallingManager.builder()
            .toolCallbackResolver(new DelegatingToolCallbackResolver(List.of()))
            .build();

        TestableAiAgentChatAction agentChatAction = new TestableAiAgentChatAction(
            mock(AiAgentToolFacade.class), clusterElementDefinitionService, toolCallingManager);

        // === Assertion A: perform() suspends ===

        agentChatAction.performChat(inputParameters, connectionParameters, extensions, actionContext);

        ActionContext.Suspend suspend = capturedSuspend.get();

        assertThat(suspend).isNotNull();
        assertThat(suspend.continueParameters()).containsKey(ToolSuspendConstants.CONVERSATION_STATE);
        assertThat(suspend.continueParameters()).containsKey(ToolSuspendConstants.PENDING_TOOL_CALL_ID);
        assertThat(suspend.continueParameters()
            .get(ToolSuspendConstants.PENDING_TOOL_CALL_ID)).isEqualTo(TOOL_CALL_ID);

        // === Assertion B: resumePerform() patches the tool response and invokes the model again ===

        Parameters continueParameters = ParametersFactory.create(new HashMap<>(suspend.continueParameters()));
        Parameters resumeData = MockParametersFactory.create(Map.of("approved", true));

        ResumeResponse resumeResponse = agentChatAction.performResumeChat(
            inputParameters, connectionParameters, extensions, continueParameters, resumeData, actionContext);

        assertThat(callCount.get()).isEqualTo(2);

        Prompt capturedPrompt = secondCallPrompt.get();

        assertThat(capturedPrompt).isNotNull();

        ToolResponseMessage patchedToolResponse = findToolResponseMessage(capturedPrompt.getInstructions());

        assertThat(patchedToolResponse).isNotNull();

        List<ToolResponseMessage.ToolResponse> responses = patchedToolResponse.getResponses();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0)
            .id()).isEqualTo(TOOL_CALL_ID);
        assertThat(responses.get(0)
            .responseData()).contains("approved");
        assertThat(responses.get(0)
            .responseData()).doesNotContain(ToolSuspendConstants.SUSPENDED_SENTINEL);

        assertThat(resumeResponse).isNotNull();
        assertThat(resumeResponse).containsKey("data");

        @SuppressWarnings("unchecked")
        Map<String, Object> responseData = (Map<String, Object>) resumeResponse.get("data");

        assertThat(responseData).containsEntry("response", FINAL_ANSWER);
    }

    private static Parameters buildInputParameters() {
        return MockParametersFactory.create(Map.of(
            "format", ChatModel.Format.SIMPLE.name(),
            "userPrompt", "please get approval",
            "response", Map.of("responseFormat", "TEXT")));
    }

    private static Parameters buildExtensions() {
        Map<String, Object> modelElementMap = new HashMap<>();

        modelElementMap.put("name", "model_1");
        modelElementMap.put("type", "testComponent/v1/testModel");
        modelElementMap.put("parameters", Map.of("model", "gpt-4o"));

        Map<String, Object> toolElementMap = new HashMap<>();

        toolElementMap.put("name", "tool_1");
        toolElementMap.put("type", "testComponent/v1/requestApproval");
        toolElementMap.put("parameters", Map.of());

        return MockParametersFactory.create(
            Map.of("clusterElements",
                Map.of(
                    "model", modelElementMap,
                    "tools", List.of(toolElementMap))));
    }

    private static Map<String, ComponentConnection> buildConnectionParameters() {
        Map<String, ComponentConnection> connectionParameters = new HashMap<>();

        connectionParameters.put("model_1", new ComponentConnection("testComponent", 1, 1L, Map.of(), null));
        connectionParameters.put("tool_1", new ComponentConnection("testComponent", 1, 2L, Map.of(), null));

        return connectionParameters;
    }

    private static ActionContextAware buildActionContext(AtomicReference<ActionContext.Suspend> capturedSuspend) {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        doAnswer(invocation -> {
            capturedSuspend.set(invocation.getArgument(0));

            return null;
        }).when(actionContext)
            .suspend(any());

        when(actionContext.getSuspend()).thenAnswer(invocation -> capturedSuspend.get());
        when(actionContext.isEditorEnvironment()).thenReturn(false);

        return actionContext;
    }

    private static org.springframework.ai.chat.model.ChatModel buildChatModel(
        AtomicInteger callCount, AtomicReference<Prompt> secondCallPrompt) {

        return new org.springframework.ai.chat.model.ChatModel() {

            @Override
            public ChatResponse call(Prompt prompt) {
                int count = callCount.incrementAndGet();

                if (count == 1) {
                    AssistantMessage assistantMessage = AssistantMessage.builder()
                        .content("")
                        .toolCalls(
                            List.of(new AssistantMessage.ToolCall(TOOL_CALL_ID, "function", TOOL_NAME, "{}")))
                        .build();

                    return ChatResponse.builder()
                        .generations(List.of(new Generation(assistantMessage)))
                        .build();
                }

                secondCallPrompt.set(prompt);

                return ChatResponse.builder()
                    .generations(List.of(new Generation(new AssistantMessage(FINAL_ANSWER))))
                    .build();
            }

            @Override
            public ToolCallingChatOptions getOptions() {
                return ToolCallingChatOptions.builder()
                    .build();
            }
        };
    }

    private static ToolCallbackProvider buildSuspendingToolCallbackProvider() {
        ToolDefinition toolDefinition = DefaultToolDefinition.builder()
            .name(TOOL_NAME)
            .description("request approval from a human")
            .inputSchema("{}")
            .build();

        ToolCallback suspendingToolCallback = new ToolCallback() {

            @Override
            public ToolDefinition getToolDefinition() {
                return toolDefinition;
            }

            @Override
            public String call(String toolInput) {
                throw new UnsupportedOperationException("must be called with ToolContext");
            }

            @Override
            public String call(String toolInput, @Nullable ToolContext toolContext) {
                if (toolContext == null) {
                    throw new IllegalStateException("toolContext is required");
                }

                ActionContext context =
                    (ActionContext) toolContext.getContext()
                        .get(AiAgentToolContextKey.ACTION_CONTEXT);

                context.suspend(new ActionContext.Suspend(new HashMap<>(), Instant.now()
                    .plusSeconds(60)));

                return ToolSuspendConstants.SUSPENDED_SENTINEL;
            }
        };

        return () -> new ToolCallback[] {
            suspendingToolCallback
        };
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private static ClusterElementDefinitionService buildClusterElementDefinitionService(
        org.springframework.ai.chat.model.ChatModel chatModel, ToolCallbackProvider toolCallbackProvider)
        throws Exception {

        ClusterElementDefinitionService clusterElementDefinitionService =
            mock(ClusterElementDefinitionService.class);

        ModelFunction modelFunction = mock(ModelFunction.class);

        when(clusterElementDefinitionService.<ModelFunction>getClusterElement(
            eq("testComponent"), eq(1), eq("testModel"))).thenReturn(modelFunction);
        when(modelFunction.apply(any(), any(), anyBoolean())).thenAnswer(invocation -> chatModel);

        ToolCallbackProviderFunction toolCallbackProviderFunction =
            (inputParams, connectionParams, ctx) -> toolCallbackProvider;

        when(clusterElementDefinitionService.<ToolCallbackProviderFunction>getClusterElement(
            eq("testComponent"), eq(1), eq("requestApproval"))).thenReturn(toolCallbackProviderFunction);

        return clusterElementDefinitionService;
    }

    private static ToolResponseMessage findToolResponseMessage(List<Message> messages) {
        for (Message message : messages) {
            if (message instanceof ToolResponseMessage toolResponseMessage) {
                return toolResponseMessage;
            }
        }

        return null;
    }

    /**
     * Thin subclass of {@link AbstractAiAgentChatAction} that exposes the protected perform and resumeChat methods for
     * testing.
     */
    private static final class TestableAiAgentChatAction extends AbstractAiAgentChatAction {

        private TestableAiAgentChatAction(
            AiAgentToolFacade aiAgentToolFacade,
            ClusterElementDefinitionService clusterElementDefinitionService,
            ToolCallingManager toolCallingManager) {

            super(aiAgentToolFacade, clusterElementDefinitionService, new AgentToolCallingManagers(toolCallingManager));
        }

        public Object performChat(
            Parameters inputParameters, Map<String, ComponentConnection> connectionParameters, Parameters extensions,
            ActionContext context) throws Exception {

            org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec spec =
                getChatClientRequestSpec(inputParameters, connectionParameters, extensions, null, context);

            spec.toolContext(Map.of(AiAgentToolContextKey.ACTION_CONTEXT, context));

            return ModelUtils.getChatActionResult(spec.call(), inputParameters, context)
                .response();
        }

        public ResumeResponse performResumeChat(
            Parameters inputParameters, Map<String, ComponentConnection> connectionParameters, Parameters extensions,
            Parameters continueParameters, Parameters data, ActionContext context) throws Exception {

            Object response = resumeChat(
                inputParameters, connectionParameters, extensions, continueParameters, data, context);

            return ResumeResponse.of(new HashMap<>(Map.of("response", response)));
        }
    }
}
