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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
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
    void testConversationSearchFindsArchivedEventsAndSkipsSummaries() throws Exception {
        SessionRepository sessionRepository = InMemorySessionRepository.builder()
            .build();

        sessionRepository.save(Session.builder()
            .id(CONVERSATION_ID)
            .userId("user-1")
            .createdAt(Instant.now())
            .build());

        appendEvent(sessionRepository, "alpha archived", true, false);
        appendEvent(sessionRepository, "alpha summary", false, true);
        appendEvent(sessionRepository, "alpha live", false, false);

        ChatMemoryFunction.Result result = applySessionChatMemory(
            sessionRepository, Map.of("conversationId", CONVERSATION_ID, "enableConversationSearch", true));

        ToolCallback conversationSearchToolCallback = result.toolCallbacks()[0];

        String searchResult = conversationSearchToolCallback.call(
            "{\"innerThought\":\"recall\",\"query\":\"alpha\"}",
            new ToolContext(Map.of("chat_memory_conversation_id", CONVERSATION_ID)));

        assertThat(searchResult)
            .contains("alpha archived", "alpha live")
            .doesNotContain("alpha summary");
    }

    @Test
    void testConversationSearchRejectsNonPositiveSearchPageSize() {
        SessionRepository sessionRepository = InMemorySessionRepository.builder()
            .build();

        assertThatThrownBy(() -> applySessionChatMemory(
            sessionRepository,
            Map.of("conversationId", CONVERSATION_ID, "enableConversationSearch", true, "searchPageSize", 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Search page size must be");
    }

    @Test
    void testRecursiveSummarizationWithoutModelChildIsRejected() {
        SessionRepository sessionRepository = InMemorySessionRepository.builder()
            .build();

        assertThatThrownBy(() -> applySessionChatMemory(
            sessionRepository,
            Map.of(
                "conversationId", CONVERSATION_ID, "compactionStrategy", "RECURSIVE_SUMMARIZATION", "maxEvents", 20,
                "maxEventsToKeep", 10)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("requires a Model child");
    }

    @Test
    void testSlidingWindowTriggerCountsEventsNotTurns() {
        CompactionTrigger compactionTrigger = resolveCompactionTrigger(
            Map.of("compactionStrategy", "SLIDING_WINDOW", "maxEvents", 2));

        assertThat(compactionTrigger.shouldCompact(oneToolUsingTurn())).isTrue();
    }

    @Test
    void testRecursiveSummarizationTriggerCountsEventsNotTurns() {
        CompactionTrigger compactionTrigger = resolveCompactionTrigger(
            Map.of(
                "compactionStrategy", "RECURSIVE_SUMMARIZATION", "maxEvents", 2, "maxEventsToKeep", 1, "overlapSize",
                0));

        assertThat(compactionTrigger.shouldCompact(oneToolUsingTurn())).isTrue();
    }

    @Test
    void testEventCountTriggerIgnoresSyntheticEventsLikeTheStrategies() {
        CompactionTrigger compactionTrigger = resolveCompactionTrigger(
            Map.of("compactionStrategy", "SLIDING_WINDOW", "maxEvents", 2));

        Session session = compactionSession();

        CompactionRequest compactionRequest = CompactionRequest.of(
            session,
            List.of(
                compactionEvent(new UserMessage("question"), false),
                compactionEvent(new AssistantMessage("answer"), false),
                compactionEvent(new AssistantMessage("summary"), true)));

        assertThat(compactionTrigger.shouldCompact(compactionRequest)).isFalse();
    }

    @Test
    void testRecursiveSummarizationDoesNotRunAgainOnTheTurnAfterACompaction() {
        CompactionTrigger compactionTrigger = resolveCompactionTrigger(
            Map.of("compactionStrategy", "RECURSIVE_SUMMARIZATION", "maxEvents", 20, "maxEventsToKeep", 10));

        assertThat(compactionTrigger.shouldCompact(eventsAfterCompaction(10, 2))).isFalse();
        assertThat(compactionTrigger.shouldCompact(eventsAfterCompaction(10, 12))).isTrue();
    }

    @Test
    void testRecursiveSummarizationRejectsMaxEventsNotAboveMaxEventsToKeep() {
        Parameters inputParameters = MockParametersFactory.create(
            Map.of("compactionStrategy", "RECURSIVE_SUMMARIZATION", "maxEvents", 10, "maxEventsToKeep", 10));

        assertThatThrownBy(() -> SessionChatMemory.resolveCompaction(inputParameters, () -> mock(ChatClient.class)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testSlidingWindowRejectsNonPositiveMaxEvents() {
        assertCompactionRejected(Map.of("compactionStrategy", "SLIDING_WINDOW", "maxEvents", 0), "Max events");
    }

    @Test
    void testTurnWindowRejectsNonPositiveMaxTurns() {
        assertCompactionRejected(Map.of("compactionStrategy", "TURN_WINDOW", "maxTurns", 0), "Max turns");
    }

    @Test
    void testTokenCountRejectsNonPositiveMaxTokens() {
        assertCompactionRejected(Map.of("compactionStrategy", "TOKEN_COUNT", "maxTokens", -1), "Max tokens");
    }

    @Test
    void testRecursiveSummarizationRejectsNonPositiveMaxEventsToKeep() {
        assertCompactionRejected(
            Map.of(
                "compactionStrategy", "RECURSIVE_SUMMARIZATION", "maxEvents", 20, "maxEventsToKeep", 0, "overlapSize",
                0),
            "Max events to keep");
    }

    @Test
    void testRecursiveSummarizationRejectsOverlapSizeNotBelowMaxEventsToKeep() {
        assertCompactionRejected(
            Map.of(
                "compactionStrategy", "RECURSIVE_SUMMARIZATION", "maxEvents", 20, "maxEventsToKeep", 5, "overlapSize",
                5),
            "Overlap size");
    }

    @Test
    void testRecursiveSummarizationRejectsNegativeOverlapSize() {
        assertCompactionRejected(
            Map.of(
                "compactionStrategy", "RECURSIVE_SUMMARIZATION", "maxEvents", 20, "maxEventsToKeep", 5, "overlapSize",
                -1),
            "Overlap size");
    }

    @Test
    void testUnknownCompactionStrategyIsRejected() {
        Parameters inputParameters = MockParametersFactory.create(Map.of("compactionStrategy", "SLIDING"));

        assertThatThrownBy(() -> SessionChatMemory.resolveCompaction(inputParameters, () -> mock(ChatClient.class)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testNoneHasNoCompaction() throws Exception {
        Parameters inputParameters = MockParametersFactory.create(Map.of("compactionStrategy", "NONE"));

        assertThat(SessionChatMemory.resolveCompaction(inputParameters, () -> mock(ChatClient.class))).isNull();
    }

    private static void assertCompactionRejected(Map<String, Object> inputParameterValues, String expectedLabel) {
        Parameters inputParameters = MockParametersFactory.create(inputParameterValues);

        assertThatThrownBy(() -> SessionChatMemory.resolveCompaction(inputParameters, () -> mock(ChatClient.class)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageStartingWith(expectedLabel + " must be");
    }

    private static CompactionRequest eventsAfterCompaction(int keptEventCount, int newEventCount) {
        List<SessionEvent> events = new ArrayList<>();

        events.add(compactionEvent(new AssistantMessage("summary"), true));

        for (int index = 0; index < keptEventCount + newEventCount; index++) {
            Message message = index % 2 == 0
                ? new UserMessage("question " + index) : new AssistantMessage("answer " + index);

            events.add(compactionEvent(message, false));
        }

        return CompactionRequest.of(compactionSession(), events);
    }

    private static CompactionTrigger resolveCompactionTrigger(Map<String, Object> inputParameterValues) {
        try {
            SessionChatMemory.Compaction compaction = SessionChatMemory.resolveCompaction(
                MockParametersFactory.create(inputParameterValues), () -> mock(ChatClient.class));

            return Objects.requireNonNull(compaction)
                .trigger();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
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

    private static void appendEvent(
        SessionRepository sessionRepository, String text, boolean archived, boolean synthetic) {

        sessionRepository.appendEvent(SessionEvent.builder()
            .sessionId(CONVERSATION_ID)
            .message(new UserMessage(text))
            .archived(archived)
            .metadata(synthetic ? Map.of(SessionEvent.METADATA_SYNTHETIC, true) : Map.of())
            .build());
    }

    private static CompactionRequest oneToolUsingTurn() {
        return CompactionRequest.of(
            compactionSession(),
            List.of(
                compactionEvent(new UserMessage("look it up"), false),
                compactionEvent(
                    AssistantMessage.builder()
                        .content("")
                        .toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function", "lookup", "{}")))
                        .build(),
                    false),
                compactionEvent(
                    ToolResponseMessage.builder()
                        .responses(List.of(new ToolResponseMessage.ToolResponse("call-1", "lookup", "found")))
                        .build(),
                    false)));
    }

    private static Session compactionSession() {
        return Session.builder()
            .id("compaction-session")
            .userId("user-1")
            .createdAt(Instant.now())
            .build();
    }

    private static SessionEvent compactionEvent(Message message, boolean synthetic) {
        return SessionEvent.builder()
            .sessionId("compaction-session")
            .message(message)
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
