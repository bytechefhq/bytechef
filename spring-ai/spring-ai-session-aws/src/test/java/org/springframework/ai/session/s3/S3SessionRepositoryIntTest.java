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

package org.springframework.ai.session.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.net.URI;
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
import org.mockito.AdditionalAnswers;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.compaction.CompactionPlan;
import org.springframework.ai.session.store.ConditionalWriteRetry;
import org.springframework.ai.session.store.StoredSession;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import tools.jackson.databind.json.JsonMapper;

class S3SessionRepositoryIntTest {

    private static final String BUCKET = "session-test";
    private static final int CONTENTION_EVENTS_PER_THREAD = 5;
    private static final int CONTENTION_MAX_ATTEMPTS = 50;
    private static final int CONTENTION_THREAD_COUNT = 8;
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
        .build();

    private static LocalStackContainer localStack;
    private static S3Client s3Client;
    private static S3SessionRepository repository;

    @BeforeAll
    static void beforeAll() {
        localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.4"))
            .withServices(Service.S3);

        localStack.start();

        s3Client = S3Client.builder()
            .endpointOverride(URI.create(localStack.getEndpoint()
                .toString()))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())))
            .region(Region.of(localStack.getRegion()))
            .forcePathStyle(true)
            .build();

        s3Client.createBucket(CreateBucketRequest.builder()
            .bucket(BUCKET)
            .build());

        repository = S3SessionRepository.builder()
            .s3Client(s3Client)
            .bucketName(BUCKET)
            .build();
    }

    @AfterAll
    static void afterAll() {
        if (localStack != null) {
            localStack.stop();
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

        S3SessionRepository interleavingRepository = interleavingRepository(afterNextGet);

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

        S3SessionRepository interleavingRepository = interleavingRepository(afterNextGet);

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

    @Test
    void testScansOnlyTouchJsonObjectsUnderTheKeyPrefix() {
        String bucket = "session-prefix-test";

        s3Client.createBucket(CreateBucketRequest.builder()
            .bucket(bucket)
            .build());

        putSessionObject(bucket, "session/prefixed-expired.json", "prefixed-expired", true);
        putSessionObject(bucket, "session/prefixed-live.json", "prefixed-live", false);
        putSessionObject(bucket, "session/prefixed-text.txt", "prefixed-text", true);
        putSessionObject(bucket, "other/other-expired.json", "other-expired", true);

        S3SessionRepository prefixedRepository = S3SessionRepository.builder()
            .s3Client(s3Client)
            .bucketName(bucket)
            .keyPrefix("session/")
            .build();

        assertEquals(Set.of("prefixed-expired", "prefixed-live"), prefixedRepository.findByUserId("user-1")
            .stream()
            .map(Session::id)
            .collect(Collectors.toSet()));
        assertEquals(1, prefixedRepository.deleteExpiredSessions(Instant.now()));

        ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(ListObjectsV2Request.builder()
            .bucket(bucket)
            .build());

        List<String> remainingObjectKeys = listObjectsV2Response.contents()
            .stream()
            .map(S3Object::key)
            .sorted()
            .toList();

        assertEquals(
            List.of("other/other-expired.json", "session/prefixed-live.json", "session/prefixed-text.txt"),
            remainingObjectKeys);
    }

    @Test
    void testConcurrentAppendsFromManyThreadsAreAllKept() throws InterruptedException {
        newSession("s-contention");

        AtomicInteger retryCount = new AtomicInteger();
        AtomicInteger readCount = new AtomicInteger();
        CountDownLatch firstReadsLatch = new CountDownLatch(CONTENTION_THREAD_COUNT);

        S3Client contendedS3Client = mock(S3Client.class, AdditionalAnswers.delegatesTo(s3Client));

        doAnswer(invocation -> {
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(
                invocation.getArgument(0, GetObjectRequest.class));

            if (readCount.getAndIncrement() < CONTENTION_THREAD_COUNT) {
                firstReadsLatch.countDown();

                if (!firstReadsLatch.await(30, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting for every thread to read the session");
                }
            }

            return response;
        }).when(contendedS3Client)
            .getObjectAsBytes(any(GetObjectRequest.class));

        S3SessionRepository contendedRepository = S3SessionRepository.builder()
            .s3Client(contendedS3Client)
            .bucketName(BUCKET)
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
                                event("s-contention", "thread-" + currentThreadIndex + "-event-" + eventIndex));
                        }
                    } catch (Throwable throwable) {
                        failures.add(throwable);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();

            assertTrue(doneLatch.await(5, TimeUnit.MINUTES));
        }

        assertTrue(failures.isEmpty(), () -> "Appending threads failed: " + failures);

        int expectedEventCount = CONTENTION_THREAD_COUNT * CONTENTION_EVENTS_PER_THREAD;

        Set<String> eventTexts = repository.findEvents("s-contention", EventFilter.all())
            .stream()
            .map(sessionEvent -> sessionEvent.getMessage()
                .getText())
            .collect(Collectors.toSet());

        assertEquals(expectedEventCount, eventTexts.size());
        assertEquals(expectedEventCount, repository.getEventVersion("s-contention"));
        assertTrue(
            retryCount.get() >= CONTENTION_THREAD_COUNT - 1,
            () -> "Expected every thread but one to retry its first append, but retries were " + retryCount.get());
    }

    private static void putSessionObject(String bucket, String objectKey, String sessionId, boolean expired) {
        Instant now = Instant.now();

        Instant expiresAt = expired ? now.minusSeconds(60) : now.plusSeconds(3600);

        StoredSession storedSession = StoredSession.fromSession(
            Session.builder()
                .id(sessionId)
                .userId("user-1")
                .createdAt(now.minusSeconds(120))
                .expiresAt(expiresAt)
                .build(),
            0L, List.of());

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build(),
            RequestBody.fromBytes(JSON_MAPPER.writeValueAsBytes(storedSession)));
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

    private static S3SessionRepository interleavingRepository(Runnable[] afterNextGet) {
        S3Client interleavingS3Client = mock(S3Client.class, AdditionalAnswers.delegatesTo(s3Client));

        doAnswer(invocation -> {
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(
                invocation.getArgument(0, GetObjectRequest.class));

            Runnable runnable = afterNextGet[0];

            if (runnable != null) {
                afterNextGet[0] = null;

                runnable.run();
            }

            return response;
        }).when(interleavingS3Client)
            .getObjectAsBytes(any(GetObjectRequest.class));

        return S3SessionRepository.builder()
            .s3Client(interleavingS3Client)
            .bucketName(BUCKET)
            .build();
    }

    private CompactionPlan archiveAndAppend(SessionEvent archivedEvent, String insertedText) {
        return new CompactionPlan(
            Set.of(archivedEvent.getId()),
            List.of(CompactionPlan.Insert.atEnd(List.of(event(archivedEvent.getSessionId(), insertedText)))));
    }
}
