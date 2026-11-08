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

import static com.bytechef.component.definition.ActionDefinition.SseEmitterHandler.SseEmitter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.facade.AiAgentToolFacade;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.ai.conversation.AgentConversationRecorder;
import com.bytechef.platform.ai.conversation.AgentConversationRecorder.AgentConversation;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.MultipleConnectionsStreamPerformFunction;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.resolution.DelegatingToolCallbackResolver;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AiAgentStreamChatActionTenantPropagationIntTest {

    private static final long AI_AGENT_ID = 20L;
    private static final String CONVERSATION_ID = "conversation-1";
    private static final long CREATOR_USER_ID = 30L;
    private static final Long ENVIRONMENT_ID = 2L;
    private static final String FINAL_ANSWER = "streamed answer";

    private static final int LATCH_TIMEOUT_SECONDS = 10;

    private static final String TENANT_ID = "tenant_732";
    private static final String WORKFLOW_ID = "workflow-1";
    private static final long WORKSPACE_ID = 10L;

    @Test
    void testRecorderObservesPerformThreadTenantWithoutBlockingStreamCompletion() throws Exception {
        AtomicReference<String> observedTenantId = new AtomicReference<>();
        AtomicReference<String> observedThreadName = new AtomicReference<>();
        AtomicReference<AgentConversation> observedAgentConversation = new AtomicReference<>();

        CountDownLatch recorderRelease = new CountDownLatch(1);
        CountDownLatch recorderFinished = new CountDownLatch(1);
        AtomicBoolean recorderReleaseObserved = new AtomicBoolean();

        AgentConversationRecorder agentConversationRecorder = agentConversation -> {
            observedTenantId.set(TenantContext.getCurrentTenantId());

            Thread thread = Thread.currentThread();

            observedThreadName.set(thread.getName());
            observedAgentConversation.set(agentConversation);

            try {
                recorderReleaseObserved.set(recorderRelease.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS));
            } catch (InterruptedException interruptedException) {
                thread.interrupt();
            }

            recorderFinished.countDown();
        };

        ActionDefinition actionDefinition = AiAgentStreamChatAction.of(
            mock(AiAgentToolFacade.class), buildClusterElementDefinitionService(),
            DefaultToolCallingManager.builder()
                .toolCallbackResolver(new DelegatingToolCallbackResolver(List.of()))
                .build(),
            presentProvider(agentConversationRecorder));

        MultipleConnectionsStreamPerformFunction performFunction =
            (MultipleConnectionsStreamPerformFunction) actionDefinition.getPerform()
                .orElseThrow();

        ActionContextAware context = mock(ActionContextAware.class);

        when(context.isEditorEnvironment()).thenReturn(false);

        when(context.getWorkflowId()).thenReturn(WORKFLOW_ID);
        when(context.getEnvironmentId()).thenReturn(ENVIRONMENT_ID);

        Thread performThread = Thread.currentThread();
        String previousTenantId = TenantContext.getCurrentTenantId();

        SseEmitterHandler sseEmitterHandler;

        try {
            TenantContext.setCurrentTenantId(TENANT_ID);

            sseEmitterHandler = performFunction.apply(
                buildInputParameters(), buildConnectionParameters(), buildExtensions(), context);
        } finally {
            TenantContext.setCurrentTenantId(previousTenantId);
        }

        String completionThreadName;

        try {
            completionThreadName = driveHandlerToCompletion(sseEmitterHandler);
        } finally {
            recorderRelease.countDown();
        }

        boolean recorded = recorderFinished.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertThat(recorded).as("the conversation recorder did not run")
            .isTrue();
        assertThat(recorderReleaseObserved).as("the conversation recorder was not released cleanly")
            .isTrue();

        assertThat(observedAgentConversation.get())
            .isEqualTo(
                new AgentConversation(
                    WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, CONVERSATION_ID, null, null, WORKFLOW_ID,
                    ENVIRONMENT_ID));

        assertThat(observedThreadName.get()).isNotEqualTo(performThread.getName());

        assertThat(observedThreadName.get())
            .as("the conversation recorder must not run on the stream-completion thread")
            .isNotEqualTo(completionThreadName);

        assertThat(observedTenantId.get()).isEqualTo(TENANT_ID);
    }

    private static String driveHandlerToCompletion(SseEmitterHandler sseEmitterHandler) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> completionThreadName = new AtomicReference<>();

        SseEmitter sseEmitter = new SseEmitter() {

            @Override
            public void send(Object data) {
            }

            @Override
            public void complete() {
                recordCompletion();
            }

            @Override
            public void error(Throwable throwable) {
                recordCompletion();
            }

            @Override
            public void addTimeoutListener(Runnable runnable) {
            }

            private void recordCompletion() {
                Thread thread = Thread.currentThread();

                completionThreadName.set(thread.getName());

                latch.countDown();
            }
        };

        sseEmitterHandler.handle(sseEmitter);

        boolean completed = latch.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertThat(completed)
            .as("SSE handler did not complete — the turn recorder is blocking the stream-completion thread")
            .isTrue();

        return completionThreadName.get();
    }

    private static Parameters buildInputParameters() {
        return MockParametersFactory.create(
            Map.of(
                "format", "SIMPLE",
                "userPrompt", "hello",
                "response", Map.of("responseFormat", "TEXT")));
    }

    private static Map<String, ComponentConnection> buildConnectionParameters() {
        Map<String, ComponentConnection> connectionParameters = new HashMap<>();

        connectionParameters.put("model_1", new ComponentConnection("testComponent", 1, 1L, Map.of(), null));

        return connectionParameters;
    }

    private static Parameters buildExtensions() {
        Map<String, Object> modelElement = new HashMap<>();

        modelElement.put("name", "model_1");
        modelElement.put("type", "testComponent/v1/testModel");
        modelElement.put("parameters", Map.of("model", "gpt-4o"));

        Map<String, Object> chatMemoryElement = new HashMap<>();

        chatMemoryElement.put("name", "chatMemory_1");
        chatMemoryElement.put("type", "testComponent/v1/testChatMemory");
        chatMemoryElement.put("parameters", Map.of("conversationId", CONVERSATION_ID));

        Map<String, Object> extensionsMap = new HashMap<>();

        extensionsMap.put("clusterElements", Map.of("model", modelElement, "chatMemory", chatMemoryElement));
        extensionsMap.put("aiHubWorkspaceId", WORKSPACE_ID);
        extensionsMap.put("aiHubAgentId", AI_AGENT_ID);
        extensionsMap.put("aiHubCreatorUserId", CREATOR_USER_ID);

        return MockParametersFactory.create(extensionsMap);
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private static ClusterElementDefinitionService buildClusterElementDefinitionService() throws Exception {
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);

        ModelFunction modelFunction = mock(ModelFunction.class);

        when(clusterElementDefinitionService.<ModelFunction>getClusterElement(
            eq("testComponent"), eq(1), eq("testModel"))).thenReturn(modelFunction);
        when(modelFunction.apply(any(), any(), anyBoolean())).thenAnswer(invocation -> buildChatModel());

        ChatMemoryFunction chatMemoryFunction = mock(ChatMemoryFunction.class);

        when(clusterElementDefinitionService.<ChatMemoryFunction>getClusterElement(
            eq("testComponent"), eq(1), eq("testChatMemory"))).thenReturn(chatMemoryFunction);
        when(chatMemoryFunction.apply(any(), any(), any(), any(), any())).thenReturn(buildChatMemoryResult());

        return clusterElementDefinitionService;
    }

    private static ChatMemoryFunction.Result buildChatMemoryResult() {
        MessageWindowChatMemory messageWindowChatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(new InMemoryChatMemoryRepository())
            .maxMessages(10)
            .build();

        MessageChatMemoryAdvisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor
            .builder(messageWindowChatMemory)
            .build();

        return ChatMemoryFunction.Result.of(messageChatMemoryAdvisor, messageWindowChatMemory);
    }

    private static org.springframework.ai.chat.model.ChatModel buildChatModel() {
        return new org.springframework.ai.chat.model.ChatModel() {

            @Override
            public ChatResponse call(Prompt prompt) {
                return buildFinalAnswerResponse();
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                return Flux.just(buildFinalAnswerResponse())
                    .subscribeOn(Schedulers.boundedElastic());
            }

            @Override
            public ToolCallingChatOptions getOptions() {
                return ToolCallingChatOptions.builder()
                    .build();
            }
        };
    }

    private static ChatResponse buildFinalAnswerResponse() {
        return ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(FINAL_ANSWER))))
            .build();
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> presentProvider(T value) {
        ObjectProvider<T> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(value);

        return objectProvider;
    }
}
