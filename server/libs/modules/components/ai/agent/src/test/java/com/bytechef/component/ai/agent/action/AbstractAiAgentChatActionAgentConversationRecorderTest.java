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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.agent.tool.AgentToolCallingManagers;
import com.bytechef.component.ai.llm.facade.AiAgentToolFacade;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.ai.conversation.AgentConversationRecorder;
import com.bytechef.platform.ai.conversation.AgentConversationRecorder.AgentConversation;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.tenant.TenantContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AbstractAiAgentChatActionAgentConversationRecorderTest {

    private static final long WORKSPACE_ID = 10L;
    private static final long AI_AGENT_ID = 20L;
    private static final long CREATOR_USER_ID = 30L;
    private static final String CONVERSATION_ID = "conversation-1";
    private static final Parameters INPUT_PARAMETERS = MockParametersFactory.create(Map.of());

    @Mock
    private AiAgentToolFacade aiAgentToolFacade;

    @Mock
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @Mock
    private ToolCallingManager toolCallingManager;

    @Test
    void testRecorderCalledOnceWithResolvedConversationId() {
        AgentConversationRecorder agentConversationRecorder = mock(AgentConversationRecorder.class);
        AbstractAiAgentChatAction action = createAction(presentProvider(agentConversationRecorder));
        ActionContext context = mock(ActionContext.class);

        action.recordAgentConversationTurn(
            INPUT_PARAMETERS, buildExtensions(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, CONVERSATION_ID), context);

        verify(agentConversationRecorder, times(1)).recordTurn(
            new AgentConversation(
                WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, CONVERSATION_ID, null, null, null, null));
    }

    @Test
    void testExecutionContextAnchorsAreForwarded() {
        AgentConversationRecorder agentConversationRecorder = mock(AgentConversationRecorder.class);
        AbstractAiAgentChatAction action = createAction(presentProvider(agentConversationRecorder));
        ActionContextAware context = mock(ActionContextAware.class);

        when(context.getWorkflowId()).thenReturn("workflow-1");
        when(context.getEnvironmentId()).thenReturn(2L);

        action.recordAgentConversationTurn(
            INPUT_PARAMETERS, buildExtensions(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, CONVERSATION_ID), context);

        verify(agentConversationRecorder, times(1)).recordTurn(
            new AgentConversation(
                WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, CONVERSATION_ID, null, null, "workflow-1", 2L));
    }

    @Test
    void testThrowingRecorderDoesNotFailTurn() {
        AgentConversationRecorder agentConversationRecorder = mock(AgentConversationRecorder.class);

        doThrow(new RuntimeException("recorder unavailable"))
            .when(agentConversationRecorder)
            .recordTurn(any());

        AbstractAiAgentChatAction action = createAction(presentProvider(agentConversationRecorder));
        ActionContext context = mock(ActionContext.class);

        assertDoesNotThrow(
            () -> action.recordAgentConversationTurn(
                INPUT_PARAMETERS, buildExtensions(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, CONVERSATION_ID),
                context));

        verify(agentConversationRecorder, times(1)).recordTurn(any());
        verify(context, times(1)).log(any());
    }

    @Test
    void testNoRecorderBeanMeansNoCall() {
        AbstractAiAgentChatAction action = createAction(emptyProvider());
        ActionContext context = mock(ActionContext.class);

        assertDoesNotThrow(
            () -> action.recordAgentConversationTurn(
                INPUT_PARAMETERS, buildExtensions(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, CONVERSATION_ID),
                context));

        verifyNoInteractions(context);
    }

    @Test
    void testNoRecorderProviderAtAllMeansNoCall() {
        AbstractAiAgentChatAction action = new AbstractAiAgentChatAction(
            aiAgentToolFacade, clusterElementDefinitionService, new AgentToolCallingManagers(toolCallingManager)) {};
        ActionContext context = mock(ActionContext.class);

        assertDoesNotThrow(
            () -> action.recordAgentConversationTurn(
                INPUT_PARAMETERS, buildExtensions(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, CONVERSATION_ID),
                context));

        verifyNoInteractions(context);
    }

    @Test
    void testNullConversationIdMeansNoCall() {
        AgentConversationRecorder agentConversationRecorder = mock(AgentConversationRecorder.class);
        AbstractAiAgentChatAction action = createAction(presentProvider(agentConversationRecorder));
        ActionContext context = mock(ActionContext.class);

        Map<String, Object> extensionsMap = new HashMap<>();

        extensionsMap.put("aiHubWorkspaceId", WORKSPACE_ID);
        extensionsMap.put("aiHubAgentId", AI_AGENT_ID);
        extensionsMap.put("aiHubCreatorUserId", CREATOR_USER_ID);

        action.recordAgentConversationTurn(INPUT_PARAMETERS, MockParametersFactory.create(extensionsMap), context);

        verify(agentConversationRecorder, never()).recordTurn(any());
    }

    @Test
    void testMissingIdentityStampMeansNoCallAndNoWarning() {
        AgentConversationRecorder agentConversationRecorder = mock(AgentConversationRecorder.class);
        AbstractAiAgentChatAction action = createAction(presentProvider(agentConversationRecorder));
        ActionContext context = mock(ActionContext.class);

        Map<String, Object> extensionsMap = new HashMap<>();

        extensionsMap.put("clusterElements", Map.of("chatMemory", buildChatMemoryElement(CONVERSATION_ID)));

        action.recordAgentConversationTurn(INPUT_PARAMETERS, MockParametersFactory.create(extensionsMap), context);

        verify(agentConversationRecorder, never()).recordTurn(any());
        verify(context, never()).log(any());
    }

    @Test
    void testPartialIdentityStampWarnsOnceAndRecordsNothing() {
        AgentConversationRecorder agentConversationRecorder = mock(AgentConversationRecorder.class);
        AbstractAiAgentChatAction action = createAction(presentProvider(agentConversationRecorder));
        ActionContext context = mock(ActionContext.class);

        Map<String, Object> extensionsMap = new HashMap<>();

        extensionsMap.put("clusterElements", Map.of("chatMemory", buildChatMemoryElement(CONVERSATION_ID)));
        extensionsMap.put("aiHubAgentId", AI_AGENT_ID);

        Parameters extensions = MockParametersFactory.create(extensionsMap);

        action.recordAgentConversationTurn(INPUT_PARAMETERS, extensions, context);
        action.recordAgentConversationTurn(INPUT_PARAMETERS, extensions, context);

        verify(agentConversationRecorder, never()).recordTurn(any());
        verify(context, times(1)).log(any());
    }

    @Test
    void testEditorEnvironmentTurnIsNotRecorded() {
        AgentConversationRecorder agentConversationRecorder = mock(AgentConversationRecorder.class);
        AbstractAiAgentChatAction action = createAction(presentProvider(agentConversationRecorder));
        ActionContext context = mock(ActionContext.class);

        when(context.isEditorEnvironment()).thenReturn(true);

        action.recordAgentConversationTurn(
            INPUT_PARAMETERS, buildExtensions(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, CONVERSATION_ID), context);

        verify(agentConversationRecorder, never()).recordTurn(any());
    }

    @Test
    void testBlankConversationIdMeansNoCall() {
        AgentConversationRecorder agentConversationRecorder = mock(AgentConversationRecorder.class);
        AbstractAiAgentChatAction action = createAction(presentProvider(agentConversationRecorder));
        ActionContext context = mock(ActionContext.class);

        action.recordAgentConversationTurn(
            INPUT_PARAMETERS, buildExtensions(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, " "), context);

        verify(agentConversationRecorder, never()).recordTurn(any());
    }

    @Test
    void testChatMemoryWithoutAConversationIdMeansNoCall() {
        AgentConversationRecorder agentConversationRecorder = mock(AgentConversationRecorder.class);
        AbstractAiAgentChatAction action = createAction(presentProvider(agentConversationRecorder));
        ActionContext context = mock(ActionContext.class);

        Map<String, Object> chatMemoryElement = buildChatMemoryElement(CONVERSATION_ID);

        chatMemoryElement.put("parameters", Map.of());

        Map<String, Object> extensionsMap = new HashMap<>();

        extensionsMap.put("clusterElements", Map.of("chatMemory", chatMemoryElement));
        extensionsMap.put("aiHubWorkspaceId", WORKSPACE_ID);
        extensionsMap.put("aiHubAgentId", AI_AGENT_ID);
        extensionsMap.put("aiHubCreatorUserId", CREATOR_USER_ID);

        action.recordAgentConversationTurn(INPUT_PARAMETERS, MockParametersFactory.create(extensionsMap), context);

        verify(agentConversationRecorder, never()).recordTurn(any());
    }

    @Test
    void testSubagentTurnIsNotRecorded() {
        AgentConversationRecorder agentConversationRecorder = mock(AgentConversationRecorder.class);
        AbstractAiAgentChatAction action = createAction(presentProvider(agentConversationRecorder));
        ActionContext context = mock(ActionContext.class);

        action.recordAgentConversationTurn(
            MockParametersFactory.create(Map.of("subagentConversationId", "conversation-1:aiAgent_2")),
            buildExtensions(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, CONVERSATION_ID), context);

        verify(agentConversationRecorder, never()).recordTurn(any());
    }

    @Test
    void testRepeatedRecorderFailuresNeverFailTheTurn() {
        AgentConversationRecorder agentConversationRecorder = mock(AgentConversationRecorder.class);

        doThrow(new RuntimeException("recorder unavailable"))
            .when(agentConversationRecorder)
            .recordTurn(any());

        AbstractAiAgentChatAction action = createAction(presentProvider(agentConversationRecorder));
        ActionContext context = mock(ActionContext.class);
        Parameters extensions = buildExtensions(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, CONVERSATION_ID);

        assertDoesNotThrow(() -> action.recordAgentConversationTurn(INPUT_PARAMETERS, extensions, context));
        assertDoesNotThrow(() -> action.recordAgentConversationTurn(INPUT_PARAMETERS, extensions, context));

        verify(agentConversationRecorder, times(2)).recordTurn(any());
        verify(context, times(2)).log(any());
    }

    @Test
    void testDeferredRecorderRunsUnderTheCapturingThreadTenant() throws InterruptedException {
        AtomicReference<String> observedTenantId = new AtomicReference<>();
        AtomicReference<Thread> observedThread = new AtomicReference<>();
        CountDownLatch recorded = new CountDownLatch(1);

        AgentConversationRecorder agentConversationRecorder = agentConversation -> {
            observedTenantId.set(TenantContext.getCurrentTenantId());
            observedThread.set(Thread.currentThread());

            recorded.countDown();
        };

        AbstractAiAgentChatAction action = createAction(presentProvider(agentConversationRecorder));
        ActionContext context = mock(ActionContext.class);
        String previousTenantId = TenantContext.getCurrentTenantId();

        Runnable turnRecorder;

        try {
            TenantContext.setCurrentTenantId("tenant_732");

            turnRecorder = action.createAgentConversationTurnRecorder(
                INPUT_PARAMETERS, buildExtensions(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, CONVERSATION_ID),
                context);
        } finally {
            TenantContext.setCurrentTenantId(previousTenantId);
        }

        turnRecorder.run();

        assertThat(recorded.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(observedTenantId.get()).isEqualTo("tenant_732");
        assertThat(observedThread.get()).isNotEqualTo(Thread.currentThread());
    }

    private static Parameters buildExtensions(
        long workspaceId, long aiAgentId, long creatorUserId, String conversationId) {

        Map<String, Object> extensionsMap = new HashMap<>();

        extensionsMap.put("clusterElements", Map.of("chatMemory", buildChatMemoryElement(conversationId)));
        extensionsMap.put("aiHubWorkspaceId", workspaceId);
        extensionsMap.put("aiHubAgentId", aiAgentId);
        extensionsMap.put("aiHubCreatorUserId", creatorUserId);

        return MockParametersFactory.create(extensionsMap);
    }

    private static Map<String, Object> buildChatMemoryElement(String conversationId) {
        Map<String, Object> chatMemoryParams = new HashMap<>();

        chatMemoryParams.put("conversationId", conversationId);

        Map<String, Object> chatMemoryElement = new HashMap<>();

        chatMemoryElement.put("name", "chatMemory_1");
        chatMemoryElement.put("type", "chatMemory/v1/chatMemory");
        chatMemoryElement.put("parameters", chatMemoryParams);

        return chatMemoryElement;
    }

    private AbstractAiAgentChatAction createAction(
        ObjectProvider<AgentConversationRecorder> agentConversationRecorderObjectProvider) {

        return new AbstractAiAgentChatAction(
            aiAgentToolFacade, clusterElementDefinitionService, new AgentToolCallingManagers(toolCallingManager),
            List.of(), null, agentConversationRecorderObjectProvider) {};
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> presentProvider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(value);

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> emptyProvider() {
        ObjectProvider<T> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(null);

        return provider;
    }
}
