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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.compaction.CompactionPlan;
import org.springframework.ai.session.store.StoredSession.StoredEvent;
import org.springframework.util.Assert;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractDocumentSessionRepository<T> implements SessionRepository {

    private static final Logger log = LoggerFactory.getLogger(AbstractDocumentSessionRepository.class);

    private static final int MAX_LOGGED_FAILED_DOCUMENT_KEYS = 20;

    private final ConditionalWriteRetry conditionalWriteRetry;
    private final JsonMapper jsonMapper;
    private final int maxArchivedEvents;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    protected AbstractDocumentSessionRepository(
        JsonMapper jsonMapper, ConditionalWriteRetry conditionalWriteRetry, int maxArchivedEvents) {

        if (maxArchivedEvents < 0) {
            throw new IllegalArgumentException("maxArchivedEvents must not be negative");
        }

        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper must not be null");
        this.conditionalWriteRetry = Objects.requireNonNull(
            conditionalWriteRetry, "conditionalWriteRetry must not be null");
        this.maxArchivedEvents = maxArchivedEvents;
    }

    @Override
    public final Session save(Session session) {
        Assert.notNull(session, "session must not be null");

        return conditionalWriteRetry.call("save", session.id(), () -> trySave(session));
    }

    @Override
    public final boolean saveIfAbsent(Session session) {
        Assert.notNull(session, "session must not be null");

        StoredSession document = StoredSession.fromSession(session, 0L, List.of());

        return conditionalWriteRetry.call("create", session.id(), () -> switch (tryCreate(document)) {
            case CREATED -> Optional.of(Boolean.TRUE);
            case ALREADY_EXISTS -> Optional.of(Boolean.FALSE);
            case CONFLICT -> Optional.empty();
        });
    }

    @Override
    @Nullable
    public final Session findById(String sessionId) {
        Assert.hasText(sessionId, "sessionId must not be null or empty");

        LoadedSession<T> loadedSession = load(sessionId);

        return loadedSession == null ? null : loadedSession.document()
            .toSession();
    }

    @Override
    public final List<Session> findByUserId(String userId) {
        Assert.hasText(userId, "userId must not be null or empty");

        List<Session> sessions = new ArrayList<>();

        forEachDocument("findByUserId", loadedSession -> {
            StoredSession document = loadedSession.document();

            if (userId.equals(document.userId())) {
                sessions.add(document.toSession());
            }
        });

        return sessions;
    }

    @Override
    public final int deleteExpiredSessions(Instant before) {
        Assert.notNull(before, "before must not be null");

        AtomicInteger deletedCount = new AtomicInteger();

        forEachDocument("deleteExpiredSessions", loadedSession -> {
            StoredSession document = loadedSession.document();

            if (document.isExpired(before) && tryDeleteIfExpired(loadedSession, before)) {
                deletedCount.incrementAndGet();
            }
        });

        return deletedCount.get();
    }

    @Override
    public final void delete(String sessionId) {
        Assert.hasText(sessionId, "sessionId must not be null or empty");

        deleteDocument(documentKey(sessionId));
    }

    @Override
    public final void appendEvent(SessionEvent event) {
        Assert.notNull(event, "event must not be null");

        String sessionId = event.getSessionId();
        StoredEvent storedEvent = StoredEvent.fromEvent(event, jsonMapper);

        conditionalWriteRetry.run("append an event", sessionId, () -> tryAppendEvent(sessionId, storedEvent));
    }

    @Override
    public final boolean applyCompaction(String sessionId, CompactionPlan plan, long expectedVersion) {
        Assert.hasText(sessionId, "sessionId must not be null or empty");
        Assert.notNull(plan, "plan must not be null");

        LoadedSession<T> loadedSession = requireSession(sessionId);

        StoredSession document = loadedSession.document();

        if (document.version() != expectedVersion) {
            return false;
        }

        List<SessionEvent> compactedEvents = plan.applyTo(document.toEvents(jsonMapper));

        StoredSession replacedDocument = document.replaceEvents(
            StoredSession.toStoredEvents(compactedEvents, jsonMapper), maxArchivedEvents);

        return tryPut(replacedDocument, loadedSession);
    }

    @Override
    public final long getEventVersion(String sessionId) {
        Assert.hasText(sessionId, "sessionId must not be null or empty");

        LoadedSession<T> loadedSession = load(sessionId);

        return loadedSession == null ? 0L : loadedSession.document()
            .version();
    }

    @Override
    public final List<SessionEvent> findEvents(String sessionId, EventFilter filter) {
        Assert.hasText(sessionId, "sessionId must not be null or empty");
        Assert.notNull(filter, "filter must not be null");

        LoadedSession<T> loadedSession = load(sessionId);

        if (loadedSession == null) {
            return List.of();
        }

        StoredSession document = loadedSession.document();

        return filter.apply(document.toEvents(jsonMapper));
    }

    @Override
    public final List<SessionEvent> findEventsByUserId(String userId, EventFilter filter) {
        Assert.hasText(userId, "userId must not be null or empty");
        Assert.notNull(filter, "filter must not be null");

        EventFilter perSessionFilter = filter.withoutWindow();
        List<SessionEvent> matches = new ArrayList<>();

        forEachDocument("findEventsByUserId", loadedSession -> {
            StoredSession document = loadedSession.document();

            if (userId.equals(document.userId())) {
                matches.addAll(perSessionFilter.apply(document.toEvents(jsonMapper)));
            }
        });

        matches.sort(Comparator.comparing(SessionEvent::getTimestamp)
            .thenComparing(SessionEvent::getId));

        return filter.applyWindow(matches);
    }

    protected final JsonMapper jsonMapper() {
        return jsonMapper;
    }

    protected abstract String documentKey(String sessionId);

    protected abstract List<String> listDocumentKeys();

    @Nullable
    protected abstract LoadedSession<T> loadDocument(String documentKey);

    protected abstract boolean tryPut(StoredSession document, @Nullable LoadedSession<T> current);

    protected CreateResult tryCreate(StoredSession document) {
        return tryPut(document, null) ? CreateResult.CREATED : CreateResult.ALREADY_EXISTS;
    }

    protected abstract boolean tryDeleteIfExpired(LoadedSession<T> loadedSession, Instant before);

    protected abstract void deleteDocument(String documentKey);

    @Nullable
    private LoadedSession<T> load(String sessionId) {
        return loadVerifiedDocument(documentKey(sessionId));
    }

    @Nullable
    private LoadedSession<T> loadVerifiedDocument(String documentKey) {
        LoadedSession<T> loadedSession = loadDocument(documentKey);

        if (loadedSession == null) {
            return null;
        }

        StoredSession document = loadedSession.document();

        if (!documentKey.equals(documentKey(document.id()))) {
            throw new UnreadableSessionDocumentException(
                "Session document " + documentKey + " holds session " + document.id() + " that belongs under " +
                    documentKey(document.id()));
        }

        return loadedSession;
    }

    private void forEachDocument(String operation, Consumer<LoadedSession<T>> action) {
        List<String> failedDocumentKeys = new ArrayList<>();

        for (String documentKey : listDocumentKeys()) {
            try {
                LoadedSession<T> loadedSession = loadVerifiedDocument(documentKey);

                if (loadedSession != null) {
                    action.accept(loadedSession);
                }
            } catch (JacksonException | UnreadableSessionDocumentException exception) {
                log.warn("Skipping session document {} that could not be processed", documentKey, exception);

                failedDocumentKeys.add(documentKey);
            }
        }

        if (!failedDocumentKeys.isEmpty()) {
            int loggedKeyCount = Math.min(failedDocumentKeys.size(), MAX_LOGGED_FAILED_DOCUMENT_KEYS);

            log.error(
                "{} skipped {} session documents that could not be processed (first {} listed): {}", operation,
                failedDocumentKeys.size(), loggedKeyCount, failedDocumentKeys.subList(0, loggedKeyCount));
        }
    }

    private LoadedSession<T> requireSession(String sessionId) {
        LoadedSession<T> loadedSession = load(sessionId);

        if (loadedSession == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        return loadedSession;
    }

    private boolean tryAppendEvent(String sessionId, StoredEvent storedEvent) {
        LoadedSession<T> loadedSession = requireSession(sessionId);

        StoredSession document = loadedSession.document();

        if (document.containsEvent(storedEvent.id())) {
            return true;
        }

        StoredSession appendedDocument = document.appendEvent(storedEvent, maxArchivedEvents);

        return tryPut(appendedDocument, loadedSession);
    }

    private Optional<Session> trySave(Session session) {
        LoadedSession<T> existingSession = load(session.id());

        StoredSession document = existingSession != null
            ? existingSession.document()
                .withSession(session)
            : StoredSession.fromSession(session, 0L, List.of());

        if (tryPut(document, existingSession)) {
            return Optional.of(document.toSession());
        }

        return Optional.empty();
    }

    protected enum CreateResult {
        CREATED, ALREADY_EXISTS, CONFLICT
    }

    protected record LoadedSession<T>(StoredSession document, T casToken) {

        public LoadedSession {
            Objects.requireNonNull(document, "document must not be null");
            Objects.requireNonNull(casToken, "casToken must not be null");
        }
    }
}
