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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.agent.tool.AgentToolCallingManagers;
import com.bytechef.component.ai.llm.facade.AiAgentToolFacade;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.ai.guardrails.AiGuardrailsAdvisorProvider;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tool.execution.ToolExecutionRecorder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.DefaultChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Coverage for the {@link AiGuardrailsAdvisorProvider} wiring in
 * {@link AbstractAiAgentChatAction#getChatClientRequestSpec}: the optional CE SPI is consulted through an
 * {@link ObjectProvider}, the returned advisor (when present) is registered ahead of the rest of the advisor chain,
 * absence of a bean leaves the chain exactly as before, and a blocking violation raised by the advisor propagates out
 * of the request-spec build (and, transitively, {@code perform()}) rather than being swallowed here.
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AbstractAiAgentChatActionGuardrailsTest {

    @Mock
    private AiAgentToolFacade aiAgentToolFacade;

    @Mock
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @Mock
    private ToolCallingManager toolCallingManager;

    @Test
    void testGuardrailsAdvisorRegisteredWhenProviderPresent() throws Exception {
        Parameters inputParameters = MockParametersFactory.create(Map.of());
        Parameters extensions = buildExtensions();

        stubModelLookup();

        Map<String, ComponentConnection> connectionParameters = buildConnectionParameters();

        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getPlatformType()).thenReturn(PlatformType.AUTOMATION);
        when(actionContext.getJobPrincipalId()).thenReturn(42L);

        FakeGuardrailsAdvisor guardrailsAdvisor = new FakeGuardrailsAdvisor();

        AiGuardrailsAdvisorProvider provider = mock(AiGuardrailsAdvisorProvider.class);

        when(provider.getAdvisor(PlatformType.AUTOMATION, 42L, "ai_agent"))
            .thenReturn(Optional.of(guardrailsAdvisor));

        TestAiAgentChatAction action = new TestAiAgentChatAction(
            aiAgentToolFacade, clusterElementDefinitionService, toolCallingManager, null,
            presentProvider(provider));

        try (MockedStatic<ModelUtils> modelUtilsMockedStatic = mockStatic(ModelUtils.class)) {
            modelUtilsMockedStatic.when(() -> ModelUtils.getMessages(any(), any()))
                .thenReturn(List.of());

            ChatClient.ChatClientRequestSpec spec = action.getChatClientRequestSpec(
                inputParameters, connectionParameters, extensions, null, actionContext);

            List<Advisor> advisors = ((DefaultChatClient.DefaultChatClientRequestSpec) spec).getAdvisors();

            // The guardrails advisor self-orders at HIGHEST_PRECEDENCE and is also registered first in the chain —
            // both facts place it ahead of every other advisor Spring AI ends up executing.
            assertThat(advisors).contains(guardrailsAdvisor);
            assertThat(advisors.indexOf(guardrailsAdvisor)).isZero();
        }
    }

    @Test
    void testNoProviderMeansNoAdvisor() throws Exception {
        Parameters inputParameters = MockParametersFactory.create(Map.of());
        Parameters extensions = buildExtensions();

        stubModelLookup();

        Map<String, ComponentConnection> connectionParameters = buildConnectionParameters();

        ActionContextAware actionContext = mock(ActionContextAware.class);

        // No ObjectProvider at all (the pre-Task-6 constructor) and an empty ObjectProvider both mean the request
        // spec is built exactly as it was before this wiring existed — no guardrails advisor, no interaction with
        // the platform-type/job-principal accessors that only exist to feed the provider call.
        TestAiAgentChatAction action = new TestAiAgentChatAction(
            aiAgentToolFacade, clusterElementDefinitionService, toolCallingManager, null,
            emptyProvider());

        try (MockedStatic<ModelUtils> modelUtilsMockedStatic = mockStatic(ModelUtils.class)) {
            modelUtilsMockedStatic.when(() -> ModelUtils.getMessages(any(), any()))
                .thenReturn(List.of());

            ChatClient.ChatClientRequestSpec spec = action.getChatClientRequestSpec(
                inputParameters, connectionParameters, extensions, null, actionContext);

            List<Advisor> advisors = ((DefaultChatClient.DefaultChatClientRequestSpec) spec).getAdvisors();

            assertThat(advisors).noneMatch(FakeGuardrailsAdvisor.class::isInstance);
        }

        verify(actionContext, never()).getPlatformType();
        verify(actionContext, never()).getJobPrincipalId();
    }

    @Test
    void testBlockingViolationFailsTheStep() throws Exception {
        Parameters inputParameters = MockParametersFactory.create(Map.of());
        Parameters extensions = buildExtensions();

        ChatModel chatModel = stubModelLookup();

        // The advisor chain executes for real in this test (spec.call().content() below), so
        // DefaultChatClientUtils.toChatClientRequest needs a real ChatOptions to .mutate() while assembling the
        // request — that happens before any advisor (including the blocking one) runs.
        when(chatModel.getOptions()).thenReturn(ChatOptions.builder()
            .build());

        Map<String, ComponentConnection> connectionParameters = buildConnectionParameters();

        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getPlatformType()).thenReturn(PlatformType.AUTOMATION);
        when(actionContext.getJobPrincipalId()).thenReturn(42L);

        // Stands in for the EE AiGuardrailViolationException without the CE component depending on the EE
        // guardrails module: a BLOCK-mode advisor throws before delegating to the rest of the chain, and its
        // message names only the violation category — never the offending content.
        BlockingGuardrailAdvisor blockingAdvisor = new BlockingGuardrailAdvisor("blocked-term");

        AiGuardrailsAdvisorProvider provider = mock(AiGuardrailsAdvisorProvider.class);

        when(provider.getAdvisor(PlatformType.AUTOMATION, 42L, "ai_agent"))
            .thenReturn(Optional.of(blockingAdvisor));

        TestAiAgentChatAction action = new TestAiAgentChatAction(
            aiAgentToolFacade, clusterElementDefinitionService, toolCallingManager, null,
            presentProvider(provider));

        try (MockedStatic<ModelUtils> modelUtilsMockedStatic = mockStatic(ModelUtils.class)) {
            modelUtilsMockedStatic.when(() -> ModelUtils.getMessages(any(), any()))
                .thenReturn(List.of());

            ChatClient.ChatClientRequestSpec spec = action.getChatClientRequestSpec(
                inputParameters, connectionParameters, extensions, null, actionContext);

            // getChatClientRequestSpec itself never invokes the advisor chain — the violation only surfaces once a
            // terminal call (call().content()/.chatResponse()) actually walks it. Nothing in this component catches
            // it along the way, so it propagates straight out to the caller (perform()), failing the step.
            assertThatThrownBy(() -> spec.call()
                .content())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Blocked by AI guardrail: blocked-term");
        }
    }

    private Parameters buildExtensions() {
        return MockParametersFactory.create(
            Map.of("clusterElements", Map.of("model", buildModelElement())));
    }

    private Map<String, ComponentConnection> buildConnectionParameters() {
        ComponentConnection componentConnection = new ComponentConnection(
            "testComponent", 1, 1L, Map.of(), null);

        return Map.of("model_1", componentConnection);
    }

    private static Map<String, Object> buildModelElement() {
        HashMap<String, Object> modelParams = new HashMap<>();
        modelParams.put("model", "gpt-4o");

        Map<String, Object> modelElement = new HashMap<>();
        modelElement.put("name", "model_1");
        modelElement.put("type", "testComponent/v1/testModel");
        modelElement.put("parameters", modelParams);

        return modelElement;
    }

    private ChatModel stubModelLookup() throws Exception {
        ModelFunction modelFunction = mock(ModelFunction.class);
        ChatModel chatModel = mock(ChatModel.class);

        when(clusterElementDefinitionService.<ModelFunction>getClusterElement(
            eq("testComponent"), eq(1), eq("testModel"))).thenReturn(modelFunction);
        when(modelFunction.apply(any(), any(), anyBoolean())).thenAnswer(invocation -> chatModel);

        return chatModel;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> presentProvider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);

        doAnswer(invocation -> {
            Consumer<T> consumer = invocation.getArgument(0);

            consumer.accept(value);

            return null;
        }).when(provider)
            .ifAvailable(any());

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> emptyProvider() {
        return mock(ObjectProvider.class);
    }

    private static class TestAiAgentChatAction extends AbstractAiAgentChatAction {

        TestAiAgentChatAction(
            AiAgentToolFacade aiAgentToolFacade, ClusterElementDefinitionService clusterElementDefinitionService,
            ToolCallingManager toolCallingManager,
            ObjectProvider<ToolExecutionRecorder> toolExecutionRecorderObjectProvider,
            ObjectProvider<AiGuardrailsAdvisorProvider> aiGuardrailsAdvisorProviderObjectProvider) {

            super(
                aiAgentToolFacade, clusterElementDefinitionService, new AgentToolCallingManagers(toolCallingManager),
                toolExecutionRecorderObjectProvider, aiGuardrailsAdvisorProviderObjectProvider);
        }
    }

    /** Minimal no-op advisor used to assert presence/position in the built chain. */
    private static class FakeGuardrailsAdvisor implements CallAdvisor {

        @Override
        public String getName() {
            return "FakeGuardrailsAdvisor";
        }

        @Override
        public int getOrder() {
            return HIGHEST_PRECEDENCE;
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
            return callAdvisorChain.nextCall(chatClientRequest);
        }
    }

    /** Simulates a BLOCK-mode guardrail violation: throws before delegating to the rest of the chain. */
    private static class BlockingGuardrailAdvisor implements CallAdvisor {

        private final String category;

        BlockingGuardrailAdvisor(String category) {
            this.category = category;
        }

        @Override
        public String getName() {
            return "BlockingGuardrailAdvisor";
        }

        @Override
        public int getOrder() {
            return HIGHEST_PRECEDENCE;
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
            throw new RuntimeException("Blocked by AI guardrail: " + category);
        }
    }
}
