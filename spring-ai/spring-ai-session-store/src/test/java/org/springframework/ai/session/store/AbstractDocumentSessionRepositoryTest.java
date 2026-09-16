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

package org.springframework.ai.session.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class AbstractDocumentSessionRepositoryTest {

    private static final Logger log = (Logger) LoggerFactory.getLogger(AbstractDocumentSessionRepository.class);

    private final Map<String, String> documents = new TreeMap<>();
    private final Set<String> failingDeleteKeys = new HashSet<>();
    private final Set<String> failingLoadKeys = new HashSet<>();
    private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

    private InMemorySessionRepository repository;

    @BeforeEach
    void setUp() {
        documents.clear();
        failingDeleteKeys.clear();
        failingLoadKeys.clear();

        repository = new InMemorySessionRepository(documents, failingLoadKeys, failingDeleteKeys);

        logAppender.start();

        log.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        log.detachAppender(logAppender);

        logAppender.stop();
    }

    @Test
    void testEveryScanLogsOneErrorNamingTheUnreadableDocuments() {
        repository.save(Session.builder()
            .id("s-readable")
            .userId("user-1")
            .createdAt(Instant.now())
            .build());

        documents.put("s-garbage", "not json");
        documents.put("s-invalid", "{\"id\":\"s-invalid\",\"version\":0,\"events\":[]}");

        assertEquals(1, repository.findByUserId("user-1")
            .size());
        assertTrue(repository.findEventsByUserId("user-1", EventFilter.all())
            .isEmpty());
        assertEquals(0, repository.deleteExpiredSessions(Instant.now()));

        List<ILoggingEvent> errorEvents = errorEvents();

        assertEquals(3, errorEvents.size());

        for (ILoggingEvent errorEvent : errorEvents) {
            String formattedMessage = errorEvent.getFormattedMessage();

            assertTrue(formattedMessage.contains(" 2 "), formattedMessage);
            assertTrue(formattedMessage.contains("s-garbage"), formattedMessage);
            assertTrue(formattedMessage.contains("s-invalid"), formattedMessage);
        }
    }

    @Test
    void testScanOfReadableDocumentsLogsNoError() {
        repository.save(Session.builder()
            .id("s-readable")
            .userId("user-1")
            .createdAt(Instant.now())
            .build());

        assertEquals(1, repository.findByUserId("user-1")
            .size());
        assertTrue(errorEvents().isEmpty());
    }

    @Test
    void testDeleteExpiredSessionsAbortsWhenTheStoreFailsWhileLoading() {
        saveExpiredSession("s-a");
        saveExpiredSession("s-b");
        saveExpiredSession("s-c");

        failingLoadKeys.add("s-a");

        assertThrows(UncheckedIOException.class, () -> repository.deleteExpiredSessions(Instant.now()));
        assertEquals(Set.of("s-a", "s-b", "s-c"), documents.keySet());
        assertTrue(errorEvents().isEmpty());
        assertTrue(warnEvents().isEmpty());
    }

    @Test
    void testDeleteExpiredSessionsAbortsWhenTheStoreFailsWhileDeleting() {
        saveExpiredSession("s-a");
        saveExpiredSession("s-b");

        failingDeleteKeys.add("s-a");

        assertThrows(UncheckedIOException.class, () -> repository.deleteExpiredSessions(Instant.now()));
        assertEquals(Set.of("s-a", "s-b"), documents.keySet());
        assertTrue(errorEvents().isEmpty());
    }

    @Test
    void testDeleteExpiredSessionsSkipsCorruptDocumentAndDeletesTheRest() {
        saveExpiredSession("s-b");

        documents.put("s-a", "not json");

        assertEquals(1, repository.deleteExpiredSessions(Instant.now()));
        assertEquals(Set.of("s-a"), documents.keySet());

        List<ILoggingEvent> errorEvents = errorEvents();

        assertEquals(1, errorEvents.size());
        assertTrue(errorEvents.get(0)
            .getFormattedMessage()
            .contains("[s-a]"));
    }

    @Test
    void testListingsAbortWhenTheStoreFailsWhileLoading() {
        saveExpiredSession("s-a");
        saveExpiredSession("s-b");

        failingLoadKeys.add("s-a");

        assertThrows(UncheckedIOException.class, () -> repository.findByUserId("user-1"));
        assertThrows(
            UncheckedIOException.class, () -> repository.findEventsByUserId("user-1", EventFilter.all()));
    }

    @Test
    void testSkippedDocumentLogNamesOnlyTheFirstTwentyKeys() {
        for (int index = 10; index < 35; index++) {
            documents.put("s-garbage-" + index, "not json");
        }

        assertTrue(repository.findByUserId("user-1")
            .isEmpty());

        List<ILoggingEvent> errorEvents = errorEvents();

        assertEquals(1, errorEvents.size());

        String formattedMessage = errorEvents.get(0)
            .getFormattedMessage();

        assertTrue(formattedMessage.contains(" 25 "), formattedMessage);
        assertTrue(formattedMessage.contains("s-garbage-29"), formattedMessage);
        assertFalse(formattedMessage.contains("s-garbage-30"), formattedMessage);
    }

    @Test
    void testReadOfDocumentHoldingAnotherSessionFailsNamingTheKey() {
        saveExpiredSession("s-b");

        documents.put("s-a", documents.get("s-b"));

        UnreadableSessionDocumentException exception = assertThrows(
            UnreadableSessionDocumentException.class, () -> repository.findById("s-a"));

        assertTrue(exception.getMessage()
            .contains("s-a"), exception.getMessage());
    }

    @Test
    void testAppendToDocumentHoldingAnotherSessionDoesNotWriteEitherSession() {
        saveExpiredSession("s-b");

        String sessionB = documents.get("s-b");

        documents.put("s-a", sessionB);

        assertThrows(
            IllegalStateException.class, () -> repository.appendEvent(SessionEvent.builder()
                .sessionId("s-a")
                .message(new UserMessage("misrouted"))
                .build()));
        assertEquals(sessionB, documents.get("s-a"));
        assertEquals(sessionB, documents.get("s-b"));
    }

    @Test
    void testSweepSkipsDocumentHoldingAnotherSession() {
        saveExpiredSession("s-b");

        documents.put("s-a", documents.get("s-b"));

        assertEquals(1, repository.deleteExpiredSessions(Instant.now()));
        assertTrue(documents.containsKey("s-a"));
        assertFalse(documents.containsKey("s-b"));

        List<ILoggingEvent> errorEvents = errorEvents();

        assertEquals(1, errorEvents.size());
        assertTrue(errorEvents.get(0)
            .getFormattedMessage()
            .contains("[s-a]"));
    }

    @Test
    void testLoadedSessionRequiresCasToken() {
        StoredSession document = new StoredSession("s-a", "user-1", 1L, null, Map.of(), 0L, 0L, List.of());

        assertThrows(
            NullPointerException.class, () -> new AbstractDocumentSessionRepository.LoadedSession<>(document, null));
    }

    @Test
    void testConstructorRejectsInvalidSettings() {
        JsonMapper jsonMapper = JsonMapper.builder()
            .build();
        ConditionalWriteRetry conditionalWriteRetry = ConditionalWriteRetry.defaults();

        assertThrows(
            NullPointerException.class,
            () -> new MinimalSessionRepository(null, conditionalWriteRetry, StoredSession.DEFAULT_MAX_ARCHIVED_EVENTS));
        assertThrows(
            NullPointerException.class,
            () -> new MinimalSessionRepository(jsonMapper, null, StoredSession.DEFAULT_MAX_ARCHIVED_EVENTS));
        assertThrows(
            IllegalArgumentException.class, () -> new MinimalSessionRepository(jsonMapper, conditionalWriteRetry, -1));
    }

    private void saveExpiredSession(String sessionId) {
        repository.save(Session.builder()
            .id(sessionId)
            .userId("user-1")
            .createdAt(Instant.now()
                .minusSeconds(120))
            .expiresAt(Instant.now()
                .minusSeconds(60))
            .build());
    }

    private List<ILoggingEvent> errorEvents() {
        return logAppender.list.stream()
            .filter(loggingEvent -> loggingEvent.getLevel() == Level.ERROR)
            .toList();
    }

    private List<ILoggingEvent> warnEvents() {
        return logAppender.list.stream()
            .filter(loggingEvent -> loggingEvent.getLevel() == Level.WARN)
            .toList();
    }

    private static final class MinimalSessionRepository extends AbstractDocumentSessionRepository<String> {

        private MinimalSessionRepository(
            JsonMapper jsonMapper, ConditionalWriteRetry conditionalWriteRetry, int maxArchivedEvents) {

            super(jsonMapper, conditionalWriteRetry, maxArchivedEvents);
        }

        @Override
        protected String documentKey(String sessionId) {
            return sessionId;
        }

        @Override
        protected List<String> listDocumentKeys() {
            return List.of();
        }

        @Override
        protected LoadedSession<String> loadDocument(String documentKey) {
            return null;
        }

        @Override
        protected boolean tryPut(StoredSession document, LoadedSession<String> current) {
            return false;
        }

        @Override
        protected boolean tryDeleteIfExpired(LoadedSession<String> loadedSession, Instant before) {
            return false;
        }

        @Override
        protected void deleteDocument(String documentKey) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class InMemorySessionRepository extends AbstractDocumentSessionRepository<String> {

        private final Map<String, String> documents;
        private final Set<String> failingDeleteKeys;
        private final Set<String> failingLoadKeys;

        private InMemorySessionRepository(
            Map<String, String> documents, Set<String> failingLoadKeys, Set<String> failingDeleteKeys) {

            super(
                JsonMapper.builder()
                    .build(),
                ConditionalWriteRetry.defaults(), StoredSession.DEFAULT_MAX_ARCHIVED_EVENTS);

            this.documents = documents;
            this.failingDeleteKeys = failingDeleteKeys;
            this.failingLoadKeys = failingLoadKeys;
        }

        @Override
        protected String documentKey(String sessionId) {
            return sessionId;
        }

        @Override
        protected List<String> listDocumentKeys() {
            return new ArrayList<>(documents.keySet());
        }

        @Override
        protected LoadedSession<String> loadDocument(String documentKey) {
            if (failingLoadKeys.contains(documentKey)) {
                throw new UncheckedIOException(new IOException("Simulated store outage reading " + documentKey));
            }

            String json = documents.get(documentKey);

            if (json == null) {
                return null;
            }

            return new LoadedSession<>(jsonMapper().readValue(json, StoredSession.class), json);
        }

        @Override
        protected boolean tryPut(StoredSession document, LoadedSession<String> current) {
            String key = documentKey(document.id());

            String expectedJson = current == null ? null : current.casToken();

            if (!Objects.equals(documents.get(key), expectedJson)) {
                return false;
            }

            documents.put(key, jsonMapper().writeValueAsString(document));

            return true;
        }

        @Override
        protected boolean tryDeleteIfExpired(LoadedSession<String> loadedSession, Instant before) {
            StoredSession document = loadedSession.document();

            if (failingDeleteKeys.contains(document.id())) {
                throw new UncheckedIOException(new IOException("Simulated store outage deleting " + document.id()));
            }

            return documents.remove(documentKey(document.id()), loadedSession.casToken());
        }

        @Override
        protected void deleteDocument(String documentKey) {
            documents.remove(documentKey);
        }
    }
}
