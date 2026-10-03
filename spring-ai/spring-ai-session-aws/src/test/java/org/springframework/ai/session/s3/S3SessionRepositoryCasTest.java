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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.compaction.CompactionPlan;
import org.springframework.ai.session.store.AbstractDocumentSessionRepository;
import org.springframework.ai.session.store.ConditionalWriteRetry;
import org.springframework.ai.session.store.StoredSession;
import org.springframework.ai.session.store.StoredSession.StoredEvent;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class S3SessionRepositoryCasTest {

    private static final String SESSION_ID = "sess";
    private static final String BUCKET = "b";
    private static final String ETAG = "\"etag-v5\"";
    private static final String UPDATED_ETAG = "\"etag-v6\"";
    private static final long VERSION = 5L;

    private static final Logger log = (Logger) LoggerFactory.getLogger(
        AbstractDocumentSessionRepository.class);

    private final JsonMapper jsonMapper = JsonMapper.builder()
        .build();

    private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
    private final List<Long> recordedDelays = new ArrayList<>();
    private S3Client s3Client;
    private S3SessionRepository repository;
    private byte[] storedSessionBytes;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);

        StoredSession storedSession = new StoredSession(
            SESSION_ID, "user-1", System.currentTimeMillis(), null, Map.of(), VERSION, 0L, List.of());

        storedSessionBytes = JsonMapper.builder()
            .build()
            .writeValueAsBytes(storedSession);

        repository = S3SessionRepository.builder()
            .s3Client(s3Client)
            .bucketName(BUCKET)
            .conditionalWriteRetry(new ConditionalWriteRetry(
                ConditionalWriteRetry.DEFAULT_MAX_ATTEMPTS, ConditionalWriteRetry.DEFAULT_BASE_DELAY_MILLIS,
                ConditionalWriteRetry.DEFAULT_MAX_DELAY_MILLIS, recordedDelays::add))
            .build();

        logAppender.start();

        log.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        log.detachAppender(logAppender);

        logAppender.stop();
    }

    @Test
    void testAppendEventGivesUpAfterEveryAttemptConflicts() {
        stubStoredSession();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(preconditionFailed());

        IllegalStateException exception = assertThrows(
            IllegalStateException.class, () -> repository.appendEvent(SessionEvent.builder()
                .sessionId(SESSION_ID)
                .message(new UserMessage("x"))
                .build()));

        verify(s3Client, times(ConditionalWriteRetry.DEFAULT_MAX_ATTEMPTS))
            .putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(s3Client, times(ConditionalWriteRetry.DEFAULT_MAX_ATTEMPTS))
            .getObjectAsBytes(any(GetObjectRequest.class));

        assertEquals(ConditionalWriteRetry.DEFAULT_MAX_ATTEMPTS - 1, recordedDelays.size());
        assertTrue(exception.getMessage()
            .contains(SESSION_ID));
        assertTrue(exception.getMessage()
            .contains("concurrent writers"));
    }

    @Test
    void testSaveGivesUpAfterEveryAttemptConflicts() {
        stubStoredSession();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(preconditionFailed());

        assertThrows(IllegalStateException.class, () -> repository.save(newSession()));

        verify(s3Client, times(ConditionalWriteRetry.DEFAULT_MAX_ATTEMPTS))
            .putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testApplyCompactionReturnsFalseWhenPutThrows412() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenReturn(ResponseBytes.fromByteArray(
                GetObjectResponse.builder()
                    .eTag(ETAG)
                    .build(),
                storedSessionBytes));

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(S3Exception.builder()
                .statusCode(412)
                .message("PreconditionFailed")
                .build());

        List<SessionEvent> events = List.of(SessionEvent.builder()
            .sessionId(SESSION_ID)
            .message(new UserMessage("x"))
            .build());

        boolean result = repository.applyCompaction(SESSION_ID, appendPlan(events), VERSION);

        assertFalse(result);
    }

    @Test
    void testApplyCompactionReturnsTrueWhenPutSucceeds() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenReturn(ResponseBytes.fromByteArray(
                GetObjectResponse.builder()
                    .eTag(ETAG)
                    .build(),
                storedSessionBytes));

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(PutObjectResponse.builder()
                .build());

        List<SessionEvent> events = List.of(SessionEvent.builder()
            .sessionId(SESSION_ID)
            .message(new UserMessage("x"))
            .build());

        boolean result = repository.applyCompaction(SESSION_ID, appendPlan(events), VERSION);

        assertTrue(result);
    }

    @Test
    void testApplyCompactionReturnsFalseOnStaleVersionWithoutInvokingPut() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenReturn(ResponseBytes.fromByteArray(
                GetObjectResponse.builder()
                    .eTag(ETAG)
                    .build(),
                storedSessionBytes));

        List<SessionEvent> events = List.of(SessionEvent.builder()
            .sessionId(SESSION_ID)
            .message(new UserMessage("x"))
            .build());

        long staleVersion = VERSION - 1L;

        boolean result = repository.applyCompaction(SESSION_ID, appendPlan(events), staleVersion);

        assertFalse(result);

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testSaveOfNewSessionSendsIfNoneMatchPrecondition() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenThrow(NoSuchKeyException.builder()
                .build());

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(PutObjectResponse.builder()
                .build());

        repository.save(newSession());

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest putObjectRequest = requestCaptor.getValue();

        assertEquals("*", putObjectRequest.ifNoneMatch());
        assertNull(putObjectRequest.ifMatch());
    }

    @Test
    void testSaveRetriesWithIfMatchWhenConcurrentCreateWins() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenThrow(NoSuchKeyException.builder()
                .build())
            .thenReturn(ResponseBytes.fromByteArray(
                GetObjectResponse.builder()
                    .eTag(ETAG)
                    .build(),
                storedSessionBytes));

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(S3Exception.builder()
                .statusCode(412)
                .message("PreconditionFailed")
                .build())
            .thenReturn(PutObjectResponse.builder()
                .build());

        repository.save(newSession());

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        verify(s3Client, times(2)).putObject(requestCaptor.capture(), any(RequestBody.class));

        List<PutObjectRequest> putObjectRequests = requestCaptor.getAllValues();

        assertEquals("*", putObjectRequests.get(0)
            .ifNoneMatch());
        assertEquals(ETAG, putObjectRequests.get(1)
            .ifMatch());
    }

    @Test
    void testSaveRecreatesSessionDeletedBetweenReadAndConditionalPut() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenReturn(storedSessionResponse(ETAG, storedSessionBytes))
            .thenThrow(NoSuchKeyException.builder()
                .build());
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(NoSuchKeyException.builder()
                .statusCode(404)
                .message("NoSuchKey")
                .build())
            .thenReturn(PutObjectResponse.builder()
                .build());

        repository.save(newSession());

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        verify(s3Client, times(2)).putObject(requestCaptor.capture(), any(RequestBody.class));

        List<PutObjectRequest> putObjectRequests = requestCaptor.getAllValues();

        assertEquals(ETAG, putObjectRequests.get(0)
            .ifMatch());
        assertEquals("*", putObjectRequests.get(1)
            .ifNoneMatch());
        assertEquals(1, recordedDelays.size());
    }

    @Test
    void testAppendEventOnSessionDeletedBetweenReadAndConditionalPutThrowsSessionNotFound() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenReturn(storedSessionResponse(ETAG, storedSessionBytes))
            .thenThrow(NoSuchKeyException.builder()
                .build());
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(NoSuchKeyException.builder()
                .statusCode(404)
                .message("NoSuchKey")
                .build());

        assertThrows(
            IllegalArgumentException.class, () -> repository.appendEvent(SessionEvent.builder()
                .sessionId(SESSION_ID)
                .message(new UserMessage("x"))
                .build()));
    }

    @Test
    void testAppendEventOnDeletedBucketFailsWithoutRetrying() {
        stubStoredSession();

        S3Exception noSuchBucketException = s3Exception(404, "NoSuchBucket");

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(noSuchBucketException);

        assertSame(
            noSuchBucketException, assertThrows(
                S3Exception.class, () -> repository.appendEvent(SessionEvent.builder()
                    .sessionId(SESSION_ID)
                    .message(new UserMessage("x"))
                    .build())));

        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        assertTrue(recordedDelays.isEmpty());
    }

    @Test
    void testAppendEventTreatsNoSuchKeyErrorCodeAsSessionDeletedBetweenReadAndConditionalPut() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenReturn(storedSessionResponse(ETAG, storedSessionBytes))
            .thenThrow(NoSuchKeyException.builder()
                .build());
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(s3Exception(404, "NoSuchKey"));

        assertThrows(
            IllegalArgumentException.class, () -> repository.appendEvent(SessionEvent.builder()
                .sessionId(SESSION_ID)
                .message(new UserMessage("x"))
                .build()));
    }

    @Test
    void testDeleteExpiredSessionsOnDeletedBucketFails() {
        stubExpiredSessions(SESSION_ID);

        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenThrow(s3Exception(404, "NoSuchBucket"));

        assertThrows(S3Exception.class, () -> repository.deleteExpiredSessions(Instant.now()));
    }

    @Test
    void testSaveIfAbsentReturnsFalseWithoutReadingWhenSessionExists() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(preconditionFailed());

        assertFalse(repository.saveIfAbsent(newSession()));

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        verify(s3Client, never()).getObjectAsBytes(any(GetObjectRequest.class));

        assertEquals("*", requestCaptor.getValue()
            .ifNoneMatch());
    }

    @Test
    void testSaveIfAbsentRetriesConditionalRequestConflict() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(conditionalRequestConflict())
            .thenReturn(PutObjectResponse.builder()
                .build());

        assertTrue(repository.saveIfAbsent(newSession()));

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        verify(s3Client, times(2)).putObject(requestCaptor.capture(), any(RequestBody.class));

        for (PutObjectRequest putObjectRequest : requestCaptor.getAllValues()) {
            assertEquals("*", putObjectRequest.ifNoneMatch());
        }

        assertEquals(1, recordedDelays.size());
    }

    @Test
    void testSaveIfAbsentReturnsFalseWhenSessionAppearsAfterConflict() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(conditionalRequestConflict())
            .thenThrow(preconditionFailed());

        assertFalse(repository.saveIfAbsent(newSession()));

        verify(s3Client, times(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testSaveIfAbsentGivesUpAfterEveryAttemptConflicts() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(conditionalRequestConflict());

        IllegalStateException exception = assertThrows(
            IllegalStateException.class, () -> repository.saveIfAbsent(newSession()));

        assertTrue(exception.getMessage()
            .contains("concurrent writers"));

        verify(s3Client, times(ConditionalWriteRetry.DEFAULT_MAX_ATTEMPTS))
            .putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {
        403, 500
    })
    void testSaveIfAbsentRethrowsNonConflictPutErrorsWithoutRetrying(int statusCode) {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(s3Exception(statusCode));

        S3Exception exception = assertThrows(S3Exception.class, () -> repository.saveIfAbsent(newSession()));

        assertEquals(statusCode, exception.statusCode());
        assertTrue(recordedDelays.isEmpty());
    }

    @Test
    void testDeleteExpiredSessionsSkipsSessionWhenConditionalDeleteFails() {
        StoredSession expiredSession = new StoredSession(
            SESSION_ID, "user-1", System.currentTimeMillis() - 120_000L, System.currentTimeMillis() - 60_000L,
            Map.of(), VERSION, 0L, List.of());

        byte[] expiredSessionBytes = JsonMapper.builder()
            .build()
            .writeValueAsBytes(expiredSession);

        ListObjectsV2Iterable listObjectsV2Iterable = mock(ListObjectsV2Iterable.class);
        S3Object s3Object = S3Object.builder()
            .key(SESSION_ID + ".json")
            .build();

        when(listObjectsV2Iterable.contents()).thenReturn(() -> List.of(s3Object)
            .iterator());
        when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class))).thenReturn(listObjectsV2Iterable);
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenAnswer(invocation -> ResponseBytes.fromByteArray(
                GetObjectResponse.builder()
                    .eTag(ETAG)
                    .build(),
                expiredSessionBytes));
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
            .thenThrow(S3Exception.builder()
                .statusCode(412)
                .message("PreconditionFailed")
                .build());

        assertEquals(0, repository.deleteExpiredSessions(Instant.now()));

        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);

        verify(s3Client).deleteObject(requestCaptor.capture());

        DeleteObjectRequest deleteObjectRequest = requestCaptor.getValue();

        assertEquals(ETAG, deleteObjectRequest.ifMatch());
    }

    @Test
    void testApplyCompactionReturnsFalseWhenPutThrows409() {
        stubStoredSession();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(conditionalRequestConflict());

        List<SessionEvent> events = List.of(SessionEvent.builder()
            .sessionId(SESSION_ID)
            .message(new UserMessage("x"))
            .build());

        assertFalse(repository.applyCompaction(SESSION_ID, appendPlan(events), VERSION));
    }

    @Test
    void testAppendEventSendsIfMatchAndRetriesAfterPreconditionFailure() {
        stubStoredSession();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(S3Exception.builder()
                .statusCode(412)
                .message("PreconditionFailed")
                .build())
            .thenReturn(PutObjectResponse.builder()
                .build());

        repository.appendEvent(SessionEvent.builder()
            .sessionId(SESSION_ID)
            .message(new UserMessage("x"))
            .build());

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        verify(s3Client, times(2)).putObject(requestCaptor.capture(), any(RequestBody.class));

        for (PutObjectRequest putObjectRequest : requestCaptor.getAllValues()) {
            assertEquals(ETAG, putObjectRequest.ifMatch());
            assertNull(putObjectRequest.ifNoneMatch());
        }
    }

    @Test
    void testAppendEventRetriesAfterConditionalRequestConflict() {
        stubStoredSession();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(conditionalRequestConflict())
            .thenReturn(PutObjectResponse.builder()
                .build());

        repository.appendEvent(SessionEvent.builder()
            .sessionId(SESSION_ID)
            .message(new UserMessage("x"))
            .build());

        verify(s3Client, times(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testDeleteExpiredSessionsSkipsSessionWhenConditionalDeleteConflicts() {
        StoredSession expiredSession = new StoredSession(
            SESSION_ID, "user-1", System.currentTimeMillis() - 120_000L, System.currentTimeMillis() - 60_000L,
            Map.of(), VERSION, 0L, List.of());

        byte[] expiredSessionBytes = JsonMapper.builder()
            .build()
            .writeValueAsBytes(expiredSession);

        ListObjectsV2Iterable listObjectsV2Iterable = mock(ListObjectsV2Iterable.class);
        S3Object s3Object = S3Object.builder()
            .key(SESSION_ID + ".json")
            .build();

        when(listObjectsV2Iterable.contents()).thenReturn(() -> List.of(s3Object)
            .iterator());
        when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class))).thenReturn(listObjectsV2Iterable);
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenAnswer(invocation -> ResponseBytes.fromByteArray(
                GetObjectResponse.builder()
                    .eTag(ETAG)
                    .build(),
                expiredSessionBytes));
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenThrow(conditionalRequestConflict());

        assertEquals(0, repository.deleteExpiredSessions(Instant.now()));
    }

    @Test
    void testListingsSkipUnreadableDocuments() {
        ListObjectsV2Iterable listObjectsV2Iterable = mock(ListObjectsV2Iterable.class);
        List<S3Object> s3Objects = List.of(
            S3Object.builder()
                .key("garbage.json")
                .build(),
            S3Object.builder()
                .key("invalid.json")
                .build(),
            S3Object.builder()
                .key(SESSION_ID + ".json")
                .build());

        when(listObjectsV2Iterable.contents()).thenReturn(s3Objects::iterator);
        when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class))).thenReturn(listObjectsV2Iterable);
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenAnswer(invocation -> {
            GetObjectRequest getObjectRequest = invocation.getArgument(0);

            byte[] content = switch (getObjectRequest.key()) {
                case "garbage.json" -> "not json".getBytes(StandardCharsets.UTF_8);
                case "invalid.json" -> "{\"id\":\"invalid\",\"version\":0,\"events\":[]}"
                    .getBytes(StandardCharsets.UTF_8);
                default -> storedSessionBytes;
            };

            return ResponseBytes.fromByteArray(
                GetObjectResponse.builder()
                    .eTag(ETAG)
                    .build(),
                content);
        });

        assertEquals(List.of(SESSION_ID), repository.findByUserId("user-1")
            .stream()
            .map(Session::id)
            .toList());
        assertTrue(repository.findEventsByUserId("user-1", EventFilter.all())
            .isEmpty());
        assertEquals(0, repository.deleteExpiredSessions(Instant.now()));
    }

    @Test
    void testBuilderRejectsMissingSettings() {
        assertThrows(
            IllegalArgumentException.class, () -> S3SessionRepository.builder()
                .s3Client(s3Client)
                .bucketName(BUCKET)
                .keyPrefix(null)
                .build());
        assertThrows(
            IllegalArgumentException.class, () -> S3SessionRepository.builder()
                .s3Client(s3Client)
                .bucketName(BUCKET)
                .jsonMapper(null)
                .build());
        assertThrows(
            IllegalArgumentException.class, () -> S3SessionRepository.builder()
                .s3Client(s3Client)
                .bucketName(BUCKET)
                .conditionalWriteRetry(null)
                .build());
    }

    @Test
    void testAppendEventRetryRereadsTheDocumentAndUsesTheNewEtag() throws IOException {
        StoredSession concurrentlyUpdatedSession = new StoredSession(
            SESSION_ID, "user-1", System.currentTimeMillis(), null, Map.of("title", "updated"), VERSION, 1L,
            List.of());

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenReturn(storedSessionResponse(ETAG, storedSessionBytes))
            .thenReturn(storedSessionResponse(UPDATED_ETAG, jsonMapper.writeValueAsBytes(concurrentlyUpdatedSession)));
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(preconditionFailed())
            .thenReturn(PutObjectResponse.builder()
                .build());

        SessionEvent sessionEvent = SessionEvent.builder()
            .sessionId(SESSION_ID)
            .message(new UserMessage("x"))
            .build();

        repository.appendEvent(sessionEvent);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);

        verify(s3Client, times(2)).getObjectAsBytes(any(GetObjectRequest.class));
        verify(s3Client, times(2)).putObject(requestCaptor.capture(), bodyCaptor.capture());

        assertEquals(ETAG, requestCaptor.getAllValues()
            .get(0)
            .ifMatch());
        assertEquals(UPDATED_ETAG, requestCaptor.getAllValues()
            .get(1)
            .ifMatch());

        StoredSession firstDocument = readBody(bodyCaptor.getAllValues()
            .get(0));
        StoredSession secondDocument = readBody(bodyCaptor.getAllValues()
            .get(1));

        assertEquals(VERSION + 1, firstDocument.version());
        assertEquals(1L, firstDocument.revision());
        assertEquals(VERSION + 1, secondDocument.version());
        assertEquals(2L, secondDocument.revision());
        assertEquals("updated", secondDocument.metadata()
            .get("title"));
        assertTrue(secondDocument.containsEvent(sessionEvent.getId()));
        assertEquals(1, secondDocument.events()
            .size());
    }

    @Test
    void testAppendEventStopsWhenTheRetryFindsTheEventAlreadyWritten() {
        SessionEvent sessionEvent = SessionEvent.builder()
            .sessionId(SESSION_ID)
            .message(new UserMessage("x"))
            .build();

        StoredSession storedSession = jsonMapper.readValue(storedSessionBytes, StoredSession.class);

        StoredSession writtenSession = storedSession.appendEvent(
            StoredEvent.fromEvent(sessionEvent, jsonMapper), StoredSession.DEFAULT_MAX_ARCHIVED_EVENTS);

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenReturn(storedSessionResponse(ETAG, storedSessionBytes))
            .thenReturn(storedSessionResponse(UPDATED_ETAG, jsonMapper.writeValueAsBytes(writtenSession)));
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(preconditionFailed());

        repository.appendEvent(sessionEvent);

        verify(s3Client, times(2)).getObjectAsBytes(any(GetObjectRequest.class));
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {
        403, 500
    })
    void testAppendEventRethrowsNonConflictPutErrorsWithoutRetrying(int statusCode) {
        stubStoredSession();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(s3Exception(statusCode));

        S3Exception exception = assertThrows(
            S3Exception.class, () -> repository.appendEvent(SessionEvent.builder()
                .sessionId(SESSION_ID)
                .message(new UserMessage("x"))
                .build()));

        assertEquals(statusCode, exception.statusCode());

        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        assertTrue(recordedDelays.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {
        403, 500
    })
    void testSaveRethrowsNonConflictPutErrorsWithoutRetrying(int statusCode) {
        stubStoredSession();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(s3Exception(statusCode));

        S3Exception exception = assertThrows(S3Exception.class, () -> repository.save(newSession()));

        assertEquals(statusCode, exception.statusCode());

        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {
        403, 500
    })
    void testDeleteExpiredSessionsAbortsOnNonConflictDeleteErrors(int statusCode) {
        stubExpiredSessions("failing", "deletable");

        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenAnswer(invocation -> {
            DeleteObjectRequest deleteObjectRequest = invocation.getArgument(0);

            if ("failing.json".equals(deleteObjectRequest.key())) {
                throw s3Exception(statusCode);
            }

            return DeleteObjectResponse.builder()
                .build();
        });

        S3Exception exception = assertThrows(
            S3Exception.class, () -> repository.deleteExpiredSessions(Instant.now()));

        assertEquals(statusCode, exception.statusCode());

        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void testReadDeniedAccessFailsNamingTheObjectAndEveryPermissionThatCanDenyIt() {
        S3Exception forbiddenException = s3Exception(403);

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(forbiddenException);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class, () -> repository.findById(SESSION_ID));

        assertTrue(exception.getMessage()
            .contains("s3://" + BUCKET + "/" + SESSION_ID + ".json"), exception.getMessage());
        assertTrue(exception.getMessage()
            .contains("s3:GetObject"), exception.getMessage());
        assertTrue(exception.getMessage()
            .contains("kms:Decrypt"), exception.getMessage());
        assertTrue(exception.getMessage()
            .contains("s3:ListBucket"), exception.getMessage());
        assertSame(forbiddenException, exception.getCause());
    }

    @Test
    void testReadServerErrorIsRethrown() {
        S3Exception serverException = s3Exception(500);

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(serverException);

        assertSame(serverException, assertThrows(S3Exception.class, () -> repository.findById(SESSION_ID)));
    }

    @Test
    void testReadOfObjectWithoutEtagFailsNamingBucketAndKey() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(storedSessionResponse(
            null, storedSessionBytes));

        IllegalStateException exception = assertThrows(
            IllegalStateException.class, () -> repository.findById(SESSION_ID));

        assertTrue(exception.getMessage()
            .contains("s3://" + BUCKET + "/" + SESSION_ID + ".json"), exception.getMessage());
    }

    @Test
    void testAppendEventToObjectWithoutEtagWritesNothing() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(storedSessionResponse(
            null, storedSessionBytes));

        assertThrows(
            IllegalStateException.class, () -> repository.appendEvent(SessionEvent.builder()
                .sessionId(SESSION_ID)
                .message(new UserMessage("x"))
                .build()));

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testDeleteExpiredSessionsNeverDeletesObjectWithoutEtag() {
        stubExpiredSessions(SESSION_ID);

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(storedSessionResponse(
            null, expiredSessionBytes(SESSION_ID)));

        assertThrows(IllegalStateException.class, () -> repository.deleteExpiredSessions(Instant.now()));

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void testDeleteExpiredSessionsTreatsAnAlreadyDeletedSessionAsNotDeleted() {
        stubExpiredSessions(SESSION_ID);

        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenThrow(NoSuchKeyException.builder()
            .statusCode(404)
            .message("NoSuchKey")
            .build());

        assertEquals(0, repository.deleteExpiredSessions(Instant.now()));
        assertTrue(errorEvents().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {
        403, 500
    })
    void testDeleteExpiredSessionsAbortsOnReadErrors(int statusCode) {
        stubExpiredSessions("unreadable", "deletable");

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenAnswer(invocation -> {
            GetObjectRequest getObjectRequest = invocation.getArgument(0);

            if ("unreadable.json".equals(getObjectRequest.key())) {
                throw s3Exception(statusCode);
            }

            return storedSessionResponse(ETAG, expiredSessionBytes("deletable"));
        });

        assertThrows(RuntimeException.class, () -> repository.deleteExpiredSessions(Instant.now()));

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
        assertTrue(errorEvents().isEmpty());
    }

    @Test
    void testAppendEventPutsTheBumpedDocumentWithTheEvent() throws IOException {
        stubStoredSession();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(PutObjectResponse.builder()
                .build());

        SessionEvent sessionEvent = SessionEvent.builder()
            .sessionId(SESSION_ID)
            .message(new UserMessage("x"))
            .build();

        repository.appendEvent(sessionEvent);

        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);

        verify(s3Client).putObject(any(PutObjectRequest.class), bodyCaptor.capture());

        StoredSession putDocument = readBody(bodyCaptor.getValue());

        assertEquals(VERSION + 1, putDocument.version());
        assertEquals(1L, putDocument.revision());
        assertTrue(putDocument.containsEvent(sessionEvent.getId()));
    }

    @Test
    void testApplyCompactionPutsTheBumpedDocumentWithTheInsertedEvents() throws IOException {
        stubStoredSession();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(PutObjectResponse.builder()
                .build());

        SessionEvent insertedEvent = SessionEvent.builder()
            .sessionId(SESSION_ID)
            .message(new UserMessage("summary"))
            .build();

        assertTrue(repository.applyCompaction(SESSION_ID, appendPlan(List.of(insertedEvent)), VERSION));

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);

        verify(s3Client).putObject(requestCaptor.capture(), bodyCaptor.capture());

        StoredSession putDocument = readBody(bodyCaptor.getValue());

        assertEquals(ETAG, requestCaptor.getValue()
            .ifMatch());
        assertEquals(VERSION + 1, putDocument.version());
        assertEquals(1L, putDocument.revision());
        assertTrue(putDocument.containsEvent(insertedEvent.getId()));
    }

    @Test
    void testApplyCompactionPrunesArchivedEventsBeyondTheLimit() throws IOException {
        List<StoredEvent> storedEvents = new ArrayList<>();

        for (int index = 0; index < 4; index++) {
            storedEvents.add(new StoredEvent(
                "archived-" + index, SESSION_ID, index, MessageType.USER, "old " + index, null, false, true, Map.of()));
        }

        storedEvents.add(
            new StoredEvent("active", SESSION_ID, 10L, MessageType.USER, "active", null, false, false, Map.of()));

        StoredSession storedSession = new StoredSession(
            SESSION_ID, "user-1", System.currentTimeMillis(), null, Map.of(), VERSION, 0L, storedEvents);

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenReturn(storedSessionResponse(ETAG, jsonMapper.writeValueAsBytes(storedSession)));
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(PutObjectResponse.builder()
                .build());

        S3SessionRepository limitedRepository = S3SessionRepository.builder()
            .s3Client(s3Client)
            .bucketName(BUCKET)
            .maxArchivedEvents(2)
            .build();

        CompactionPlan archiveActivePlan = new CompactionPlan(Set.of("active"), List.of());

        assertTrue(limitedRepository.applyCompaction(SESSION_ID, archiveActivePlan, VERSION));

        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);

        verify(s3Client).putObject(any(PutObjectRequest.class), bodyCaptor.capture());

        StoredSession putDocument = readBody(bodyCaptor.getValue());

        assertEquals(List.of("archived-3", "active"), putDocument.events()
            .stream()
            .map(StoredEvent::id)
            .toList());
    }

    @Test
    void testBuilderRejectsNegativeMaxArchivedEvents() {
        assertThrows(
            IllegalArgumentException.class, () -> S3SessionRepository.builder()
                .s3Client(s3Client)
                .bucketName(BUCKET)
                .maxArchivedEvents(-1)
                .build());
    }

    private List<ILoggingEvent> errorEvents() {
        return logAppender.list.stream()
            .filter(loggingEvent -> loggingEvent.getLevel() == Level.ERROR)
            .toList();
    }

    private StoredSession readBody(RequestBody requestBody) throws IOException {
        try (InputStream inputStream = requestBody.contentStreamProvider()
            .newStream()) {

            return jsonMapper.readValue(inputStream.readAllBytes(), StoredSession.class);
        }
    }

    private static ResponseBytes<GetObjectResponse> storedSessionResponse(String etag, byte[] content) {
        return ResponseBytes.fromByteArray(
            GetObjectResponse.builder()
                .eTag(etag)
                .build(),
            content);
    }

    private static S3Exception s3Exception(int statusCode) {
        return (S3Exception) S3Exception.builder()
            .statusCode(statusCode)
            .message("status " + statusCode)
            .build();
    }

    private static S3Exception s3Exception(int statusCode, String errorCode) {
        return (S3Exception) S3Exception.builder()
            .statusCode(statusCode)
            .awsErrorDetails(AwsErrorDetails.builder()
                .errorCode(errorCode)
                .build())
            .build();
    }

    private static S3Exception preconditionFailed() {
        return (S3Exception) S3Exception.builder()
            .statusCode(412)
            .message("PreconditionFailed")
            .build();
    }

    private static S3Exception conditionalRequestConflict() {
        return (S3Exception) S3Exception.builder()
            .statusCode(409)
            .message("ConditionalRequestConflict")
            .build();
    }

    private void stubExpiredSessions(String... sessionIds) {
        List<S3Object> s3Objects = new ArrayList<>();

        for (String sessionId : sessionIds) {
            s3Objects.add(S3Object.builder()
                .key(sessionId + ".json")
                .build());
        }

        ListObjectsV2Iterable listObjectsV2Iterable = mock(ListObjectsV2Iterable.class);

        when(listObjectsV2Iterable.contents()).thenReturn(s3Objects::iterator);
        when(s3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class))).thenReturn(listObjectsV2Iterable);
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenAnswer(invocation -> {
            GetObjectRequest getObjectRequest = invocation.getArgument(0);

            String objectKey = getObjectRequest.key();

            return storedSessionResponse(
                ETAG, expiredSessionBytes(objectKey.substring(0, objectKey.length() - ".json".length())));
        });
    }

    private byte[] expiredSessionBytes(String sessionId) {
        return jsonMapper.writeValueAsBytes(new StoredSession(
            sessionId, "user-1", System.currentTimeMillis() - 120_000L, System.currentTimeMillis() - 60_000L,
            Map.of(), VERSION, 0L, List.of()));
    }

    private void stubStoredSession() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenAnswer(invocation -> ResponseBytes.fromByteArray(
                GetObjectResponse.builder()
                    .eTag(ETAG)
                    .build(),
                storedSessionBytes));
    }

    private static CompactionPlan appendPlan(List<SessionEvent> events) {
        return new CompactionPlan(Set.of(), List.of(CompactionPlan.Insert.atEnd(events)));
    }

    private static Session newSession() {
        return Session.builder()
            .id(SESSION_ID)
            .userId("user-1")
            .createdAt(Instant.now())
            .build();
    }
}
