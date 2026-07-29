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

package org.springframework.ai.session.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import redis.clients.jedis.UnifiedJedis;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Unit tests for {@link RedisSessionRepository} against a map-backed {@link UnifiedJedis} fake whose {@code eval}
 * answer reproduces the CAS script's observable semantics (absent-key create when the expected version is {@code -1},
 * version-field comparison otherwise). The real Lua script is exercised by {@code RedisSessionRepositoryIntTest}
 * against a Redis container.
 *
 * @author Ivica Cardic
 */
class RedisSessionRepositoryTest {

    private static final String KEY_PREFIX = "test-session:";

    private final Map<String, String> store = new HashMap<>();
    private final JsonMapper jsonMapper = JsonMapper.builder()
        .build();

    private RedisSessionRepository repository;

    @BeforeEach
    void setUp() {
        store.clear();

        UnifiedJedis jedis = mock(UnifiedJedis.class);

        when(jedis.get(anyString())).thenAnswer(invocation -> store.get(invocation.getArgument(0, String.class)));
        when(jedis.set(anyString(), anyString())).thenAnswer(invocation -> {
            store.put(invocation.getArgument(0, String.class), invocation.getArgument(1, String.class));

            return "OK";
        });
        when(jedis.del(anyString())).thenAnswer(
            invocation -> store.remove(invocation.getArgument(0, String.class)) != null ? 1L : 0L);
        when(jedis.keys(anyString())).thenAnswer(invocation -> {
            String pattern = invocation.getArgument(0, String.class);
            String prefix = pattern.endsWith("*") ? pattern.substring(0, pattern.length() - 1) : pattern;

            return store.keySet()
                .stream()
                .filter(key -> key.startsWith(prefix))
                .collect(java.util.stream.Collectors.toSet());
        });
        when(jedis.eval(anyString(), anyList(), anyList())).thenAnswer(invocation -> {
            List<String> keys = invocation.getArgument(1);
            List<String> args = invocation.getArgument(2);

            return casEval(keys.get(0), args.get(0), args.get(1));
        });

        repository = RedisSessionRepository.builder()
            .jedis(jedis)
            .keyPrefix(KEY_PREFIX)
            .build();
    }

    private Long casEval(String key, String newDocument, String expectedVersion) {
        String current = store.get(key);

        if (current == null) {
            if (!"-1".equals(expectedVersion)) {
                return 0L;
            }

            store.put(key, newDocument);

            return 1L;
        }

        JsonNode currentNode = jsonMapper.readTree(current);

        JsonNode versionNode = currentNode.get("version");

        if (versionNode == null || versionNode.asLong() != Long.parseLong(expectedVersion)) {
            return 0L;
        }

        store.put(key, newDocument);

        return 1L;
    }

    private Session newSession(String id) {
        return repository.save(Session.builder()
            .id(id)
            .userId("user-1")
            .createdAt(Instant.now())
            .build());
    }

    private SessionEvent event(String sessionId, String text) {
        return SessionEvent.builder()
            .sessionId(sessionId)
            .message(new UserMessage(text))
            .build();
    }

    @Test
    void testSaveAndFindById() {
        newSession("s-find");

        assertTrue(repository.findById("s-find")
            .isPresent());
        assertEquals("user-1", repository.findById("s-find")
            .get()
            .userId());
    }

    @Test
    void testSaveExistingSessionPreservesVersionAndEvents() {
        newSession("s-resave");

        repository.appendEvent(event("s-resave", "kept"));

        repository.save(Session.builder()
            .id("s-resave")
            .userId("user-2")
            .createdAt(Instant.now())
            .build());

        assertEquals(1L, repository.getEventVersion("s-resave"));
        assertEquals("user-2", repository.findById("s-resave")
            .get()
            .userId());
        assertEquals(1, repository.findEvents("s-resave", EventFilter.all())
            .size());
    }

    @Test
    void testAppendEventIncrementsVersionAndIsReadable() {
        newSession("s-append");

        assertEquals(0L, repository.getEventVersion("s-append"));

        repository.appendEvent(event("s-append", "hello"));

        assertEquals(1L, repository.getEventVersion("s-append"));

        List<SessionEvent> events = repository.findEvents("s-append", EventFilter.all());

        assertEquals(1, events.size());
        assertEquals("hello", events.get(0)
            .getMessage()
            .getText());
    }

    @Test
    void testAppendEventOnMissingSessionThrows() {
        assertThrows(IllegalArgumentException.class, () -> repository.appendEvent(event("nope", "x")));
    }

    @Test
    void testToolMessagesRoundTrip() {
        newSession("s-tools");

        AssistantMessage assistantMessage = AssistantMessage.builder()
            .content("calling a tool")
            .toolCalls(List.of(new ToolCall("call-1", "function", "myTool", "{\"city\":\"Zagreb\"}")))
            .build();

        repository.appendEvent(SessionEvent.builder()
            .sessionId("s-tools")
            .message(assistantMessage)
            .build());
        repository.appendEvent(SessionEvent.builder()
            .sessionId("s-tools")
            .message(ToolResponseMessage.builder()
                .responses(List.of(new ToolResponse("call-1", "myTool", "{\"temp\":21}")))
                .build())
            .build());

        List<SessionEvent> events = repository.findEvents("s-tools", EventFilter.all());

        assertEquals(2, events.size());

        AssistantMessage readAssistantMessage = assertInstanceOf(
            AssistantMessage.class, events.get(0)
                .getMessage());

        assertTrue(readAssistantMessage.hasToolCalls());
        assertEquals("myTool", readAssistantMessage.getToolCalls()
            .get(0)
            .name());

        ToolResponseMessage readToolResponseMessage = assertInstanceOf(
            ToolResponseMessage.class, events.get(1)
                .getMessage());

        assertEquals("{\"temp\":21}", readToolResponseMessage.getResponses()
            .get(0)
            .responseData());
    }

    @Test
    void testFindEventsReturnsEmptyForUnknownSession() {
        assertTrue(repository.findEvents("unknown", EventFilter.all())
            .isEmpty());
    }

    @Test
    void testFindEventsLastN() {
        newSession("s-lastn");

        repository.appendEvent(event("s-lastn", "one"));
        repository.appendEvent(event("s-lastn", "two"));
        repository.appendEvent(event("s-lastn", "three"));

        List<SessionEvent> events = repository.findEvents("s-lastn", EventFilter.lastN(2));

        assertEquals(2, events.size());
        assertEquals("two", events.get(0)
            .getMessage()
            .getText());
        assertEquals("three", events.get(1)
            .getMessage()
            .getText());
    }

    @Test
    void testReplaceEventsCasSucceedsThenFailsOnStaleVersion() {
        newSession("s-cas");

        repository.appendEvent(event("s-cas", "v1"));

        long version = repository.getEventVersion("s-cas");

        assertTrue(repository.replaceEvents("s-cas", List.of(event("s-cas", "compacted")), version));
        assertFalse(repository.replaceEvents("s-cas", List.of(event("s-cas", "again")), version));
    }

    @Test
    void testReplaceEventsWithoutVersionOverwritesUnconditionally() {
        newSession("s-replace");

        repository.appendEvent(event("s-replace", "old"));

        repository.replaceEvents("s-replace", List.of(event("s-replace", "new")));

        List<SessionEvent> events = repository.findEvents("s-replace", EventFilter.all());

        assertEquals(1, events.size());
        assertEquals("new", events.get(0)
            .getMessage()
            .getText());
    }

    @Test
    void testFindByUserIdAndDelete() {
        newSession("s-del");

        assertFalse(repository.findByUserId("user-1")
            .isEmpty());

        repository.delete("s-del");

        assertTrue(repository.findById("s-del")
            .isEmpty());
    }

    @Test
    void testFindExpiredSessionIds() {
        repository.save(Session.builder()
            .id("s-expired")
            .userId("user-1")
            .createdAt(Instant.now()
                .minusSeconds(120))
            .expiresAt(Instant.now()
                .minusSeconds(60))
            .build());

        assertTrue(repository.findExpiredSessionIds(Instant.now())
            .contains("s-expired"));
    }
}
