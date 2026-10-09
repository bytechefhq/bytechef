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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.agent.chat.memory.session.compaction.ContextLoggingSessionService;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextConsumer;
import com.bytechef.component.definition.Context.Log;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import com.bytechef.platform.component.definition.ai.agent.ConversationHistoryReader;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.session.compaction.CompactionRequest;
import org.springframework.ai.session.compaction.CompactionTrigger;
import org.springframework.ai.session.compaction.TokenCountCompactionStrategy;
import org.springframework.ai.session.compaction.TokenCountTrigger;
import org.springframework.ai.session.compaction.TurnCountTrigger;
import org.springframework.ai.session.compaction.TurnWindowCompactionStrategy;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
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

        Parameters inputParameters = MockParametersFactory.create(Map.of("conversationId", CONVERSATION_ID));
        ChatMemoryFunction chatMemoryFunction = SessionChatMemory.of(
            "Test Chat Memory", (
                resolverInputParameters, connectionParameters, extensions,
                componentConnections) -> sessionRepository,
            clusterElementDefinitionService)
            .getElement();

        ChatMemoryFunction.Result result = chatMemoryFunction.apply(
            inputParameters, MockParametersFactory.create(Map.of()), MockParametersFactory.create(Map.of()),
            Map.of());

        SessionMemoryAdvisor sessionMemoryAdvisor = assertInstanceOf(SessionMemoryAdvisor.class, result.advisor());

        assertEquals(ChatMemoryFunction.TOOL_MESSAGE_PERSISTENCE_ADVISOR_ORDER, sessionMemoryAdvisor.getOrder());
        assertTrue(result.supportsToolMessagePersistence());

        ConversationHistoryReader conversationHistoryReader = result.conversationHistoryReader();

        List<Message> messages = conversationHistoryReader.read(CONVERSATION_ID);

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

        List<ToolCallback> toolCallbacks = result.toolCallbacks();

        ToolCallback conversationSearchToolCallback = toolCallbacks.getFirst();

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
    void testRecursiveSummarizationWithoutModelNamesTheComponent() {
        ChatMemoryFunction chatMemoryFunction = SessionChatMemory.of(
            "JDBC Chat Memory",
            (inputParameters, connectionParameters, extensions, componentConnections) -> InMemorySessionRepository
                .builder()
                .build(),
            mock(ClusterElementDefinitionService.class))
            .getElement();

        Parameters inputParameters = MockParametersFactory.create(
            Map.of(
                "conversationId", CONVERSATION_ID, "compactionStrategy", "RECURSIVE_SUMMARIZATION", "maxEvents", 20,
                "maxEventsToKeep", 10));
        Parameters emptyParameters = MockParametersFactory.create(Map.of());

        assertThatThrownBy(
            () -> chatMemoryFunction.apply(inputParameters, emptyParameters, emptyParameters, Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Recursive summarization requires a Model child to be configured on JDBC Chat Memory.");
    }

    @Test
    void testRecursiveSummarizationWithoutClusterElementDefinitionServiceNamesTheComponent() {
        ChatMemoryFunction chatMemoryFunction = SessionChatMemory.of(
            "Redis Chat Memory",
            (inputParameters, connectionParameters, extensions, componentConnections) -> InMemorySessionRepository
                .builder()
                .build(),
            null)
            .getElement();

        Parameters inputParameters = MockParametersFactory.create(
            Map.of(
                "conversationId", CONVERSATION_ID, "compactionStrategy", "RECURSIVE_SUMMARIZATION", "maxEvents", 20,
                "maxEventsToKeep", 10));
        Parameters emptyParameters = MockParametersFactory.create(Map.of());

        assertThatThrownBy(
            () -> chatMemoryFunction.apply(inputParameters, emptyParameters, emptyParameters, Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Recursive summarization requires a Model child to be configured on Redis Chat Memory.");
    }

    @Test
    void testResolverReceivesTheConnectionParameters() throws Exception {
        List<Parameters> receivedConnectionParameters = new ArrayList<>();

        ChatMemoryFunction chatMemoryFunction = SessionChatMemory.of(
            "Test Chat Memory", (inputParameters, connectionParameters, extensions, componentConnections) -> {
                receivedConnectionParameters.add(connectionParameters);

                return InMemorySessionRepository.builder()
                    .build();
            },
            mock(ClusterElementDefinitionService.class))
            .getElement();

        chatMemoryFunction.apply(
            MockParametersFactory.create(Map.of("conversationId", CONVERSATION_ID)),
            MockParametersFactory.create(Map.of("host", "redis.internal")), MockParametersFactory.create(Map.of()),
            Map.of());

        assertThat(receivedConnectionParameters).singleElement()
            .satisfies(connectionParameters -> assertEquals("redis.internal", connectionParameters.getString("host")));
    }

    @Test
    void testRecursiveSummarizationBuildsTheSummarizerFromTheModelChild() throws Exception {
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);

        SessionRepository sessionRepository = InMemorySessionRepository.builder()
            .build();

        ModelFunction modelFunction = mock(ModelFunction.class);

        doReturn(mock(ChatModel.class)).when(modelFunction)
            .apply(any(), any(), anyBoolean());

        when(clusterElementDefinitionService.<ModelFunction>getClusterElement(eq("openAi"), eq(1), eq("model")))
            .thenReturn(modelFunction);

        Parameters extensions = MockParametersFactory.create(
            Map.of(
                "clusterElements",
                Map.of(
                    "model",
                    Map.of(
                        "name", "model_1",
                        "type", "openAi/v1/model",
                        "parameters", Map.of("model", "gpt-4o-mini")))));

        Map<String, ComponentConnection> componentConnections = Map.of(
            "model_1", new ComponentConnection("openAi", 1, 2L, Map.of("token", "test-token"), null));

        ChatMemoryFunction chatMemoryFunction = SessionChatMemory.of(
            "Test Chat Memory", (
                resolverInputParameters, connectionParameters, resolverExtensions,
                resolverComponentConnections) -> sessionRepository,
            clusterElementDefinitionService)
            .getElement();

        ChatMemoryFunction.Result result = chatMemoryFunction.apply(
            MockParametersFactory.create(
                Map.of(
                    "conversationId", CONVERSATION_ID, "compactionStrategy", "RECURSIVE_SUMMARIZATION", "maxEvents",
                    20, "maxEventsToKeep", 10, "overlapSize", 2)),
            MockParametersFactory.create(Map.of()), extensions, componentConnections);

        assertInstanceOf(SessionMemoryAdvisor.class, result.advisor());

        ArgumentCaptor<Parameters> inputParametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
        ArgumentCaptor<Parameters> connectionParametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);

        verify(modelFunction).apply(
            inputParametersArgumentCaptor.capture(), connectionParametersArgumentCaptor.capture(), eq(false));

        Parameters modelInputParameters = inputParametersArgumentCaptor.getValue();
        Parameters modelConnectionParameters = connectionParametersArgumentCaptor.getValue();

        assertEquals("gpt-4o-mini", modelInputParameters.getString("model"));
        assertEquals("test-token", modelConnectionParameters.getString("token"));
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
    void testTurnWindowCompactsOnceTheTurnsExceedMaxTurns() {
        SessionChatMemory.Compaction compaction = resolveCompaction(
            Map.of("compactionStrategy", "TURN_WINDOW", "maxTurns", 3));

        TurnWindowCompactionStrategy turnWindowCompactionStrategy = assertInstanceOf(
            TurnWindowCompactionStrategy.class, compaction.strategy());
        TurnCountTrigger turnCountTrigger = assertInstanceOf(TurnCountTrigger.class, compaction.trigger());

        assertEquals(3, turnWindowCompactionStrategy.getMaxTurns());
        assertEquals(3, turnCountTrigger.getMaxTurns());
        assertThat(turnCountTrigger.shouldCompact(turns(3))).isFalse();
        assertThat(turnCountTrigger.shouldCompact(turns(4))).isTrue();
    }

    @Test
    void testTokenCountCompactsOnceTheTokensReachMaxTokens() {
        CompactionRequest compactionRequest = turns(4);

        int tokenCount = estimateTokens(compactionRequest);

        SessionChatMemory.Compaction compaction = resolveCompaction(
            Map.of("compactionStrategy", "TOKEN_COUNT", "maxTokens", tokenCount));

        TokenCountCompactionStrategy tokenCountCompactionStrategy = assertInstanceOf(
            TokenCountCompactionStrategy.class, compaction.strategy());
        TokenCountTrigger tokenCountTrigger = assertInstanceOf(TokenCountTrigger.class, compaction.trigger());

        assertEquals(tokenCount, tokenCountCompactionStrategy.getMaxTokens());
        assertEquals(tokenCount, tokenCountTrigger.getThreshold());
        assertThat(tokenCountTrigger.shouldCompact(compactionRequest)).isTrue();

        CompactionTrigger aboveTokenCountTrigger = resolveCompactionTrigger(
            Map.of("compactionStrategy", "TOKEN_COUNT", "maxTokens", tokenCount + 1));

        assertThat(aboveTokenCountTrigger.shouldCompact(compactionRequest)).isFalse();
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
    void testSessionServiceIsWrappedWithContextLoggingWhenAContextIsGiven() {
        SessionService sessionService = mock(SessionService.class);

        assertThat(SessionChatMemory.withContextLogging(sessionService, mock(Context.class)))
            .isInstanceOf(ContextLoggingSessionService.class);
        assertThat(SessionChatMemory.withContextLogging(sessionService, null)).isSameAs(sessionService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testAdvisorLogsCompactionFailuresToTheContext() throws Exception {
        IllegalStateException storageFailure = new IllegalStateException("Redis connection refused");

        SessionRepository sessionRepository = mock(
            SessionRepository.class, AdditionalAnswers.delegatesTo(InMemorySessionRepository.builder()
                .build()));

        sessionRepository.save(Session.builder()
            .id(CONVERSATION_ID)
            .userId("user-1")
            .createdAt(Instant.now())
            .build());

        appendEvent(sessionRepository, "question", false, false);

        doThrow(storageFailure).when(sessionRepository)
            .findEvents(eq(CONVERSATION_ID), any(EventFilter.class));

        Context context = mock(Context.class);

        ChatMemoryFunction.Result result = applySessionChatMemory(
            sessionRepository,
            Map.of("conversationId", CONVERSATION_ID, "compactionStrategy", "SLIDING_WINDOW", "maxEvents", 1),
            context);

        SessionMemoryAdvisor sessionMemoryAdvisor = assertInstanceOf(SessionMemoryAdvisor.class, result.advisor());

        ChatClientResponse chatClientResponse = new ChatClientResponse(
            new ChatResponse(List.of(new Generation(new AssistantMessage("answer")))),
            Map.of(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, CONVERSATION_ID));

        sessionMemoryAdvisor.after(chatClientResponse, mock(AdvisorChain.class));

        ArgumentCaptor<ContextConsumer<Log>> logConsumerCaptor = ArgumentCaptor.forClass(ContextConsumer.class);

        verify(context).log(logConsumerCaptor.capture());

        Log log = mock(Log.class);

        ContextConsumer<Log> logConsumer = logConsumerCaptor.getValue();

        logConsumer.accept(log);

        verify(log).warn(contains(CONVERSATION_ID), same(storageFailure));
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
        SessionChatMemory.Compaction compaction = resolveCompaction(inputParameterValues);

        return compaction.trigger();
    }

    private static SessionChatMemory.Compaction resolveCompaction(Map<String, Object> inputParameterValues) {
        try {
            return Objects.requireNonNull(
                SessionChatMemory.resolveCompaction(
                    MockParametersFactory.create(inputParameterValues), () -> mock(ChatClient.class)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static CompactionRequest turns(int turnCount) {
        List<SessionEvent> events = new ArrayList<>();

        for (int index = 0; index < turnCount; index++) {
            events.add(compactionEvent(new UserMessage("question " + index), false));
            events.add(compactionEvent(new AssistantMessage("answer " + index), false));
        }

        return CompactionRequest.of(compactionSession(), events);
    }

    private static int estimateTokens(CompactionRequest compactionRequest) {
        JTokkitTokenCountEstimator tokenCountEstimator = new JTokkitTokenCountEstimator();

        int tokenCount = 0;

        for (SessionEvent sessionEvent : compactionRequest.events()) {
            Message message = sessionEvent.getMessage();

            String role = message.getMessageType() == MessageType.USER ? "User" : "Assistant";

            tokenCount += tokenCountEstimator.estimate(role + ": " + message.getText());
        }

        return tokenCount;
    }

    private static ChatMemoryFunction.Result applySessionChatMemory(
        SessionRepository sessionRepository, Map<String, Object> inputParameterValues) throws Exception {

        return applySessionChatMemory(sessionRepository, inputParameterValues, null);
    }

    private static ChatMemoryFunction.Result applySessionChatMemory(
        SessionRepository sessionRepository, Map<String, Object> inputParameterValues, @Nullable Context context)
        throws Exception {

        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);

        Parameters extensions = MockParametersFactory.create(Map.of());

        ChatMemoryFunction chatMemoryFunction = SessionChatMemory.of(
            "Test Chat Memory", (
                resolverInputParameters, resolverConnectionParameters, resolverExtensions,
                resolverComponentConnections) -> sessionRepository,
            clusterElementDefinitionService)
            .getElement();

        Parameters inputParameters = MockParametersFactory.create(inputParameterValues);
        Parameters connectionParameters = MockParametersFactory.create(Map.of());
        Map<String, ComponentConnection> componentConnections = Map.of();

        if (context == null) {
            return chatMemoryFunction.apply(inputParameters, connectionParameters, extensions, componentConnections);
        }

        return chatMemoryFunction.apply(
            inputParameters, connectionParameters, extensions, componentConnections, context);
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
