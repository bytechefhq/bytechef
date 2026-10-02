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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.compaction.CompactionPlan;
import org.springframework.ai.session.store.ConditionalWriteRetry;
import org.springframework.ai.session.store.UnreadableSessionDocumentException;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

/**
 * @author Ivica Cardic
 */
class RedisSessionRepositoryTest {

    private static final String KEY_PREFIX = "test-session:";

    private final Map<String, Object> store = new HashMap<>();
    private final List<Long> recordedDelays = new ArrayList<>();
    private Runnable afterNextGet;
    private Object scriptResult;
    private int casEvalCount;
    private int remainingCasConflicts;

    private UnifiedJedis jedis;
    private RedisSessionRepository repository;

    @BeforeEach
    void setUp() {
        store.clear();
        recordedDelays.clear();

        casEvalCount = 0;
        remainingCasConflicts = 0;
        scriptResult = null;

        jedis = mock(UnifiedJedis.class);

        when(jedis.get(anyString())).thenAnswer(invocation -> {
            Object value = store.get(invocation.getArgument(0, String.class));

            runAfterNextGet();

            if (value instanceof Map<?, ?> || value instanceof List<?>) {
                throw wrongType();
            }

            return value;
        });
        when(jedis.hmget(anyString(), any(String[].class))).thenAnswer(invocation -> {
            Object[] arguments = invocation.getArguments();

            Object value = store.get((String) arguments[0]);

            runAfterNextGet();

            if (value instanceof String || value instanceof List<?>) {
                throw wrongType();
            }

            List<String> fieldValues = new ArrayList<>();

            for (int argumentIndex = 1; argumentIndex < arguments.length; argumentIndex++) {
                Object field = arguments[argumentIndex];

                fieldValues.add(value == null ? null : (String) ((Map<?, ?>) value).get(field));
            }

            return fieldValues;
        });
        when(jedis.del(anyString())).thenAnswer(
            invocation -> store.remove(invocation.getArgument(0, String.class)) != null ? 1L : 0L);
        when(jedis.scan(anyString(), any(ScanParams.class))).thenAnswer(invocation -> {
            ScanParams scanParams = invocation.getArgument(1);

            Pattern matchPattern = globPattern(scanParams.match());

            List<String> matchingKeys = store.keySet()
                .stream()
                .filter(key -> matchPattern.matcher(key)
                    .matches())
                .sorted()
                .toList();

            int middle = matchingKeys.size() / 2;

            if (ScanParams.SCAN_POINTER_START.equals(invocation.getArgument(0, String.class))) {
                return new ScanResult<>("1", List.copyOf(matchingKeys.subList(0, middle)));
            }

            return new ScanResult<>(
                ScanParams.SCAN_POINTER_START, List.copyOf(matchingKeys.subList(middle, matchingKeys.size())));
        });
        when(jedis.eval(anyString(), anyList(), anyList())).thenAnswer(invocation -> {
            String script = invocation.getArgument(0, String.class);
            List<String> keys = invocation.getArgument(1);
            List<String> args = invocation.getArgument(2);

            if (RedisSessionRepository.DELETE_IF_EXPIRED_SCRIPT.equals(script)) {
                return deleteIfExpiredEval(keys.get(0), Long.parseLong(args.get(0)), args.get(1));
            }

            if (RedisSessionRepository.CAS_SCRIPT.equals(script)) {
                return scriptResult != null ? scriptResult : casEval(keys.get(0), args);
            }

            throw new IllegalStateException("Unexpected script: " + script);
        });

        repository = RedisSessionRepository.builder()
            .jedis(jedis)
            .keyPrefix(KEY_PREFIX)
            .conditionalWriteRetry(new ConditionalWriteRetry(
                ConditionalWriteRetry.DEFAULT_MAX_ATTEMPTS, ConditionalWriteRetry.DEFAULT_BASE_DELAY_MILLIS,
                ConditionalWriteRetry.DEFAULT_MAX_DELAY_MILLIS, recordedDelays::add))
            .build();
    }

    private void runAfterNextGet() {
        Runnable runnable = afterNextGet;

        if (runnable != null) {
            afterNextGet = null;

            runnable.run();
        }
    }

    private static Pattern globPattern(String glob) {
        StringBuilder regex = new StringBuilder();

        for (char character : glob.toCharArray()) {
            if (character == '*') {
                regex.append(".*");
            } else if (character == '?') {
                regex.append('.');
            } else {
                regex.append(Pattern.quote(String.valueOf(character)));
            }
        }

        return Pattern.compile(regex.toString());
    }

    private static JedisDataException wrongType() {
        return new JedisDataException("WRONGTYPE Operation against a key holding the wrong kind of value");
    }

    private Object casEval(String key, List<String> args) {
        casEvalCount++;

        if (remainingCasConflicts > 0) {
            remainingCasConflicts--;

            return 0L;
        }

        String expectedRevision = args.get(1);
        String legacyDocument = args.get(4);

        Object current = store.get(key);

        if (current instanceof List<?>) {
            return "list";
        }

        if (current == null) {
            if (!"-1".equals(expectedRevision)) {
                return 0L;
            }
        } else if (current instanceof Map<?, ?> fields) {
            Object revision = fields.get(RedisSessionRepository.REVISION_FIELD);

            if (revision == null) {
                return -1L;
            }

            if (!expectedRevision.equals(revision)) {
                return 0L;
            }
        } else if (legacyDocument.isEmpty() || !legacyDocument.equals(current)) {
            return 0L;
        }

        store.put(
            key, new HashMap<>(Map.of(
                RedisSessionRepository.REVISION_FIELD, args.get(2), RedisSessionRepository.EXPIRES_AT_FIELD,
                args.get(3), RedisSessionRepository.DOCUMENT_FIELD, args.get(0))));

        return 1L;
    }

    private Long deleteIfExpiredEval(String key, long cutoffEpochMilli, String legacyDocument) {
        Object current = store.get(key);

        if (current instanceof Map<?, ?> fields) {
            String expiresAt = (String) fields.get(RedisSessionRepository.EXPIRES_AT_FIELD);

            if (expiresAt == null || expiresAt.isEmpty() || Long.parseLong(expiresAt) >= cutoffEpochMilli) {
                return 0L;
            }
        } else if (current == null || legacyDocument.isEmpty() || !legacyDocument.equals(current)) {
            return 0L;
        }

        store.remove(key);

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

        Session foundSession = repository.findById("s-find");

        assertNotNull(foundSession);
        assertEquals("user-1", Objects.requireNonNull(foundSession)
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

        Session resavedSession = repository.findById("s-resave");

        assertNotNull(resavedSession);
        assertEquals("user-2", Objects.requireNonNull(resavedSession)
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
    void testAppendEventKeepsMetadataSavedConcurrently() {
        newSession("s-race");

        afterNextGet = () -> repository.save(Session.builder()
            .id("s-race")
            .userId("user-1")
            .createdAt(Instant.now())
            .metadata(Map.of("title", "saved concurrently"))
            .build());

        repository.appendEvent(event("s-race", "appended"));

        Session session = Objects.requireNonNull(repository.findById("s-race"));

        assertEquals("saved concurrently", session.metadata()
            .get("title"));
        assertEquals(1, repository.findEvents("s-race", EventFilter.all())
            .size());
    }

    @Test
    void testSaveKeepsEventAppendedConcurrently() {
        newSession("s-race-save");

        afterNextGet = () -> repository.appendEvent(event("s-race-save", "appended"));

        repository.save(Session.builder()
            .id("s-race-save")
            .userId("user-1")
            .createdAt(Instant.now())
            .metadata(Map.of("title", "saved"))
            .build());

        Session session = Objects.requireNonNull(repository.findById("s-race-save"));

        assertEquals("saved", session.metadata()
            .get("title"));
        assertEquals(1, repository.findEvents("s-race-save", EventFilter.all())
            .size());
    }

    @Test
    void testAppendEventGivesUpAfterEveryAttemptConflicts() {
        newSession("s-exhausted");

        casEvalCount = 0;
        remainingCasConflicts = Integer.MAX_VALUE;

        IllegalStateException exception = assertThrows(
            IllegalStateException.class, () -> repository.appendEvent(event("s-exhausted", "lost")));

        assertEquals(ConditionalWriteRetry.DEFAULT_MAX_ATTEMPTS, casEvalCount);
        assertEquals(ConditionalWriteRetry.DEFAULT_MAX_ATTEMPTS - 1, recordedDelays.size());
        assertTrue(exception.getMessage()
            .contains("s-exhausted"));
        assertTrue(exception.getMessage()
            .contains("concurrent writers"));
    }

    @Test
    void testSaveGivesUpAfterEveryAttemptConflicts() {
        newSession("s-save-exhausted");

        casEvalCount = 0;
        remainingCasConflicts = Integer.MAX_VALUE;

        assertThrows(IllegalStateException.class, () -> newSession("s-save-exhausted"));
        assertEquals(ConditionalWriteRetry.DEFAULT_MAX_ATTEMPTS, casEvalCount);
    }

    @Test
    void testAppendEventBacksOffBeforeRetryingAConflict() {
        newSession("s-backoff");

        remainingCasConflicts = 2;

        repository.appendEvent(event("s-backoff", "eventually"));

        assertEquals(2, recordedDelays.size());
        assertEquals(1, repository.findEvents("s-backoff", EventFilter.all())
            .size());
    }

    @Test
    void testSaveIfAbsentOnKeyOfAnotherTypeFailsNamingKeyAndType() {
        store.put(KEY_PREFIX + "s-list", List.of("item"));

        IllegalStateException exception = assertThrows(
            IllegalStateException.class, () -> repository.saveIfAbsent(Session.builder()
                .id("s-list")
                .userId("user-1")
                .createdAt(Instant.now())
                .build()));

        assertTrue(exception.getMessage()
            .contains(KEY_PREFIX + "s-list"), exception.getMessage());
        assertTrue(exception.getMessage()
            .contains("list"), exception.getMessage());
        assertEquals(1, casEvalCount);
    }

    @Test
    void testAppendEventFailsWithoutRetryingWhenKeyTurnsIntoAnotherType() {
        newSession("s-retyped");

        casEvalCount = 0;

        afterNextGet = () -> store.put(KEY_PREFIX + "s-retyped", List.of("item"));

        IllegalStateException exception = assertThrows(
            IllegalStateException.class, () -> repository.appendEvent(event("s-retyped", "lost")));

        assertTrue(exception.getMessage()
            .contains("list"), exception.getMessage());
        assertFalse(exception.getMessage()
            .contains("concurrent writers"), exception.getMessage());
        assertEquals(1, casEvalCount);
        assertTrue(recordedDelays.isEmpty());
    }

    @Test
    void testAppendEventToHashWithoutRevisionFailsWithoutRetrying() {
        newSession("s-unversioned");

        Map<?, ?> fields = assertInstanceOf(Map.class, store.get(KEY_PREFIX + "s-unversioned"));

        store.put(
            KEY_PREFIX + "s-unversioned",
            new HashMap<>(Map.of(
                RedisSessionRepository.DOCUMENT_FIELD, fields.get(RedisSessionRepository.DOCUMENT_FIELD))));

        casEvalCount = 0;

        IllegalStateException exception = assertThrows(
            IllegalStateException.class, () -> repository.appendEvent(event("s-unversioned", "lost")));

        assertTrue(exception.getMessage()
            .contains(KEY_PREFIX + "s-unversioned"), exception.getMessage());
        assertTrue(exception.getMessage()
            .contains("no " + RedisSessionRepository.REVISION_FIELD + " field"), exception.getMessage());
        assertEquals(1, casEvalCount);
        assertTrue(recordedDelays.isEmpty());
    }

    @Test
    void testUnexpectedScriptResultFailsInsteadOfReportingAConflict() {
        scriptResult = 2L;

        assertThrows(
            IllegalStateException.class, () -> repository.saveIfAbsent(Session.builder()
                .id("s-unexpected")
                .userId("user-1")
                .createdAt(Instant.now())
                .build()));
        assertTrue(recordedDelays.isEmpty());
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
    void testApplyCompactionCasSucceedsThenFailsOnStaleVersion() {
        newSession("s-cas");

        SessionEvent firstEvent = event("s-cas", "v1");

        repository.appendEvent(firstEvent);

        long version = repository.getEventVersion("s-cas");

        assertTrue(repository.applyCompaction("s-cas", archiveAndAppend(firstEvent, "compacted"), version));
        assertFalse(repository.applyCompaction("s-cas", archiveAndAppend(firstEvent, "again"), version));
    }

    @Test
    void testApplyCompactionArchivesInPlaceAndAppendsInsertedEvents() {
        newSession("s-replace");

        SessionEvent oldEvent = event("s-replace", "old");

        repository.appendEvent(oldEvent);

        long currentVersion = repository.getEventVersion("s-replace");

        assertTrue(repository.applyCompaction("s-replace", archiveAndAppend(oldEvent, "new"), currentVersion));

        List<SessionEvent> events = repository.findEvents("s-replace", EventFilter.all());

        assertEquals(2, events.size());
        assertEquals("old", events.get(0)
            .getMessage()
            .getText());
        assertTrue(events.get(0)
            .isArchived());
        assertEquals("new", events.get(1)
            .getMessage()
            .getText());

        List<SessionEvent> activeEvents = repository.findEvents("s-replace", EventFilter.active());

        assertEquals(1, activeEvents.size());
        assertEquals(currentVersion + 1, repository.getEventVersion("s-replace"));
    }

    @Test
    void testApplyCompactionRejectsArchiveIdNotInLog() {
        newSession("s-unknown");

        repository.appendEvent(event("s-unknown", "kept"));

        long currentVersion = repository.getEventVersion("s-unknown");

        SessionEvent foreignEvent = event("s-unknown", "never appended");

        assertThrows(
            IllegalArgumentException.class,
            () -> repository.applyCompaction("s-unknown", archiveAndAppend(foreignEvent, "x"), currentVersion));
        assertEquals(currentVersion, repository.getEventVersion("s-unknown"));
    }

    @Test
    void testAppendEventIsIdempotentById() {
        newSession("s-idempotent");

        SessionEvent sessionEvent = event("s-idempotent", "once");

        repository.appendEvent(sessionEvent);

        long version = repository.getEventVersion("s-idempotent");

        repository.appendEvent(sessionEvent);

        assertEquals(1, repository.findEvents("s-idempotent", EventFilter.all())
            .size());
        assertEquals(version, repository.getEventVersion("s-idempotent"));
    }

    @Test
    void testSaveIfAbsentInsertsOnlyOnce() {
        Session session = Session.builder()
            .id("s-absent")
            .userId("user-1")
            .createdAt(Instant.now())
            .build();

        assertTrue(repository.saveIfAbsent(session));
        assertFalse(repository.saveIfAbsent(session));
    }

    @Test
    void testSaveKeepsOriginalCreatedAtAndEvents() {
        Instant createdAt = Instant.ofEpochMilli(1_000L);

        repository.save(Session.builder()
            .id("s-update")
            .userId("user-1")
            .createdAt(createdAt)
            .build());

        repository.appendEvent(event("s-update", "kept"));

        Session savedSession = repository.save(Session.builder()
            .id("s-update")
            .userId("user-2")
            .createdAt(Instant.now())
            .build());

        assertEquals(createdAt, savedSession.createdAt());
        assertEquals("user-2", savedSession.userId());
        assertEquals(1, repository.findEvents("s-update", EventFilter.all())
            .size());
    }

    @Test
    void testFindEventsByUserIdSpansSessionsAndAppliesWindow() {
        newSession("s-user-a");
        newSession("s-user-b");

        repository.appendEvent(eventAt("s-user-a", "first", 1_000L));
        repository.appendEvent(eventAt("s-user-b", "second", 2_000L));
        repository.appendEvent(eventAt("s-user-a", "third", 3_000L));

        List<SessionEvent> events = repository.findEventsByUserId("user-1", EventFilter.lastN(2));

        assertEquals(2, events.size());
        assertEquals("second", events.get(0)
            .getMessage()
            .getText());
        assertEquals("third", events.get(1)
            .getMessage()
            .getText());
    }

    @Test
    void testListingsSkipUnreadableDocuments() {
        newSession("s-readable");

        repository.appendEvent(event("s-readable", "kept"));

        store.put(KEY_PREFIX + "s-garbage", "not json");
        store.put(KEY_PREFIX + "s-invalid", "{\"id\":\"s-invalid\",\"version\":0,\"events\":[]}");

        assertEquals(List.of("s-readable"), repository.findByUserId("user-1")
            .stream()
            .map(Session::id)
            .toList());
        assertEquals(1, repository.findEventsByUserId("user-1", EventFilter.all())
            .size());
        assertEquals(0, repository.deleteExpiredSessions(Instant.now()));
    }

    @Test
    void testSweepSkipsKeyOfAnotherTypeAndDeletesExpiredSessions() {
        repository.save(Session.builder()
            .id("s-expired")
            .userId("user-1")
            .createdAt(Instant.now()
                .minusSeconds(120))
            .expiresAt(Instant.now()
                .minusSeconds(60))
            .build());

        store.put(KEY_PREFIX + "s-list", List.of("item"));

        assertEquals(1, repository.deleteExpiredSessions(Instant.now()));
        assertNull(repository.findById("s-expired"));
        assertTrue(store.containsKey(KEY_PREFIX + "s-list"));
    }

    @Test
    void testReadOfKeyOfAnotherTypeFailsNamingTheKey() {
        store.put(KEY_PREFIX + "s-list", List.of("item"));

        UnreadableSessionDocumentException exception = assertThrows(
            UnreadableSessionDocumentException.class, () -> repository.findById("s-list"));

        assertTrue(exception.getMessage()
            .contains(KEY_PREFIX + "s-list"), exception.getMessage());
    }

    @Test
    void testHashWithRevisionButNoDocumentIsUnreadableAndSkippedBySweeps() {
        repository.save(Session.builder()
            .id("s-expired")
            .userId("user-1")
            .createdAt(Instant.now()
                .minusSeconds(120))
            .expiresAt(Instant.now()
                .minusSeconds(60))
            .build());

        store.put(
            KEY_PREFIX + "s-headless", new HashMap<>(Map.of(RedisSessionRepository.REVISION_FIELD, "3")));

        UnreadableSessionDocumentException findException = assertThrows(
            UnreadableSessionDocumentException.class, () -> repository.findById("s-headless"));

        assertTrue(findException.getMessage()
            .contains(KEY_PREFIX + "s-headless"), findException.getMessage());

        casEvalCount = 0;

        assertThrows(UnreadableSessionDocumentException.class, () -> newSession("s-headless"));
        assertEquals(0, casEvalCount);
        assertTrue(recordedDelays.isEmpty());

        assertEquals(List.of("s-expired"), repository.findByUserId("user-1")
            .stream()
            .map(Session::id)
            .toList());
        assertEquals(1, repository.deleteExpiredSessions(Instant.now()));
        assertTrue(store.containsKey(KEY_PREFIX + "s-headless"));
    }

    @Test
    void testSweepAbortsWhenRedisIsUnreachable() {
        newSession("s-a");

        when(jedis.hmget(anyString(), any(String[].class))).thenThrow(
            new JedisConnectionException("Connection refused"));

        assertThrows(JedisConnectionException.class, () -> repository.deleteExpiredSessions(Instant.now()));
        assertThrows(JedisConnectionException.class, () -> repository.findByUserId("user-1"));
    }

    @Test
    void testWritesStoreTheRevisionNextToTheDocument() {
        newSession("s-layout");

        repository.appendEvent(event("s-layout", "hello"));

        Map<?, ?> fields = assertInstanceOf(Map.class, store.get(KEY_PREFIX + "s-layout"));

        assertEquals("1", fields.get(RedisSessionRepository.REVISION_FIELD));
        Session session = Objects.requireNonNull(repository.findById("s-layout"));

        assertEquals(
            Long.toString(Objects.requireNonNull(session.expiresAt())
                .toEpochMilli()),
            fields.get(RedisSessionRepository.EXPIRES_AT_FIELD));
        assertTrue(((String) fields.get(RedisSessionRepository.DOCUMENT_FIELD)).contains("hello"));
    }

    @Test
    void testReadsAndMigratesDocumentStoredAsPlainString() {
        store.put(
            KEY_PREFIX + "s-legacy",
            "{\"id\":\"s-legacy\",\"userId\":\"user-1\",\"createdAtEpochMilli\":1," +
                "\"expiresAtEpochMilli\":null,\"metadata\":{},\"version\":2,\"events\":[]}");

        assertEquals("user-1", Objects.requireNonNull(repository.findById("s-legacy"))
            .userId());
        assertEquals(1, repository.findByUserId("user-1")
            .size());

        repository.appendEvent(event("s-legacy", "migrated"));

        Map<?, ?> fields = assertInstanceOf(Map.class, store.get(KEY_PREFIX + "s-legacy"));

        assertEquals("1", fields.get(RedisSessionRepository.REVISION_FIELD));
        assertEquals(3L, repository.getEventVersion("s-legacy"));
        assertEquals(1, repository.findEvents("s-legacy", EventFilter.all())
            .size());
    }

    @Test
    void testDeleteExpiredSessionsDeletesExpiredPlainStringDocument() {
        long expiredAt = Instant.now()
            .minusSeconds(60)
            .toEpochMilli();

        store.put(
            KEY_PREFIX + "s-legacy-expired",
            "{\"id\":\"s-legacy-expired\",\"userId\":\"user-1\",\"createdAtEpochMilli\":1," +
                "\"expiresAtEpochMilli\":" + expiredAt + ",\"metadata\":{},\"version\":0,\"events\":[]}");

        assertEquals(1, repository.deleteExpiredSessions(Instant.now()));
        assertNull(repository.findById("s-legacy-expired"));
    }

    @Test
    void testCompactionPrunesArchivedEventsBeyondTheLimit() {
        RedisSessionRepository limitedRepository = RedisSessionRepository.builder()
            .jedis(jedis)
            .keyPrefix(KEY_PREFIX)
            .maxArchivedEvents(1)
            .build();

        newSession("s-prune");

        SessionEvent firstEvent = event("s-prune", "first");
        SessionEvent secondEvent = event("s-prune", "second");

        limitedRepository.appendEvent(firstEvent);
        limitedRepository.appendEvent(secondEvent);

        assertTrue(limitedRepository.applyCompaction(
            "s-prune", archiveAndAppend(firstEvent, "summary one"), limitedRepository.getEventVersion("s-prune")));
        assertTrue(limitedRepository.applyCompaction(
            "s-prune", archiveAndAppend(secondEvent, "summary two"), limitedRepository.getEventVersion("s-prune")));

        List<String> texts = limitedRepository.findEvents("s-prune", EventFilter.all())
            .stream()
            .map(sessionEvent -> sessionEvent.getMessage()
                .getText())
            .toList();

        assertEquals(List.of("second", "summary one", "summary two"), texts);
    }

    @Test
    void testBuilderRejectsMissingSettings() {
        assertThrows(
            IllegalArgumentException.class, () -> RedisSessionRepository.builder()
                .jedis(jedis)
                .jsonMapper(null)
                .build());
        assertThrows(
            IllegalArgumentException.class, () -> RedisSessionRepository.builder()
                .jedis(jedis)
                .conditionalWriteRetry(null)
                .build());
        assertThrows(
            IllegalArgumentException.class, () -> RedisSessionRepository.builder()
                .jedis(jedis)
                .maxArchivedEvents(-1)
                .build());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "tenant*:", "tenant?:", "tenant[1]:", "tenant\\1:"
    })
    void testBuilderRejectsKeyPrefixWithGlobCharacters(String keyPrefix) {
        assertThrows(
            IllegalArgumentException.class, () -> RedisSessionRepository.builder()
                .jedis(jedis)
                .keyPrefix(keyPrefix)
                .build());
    }

    @Test
    void testRepositoriesWithOverlappingPrefixesOnlySeeTheirOwnSessions() {
        RedisSessionRepository tenant1Repository = RedisSessionRepository.builder()
            .jedis(jedis)
            .keyPrefix("bytechef-session:tenant1:")
            .build();
        RedisSessionRepository tenant10Repository = RedisSessionRepository.builder()
            .jedis(jedis)
            .keyPrefix("bytechef-session:tenant10:")
            .build();

        Session session = Session.builder()
            .id("s-shared")
            .userId("user-1")
            .createdAt(Instant.now())
            .build();

        tenant1Repository.save(session);
        tenant10Repository.save(session);

        assertEquals(1, tenant1Repository.findByUserId("user-1")
            .size());
        assertTrue(store.containsKey("bytechef-session:tenant1:s-shared"));
        assertTrue(store.containsKey("bytechef-session:tenant10:s-shared"));
        assertTrue(repository.findByUserId("user-1")
            .isEmpty());
    }

    @Test
    void testFindByUserIdAndDelete() {
        newSession("s-del");

        assertTrue(repository.findByUserId("user-1")
            .stream()
            .map(Session::id)
            .toList()
            .contains("s-del"));

        repository.delete("s-del");

        assertNull(repository.findById("s-del"));
    }

    @Test
    void testDeleteExpiredSessionsDeletesOnlyExpiredSessions() {
        repository.save(Session.builder()
            .id("s-expired")
            .userId("user-1")
            .createdAt(Instant.now()
                .minusSeconds(120))
            .expiresAt(Instant.now()
                .minusSeconds(60))
            .build());

        newSession("s-live");

        assertEquals(1, repository.deleteExpiredSessions(Instant.now()));
        assertNull(repository.findById("s-expired"));
        assertNotNull(repository.findById("s-live"));
    }

    private CompactionPlan archiveAndAppend(SessionEvent archivedEvent, String insertedText) {
        return new CompactionPlan(
            Set.of(archivedEvent.getId()),
            List.of(CompactionPlan.Insert.atEnd(List.of(event(archivedEvent.getSessionId(), insertedText)))));
    }

    private SessionEvent eventAt(String sessionId, String text, long timestampEpochMilli) {
        return SessionEvent.builder()
            .sessionId(sessionId)
            .timestamp(Instant.ofEpochMilli(timestampEpochMilli))
            .message(new UserMessage(text))
            .build();
    }
}
