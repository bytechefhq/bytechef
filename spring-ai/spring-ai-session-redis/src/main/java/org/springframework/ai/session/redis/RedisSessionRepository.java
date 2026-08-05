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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.redis.StoredSession.StoredEvent;
import org.springframework.util.Assert;
import redis.clients.jedis.UnifiedJedis;
import tools.jackson.databind.json.JsonMapper;

/**
 * Redis implementation of {@link SessionRepository}.
 *
 * <p>
 * Each session is stored as a single JSON string at {@code {keyPrefix}{sessionId}}. The document contains the session
 * metadata, a monotonically increasing version counter, and the full ordered event log — the same document shape as the
 * S3 variant. Compare-and-swap semantics for {@link #compactEvents(String, List, List, long)} (and the internal
 * read-modify-write loops) are enforced atomically on the Redis server via a Lua script that compares the stored
 * document's version field before overwriting.
 *
 * @author Ivica Cardic
 */
public final class RedisSessionRepository implements SessionRepository {

    private static final int MAX_RETRIES = 5;

    /**
     * Atomic compare-and-swap of the whole session document. KEYS[1] = session key, ARGV[1] = new document JSON,
     * ARGV[2] = expected current version ({@code -1} means the key must be absent — create-if-absent). Returns 1 when
     * the swap happened, 0 when the version (or absence) precondition failed.
     */
    private static final String CAS_SCRIPT = """
        local current = redis.call('GET', KEYS[1])
        if current == false then
          if ARGV[2] == '-1' then
            redis.call('SET', KEYS[1], ARGV[1])
            return 1
          end
          return 0
        end
        local ok, doc = pcall(cjson.decode, current)
        if not ok or tostring(doc['version']) ~= ARGV[2] then
          return 0
        end
        redis.call('SET', KEYS[1], ARGV[1])
        return 1
        """;

    private static final long EXPECT_ABSENT = -1L;

    private final UnifiedJedis jedis;
    private final String keyPrefix;
    private final JsonMapper jsonMapper;

    private RedisSessionRepository(Builder builder) {
        this.jedis = builder.jedis;
        this.keyPrefix = builder.keyPrefix;
        this.jsonMapper = builder.jsonMapper;
    }

    @Override
    public Session save(Session session) {
        Assert.notNull(session, "session must not be null");

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            StoredSession existing = load(session.id());

            long version = existing != null ? existing.version() : 0L;
            List<StoredEvent> events = existing != null ? existing.events() : List.of();
            long expectedVersion = existing != null ? existing.version() : EXPECT_ABSENT;

            if (tryPut(StoredSession.fromSession(session, version, events), expectedVersion)) {
                return session;
            }
        }

        throw new IllegalStateException(
            "Failed to save session after " + MAX_RETRIES + " attempts: " + session.id());
    }

    @Override
    @Nullable
    public Session findById(String sessionId) {
        Assert.hasText(sessionId, "sessionId must not be null or empty");

        StoredSession document = load(sessionId);

        return document == null ? null : document.toSession();
    }

    @Override
    public List<Session> findByUserId(String userId) {
        Assert.hasText(userId, "userId must not be null or empty");

        List<Session> sessions = new ArrayList<>();

        for (StoredSession document : loadAll()) {
            if (userId.equals(document.userId())) {
                sessions.add(document.toSession());
            }
        }

        return sessions;
    }

    @Override
    public List<String> findExpiredSessionIds(Instant before) {
        Assert.notNull(before, "before must not be null");

        List<String> ids = new ArrayList<>();

        for (StoredSession document : loadAll()) {
            Long expiresAt = document.expiresAtEpochMilli();

            if (expiresAt != null && expiresAt < before.toEpochMilli()) {
                ids.add(document.id());
            }
        }

        return ids;
    }

    @Override
    public void delete(String sessionId) {
        Assert.hasText(sessionId, "sessionId must not be null or empty");

        jedis.del(key(sessionId));
    }

    @Override
    public void appendEvent(SessionEvent event) {
        Assert.notNull(event, "event must not be null");

        String sessionId = event.getSessionId();

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            StoredSession document = requireSession(sessionId);

            List<StoredEvent> events = new ArrayList<>(document.events());

            events.add(StoredEvent.fromEvent(event, jsonMapper));

            StoredSession next = withEvents(document, events, document.version() + 1);

            if (tryPut(next, document.version())) {
                return;
            }
        }

        throw new IllegalStateException(
            "Failed to append event after " + MAX_RETRIES + " attempts: " + sessionId);
    }

    @Override
    public boolean compactEvents(
        String sessionId, List<SessionEvent> archivedEvents, List<SessionEvent> retainedEvents,
        long expectedVersion) {

        Assert.hasText(sessionId, "sessionId must not be null or empty");
        Assert.notNull(archivedEvents, "archivedEvents must not be null");
        Assert.notNull(retainedEvents, "retainedEvents must not be null");

        StoredSession document = requireSession(sessionId);

        if (document.version() != expectedVersion) {
            return false;
        }

        // Mirror the reference repositories: previously-archived events keep their position as the oldest
        // prefix, the newly-compacted events follow marked archived, and the retained list becomes the new
        // active window. Any other previously-active event (e.g. a superseded synthetic summary) is dropped.
        List<SessionEvent> newEvents = new ArrayList<>();

        for (StoredEvent storedEvent : document.events()) {
            SessionEvent event = storedEvent.toEvent(jsonMapper);

            if (event.isArchived()) {
                newEvents.add(event);
            }
        }

        for (SessionEvent archivedEvent : archivedEvents) {
            newEvents.add(archivedEvent.asArchived());
        }

        newEvents.addAll(retainedEvents);

        StoredSession next = withEvents(
            document, StoredSession.toStoredEvents(newEvents, jsonMapper), document.version() + 1);

        return tryPut(next, document.version());
    }

    @Override
    public long getEventVersion(String sessionId) {
        Assert.hasText(sessionId, "sessionId must not be null or empty");

        StoredSession document = load(sessionId);

        return document == null ? 0L : document.version();
    }

    @Override
    public List<SessionEvent> findEvents(String sessionId, EventFilter filter) {
        Assert.hasText(sessionId, "sessionId must not be null or empty");
        Assert.notNull(filter, "filter must not be null");

        StoredSession document = load(sessionId);

        if (document == null) {
            return List.of();
        }

        List<SessionEvent> events = new ArrayList<>();

        for (StoredEvent storedEvent : document.events()) {
            events.add(storedEvent.toEvent(jsonMapper));
        }

        return applyFilter(events, filter);
    }

    private List<SessionEvent> applyFilter(List<SessionEvent> events, EventFilter filter) {
        List<SessionEvent> matched = new ArrayList<>();

        for (SessionEvent event : events) {
            if (filter.matches(event)) {
                matched.add(event);
            }
        }

        matched.sort((left, right) -> left.getTimestamp()
            .compareTo(right.getTimestamp()));

        Integer lastN = filter.lastN();

        if (lastN != null) {
            int from = Math.max(0, matched.size() - lastN);

            return Collections.unmodifiableList(new ArrayList<>(matched.subList(from, matched.size())));
        }

        Integer pageSize = filter.pageSize();

        if (pageSize != null) {
            Integer pageNumber = filter.page();

            int page = pageNumber != null ? pageNumber : 0;
            int from = Math.min(page * pageSize, matched.size());
            int to = Math.min(from + pageSize, matched.size());

            return Collections.unmodifiableList(new ArrayList<>(matched.subList(from, to)));
        }

        return Collections.unmodifiableList(matched);
    }

    private StoredSession withEvents(StoredSession document, List<StoredEvent> events, long version) {
        return new StoredSession(
            document.id(), document.userId(), document.createdAtEpochMilli(), document.expiresAtEpochMilli(),
            document.metadata(), version, events);
    }

    private StoredSession requireSession(String sessionId) {
        StoredSession document = load(sessionId);

        if (document == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        return document;
    }

    private String key(String sessionId) {
        return keyPrefix + sessionId;
    }

    @Nullable
    private StoredSession load(String sessionId) {
        String json = jedis.get(key(sessionId));

        if (json == null) {
            return null;
        }

        return jsonMapper.readValue(json, StoredSession.class);
    }

    private List<StoredSession> loadAll() {
        List<StoredSession> documents = new ArrayList<>();

        for (String key : jedis.keys(keyPrefix + "*")) {
            String json = jedis.get(key);

            if (json == null) {
                // key deleted between KEYS and GET — skip
                continue;
            }

            documents.add(jsonMapper.readValue(json, StoredSession.class));
        }

        return documents;
    }

    private void put(StoredSession document) {
        jedis.set(key(document.id()), jsonMapper.writeValueAsString(document));
    }

    private boolean tryPut(StoredSession document, long expectedVersion) {
        Object result = jedis.eval(
            CAS_SCRIPT, List.of(key(document.id())),
            List.of(jsonMapper.writeValueAsString(document), Long.toString(expectedVersion)));

        return result instanceof Long resultValue && resultValue == 1L;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private UnifiedJedis jedis;
        private String keyPrefix = "spring-ai-session:";
        private JsonMapper jsonMapper = JsonMapper.builder()
            .build();

        private Builder() {
        }

        @SuppressFBWarnings("EI_EXPOSE_REP2")
        public Builder jedis(UnifiedJedis jedis) {
            this.jedis = jedis;

            return this;
        }

        public Builder keyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;

            return this;
        }

        @SuppressFBWarnings("EI_EXPOSE_REP2")
        public Builder jsonMapper(JsonMapper jsonMapper) {
            this.jsonMapper = jsonMapper;

            return this;
        }

        public RedisSessionRepository build() {
            Assert.notNull(jedis, "jedis must not be null");
            Assert.hasText(keyPrefix, "keyPrefix must not be null or empty");

            return new RedisSessionRepository(this);
        }
    }
}
