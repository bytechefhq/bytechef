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
import com.bytechef.platform.ai.workspaceprompt.WorkspaceSystemPromptAdvisorProvider;
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
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Coverage for the {@link WorkspaceSystemPromptAdvisorProvider} wiring in
 * {@link AbstractAiAgentChatAction#getChatClientRequestSpec}: the optional CE SPI is consulted through an
 * {@link ObjectProvider}, the returned advisor (when present) is registered into the same advisor list as the
 * guardrails advisor, absence of a bean leaves the chain exactly as before, and the workspace advisor lands AFTER a
 * guardrails advisor when both providers are present (order-safe listing, per the seam's own self-ordering contract).
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AbstractAiAgentChatActionWorkspaceSystemPromptTest {

    @Mock
    private AiAgentToolFacade aiAgentToolFacade;

    @Mock
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @Mock
    private ToolCallingManager toolCallingManager;

    @Test
    void testWorkspaceSystemPromptAdvisorRegisteredWhenProviderPresent() throws Exception {
        Parameters inputParameters = MockParametersFactory.create(Map.of());
        Parameters extensions = buildExtensions();

        stubModelLookup();

        Map<String, ComponentConnection> connectionParameters = buildConnectionParameters();

        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getPlatformType()).thenReturn(PlatformType.AUTOMATION);
        when(actionContext.getJobPrincipalId()).thenReturn(42L);

        FakeWorkspaceSystemPromptAdvisor workspaceAdvisor = new FakeWorkspaceSystemPromptAdvisor();

        WorkspaceSystemPromptAdvisorProvider provider = mock(WorkspaceSystemPromptAdvisorProvider.class);

        when(provider.getAdvisor(PlatformType.AUTOMATION, 42L, "ai_agent"))
            .thenReturn(Optional.of(workspaceAdvisor));

        TestAiAgentChatAction action = new TestAiAgentChatAction(
            aiAgentToolFacade, clusterElementDefinitionService, toolCallingManager, null, null,
            presentProvider(provider));

        try (MockedStatic<ModelUtils> modelUtilsMockedStatic = mockStatic(ModelUtils.class)) {
            modelUtilsMockedStatic.when(() -> ModelUtils.getMessages(any(), any()))
                .thenReturn(List.of());

            ChatClient.ChatClientRequestSpec spec = action.getChatClientRequestSpec(
                inputParameters, connectionParameters, extensions, null, actionContext);

            List<Advisor> advisors = ((DefaultChatClient.DefaultChatClientRequestSpec) spec).getAdvisors();

            assertThat(advisors).contains(workspaceAdvisor);
        }

        verify(provider).getAdvisor(PlatformType.AUTOMATION, 42L, "ai_agent");
    }

    @Test
    void testNoProviderMeansNoWorkspaceSystemPromptAdvisor() throws Exception {
        Parameters inputParameters = MockParametersFactory.create(Map.of());
        Parameters extensions = buildExtensions();

        stubModelLookup();

        Map<String, ComponentConnection> connectionParameters = buildConnectionParameters();

        ActionContextAware actionContext = mock(ActionContextAware.class);

        // No ObjectProvider at all (the pre-Task-9 constructor) and an empty ObjectProvider both mean the request
        // spec is built exactly as it was before this wiring existed — no workspace system prompt advisor, no
        // interaction with the platform-type/job-principal accessors that only exist to feed the provider call.
        TestAiAgentChatAction action = new TestAiAgentChatAction(
            aiAgentToolFacade, clusterElementDefinitionService, toolCallingManager, null, null,
            emptyProvider());

        try (MockedStatic<ModelUtils> modelUtilsMockedStatic = mockStatic(ModelUtils.class)) {
            modelUtilsMockedStatic.when(() -> ModelUtils.getMessages(any(), any()))
                .thenReturn(List.of());

            ChatClient.ChatClientRequestSpec spec = action.getChatClientRequestSpec(
                inputParameters, connectionParameters, extensions, null, actionContext);

            List<Advisor> advisors = ((DefaultChatClient.DefaultChatClientRequestSpec) spec).getAdvisors();

            assertThat(advisors).noneMatch(FakeWorkspaceSystemPromptAdvisor.class::isInstance);
        }

        verify(actionContext, never()).getPlatformType();
        verify(actionContext, never()).getJobPrincipalId();
    }

    @Test
    void testWorkspaceSystemPromptAdvisorListedAfterGuardrailsAdvisor() throws Exception {
        Parameters inputParameters = MockParametersFactory.create(Map.of());
        Parameters extensions = buildExtensions();

        stubModelLookup();

        Map<String, ComponentConnection> connectionParameters = buildConnectionParameters();

        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getPlatformType()).thenReturn(PlatformType.AUTOMATION);
        when(actionContext.getJobPrincipalId()).thenReturn(42L);

        FakeGuardrailsAdvisor guardrailsAdvisor = new FakeGuardrailsAdvisor();

        AiGuardrailsAdvisorProvider guardrailsProvider = mock(AiGuardrailsAdvisorProvider.class);

        when(guardrailsProvider.getAdvisor(PlatformType.AUTOMATION, 42L, "ai_agent"))
            .thenReturn(Optional.of(guardrailsAdvisor));

        FakeWorkspaceSystemPromptAdvisor workspaceAdvisor = new FakeWorkspaceSystemPromptAdvisor();

        WorkspaceSystemPromptAdvisorProvider workspaceProvider = mock(WorkspaceSystemPromptAdvisorProvider.class);

        when(workspaceProvider.getAdvisor(PlatformType.AUTOMATION, 42L, "ai_agent"))
            .thenReturn(Optional.of(workspaceAdvisor));

        TestAiAgentChatAction action = new TestAiAgentChatAction(
            aiAgentToolFacade, clusterElementDefinitionService, toolCallingManager, null,
            presentProvider(guardrailsProvider), presentProvider(workspaceProvider));

        try (MockedStatic<ModelUtils> modelUtilsMockedStatic = mockStatic(ModelUtils.class)) {
            modelUtilsMockedStatic.when(() -> ModelUtils.getMessages(any(), any()))
                .thenReturn(List.of());

            ChatClient.ChatClientRequestSpec spec = action.getChatClientRequestSpec(
                inputParameters, connectionParameters, extensions, null, actionContext);

            List<Advisor> advisors = ((DefaultChatClient.DefaultChatClientRequestSpec) spec).getAdvisors();

            assertThat(advisors.indexOf(guardrailsAdvisor)).isLessThan(advisors.indexOf(workspaceAdvisor));
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
            ObjectProvider<AiGuardrailsAdvisorProvider> aiGuardrailsAdvisorProviderObjectProvider,
            ObjectProvider<WorkspaceSystemPromptAdvisorProvider> workspaceSystemPromptAdvisorProviderObjectProvider) {

            super(
                aiAgentToolFacade, clusterElementDefinitionService, new AgentToolCallingManagers(toolCallingManager),
                toolExecutionRecorderObjectProvider, aiGuardrailsAdvisorProviderObjectProvider,
                workspaceSystemPromptAdvisorProviderObjectProvider);
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

    /** Minimal no-op advisor used to assert presence/position in the built chain. */
    private static class FakeWorkspaceSystemPromptAdvisor implements CallAdvisor {

        @Override
        public String getName() {
            return "FakeWorkspaceSystemPromptAdvisor";
        }

        @Override
        public int getOrder() {
            return HIGHEST_PRECEDENCE + 1;
        }

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
            return callAdvisorChain.nextCall(chatClientRequest);
        }
    }
}
