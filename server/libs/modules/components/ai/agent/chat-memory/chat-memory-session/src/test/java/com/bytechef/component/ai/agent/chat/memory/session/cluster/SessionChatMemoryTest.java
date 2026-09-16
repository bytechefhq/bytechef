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

package com.bytechef.component.ai.agent.chat.memory.session.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import com.bytechef.platform.component.definition.ai.agent.SessionRepositoryFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.session.compaction.CompactionRequest;
import org.springframework.ai.session.compaction.CompactionTrigger;
import org.springframework.ai.tool.ToolCallback;

/**
 * @author Ivica Cardic
 */
class SessionChatMemoryTest {

    private static final String CONVERSATION_ID = "conversation-1";

    @Test
    void testApplyBuildsInsideLoopSessionMemoryResult() throws Exception {
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);

        SessionRepository sessionRepository = InMemorySessionRepository.builder()
            .build();

        seedConversation(sessionRepository);

        SessionRepositoryFunction sessionRepositoryFunction =
            (inputParameters, connectionParameters, extensions, componentConnections) -> sessionRepository;

        when(clusterElementDefinitionService.<SessionRepositoryFunction>getClusterElement(
            eq("builtInSessionChatMemory"), eq(1), eq("sessionRepository"))).thenReturn(sessionRepositoryFunction);

        Parameters inputParameters = MockParametersFactory.create(Map.of("conversationId", CONVERSATION_ID));
        Parameters extensions = MockParametersFactory.create(
            Map.of(
                "clusterElements",
                Map.of(
                    "sessionRepository",
                    Map.of(
                        "name", "sessionRepository_1",
                        "type", "builtInSessionChatMemory/v1/sessionRepository",
                        "parameters", Map.of()))));

        ComponentConnection componentConnection = new ComponentConnection(
            "builtInSessionChatMemory", 1, 1L, Map.of(), null);

        ChatMemoryFunction chatMemoryFunction = SessionChatMemory.of(clusterElementDefinitionService)
            .getElement();

        ChatMemoryFunction.Result result = chatMemoryFunction.apply(
            inputParameters, MockParametersFactory.create(Map.of()), extensions,
            Map.of("sessionRepository_1", componentConnection));

        SessionMemoryAdvisor sessionMemoryAdvisor = assertInstanceOf(SessionMemoryAdvisor.class, result.advisor());

        // Session memory persists the full tool transcript, so the advisor must sit INSIDE the tool-calling loop
        // (order greater than the tool advisor's) and declare the capability so the agent disables the tool
        // advisor's own in-loop history.
        assertEquals(ChatMemoryFunction.TOOL_MESSAGE_PERSISTENCE_ADVISOR_ORDER, sessionMemoryAdvisor.getOrder());
        assertTrue(result.supportsToolMessagePersistence());
        ChatMemory chatMemory = result.chatMemory();

        assertNotNull(chatMemory, "session memory must expose its history to guardrails");

        List<Message> messages = chatMemory.get(CONVERSATION_ID);

        assertEquals(1, messages.size());
        assertEquals("live turn", messages.getFirst()
            .getText());
    }

    @Test
    void testConversationSearchIsScopedToTheAgentBranch() throws Exception {
        SessionRepository sessionRepository = InMemorySessionRepository.builder()
            .build();

        sessionRepository.save(Session.builder()
            .id(CONVERSATION_ID)
            .userId("user-1")
            .createdAt(Instant.now())
            .build());

        appendEvent(sessionRepository, "alpha root", null);
        appendEvent(sessionRepository, "alpha research", "orch.researcher");
        appendEvent(sessionRepository, "alpha writer", "orch.writer");

        ChatMemoryFunction.Result result = applySessionChatMemory(
            sessionRepository,
            Map.of(
                "conversationId", CONVERSATION_ID, "enableConversationSearch", true, "agentBranch",
                "orch.researcher"));

        ToolCallback conversationSearchToolCallback = result.toolCallbacks()[0];

        String searchResult = conversationSearchToolCallback.call(
            "{\"innerThought\":\"recall\",\"query\":\"alpha\"}",
            new ToolContext(Map.of("chat_memory_conversation_id", CONVERSATION_ID)));

        assertThat(searchResult)
            .contains("alpha root", "alpha research")
            .doesNotContain("alpha writer");
    }

    @Test
    void testSlidingWindowTriggerCountsEventsNotTurns() {
        CompactionTrigger compactionTrigger = SessionChatMemory.resolveCompactionTrigger(
            MockParametersFactory.create(Map.of("compactionStrategy", "SLIDING_WINDOW", "maxEvents", 2)));

        assertThat(compactionTrigger.shouldCompact(oneToolUsingTurn())).isTrue();
    }

    @Test
    void testRecursiveSummarizationTriggerCountsEventsNotTurns() {
        CompactionTrigger compactionTrigger = SessionChatMemory.resolveCompactionTrigger(
            MockParametersFactory
                .create(Map.of("compactionStrategy", "RECURSIVE_SUMMARIZATION", "maxEventsToKeep", 2)));

        assertThat(compactionTrigger.shouldCompact(oneToolUsingTurn())).isTrue();
    }

    @Test
    void testEventCountTriggerIgnoresBranchAndSyntheticEventsLikeTheStrategies() {
        CompactionTrigger compactionTrigger = SessionChatMemory.resolveCompactionTrigger(
            MockParametersFactory.create(Map.of("compactionStrategy", "SLIDING_WINDOW", "maxEvents", 2)));

        Session session = compactionSession();

        CompactionRequest compactionRequest = CompactionRequest.of(
            session,
            List.of(
                compactionEvent(new UserMessage("question"), null, false),
                compactionEvent(new AssistantMessage("answer"), null, false),
                compactionEvent(new AssistantMessage("sub-agent work"), "orch.researcher", false),
                compactionEvent(new AssistantMessage("summary"), null, true)));

        assertThat(compactionTrigger.shouldCompact(compactionRequest)).isFalse();
    }

    private static ChatMemoryFunction.Result applySessionChatMemory(
        SessionRepository sessionRepository, Map<String, Object> inputParameterValues) throws Exception {

        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);

        SessionRepositoryFunction sessionRepositoryFunction =
            (inputParameters, connectionParameters, extensions, componentConnections) -> sessionRepository;

        when(clusterElementDefinitionService.<SessionRepositoryFunction>getClusterElement(
            eq("builtInSessionChatMemory"), eq(1), eq("sessionRepository"))).thenReturn(sessionRepositoryFunction);

        Parameters extensions = MockParametersFactory.create(
            Map.of(
                "clusterElements",
                Map.of(
                    "sessionRepository",
                    Map.of(
                        "name", "sessionRepository_1",
                        "type", "builtInSessionChatMemory/v1/sessionRepository",
                        "parameters", Map.of()))));

        ComponentConnection componentConnection = new ComponentConnection(
            "builtInSessionChatMemory", 1, 1L, Map.of(), null);

        ChatMemoryFunction chatMemoryFunction = SessionChatMemory.of(clusterElementDefinitionService)
            .getElement();

        return chatMemoryFunction.apply(
            MockParametersFactory.create(inputParameterValues), MockParametersFactory.create(Map.of()), extensions,
            Map.of("sessionRepository_1", componentConnection));
    }

    private static void appendEvent(SessionRepository sessionRepository, String text, String branch) {
        sessionRepository.appendEvent(SessionEvent.builder()
            .sessionId(CONVERSATION_ID)
            .message(new UserMessage(text))
            .branch(branch)
            .build());
    }

    private static CompactionRequest oneToolUsingTurn() {
        return CompactionRequest.of(
            compactionSession(),
            List.of(
                compactionEvent(new UserMessage("look it up"), null, false),
                compactionEvent(
                    AssistantMessage.builder()
                        .content("")
                        .toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function", "lookup", "{}")))
                        .build(),
                    null, false),
                compactionEvent(
                    ToolResponseMessage.builder()
                        .responses(List.of(new ToolResponseMessage.ToolResponse("call-1", "lookup", "found")))
                        .build(),
                    null, false)));
    }

    private static Session compactionSession() {
        return Session.builder()
            .id("compaction-session")
            .userId("user-1")
            .createdAt(Instant.now())
            .build();
    }

    private static SessionEvent compactionEvent(Message message, String branch, boolean synthetic) {
        return SessionEvent.builder()
            .sessionId("compaction-session")
            .message(message)
            .branch(branch)
            .metadata(synthetic ? Map.of(SessionEvent.METADATA_SYNTHETIC, true) : Map.of())
            .build();
    }

    private static void seedConversation(SessionRepository sessionRepository) {
        sessionRepository.save(Session.builder()
            .id(CONVERSATION_ID)
            .userId("user-1")
            .createdAt(Instant.now())
            .build());

        sessionRepository.appendEvent(SessionEvent.builder()
            .sessionId(CONVERSATION_ID)
            .message(new UserMessage("archived turn"))
            .archived(true)
            .build());

        sessionRepository.appendEvent(SessionEvent.builder()
            .sessionId(CONVERSATION_ID)
            .message(new UserMessage("live turn"))
            .build());
    }
}
