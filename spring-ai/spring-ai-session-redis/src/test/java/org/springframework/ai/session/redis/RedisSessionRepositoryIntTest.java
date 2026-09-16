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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.compaction.CompactionPlan;
import org.springframework.ai.session.store.ConditionalWriteRetry;
import org.springframework.ai.session.store.UnreadableSessionDocumentException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.RedisClient;

/**
 * @author Ivica Cardic
 */
class RedisSessionRepositoryIntTest {

    private static final int CONTENTION_EVENTS_PER_THREAD = 5;
    private static final int CONTENTION_MAX_ATTEMPTS = 50;
    private static final int CONTENTION_THREAD_COUNT = 8;
    private static final String KEY_PREFIX = "int-test-session:";

    private static GenericContainer<?> redis;
    private static RedisClient redisClient;
    private static RedisSessionRepository repository;

    @BeforeAll
    static void beforeAll() {
        redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

        redis.start();

        redisClient = RedisClient.create(redis.getHost(), redis.getMappedPort(6379));

        repository = RedisSessionRepository.builder()
            .jedis(redisClient)
            .keyPrefix(KEY_PREFIX)
            .build();
    }

    @AfterAll
    static void afterAll() {
        if (redisClient != null) {
            redisClient.close();
        }

        if (redis != null) {
            redis.stop();
        }
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
        newSession("int-race");

        Runnable[] afterNextGet = new Runnable[1];

        RedisSessionRepository interleavingRepository = interleavingRepository(KEY_PREFIX, afterNextGet);

        afterNextGet[0] = () -> repository.save(Session.builder()
            .id("int-race")
            .userId("user-1")
            .createdAt(Instant.now())
            .metadata(Map.of("title", "saved concurrently"))
            .build());

        interleavingRepository.appendEvent(event("int-race", "appended"));

        Session session = Objects.requireNonNull(repository.findById("int-race"));

        assertEquals("saved concurrently", session.metadata()
            .get("title"));
        assertEquals(1, repository.findEvents("int-race", EventFilter.all())
            .size());
    }

    @Test
    void testSaveKeepsEventAppendedConcurrently() {
        newSession("int-race-save");

        Runnable[] afterNextGet = new Runnable[1];

        RedisSessionRepository interleavingRepository = interleavingRepository(KEY_PREFIX, afterNextGet);

        afterNextGet[0] = () -> repository.appendEvent(event("int-race-save", "appended"));

        interleavingRepository.save(Session.builder()
            .id("int-race-save")
            .userId("user-1")
            .createdAt(Instant.now())
            .metadata(Map.of("title", "saved"))
            .build());

        Session session = Objects.requireNonNull(repository.findById("int-race-save"));

        assertEquals("saved", session.metadata()
            .get("title"));
        assertEquals(1, repository.findEvents("int-race-save", EventFilter.all())
            .size());
    }

    @Test
    void testSaveIfAbsentInsertsOnlyOnce() {
        Session session = Session.builder()
            .id("int-absent")
            .userId("user-1")
            .createdAt(Instant.now())
            .build();

        assertTrue(repository.saveIfAbsent(session));
        assertFalse(repository.saveIfAbsent(session));
    }

    @Test
    void testApplyCompactionArchivesInPlaceAndAppendsInsertedEvents() {
        newSession("int-replace");

        SessionEvent oldEvent = event("int-replace", "old");

        repository.appendEvent(oldEvent);

        long currentVersion = repository.getEventVersion("int-replace");

        assertTrue(repository.applyCompaction("int-replace", archiveAndAppend(oldEvent, "new"), currentVersion));

        List<SessionEvent> events = repository.findEvents("int-replace", EventFilter.all());

        assertEquals(2, events.size());
        assertEquals("old", events.get(0)
            .getMessage()
            .getText());
        assertTrue(events.get(0)
            .isArchived());
        assertEquals("new", events.get(1)
            .getMessage()
            .getText());
        assertEquals(1, repository.findEvents("int-replace", EventFilter.active())
            .size());
        assertEquals(currentVersion + 1, repository.getEventVersion("int-replace"));
    }

    @Test
    void testDeleteExpiredSessionsKeepsSessionExtendedConcurrently() {
        String keyPrefix = "int-test-expiry:";

        RedisSessionRepository expiryRepository = RedisSessionRepository.builder()
            .jedis(redisClient)
            .keyPrefix(keyPrefix)
            .build();

        expiryRepository.save(Session.builder()
            .id("int-extended")
            .userId("user-1")
            .createdAt(Instant.now()
                .minusSeconds(120))
            .expiresAt(Instant.now()
                .minusSeconds(60))
            .build());

        Runnable[] afterNextGet = new Runnable[1];

        RedisSessionRepository interleavingRepository = interleavingRepository(keyPrefix, afterNextGet);

        afterNextGet[0] = () -> expiryRepository.save(Session.builder()
            .id("int-extended")
            .userId("user-1")
            .createdAt(Instant.now())
            .expiresAt(Instant.now()
                .plusSeconds(3600))
            .build());

        assertEquals(0, interleavingRepository.deleteExpiredSessions(Instant.now()));
        assertNotNull(expiryRepository.findById("int-extended"));
    }

    @Test
    void testSaveUpdatesDocumentWrittenWithoutRevision() {
        redisClient.set(
            KEY_PREFIX + "int-legacy",
            "{\"id\":\"int-legacy\",\"userId\":\"user-1\",\"createdAtEpochMilli\":1," +
                "\"expiresAtEpochMilli\":null,\"metadata\":{},\"version\":2,\"events\":[]}");

        repository.appendEvent(event("int-legacy", "appended"));

        assertEquals("hash", redisClient.type(KEY_PREFIX + "int-legacy"));
        assertEquals("1", redisClient.hget(KEY_PREFIX + "int-legacy", RedisSessionRepository.REVISION_FIELD));
        assertEquals(3L, repository.getEventVersion("int-legacy"));
        assertEquals(1, repository.findEvents("int-legacy", EventFilter.all())
            .size());
    }

    @Test
    void testAppendEventRetriesWhenPlainStringDocumentChangesConcurrently() {
        String key = KEY_PREFIX + "int-legacy-race";

        redisClient.set(key, legacyDocument("int-legacy-race", "{}"));

        Runnable[] afterNextGet = new Runnable[1];

        RedisSessionRepository interleavingRepository = interleavingRepository(KEY_PREFIX, afterNextGet);

        afterNextGet[0] = () -> redisClient.set(key, legacyDocument("int-legacy-race", "{\"title\":\"changed\"}"));

        interleavingRepository.appendEvent(event("int-legacy-race", "appended"));

        Session session = Objects.requireNonNull(repository.findById("int-legacy-race"));

        assertEquals("hash", redisClient.type(key));
        assertEquals("changed", session.metadata()
            .get("title"));
        assertEquals(1, repository.findEvents("int-legacy-race", EventFilter.all())
            .size());
    }

    @Test
    void testDeleteExpiredSessionsDeletesExpiredPlainStringDocument() {
        String keyPrefix = "int-test-legacy-expiry:";

        long expiredAt = Instant.now()
            .minusSeconds(60)
            .toEpochMilli();

        redisClient.set(
            keyPrefix + "int-legacy-expired",
            "{\"id\":\"int-legacy-expired\",\"userId\":\"user-1\",\"createdAtEpochMilli\":1," +
                "\"expiresAtEpochMilli\":" + expiredAt + ",\"metadata\":{},\"version\":0,\"events\":[]}");

        RedisSessionRepository legacyRepository = RedisSessionRepository.builder()
            .jedis(redisClient)
            .keyPrefix(keyPrefix)
            .build();

        assertEquals(1, legacyRepository.deleteExpiredSessions(Instant.now()));
        assertNull(legacyRepository.findById("int-legacy-expired"));
    }

    @Test
    void testDeleteExpiredSessionsKeepsPlainStringDocumentChangedAfterItWasRead() {
        String keyPrefix = "int-test-legacy-race-expiry:";
        String key = keyPrefix + "int-legacy-extended";

        long expiredAt = Instant.now()
            .minusSeconds(60)
            .toEpochMilli();
        long extendedExpiresAt = Instant.now()
            .plusSeconds(3600)
            .toEpochMilli();

        redisClient.set(key, legacyDocument("int-legacy-extended", "{}", expiredAt));

        Runnable[] afterNextGet = new Runnable[1];

        RedisSessionRepository interleavingRepository = interleavingRepository(keyPrefix, afterNextGet);

        afterNextGet[0] = () -> redisClient.set(key, legacyDocument("int-legacy-extended", "{}", extendedExpiresAt));

        try {
            assertEquals(0, interleavingRepository.deleteExpiredSessions(Instant.now()));
            assertEquals("string", redisClient.type(key));

            Session session = Objects.requireNonNull(interleavingRepository.findById("int-legacy-extended"));

            assertEquals(Instant.ofEpochMilli(extendedExpiresAt), session.expiresAt());
        } finally {
            redisClient.del(key);
        }
    }

    @Test
    void testApplyCompactionRacingAConcurrentSaveReturnsFalseAndKeepsTheRefreshedExpiry() {
        newSession("int-compaction-race");

        SessionEvent firstEvent = event("int-compaction-race", "first");

        repository.appendEvent(firstEvent);

        long version = repository.getEventVersion("int-compaction-race");

        Instant refreshedExpiresAt = Instant.ofEpochMilli(Instant.now()
            .plusSeconds(7200)
            .toEpochMilli());

        Runnable[] afterNextGet = new Runnable[1];

        RedisSessionRepository interleavingRepository = interleavingRepository(KEY_PREFIX, afterNextGet);

        afterNextGet[0] = () -> repository.save(Session.builder()
            .id("int-compaction-race")
            .userId("user-1")
            .createdAt(Instant.now())
            .expiresAt(refreshedExpiresAt)
            .build());

        assertFalse(interleavingRepository.applyCompaction(
            "int-compaction-race", archiveAndAppend(firstEvent, "compacted"), version));

        Session session = Objects.requireNonNull(repository.findById("int-compaction-race"));

        assertEquals(refreshedExpiresAt, session.expiresAt());
        assertEquals(version, repository.getEventVersion("int-compaction-race"));
        assertEquals(List.of("first"), repository.findEvents("int-compaction-race", EventFilter.active())
            .stream()
            .map(sessionEvent -> sessionEvent.getMessage()
                .getText())
            .toList());
    }

    @Test
    void testConcurrentAppendsFromManyThreadsAreAllKept() throws InterruptedException {
        newSession("int-contention");

        AtomicInteger retryCount = new AtomicInteger();
        AtomicInteger readCount = new AtomicInteger();
        CountDownLatch firstReadsLatch = new CountDownLatch(CONTENTION_THREAD_COUNT);

        RedisClient contendedRedisClient = spy(redisClient);

        doAnswer(invocation -> {
            Object value = invocation.callRealMethod();

            if (readCount.getAndIncrement() < CONTENTION_THREAD_COUNT) {
                firstReadsLatch.countDown();

                if (!firstReadsLatch.await(30, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting for every thread to read the session");
                }
            }

            return value;
        }).when(contendedRedisClient)
            .hmget(anyString(), any(String[].class));

        RedisSessionRepository contendedRepository = RedisSessionRepository.builder()
            .jedis(contendedRedisClient)
            .keyPrefix(KEY_PREFIX)
            .conditionalWriteRetry(new ConditionalWriteRetry(
                CONTENTION_MAX_ATTEMPTS, ConditionalWriteRetry.DEFAULT_BASE_DELAY_MILLIS,
                ConditionalWriteRetry.DEFAULT_MAX_DELAY_MILLIS, delayMillis -> {
                    retryCount.incrementAndGet();

                    sleep(delayMillis);
                }))
            .build();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(CONTENTION_THREAD_COUNT);
        Queue<Throwable> failures = new ConcurrentLinkedQueue<>();

        try (ExecutorService executorService = Executors.newFixedThreadPool(CONTENTION_THREAD_COUNT)) {
            for (int threadIndex = 0; threadIndex < CONTENTION_THREAD_COUNT; threadIndex++) {
                int currentThreadIndex = threadIndex;

                executorService.execute(() -> {
                    try {
                        if (!startLatch.await(30, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Timed out waiting for the start signal");
                        }

                        for (int eventIndex = 0; eventIndex < CONTENTION_EVENTS_PER_THREAD; eventIndex++) {
                            contendedRepository.appendEvent(
                                event("int-contention", "thread-" + currentThreadIndex + "-event-" + eventIndex));
                        }
                    } catch (Throwable throwable) {
                        failures.add(throwable);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();

            assertTrue(doneLatch.await(2, TimeUnit.MINUTES));
        }

        assertTrue(failures.isEmpty(), () -> "Appending threads failed: " + failures);

        int expectedEventCount = CONTENTION_THREAD_COUNT * CONTENTION_EVENTS_PER_THREAD;

        Set<String> eventTexts = repository.findEvents("int-contention", EventFilter.all())
            .stream()
            .map(sessionEvent -> sessionEvent.getMessage()
                .getText())
            .collect(Collectors.toSet());

        assertEquals(expectedEventCount, eventTexts.size());
        assertEquals(expectedEventCount, repository.getEventVersion("int-contention"));
        assertTrue(
            retryCount.get() >= CONTENTION_THREAD_COUNT - 1,
            () -> "Expected every thread but one to retry its first append, but retries were " + retryCount.get());
    }

    @Test
    void testCompactionPrunesArchivedEventsBeyondTheLimit() {
        RedisSessionRepository limitedRepository = RedisSessionRepository.builder()
            .jedis(redisClient)
            .keyPrefix(KEY_PREFIX)
            .maxArchivedEvents(1)
            .build();

        newSession("int-prune");

        SessionEvent firstEvent = event("int-prune", "first");
        SessionEvent secondEvent = event("int-prune", "second");

        limitedRepository.appendEvent(firstEvent);
        limitedRepository.appendEvent(secondEvent);

        assertTrue(limitedRepository.applyCompaction(
            "int-prune", archiveAndAppend(firstEvent, "summary one"), limitedRepository.getEventVersion("int-prune")));
        assertTrue(limitedRepository.applyCompaction(
            "int-prune", archiveAndAppend(secondEvent, "summary two"),
            limitedRepository.getEventVersion("int-prune")));

        List<String> texts = limitedRepository.findEvents("int-prune", EventFilter.all())
            .stream()
            .map(sessionEvent -> sessionEvent.getMessage()
                .getText())
            .toList();

        assertEquals(List.of("second", "summary one", "summary two"), texts);
    }

    @Test
    void testSaveIfAbsentOnKeyOfAnotherTypeFailsNamingKeyAndType() {
        String key = KEY_PREFIX + "int-list";

        redisClient.rpush(key, "item");

        try {
            IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> repository.saveIfAbsent(Session.builder()
                    .id("int-list")
                    .userId("user-1")
                    .createdAt(Instant.now())
                    .build()));

            assertTrue(exception.getMessage()
                .contains(key), exception.getMessage());
            assertTrue(exception.getMessage()
                .contains("list"), exception.getMessage());
            assertEquals("list", redisClient.type(key));
        } finally {
            redisClient.del(key);
        }
    }

    @Test
    void testAppendEventFailsWithoutRetryingWhenKeyTurnsIntoAnotherType() {
        String key = KEY_PREFIX + "int-retyped";

        newSession("int-retyped");

        Runnable[] afterNextGet = new Runnable[1];

        RedisSessionRepository interleavingRepository = interleavingRepository(KEY_PREFIX, afterNextGet);

        afterNextGet[0] = () -> {
            redisClient.del(key);
            redisClient.sadd(key, "member");
        };

        try {
            IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> interleavingRepository.appendEvent(event("int-retyped", "lost")));

            assertTrue(exception.getMessage()
                .contains("set"), exception.getMessage());
            assertFalse(exception.getMessage()
                .contains("concurrent writers"), exception.getMessage());
            assertEquals("set", redisClient.type(key));
        } finally {
            redisClient.del(key);
        }
    }

    @Test
    void testAppendEventToHashWithoutRevisionFailsWithoutRetrying() {
        String key = KEY_PREFIX + "int-unversioned";

        newSession("int-unversioned");

        redisClient.hdel(key, RedisSessionRepository.REVISION_FIELD);

        try {
            IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> repository.appendEvent(event("int-unversioned", "lost")));

            assertTrue(exception.getMessage()
                .contains("no " + RedisSessionRepository.REVISION_FIELD + " field"), exception.getMessage());
            assertFalse(exception.getMessage()
                .contains("concurrent writers"), exception.getMessage());
            assertEquals(0L, repository.getEventVersion("int-unversioned"));
        } finally {
            redisClient.del(key);
        }
    }

    @Test
    void testHashWithRevisionButNoDocumentIsUnreadableAndSkippedBySweeps() {
        String keyPrefix = "int-test-headless:";
        String key = keyPrefix + "int-headless";

        RedisSessionRepository headlessRepository = RedisSessionRepository.builder()
            .jedis(redisClient)
            .keyPrefix(keyPrefix)
            .build();

        headlessRepository.save(Session.builder()
            .id("int-headless-expired")
            .userId("user-1")
            .createdAt(Instant.now()
                .minusSeconds(120))
            .expiresAt(Instant.now()
                .minusSeconds(60))
            .build());

        redisClient.hset(key, RedisSessionRepository.REVISION_FIELD, "3");

        try {
            UnreadableSessionDocumentException exception = assertThrows(
                UnreadableSessionDocumentException.class, () -> headlessRepository.findById("int-headless"));

            assertTrue(exception.getMessage()
                .contains(key), exception.getMessage());
            assertThrows(UnreadableSessionDocumentException.class, () -> headlessRepository.save(Session.builder()
                .id("int-headless")
                .userId("user-1")
                .createdAt(Instant.now())
                .build()));
            assertEquals(List.of("int-headless-expired"), headlessRepository.findByUserId("user-1")
                .stream()
                .map(Session::id)
                .toList());
            assertEquals(1, headlessRepository.deleteExpiredSessions(Instant.now()));
            assertEquals("hash", redisClient.type(key));
        } finally {
            redisClient.del(key);
        }
    }

    @Test
    void testSweepSkipsKeyOfAnotherTypeAndReadOfItFails() {
        String keyPrefix = "int-test-wrong-type:";
        String listKey = keyPrefix + "int-list";

        RedisSessionRepository wrongTypeRepository = RedisSessionRepository.builder()
            .jedis(redisClient)
            .keyPrefix(keyPrefix)
            .build();

        wrongTypeRepository.save(Session.builder()
            .id("int-wrong-type-expired")
            .userId("user-1")
            .createdAt(Instant.now()
                .minusSeconds(120))
            .expiresAt(Instant.now()
                .minusSeconds(60))
            .build());

        redisClient.rpush(listKey, "item");

        try {
            assertEquals(1, wrongTypeRepository.deleteExpiredSessions(Instant.now()));
            assertNull(wrongTypeRepository.findById("int-wrong-type-expired"));
            assertEquals("list", redisClient.type(listKey));

            UnreadableSessionDocumentException exception = assertThrows(
                UnreadableSessionDocumentException.class, () -> wrongTypeRepository.findById("int-list"));

            assertTrue(exception.getMessage()
                .contains(listKey), exception.getMessage());
        } finally {
            redisClient.del(listKey);
        }
    }

    @Test
    void testScansStayWithinTheTenantPrefix() {
        RedisSessionRepository tenant1Repository = RedisSessionRepository.builder()
            .jedis(redisClient)
            .keyPrefix("bytechef-session:tenant1:")
            .build();
        RedisSessionRepository tenant10Repository = RedisSessionRepository.builder()
            .jedis(redisClient)
            .keyPrefix("bytechef-session:tenant10:")
            .build();

        for (RedisSessionRepository tenantRepository : List.of(tenant1Repository, tenant10Repository)) {
            tenantRepository.save(Session.builder()
                .id("tenant-expired")
                .userId("tenant-user")
                .createdAt(Instant.now()
                    .minusSeconds(120))
                .expiresAt(Instant.now()
                    .minusSeconds(60))
                .build());
            tenantRepository.save(Session.builder()
                .id("tenant-live")
                .userId("tenant-user")
                .createdAt(Instant.now())
                .build());
        }

        tenant1Repository.appendEvent(event("tenant-live", "tenant1 event"));
        tenant10Repository.appendEvent(event("tenant-live", "tenant10 event"));

        assertEquals(Set.of("tenant-expired", "tenant-live"), tenant1Repository.findByUserId("tenant-user")
            .stream()
            .map(Session::id)
            .collect(Collectors.toSet()));
        assertEquals(2, tenant1Repository.findByUserId("tenant-user")
            .size());
        assertEquals(List.of("tenant1 event"), tenant1Repository.findEventsByUserId("tenant-user", EventFilter.all())
            .stream()
            .map(sessionEvent -> sessionEvent.getMessage()
                .getText())
            .toList());
        assertEquals(1, tenant1Repository.deleteExpiredSessions(Instant.now()));
        assertNull(tenant1Repository.findById("tenant-expired"));
        assertNotNull(tenant10Repository.findById("tenant-expired"));
        assertNotNull(tenant10Repository.findById("tenant-live"));
    }

    @Test
    void testAppendEventToSessionDeletedBetweenReadAndConditionalWriteThrowsSessionNotFound() {
        String key = KEY_PREFIX + "int-deleted";

        newSession("int-deleted");

        Runnable[] afterNextGet = new Runnable[1];

        RedisSessionRepository interleavingRepository = interleavingRepository(KEY_PREFIX, afterNextGet);

        afterNextGet[0] = () -> redisClient.del(key);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> interleavingRepository.appendEvent(event("int-deleted", "lost")));

        assertTrue(exception.getMessage()
            .contains("Session not found"), exception.getMessage());
        assertFalse(redisClient.exists(key));
    }

    @Test
    void testAppendEventOnMissingSessionThrows() {
        assertThrows(IllegalArgumentException.class, () -> repository.appendEvent(event("nope", "x")));
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

    private static RedisSessionRepository interleavingRepository(String keyPrefix, Runnable[] afterNextGet) {
        RedisClient interleavingRedisClient = spy(redisClient);

        Answer<Object> runAfterNextGet = invocation -> {
            Object value = invocation.callRealMethod();

            Runnable runnable = afterNextGet[0];

            if (runnable != null) {
                afterNextGet[0] = null;

                runnable.run();
            }

            return value;
        };

        doAnswer(runAfterNextGet).when(interleavingRedisClient)
            .get(anyString());
        doAnswer(runAfterNextGet).when(interleavingRedisClient)
            .hmget(anyString(), any(String[].class));

        return RedisSessionRepository.builder()
            .jedis(interleavingRedisClient)
            .keyPrefix(keyPrefix)
            .build();
    }

    private static String legacyDocument(String sessionId, String metadata) {
        return legacyDocument(sessionId, metadata, null);
    }

    private static String legacyDocument(String sessionId, String metadata, Long expiresAtEpochMilli) {
        return "{\"id\":\"" + sessionId + "\",\"userId\":\"user-1\",\"createdAtEpochMilli\":1," +
            "\"expiresAtEpochMilli\":" + expiresAtEpochMilli + ",\"metadata\":" + metadata +
            ",\"version\":0,\"events\":[]}";
    }

    private static void sleep(long delayMillis) {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            throw new IllegalStateException(interruptedException);
        }
    }

    private CompactionPlan archiveAndAppend(SessionEvent archivedEvent, String insertedText) {
        return new CompactionPlan(
            Set.of(archivedEvent.getId()),
            List.of(CompactionPlan.Insert.atEnd(List.of(event(archivedEvent.getSessionId(), insertedText)))));
    }
}
