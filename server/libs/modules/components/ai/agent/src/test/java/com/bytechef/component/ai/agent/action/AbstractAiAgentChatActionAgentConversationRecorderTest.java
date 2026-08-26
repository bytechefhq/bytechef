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
import com.bytechef.platform.ai.guardrails.AiGuardrailsAdvisorProvider;
import com.bytechef.platform.ai.workspaceprompt.WorkspaceSystemPromptAdvisorProvider;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.tool.execution.ToolExecutionRecorder;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Coverage for {@link AbstractAiAgentChatAction#recordAgentConversationTurn} — the AI Hub visibility identity-stamp
 * read side (ticket 732, {@code 2026-08-17-agent-run-hub-visibility}). The write side is
 * {@code AiAgentWorkflowGeneratorTest}.
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AbstractAiAgentChatActionAgentConversationRecorderTest {

    private static final long WORKSPACE_ID = 10L;
    private static final long AI_AGENT_ID = 20L;
    private static final long CREATOR_USER_ID = 30L;
    private static final String CONVERSATION_ID = "conversation-1";

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
            buildExtensions(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, CONVERSATION_ID), context);

        verify(agentConversationRecorder, times(1)).recordTurn(
            new AgentConversation(
                WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, CONVERSATION_ID, null, null, null, null));
    }

    /**
     * The workflow id and environment ordinal come from the platform's execution context rather than from the node
     * definition — that is what lets the EE recorder verify the (definition-supplied, therefore forgeable) workspace
     * stamp. A plain {@code ActionContext} carries neither, so this pins that an {@code ActionContextAware} one does.
     */
    @Test
    void testExecutionContextAnchorsAreForwarded() {
        AgentConversationRecorder agentConversationRecorder = mock(AgentConversationRecorder.class);
        AbstractAiAgentChatAction action = createAction(presentProvider(agentConversationRecorder));
        ActionContextAware context = mock(ActionContextAware.class);

        when(context.getWorkflowId()).thenReturn("workflow-1");
        when(context.getEnvironmentId()).thenReturn(2L);

        action.recordAgentConversationTurn(
            buildExtensions(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, CONVERSATION_ID), context);

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
                buildExtensions(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, CONVERSATION_ID), context));

        verify(agentConversationRecorder, times(1)).recordTurn(any());
        verify(context, times(1)).log(any());
    }

    @Test
    void testNoRecorderBeanMeansNoCall() {
        AbstractAiAgentChatAction action = createAction(emptyProvider());
        ActionContext context = mock(ActionContext.class);

        assertDoesNotThrow(
            () -> action.recordAgentConversationTurn(
                buildExtensions(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, CONVERSATION_ID), context));

        verifyNoInteractions(context);
    }

    @Test
    void testNoRecorderProviderAtAllMeansNoCall() {
        AbstractAiAgentChatAction action = new AbstractAiAgentChatAction(
            aiAgentToolFacade, clusterElementDefinitionService, new AgentToolCallingManagers(toolCallingManager)) {};
        ActionContext context = mock(ActionContext.class);

        assertDoesNotThrow(
            () -> action.recordAgentConversationTurn(
                buildExtensions(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, CONVERSATION_ID), context));

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
        // No "clusterElements" entry at all — no CHAT_MEMORY cluster element, so conversationId resolves to null
        // (chat memory is off for this agent).

        action.recordAgentConversationTurn(MockParametersFactory.create(extensionsMap), context);

        verify(agentConversationRecorder, never()).recordTurn(any());
    }

    @Test
    void testMissingIdentityStampMeansNoCallAndNoWarning() {
        AgentConversationRecorder agentConversationRecorder = mock(AgentConversationRecorder.class);
        AbstractAiAgentChatAction action = createAction(presentProvider(agentConversationRecorder));
        ActionContext context = mock(ActionContext.class);

        // A hand-built canvas aiAgent node (out of the AI Agent entity's scope): CHAT_MEMORY is configured, so
        // conversationId resolves, but AiAgentWorkflowGenerator never ran, so none of the three identity-stamp keys
        // are present. That is the expected steady state for such a node, so it must stay silent.
        Map<String, Object> extensionsMap = new HashMap<>();

        extensionsMap.put("clusterElements", Map.of("chatMemory", buildChatMemoryElement(CONVERSATION_ID)));

        action.recordAgentConversationTurn(MockParametersFactory.create(extensionsMap), context);

        verify(agentConversationRecorder, never()).recordTurn(any());
        verify(context, never()).log(any());
    }

    /**
     * The counterpart to {@link #testMissingIdentityStampMeansNoCallAndNoWarning}: a stamp that is present but
     * incomplete means the generator ran and could not resolve a field, which is a misconfiguration ops has to be able
     * to see — but only once, so a misconfigured agent cannot flood the log on every turn.
     */
    @Test
    void testPartialIdentityStampWarnsOnceAndRecordsNothing() {
        AgentConversationRecorder agentConversationRecorder = mock(AgentConversationRecorder.class);
        AbstractAiAgentChatAction action = createAction(presentProvider(agentConversationRecorder));
        ActionContext context = mock(ActionContext.class);

        Map<String, Object> extensionsMap = new HashMap<>();

        extensionsMap.put("clusterElements", Map.of("chatMemory", buildChatMemoryElement(CONVERSATION_ID)));
        extensionsMap.put("aiHubAgentId", AI_AGENT_ID);

        Parameters extensions = MockParametersFactory.create(extensionsMap);

        action.recordAgentConversationTurn(extensions, context);
        action.recordAgentConversationTurn(extensions, context);

        verify(agentConversationRecorder, never()).recordTurn(any());
        verify(context, times(1)).log(any());
    }

    /**
     * An Agent Studio test-chat turn runs in the editor environment against a throwaway job; it is not a conversation
     * to archive in the Hub.
     */
    @Test
    void testEditorEnvironmentTurnIsNotRecorded() {
        AgentConversationRecorder agentConversationRecorder = mock(AgentConversationRecorder.class);
        AbstractAiAgentChatAction action = createAction(presentProvider(agentConversationRecorder));
        ActionContext context = mock(ActionContext.class);

        when(context.isEditorEnvironment()).thenReturn(true);

        action.recordAgentConversationTurn(
            buildExtensions(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, CONVERSATION_ID), context);

        verify(agentConversationRecorder, never()).recordTurn(any());
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
            (ObjectProvider<ToolExecutionRecorder>) null, (ObjectProvider<AiGuardrailsAdvisorProvider>) null,
            (ObjectProvider<WorkspaceSystemPromptAdvisorProvider>) null,
            agentConversationRecorderObjectProvider) {};
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
