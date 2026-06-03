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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.agent.facade.AiAgentToolFacade;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.DefaultChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingManager;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AbstractAiAgentChatActionTest {

    @Mock
    private AiAgentToolFacade aiAgentToolFacade;

    @Mock
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @Mock
    private ToolCallingManager toolCallingManager;

    @Test
    void testGetChatClientRequestSpecWithNullParameterValues() throws Exception {
        HashMap<String, Object> inputParamsMap = new HashMap<>();

        inputParamsMap.put("key1", "value1");
        inputParamsMap.put("nullableKey", null);

        Parameters inputParameters = MockParametersFactory.create(inputParamsMap);

        HashMap<String, Object> clusterElementParams = new HashMap<>();

        clusterElementParams.put("model", "gpt-4o");
        clusterElementParams.put("conversationId", "sampleConversationId");
        clusterElementParams.put("nullableParam", null);

        Map<String, Object> modelElement = new HashMap<>();

        modelElement.put("name", "model_1");
        modelElement.put("type", "testComponent/v1/testModel");
        modelElement.put("parameters", clusterElementParams);

        Map<String, Object> chatMemoryElement = new HashMap<>();

        chatMemoryElement.put("name", "chatMemory_1");
        chatMemoryElement.put("type", "testComponent/v1/testChatMemory");
        chatMemoryElement.put("parameters", clusterElementParams);

        Parameters extensions = MockParametersFactory.create(
            Map.of("clusterElements", Map.of("model", modelElement, "chatMemory", chatMemoryElement)));

        ModelFunction modelFunction = mock(ModelFunction.class);
        ChatMemoryFunction chatMemoryFunction = mock(ChatMemoryFunction.class);

        ChatModel chatModel = mock(ChatModel.class);
        BaseChatMemoryAdvisor chatMemoryAdvisor = mock(BaseChatMemoryAdvisor.class);
        ChatMemoryFunction.Result chatMemoryResult = new ChatMemoryFunction.Result(chatMemoryAdvisor, null);

        when(clusterElementDefinitionService.<ModelFunction>getClusterElement(
            eq("testComponent"), eq(1), eq("testModel"))).thenReturn(modelFunction);
        when(clusterElementDefinitionService.<ChatMemoryFunction>getClusterElement(
            eq("testComponent"), eq(1), eq("testChatMemory"))).thenReturn(chatMemoryFunction);
        when(modelFunction.apply(any(), any(), anyBoolean())).thenAnswer(invocation -> chatModel);
        when(chatMemoryFunction.apply(any(), any(), any(), any())).thenReturn(chatMemoryResult);

        ComponentConnection componentConnection = new ComponentConnection(
            "testComponent", 1, 1L, Map.of(), null);

        Map<String, ComponentConnection> connectionParameters = Map.of("model_1", componentConnection);

        ActionContext actionContext = mock(ActionContext.class);

        TestAiAgentChatAction action = new TestAiAgentChatAction(
            aiAgentToolFacade, clusterElementDefinitionService, toolCallingManager);

        try (MockedStatic<ModelUtils> modelUtilsMockedStatic = mockStatic(ModelUtils.class)) {
            modelUtilsMockedStatic.when(() -> ModelUtils.getMessages(any(), any()))
                .thenReturn(List.of());

            assertDoesNotThrow(() -> action.getChatClientRequestSpec(
                inputParameters, connectionParameters, extensions, null, actionContext));
        }
    }

    @Test
    void testMultipleCheckForViolationsRejectedAtAdvisorBuild() throws Exception {
        Parameters inputParameters = MockParametersFactory.create(Map.of());

        Map<String, Object> modelElement = buildModelElement();
        Map<String, Object> checkForViolationsA = buildGuardrailElement(
            "checkForViolationsA", "checkForViolations/v1/checkForViolations");
        Map<String, Object> checkForViolationsB = buildGuardrailElement(
            "checkForViolationsB", "checkForViolations/v1/checkForViolations");

        Parameters extensions = MockParametersFactory.create(
            Map.of(
                "clusterElements",
                Map.of(
                    "model", modelElement,
                    "guardrails", List.of(checkForViolationsA, checkForViolationsB))));

        stubModelLookup();

        ComponentConnection componentConnection = new ComponentConnection(
            "testComponent", 1, 1L, Map.of(), null);
        Map<String, ComponentConnection> connectionParameters = Map.of("model_1", componentConnection);
        ActionContext actionContext = mock(ActionContext.class);

        TestAiAgentChatAction action = new TestAiAgentChatAction(
            aiAgentToolFacade, clusterElementDefinitionService, toolCallingManager);

        try (MockedStatic<ModelUtils> modelUtilsMockedStatic = mockStatic(ModelUtils.class)) {
            modelUtilsMockedStatic.when(() -> ModelUtils.getMessages(any(), any()))
                .thenReturn(List.of());

            assertThatThrownBy(() -> action.getChatClientRequestSpec(
                inputParameters, connectionParameters, extensions, null, actionContext))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Multiple CheckForViolations");
        }
    }

    @Test
    void testMultipleSanitizeTextRejectedAtAdvisorBuild() throws Exception {
        Parameters inputParameters = MockParametersFactory.create(Map.of());

        Map<String, Object> modelElement = buildModelElement();
        Map<String, Object> sanitizeTextA = buildGuardrailElement(
            "sanitizeTextA", "sanitizeText/v1/sanitizeText");
        Map<String, Object> sanitizeTextB = buildGuardrailElement(
            "sanitizeTextB", "sanitizeText/v1/sanitizeText");

        Parameters extensions = MockParametersFactory.create(
            Map.of(
                "clusterElements",
                Map.of(
                    "model", modelElement,
                    "guardrails", List.of(sanitizeTextA, sanitizeTextB))));

        stubModelLookup();

        ComponentConnection componentConnection = new ComponentConnection(
            "testComponent", 1, 1L, Map.of(), null);
        Map<String, ComponentConnection> connectionParameters = Map.of("model_1", componentConnection);
        ActionContext actionContext = mock(ActionContext.class);

        TestAiAgentChatAction action = new TestAiAgentChatAction(
            aiAgentToolFacade, clusterElementDefinitionService, toolCallingManager);

        try (MockedStatic<ModelUtils> modelUtilsMockedStatic = mockStatic(ModelUtils.class)) {
            modelUtilsMockedStatic.when(() -> ModelUtils.getMessages(any(), any()))
                .thenReturn(List.of());

            assertThatThrownBy(() -> action.getChatClientRequestSpec(
                inputParameters, connectionParameters, extensions, null, actionContext))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Multiple SanitizeText");
        }
    }

    @Test
    void testSingleCheckForViolationsAndSingleSanitizeTextAccepted() throws Exception {
        Parameters inputParameters = MockParametersFactory.create(Map.of());

        Map<String, Object> modelElement = buildModelElement();
        Map<String, Object> checkForViolations = buildGuardrailElement(
            "checkForViolations_1", "checkForViolations/v1/checkForViolations");
        Map<String, Object> sanitizeText = buildGuardrailElement(
            "sanitizeText_1", "sanitizeText/v1/sanitizeText");

        Parameters extensions = MockParametersFactory.create(
            Map.of(
                "clusterElements",
                Map.of(
                    "model", modelElement,
                    "guardrails", List.of(checkForViolations, sanitizeText))));

        ModelFunction modelFunction = mock(ModelFunction.class);
        ChatModel chatModel = mock(ChatModel.class);

        when(clusterElementDefinitionService.<ModelFunction>getClusterElement(
            eq("testComponent"), eq(1), eq("testModel"))).thenReturn(modelFunction);
        when(modelFunction.apply(any(), any(), anyBoolean())).thenAnswer(invocation -> chatModel);

        com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction guardrailsFunction = mock(
            com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction.class);
        Advisor advisor = mock(Advisor.class);

        when(
            clusterElementDefinitionService.<com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction>getClusterElement(
                eq("checkForViolations"), eq(1), eq("checkForViolations")))
                    .thenReturn(guardrailsFunction);
        when(
            clusterElementDefinitionService.<com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction>getClusterElement(
                eq("sanitizeText"), eq(1), eq("sanitizeText")))
                    .thenReturn(guardrailsFunction);
        when(guardrailsFunction.apply(any(), any(), any(), any(), any(), any()))
            .thenReturn(advisor);

        ComponentConnection componentConnection = new ComponentConnection(
            "testComponent", 1, 1L, Map.of(), null);
        Map<String, ComponentConnection> connectionParameters = new HashMap<>();

        connectionParameters.put("model_1", componentConnection);
        connectionParameters.put("checkForViolations_1", componentConnection);
        connectionParameters.put("sanitizeText_1", componentConnection);

        ActionContext actionContext = mock(ActionContext.class);

        TestAiAgentChatAction action = new TestAiAgentChatAction(
            aiAgentToolFacade, clusterElementDefinitionService, toolCallingManager);

        try (MockedStatic<ModelUtils> modelUtilsMockedStatic = mockStatic(ModelUtils.class)) {
            modelUtilsMockedStatic.when(() -> ModelUtils.getMessages(any(), any()))
                .thenReturn(List.of());

            assertDoesNotThrow(() -> action.getChatClientRequestSpec(
                inputParameters, connectionParameters, extensions, null, actionContext));
        }

        assertThat(advisor).isNotNull();
    }

    @Test
    void testGuardrailAdvisorBuildFailureInvokesContextLog() throws Exception {
        // Pins the Context-threading promise from commit ee185584cf5: when a cluster element fails to initialize,
        // the error path MUST go through ActionContext.log(...) rather than raw SLF4J, so tenant-aware structured
        // logging captures the failure. A refactor dropping context.log for raw SLF4J would otherwise pass CI.
        Parameters inputParameters = MockParametersFactory.create(Map.of());

        Map<String, Object> modelElement = buildModelElement();
        Map<String, Object> guardrailElement = buildGuardrailElement(
            "checkForViolations_1", "checkForViolations/v1/checkForViolations");

        Parameters extensions = MockParametersFactory.create(
            Map.of(
                "clusterElements",
                Map.of(
                    "model", modelElement,
                    "guardrails", List.of(guardrailElement))));

        stubModelLookup();

        com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction guardrailsFunction = mock(
            com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction.class);

        when(
            clusterElementDefinitionService.<com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction>getClusterElement(
                eq("checkForViolations"), eq(1), eq("checkForViolations")))
                    .thenReturn(guardrailsFunction);
        when(guardrailsFunction.apply(any(), any(), any(), any(), any(), any()))
            .thenThrow(new RuntimeException("simulated guardrail init failure"));

        ComponentConnection componentConnection = new ComponentConnection(
            "testComponent", 1, 1L, Map.of(), null);
        Map<String, ComponentConnection> connectionParameters = new HashMap<>();

        connectionParameters.put("model_1", componentConnection);
        connectionParameters.put("checkForViolations_1", componentConnection);

        ActionContext actionContext = mock(ActionContext.class);

        TestAiAgentChatAction action = new TestAiAgentChatAction(
            aiAgentToolFacade, clusterElementDefinitionService, toolCallingManager);

        try (MockedStatic<ModelUtils> modelUtilsMockedStatic = mockStatic(ModelUtils.class)) {
            modelUtilsMockedStatic.when(() -> ModelUtils.getMessages(any(), any()))
                .thenReturn(List.of());

            assertThatThrownBy(() -> action.getChatClientRequestSpec(
                inputParameters, connectionParameters, extensions, null, actionContext))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("guardrails");
        }

        verify(actionContext, org.mockito.Mockito.atLeastOnce()).log(any());
    }

    @Test
    void testNoGuardrailsConfiguredRunsModelChainWithoutAddingAdvisor() throws Exception {
        Parameters inputParameters = MockParametersFactory.create(Map.of());

        Map<String, Object> modelElement = buildModelElement();

        Parameters extensions = MockParametersFactory.create(
            Map.of(
                "clusterElements",
                Map.of("model", modelElement)));

        stubModelLookup();

        ComponentConnection componentConnection = new ComponentConnection(
            "testComponent", 1, 1L, Map.of(), null);
        Map<String, ComponentConnection> connectionParameters = new HashMap<>();

        connectionParameters.put("model_1", componentConnection);

        ActionContext actionContext = mock(ActionContext.class);

        TestAiAgentChatAction action = new TestAiAgentChatAction(
            aiAgentToolFacade, clusterElementDefinitionService, toolCallingManager);

        try (MockedStatic<ModelUtils> modelUtilsMockedStatic = mockStatic(ModelUtils.class)) {
            modelUtilsMockedStatic.when(() -> ModelUtils.getMessages(any(), any()))
                .thenReturn(List.of());

            assertDoesNotThrow(() -> action.getChatClientRequestSpec(
                inputParameters, connectionParameters, extensions, null, actionContext));
        }

        verify(clusterElementDefinitionService,
            never()).<com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction>getClusterElement(
                eq("checkForViolations"), anyInt(), anyString());
        verify(clusterElementDefinitionService,
            never()).<com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction>getClusterElement(
                eq("sanitizeText"), anyInt(), anyString());
    }

    @Test
    void testChatMemoryAdvisorOrderedDownstreamOfToolCallAdvisor() throws Exception {
        // Regression for the M7 ordering bug: with disableInternalConversationHistory() active,
        // ToolCallAdvisor passes only [systemMsg, lastMessage] between loop iterations and relies on a
        // DOWNSTREAM ChatMemoryAdvisor to rehydrate the preceding assistant-with-tool-calls each turn.
        // If a future change places ChatMemoryAdvisor at a lower order than ToolCallAdvisor, memory
        // moves outside the tool-call loop and the second model call lands at OpenAI as
        // [systemMsg, toolResultMsg], producing
        // "messages with role 'tool' must be a response to a preceding message with 'tool_calls'".
        Parameters inputParameters = MockParametersFactory.create(Map.of());

        Map<String, Object> modelElement = buildModelElement();

        Map<String, Object> chatMemoryParameters = new HashMap<>();
        chatMemoryParameters.put("conversationId", "test-conversation");

        Map<String, Object> chatMemoryElement = new HashMap<>();
        chatMemoryElement.put("name", "chatMemory_1");
        chatMemoryElement.put("type", "testComponent/v1/testChatMemory");
        chatMemoryElement.put("parameters", chatMemoryParameters);

        Parameters extensions = MockParametersFactory.create(
            Map.of("clusterElements", Map.of("model", modelElement, "chatMemory", chatMemoryElement)));

        stubModelLookup();

        ChatMemoryFunction chatMemoryFunction = mock(ChatMemoryFunction.class);

        when(clusterElementDefinitionService.<ChatMemoryFunction>getClusterElement(
            eq("testComponent"), eq(1), eq("testChatMemory"))).thenReturn(chatMemoryFunction);

        // Build the chat-memory advisor exactly as the production chat-memory components do
        // (default order via the builder), so the assertion catches regressions in any of them.
        MessageChatMemoryAdvisor productionStyleChatMemoryAdvisor = MessageChatMemoryAdvisor
            .builder(mock(ChatMemory.class))
            .build();

        when(chatMemoryFunction.apply(any(), any(), any(), any())).thenReturn(productionStyleChatMemoryAdvisor);

        ComponentConnection componentConnection = new ComponentConnection(
            "testComponent", 1, 1L, Map.of(), null);
        Map<String, ComponentConnection> connectionParameters = Map.of(
            "model_1", componentConnection,
            "chatMemory_1", componentConnection);

        ActionContext actionContext = mock(ActionContext.class);

        TestAiAgentChatAction action = new TestAiAgentChatAction(
            aiAgentToolFacade, clusterElementDefinitionService, toolCallingManager);

        try (MockedStatic<ModelUtils> modelUtilsMockedStatic = mockStatic(ModelUtils.class)) {
            modelUtilsMockedStatic.when(() -> ModelUtils.getMessages(any(), any()))
                .thenReturn(List.of());

            ChatClient.ChatClientRequestSpec spec = action.getChatClientRequestSpec(
                inputParameters, connectionParameters, extensions, null, actionContext);

            List<Advisor> advisors = ((DefaultChatClient.DefaultChatClientRequestSpec) spec).getAdvisors();

            Advisor toolCallAdvisor = advisors.stream()
                .filter(ToolCallAdvisor.class::isInstance)
                .findFirst()
                .orElseThrow(() -> new AssertionError("ToolCallAdvisor missing from advisor chain"));

            Advisor chatMemoryAdvisor = advisors.stream()
                .filter(advisor -> advisor instanceof BaseChatMemoryAdvisor)
                .findFirst()
                .orElseThrow(() -> new AssertionError("ChatMemoryAdvisor missing from advisor chain"));

            assertThat(chatMemoryAdvisor.getOrder())
                .as(
                    "ChatMemoryAdvisor MUST run downstream of ToolCallAdvisor so memory participates in " +
                        "every tool-call iteration. With disableInternalConversationHistory() active, the " +
                        "inverse ordering causes OpenAI to reject the second iteration's prompt because the " +
                        "tool-result message has no preceding assistant-with-tool-calls.")
                .isGreaterThan(toolCallAdvisor.getOrder());
        }
    }

    @Test
    void testSpringAiDefaultOrdersComposeCorrectly() {
        // Safety net for Spring AI version bumps. Our chat-memory components and AbstractAiAgentChatAction
        // both rely on Spring AI's builder defaults (no explicit .order(...) / .advisorOrder(...) calls)
        // to produce ToolCallAdvisor < ChatMemoryAdvisor. If a future bump inverts these defaults, every
        // chat-memory-backed agent breaks with the same OpenAI tool-message ordering error — this test
        // surfaces the regression at the dependency-bump PR rather than in production.
        ToolCallAdvisor toolCallAdvisor = ToolCallAdvisor.builder()
            .build();

        MessageChatMemoryAdvisor chatMemoryAdvisor = MessageChatMemoryAdvisor
            .builder(mock(ChatMemory.class))
            .build();

        assertThat(chatMemoryAdvisor.getOrder())
            .as("Spring AI default ordering must keep ChatMemoryAdvisor downstream of ToolCallAdvisor")
            .isGreaterThan(toolCallAdvisor.getOrder());
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

    @Test
    void testGetAdvisorsIncludesToolCallAdvisorWithDefaultConversationHistoryWhenNoChatMemory() {
        ClusterElementMap clusterElementMap = ClusterElementMap.of(
            Map.of("clusterElements", Map.of("model", buildModelClusterElement())));

        ActionContext actionContext = mock(ActionContext.class);

        TestAiAgentChatAction action = new TestAiAgentChatAction(
            aiAgentToolFacade, clusterElementDefinitionService, toolCallingManager);

        List<Advisor> advisors = action.getAdvisors(clusterElementMap, Map.of(), actionContext);

        ToolCallAdvisor toolCallAdvisor = findToolCallAdvisor(advisors);

        assertThat(toolCallAdvisor).isNotNull();
        assertThat(advisors).noneMatch(BaseChatMemoryAdvisor.class::isInstance);
        assertThat(readConversationHistoryEnabled(toolCallAdvisor)).isTrue();
    }

    @Test
    void testGetAdvisorsDisablesConversationHistoryForMessageChatMemoryAdvisor() throws Exception {
        ClusterElementMap clusterElementMap = buildClusterElementMapWithMemory();

        MessageChatMemoryAdvisor chatMemoryAdvisor = MessageChatMemoryAdvisor
            .builder(mock(ChatMemory.class))
            .build();

        ChatMemoryFunction chatMemoryFunction = mock(ChatMemoryFunction.class);

        when(chatMemoryFunction.apply(any(), any(), any(), any())).thenReturn(chatMemoryAdvisor);
        when(clusterElementDefinitionService.<ChatMemoryFunction>getClusterElement(
            eq("memoryComponent"), eq(1), eq("memoryElement"))).thenReturn(chatMemoryFunction);

        Map<String, ComponentConnection> connectionParameters = Map.of(
            "memory_1", new ComponentConnection("memoryComponent", 1, 2L, Map.of(), null));

        List<Advisor> advisors = new TestAiAgentChatAction(
            aiAgentToolFacade, clusterElementDefinitionService, toolCallingManager)
                .getAdvisors(clusterElementMap, connectionParameters, mock(ActionContext.class));

        int chatMemoryIndex = advisors.indexOf(chatMemoryAdvisor);
        ToolCallAdvisor toolCallAdvisor = findToolCallAdvisor(advisors);

        assertThat(chatMemoryIndex).isGreaterThanOrEqualTo(0);
        assertThat(advisors.indexOf(toolCallAdvisor)).isGreaterThan(chatMemoryIndex);
        assertThat(readConversationHistoryEnabled(toolCallAdvisor)).isFalse();
    }

    @Test
    void testGetAdvisorsKeepsConversationHistoryForNonMessageChatMemoryAdvisor() throws Exception {
        ClusterElementMap clusterElementMap = buildClusterElementMapWithMemory();

        BaseChatMemoryAdvisor chatMemoryAdvisor = mock(BaseChatMemoryAdvisor.class);

        ChatMemoryFunction chatMemoryFunction = mock(ChatMemoryFunction.class);

        when(chatMemoryFunction.apply(any(), any(), any(), any())).thenReturn(chatMemoryAdvisor);
        when(clusterElementDefinitionService.<ChatMemoryFunction>getClusterElement(
            eq("memoryComponent"), eq(1), eq("memoryElement"))).thenReturn(chatMemoryFunction);

        Map<String, ComponentConnection> connectionParameters = Map.of(
            "memory_1", new ComponentConnection("memoryComponent", 1, 2L, Map.of(), null));

        List<Advisor> advisors = new TestAiAgentChatAction(
            aiAgentToolFacade, clusterElementDefinitionService, toolCallingManager)
                .getAdvisors(clusterElementMap, connectionParameters, mock(ActionContext.class));

        int chatMemoryIndex = advisors.indexOf(chatMemoryAdvisor);
        ToolCallAdvisor toolCallAdvisor = findToolCallAdvisor(advisors);

        assertThat(chatMemoryIndex).isGreaterThanOrEqualTo(0);
        assertThat(advisors.indexOf(toolCallAdvisor)).isGreaterThan(chatMemoryIndex);
        assertThat(readConversationHistoryEnabled(toolCallAdvisor)).isTrue();
    }

    private static ClusterElementMap buildClusterElementMapWithMemory() {
        Map<String, Object> chatMemoryElement = new HashMap<>();

        chatMemoryElement.put("name", "memory_1");
        chatMemoryElement.put("type", "memoryComponent/v1/memoryElement");
        chatMemoryElement.put("parameters", Map.of());

        return ClusterElementMap.of(
            Map.of("clusterElements",
                Map.of("model", buildModelClusterElement(), "chatMemory", chatMemoryElement)));
    }

    private static Map<String, Object> buildModelClusterElement() {
        Map<String, Object> modelElement = new HashMap<>();

        modelElement.put("name", "model_1");
        modelElement.put("type", "testComponent/v1/testModel");
        modelElement.put("parameters", Map.of());

        return modelElement;
    }

    private static Map<String, Object> buildGuardrailElement(String workflowNodeName, String type) {
        Map<String, Object> element = new HashMap<>();
        element.put("name", workflowNodeName);
        element.put("type", type);
        element.put("parameters", new HashMap<>());

        return element;
    }

    private void stubModelLookup() throws Exception {
        ModelFunction modelFunction = mock(ModelFunction.class);
        ChatModel chatModel = mock(ChatModel.class);

        when(clusterElementDefinitionService.<ModelFunction>getClusterElement(
            eq("testComponent"), eq(1), eq("testModel"))).thenReturn(modelFunction);
        when(modelFunction.apply(any(), any(), anyBoolean())).thenAnswer(invocation -> chatModel);
    }

    private static ToolCallAdvisor findToolCallAdvisor(List<Advisor> advisors) {
        return advisors.stream()
            .filter(ToolCallAdvisor.class::isInstance)
            .map(ToolCallAdvisor.class::cast)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected ToolCallAdvisor in advisor list"));
    }

    private static boolean readConversationHistoryEnabled(ToolCallAdvisor toolCallAdvisor) {
        try {
            Field field = ToolCallAdvisor.class.getDeclaredField("conversationHistoryEnabled");

            field.setAccessible(true);

            return field.getBoolean(toolCallAdvisor);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(
                "Unable to read conversationHistoryEnabled from ToolCallAdvisor — "
                    + "field name changed in Spring AI?",
                exception);
        }
    }

    private static class TestAiAgentChatAction extends AbstractAiAgentChatAction {

        TestAiAgentChatAction(
            AiAgentToolFacade aiAgentToolFacade, ClusterElementDefinitionService clusterElementDefinitionService,
            ToolCallingManager toolCallingManager) {

            super(aiAgentToolFacade, clusterElementDefinitionService, toolCallingManager);
        }
    }
}
