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

package com.bytechef.component.ai.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.platform.ai.sensitivedata.SensitiveDataMetrics;
import com.bytechef.platform.ai.sensitivedata.SensitiveDataRedactor;
import com.bytechef.platform.ai.sensitivedata.tokenization.PiiTokenSession;
import com.bytechef.platform.ai.sensitivedata.tokenization.PiiTokenSessionToolContext;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * @author Ivica Cardic
 */
class AgentToolCallingManagersTest {

    @Test
    void testTheReturnedManagerRestoresPiiTokensInToolCallArguments() {
        PiiTokenSession session = PiiTokenSession.create();
        String token = session.tokenFor("EMAIL_ADDRESS", "bob@acme.io");

        RecordingToolCallingManager delegate = new RecordingToolCallingManager();
        AgentToolCallingManagers agentToolCallingManagers = new AgentToolCallingManagers(delegate);

        ToolCallingManager manager = agentToolCallingManagers.getToolCallingManager(null);

        manager.executeToolCalls(
            promptWithSession(session), chatResponseWithToolCall("sendEmail", "{\"to\":\"" + token + "\"}"));

        assertThat(delegate.lastArguments()).isEqualTo("{\"to\":\"bob@acme.io\"}");
    }

    @Test
    void testWithNoSessionInToolContextTheReturnedManagerLeavesArgumentsUnchanged() {
        RecordingToolCallingManager delegate = new RecordingToolCallingManager();
        AgentToolCallingManagers agentToolCallingManagers = new AgentToolCallingManagers(delegate);

        ToolCallingManager manager = agentToolCallingManagers.getToolCallingManager(null);

        manager.executeToolCalls(
            promptWithoutSession(), chatResponseWithToolCall("sendEmail", "{\"to\":\"x\"}"));

        assertThat(delegate.lastArguments()).isEqualTo("{\"to\":\"x\"}");
    }

    @Test
    void testTheReturnedManagerRecordsToolArgsRestoredThroughThePassedInMetrics() {
        PiiTokenSession session = PiiTokenSession.create();
        String token = session.tokenFor("EMAIL_ADDRESS", "bob@acme.io");

        RecordingToolCallingManager delegate = new RecordingToolCallingManager();
        AgentToolCallingManagers agentToolCallingManagers = new AgentToolCallingManagers(delegate);

        SensitiveDataMetrics sensitiveDataMetrics = mock(SensitiveDataMetrics.class);

        ToolCallingManager manager = agentToolCallingManagers.getToolCallingManager(sensitiveDataMetrics);

        manager.executeToolCalls(
            promptWithSession(session), chatResponseWithToolCall("sendEmail", "{\"to\":\"" + token + "\"}"));

        verify(sensitiveDataMetrics).recordToolArgsRestored();
    }

    @Test
    void testWithNoSessionInToolContextTheReturnedManagerRecordsNothing() {
        RecordingToolCallingManager delegate = new RecordingToolCallingManager();
        AgentToolCallingManagers agentToolCallingManagers = new AgentToolCallingManagers(delegate);

        SensitiveDataMetrics sensitiveDataMetrics = mock(SensitiveDataMetrics.class);

        ToolCallingManager manager = agentToolCallingManagers.getToolCallingManager(sensitiveDataMetrics);

        manager.executeToolCalls(
            promptWithoutSession(), chatResponseWithToolCall("sendEmail", "{\"to\":\"x\"}"));

        verifyNoInteractions(sensitiveDataMetrics);
    }

    @Test
    void testWithoutTheToolCallingCollaboratorsMaxToolCallsFallsBackToTheDefaultManager() {
        RecordingToolCallingManager delegate = new RecordingToolCallingManager();
        AgentToolCallingManagers agentToolCallingManagers = new AgentToolCallingManagers(delegate);

        ToolCallingManager manager = agentToolCallingManagers.getToolCallingManager(null, 1);

        manager.executeToolCalls(
            promptWithoutSession(), chatResponseWithToolCall("sendEmail", "{\"to\":\"x\"}"));

        assertThat(delegate.lastArguments()).isEqualTo("{\"to\":\"x\"}");
    }

    @Test
    void testMaxToolCallsAnswersCallsBeyondTheCapWithAnErrorResponseInsteadOfRunningThem() {
        CountingToolCallback countingToolCallback = new CountingToolCallback("lookUp");

        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

        beanFactory.addBean("toolCallbackResolver", new StaticToolCallbackResolver(List.of()));
        beanFactory.addBean(
            "toolExecutionExceptionProcessor", DefaultToolExecutionExceptionProcessor.builder()
                .build());

        AgentToolCallingManagers agentToolCallingManagers = new AgentToolCallingManagers(
            new RecordingToolCallingManager(), beanFactory.getBeanProvider(SensitiveDataRedactor.class),
            beanFactory.getBeanProvider(ToolCallbackResolver.class),
            beanFactory.getBeanProvider(ToolExecutionExceptionProcessor.class),
            beanFactory.getBeanProvider(ObservationRegistry.class));

        ToolCallingManager manager = agentToolCallingManagers.getToolCallingManager(null, 1);

        AssistantMessage assistantMessage = AssistantMessage.builder()
            .content("")
            .toolCalls(
                List.of(
                    new AssistantMessage.ToolCall("call-1", "function", "lookUp", "{}"),
                    new AssistantMessage.ToolCall("call-2", "function", "lookUp", "{}")))
            .build();

        Prompt prompt = new Prompt(
            List.of(), ToolCallingChatOptions.builder()
                .toolCallbacks(countingToolCallback)
                .build());

        ToolExecutionResult toolExecutionResult = manager.executeToolCalls(
            prompt, new ChatResponse(List.of(new Generation(assistantMessage))));

        List<Message> conversationHistory = toolExecutionResult.conversationHistory();

        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) conversationHistory.getLast();

        List<ToolResponseMessage.ToolResponse> toolResponses = toolResponseMessage.getResponses();

        assertThat(countingToolCallback.calls()).isEqualTo(1);
        assertThat(toolResponses).hasSize(2);
        assertThat(toolResponses.getFirst()
            .responseData()).isEqualTo("done");
        assertThat(toolResponses.getLast()
            .responseData()).isNotEqualTo("done");
    }

    private static Prompt promptWithSession(PiiTokenSession session) {
        return new Prompt(
            List.of(),
            ToolCallingChatOptions.builder()
                .toolContext(PiiTokenSessionToolContext.into(Map.of(), session))
                .build());
    }

    private static Prompt promptWithoutSession() {
        return new Prompt(List.of(), ToolCallingChatOptions.builder()
            .build());
    }

    private static ChatResponse chatResponseWithToolCall(String toolName, String arguments) {
        AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall("call-1", "function", toolName, arguments);
        AssistantMessage assistantMessage = AssistantMessage.builder()
            .content("")
            .toolCalls(List.of(toolCall))
            .build();

        return new ChatResponse(List.of(new Generation(assistantMessage)));
    }

    private static final class CountingToolCallback implements ToolCallback {

        private final List<String> inputs = new ArrayList<>();
        private final ToolDefinition toolDefinition;

        private CountingToolCallback(String name) {
            this.toolDefinition = DefaultToolDefinition.builder()
                .name(name)
                .description(name)
                .inputSchema("{\"type\":\"object\"}")
                .build();
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return toolDefinition;
        }

        @Override
        public String call(String toolInput) {
            inputs.add(toolInput);

            return "done";
        }

        private int calls() {
            return inputs.size();
        }
    }

    private static final class RecordingToolCallingManager implements ToolCallingManager {

        private @Nullable ChatResponse lastChatResponse;

        @Override
        public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
            return List.of();
        }

        @Override
        public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
            lastChatResponse = chatResponse;

            return ToolExecutionResult.builder()
                .conversationHistory(prompt.getInstructions())
                .build();
        }

        private String lastArguments() {
            ChatResponse chatResponse = lastChatResponse;

            if (chatResponse == null) {
                throw new IllegalStateException("executeToolCalls was never called");
            }

            Generation generation = chatResponse.getResult();

            if (generation == null) {
                throw new IllegalStateException("Expected a generation in the recorded ChatResponse");
            }

            return generation.getOutput()
                .getToolCalls()
                .getFirst()
                .arguments();
        }
    }
}
