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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.s3.StoredSession.StoredEvent;
import org.springframework.util.Assert;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import tools.jackson.databind.json.JsonMapper;

/**
 * Amazon S3 implementation of {@link SessionRepository}.
 *
 * <p>
 * Each session is stored as a single JSON object at {@code {keyPrefix}{sessionId}.json}. The object contains the
 * session metadata, a monotonically increasing version counter, and the full ordered event log. Compare-and-swap
 * semantics for {@link #replaceEvents(String, List, long)} are enforced via the stored version field; S3 conditional
 * writes (If-Match ETag) are attempted first for stronger isolation, but the in-document version check is the
 * authoritative guard.
 *
 * @author Ivica Cardic
 */
public final class S3SessionRepository implements SessionRepository {

    private static final String SUFFIX = ".json";
    private static final int MAX_RETRIES = 5;

    private final S3Client s3Client;
    private final String bucketName;
    private final String keyPrefix;
    private final JsonMapper jsonMapper;

    private S3SessionRepository(Builder builder) {
        this.s3Client = builder.s3Client;
        this.bucketName = builder.bucketName;
        this.keyPrefix = builder.keyPrefix;
        this.jsonMapper = builder.jsonMapper;
    }

    @Override
    public Session save(Session session) {
        Assert.notNull(session, "session must not be null");

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            Loaded existing = load(session.id());

            long version = existing != null ? existing.document.version() : 0L;
            List<StoredEvent> events = existing != null ? existing.document.events() : List.of();
            String ifMatchEtag = existing != null ? existing.etag : null;

            if (tryPut(StoredSession.fromSession(session, version, events), ifMatchEtag)) {
                return session;
            }
        }

        throw new IllegalStateException(
            "Failed to save session after " + MAX_RETRIES + " attempts: " + session.id());
    }

    @Override
    public Optional<Session> findById(String sessionId) {
        Assert.hasText(sessionId, "sessionId must not be null or empty");

        Loaded loaded = load(sessionId);

        return loaded == null ? Optional.empty() : Optional.of(loaded.document.toSession());
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

        s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(key(sessionId))
            .build());
    }

    @Override
    public void appendEvent(SessionEvent event) {
        Assert.notNull(event, "event must not be null");

        String sessionId = event.getSessionId();

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            Loaded loaded = requireSession(sessionId);

            List<StoredEvent> events = new ArrayList<>(loaded.document.events());

            events.add(StoredEvent.fromEvent(event, jsonMapper));

            StoredSession next = withEvents(loaded.document, events, loaded.document.version() + 1);

            if (tryPut(next, loaded.etag)) {
                return;
            }
        }

        throw new IllegalStateException(
            "Failed to append event after " + MAX_RETRIES + " attempts: " + sessionId);
    }

    @Override
    public void replaceEvents(String sessionId, List<SessionEvent> events) {
        Assert.hasText(sessionId, "sessionId must not be null or empty");
        Assert.notNull(events, "events must not be null");

        Loaded loaded = requireSession(sessionId);

        StoredSession next = withEvents(
            loaded.document, StoredSession.toStoredEvents(events, jsonMapper), loaded.document.version() + 1);

        put(next, null);
    }

    @Override
    public boolean replaceEvents(String sessionId, List<SessionEvent> events, long expectedVersion) {
        Assert.hasText(sessionId, "sessionId must not be null or empty");
        Assert.notNull(events, "events must not be null");

        Loaded loaded = requireSession(sessionId);

        if (loaded.document.version() != expectedVersion) {
            return false;
        }

        StoredSession next = withEvents(
            loaded.document, StoredSession.toStoredEvents(events, jsonMapper), loaded.document.version() + 1);

        return tryPut(next, loaded.etag);
    }

    @Override
    public long getEventVersion(String sessionId) {
        Assert.hasText(sessionId, "sessionId must not be null or empty");

        Loaded loaded = load(sessionId);

        return loaded == null ? 0L : loaded.document.version();
    }

    @Override
    public List<SessionEvent> findEvents(String sessionId, EventFilter filter) {
        Assert.hasText(sessionId, "sessionId must not be null or empty");
        Assert.notNull(filter, "filter must not be null");

        Loaded loaded = load(sessionId);

        if (loaded == null) {
            return List.of();
        }

        List<SessionEvent> events = new ArrayList<>();

        for (StoredEvent storedEvent : loaded.document.events()) {
            events.add(storedEvent.toEvent(jsonMapper));
        }

        return applyFilter(events, filter);
    }

    private List<SessionEvent> applyFilter(List<SessionEvent> events, EventFilter filter) {
        List<SessionEvent> matched = new ArrayList<>();

        for (SessionEvent event : events) {
            if (matches(event, filter)) {
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

    private boolean matches(SessionEvent event, EventFilter filter) {
        Instant timestamp = event.getTimestamp();

        Instant from = filter.from();

        if (from != null && timestamp.isBefore(from)) {
            return false;
        }

        Instant to = filter.to();

        if (to != null && timestamp.isAfter(to)) {
            return false;
        }

        Set<MessageType> messageTypes = filter.messageTypes();

        if (messageTypes != null) {
            MessageType type = event.getMessage()
                .getMessageType();

            if (!messageTypes.contains(type)) {
                return false;
            }
        }

        if (filter.excludeSynthetic() && event.isSynthetic()) {
            return false;
        }

        String branch = filter.branch();

        if (branch != null && !branchVisible(event.getBranch(), branch)) {
            return false;
        }

        String keyword = filter.keyword();

        if (keyword != null) {
            String text = event.getMessage()
                .getText();

            if (text == null || !text.toLowerCase()
                .contains(keyword)) {
                return false;
            }
        }

        return true;
    }

    private boolean branchVisible(@Nullable String eventBranch, String filterBranch) {
        return eventBranch == null || eventBranch.equals(filterBranch) || filterBranch.startsWith(eventBranch + ".");
    }

    private StoredSession withEvents(StoredSession document, List<StoredEvent> events, long version) {
        return new StoredSession(
            document.id(), document.userId(), document.createdAtEpochMilli(), document.expiresAtEpochMilli(),
            document.metadata(), version, events);
    }

    private Loaded requireSession(String sessionId) {
        Loaded loaded = load(sessionId);

        if (loaded == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        return loaded;
    }

    private String key(String sessionId) {
        return keyPrefix + sessionId + SUFFIX;
    }

    @Nullable
    private Loaded load(String sessionId) {
        ResponseBytes<GetObjectResponse> response;

        try {
            response = s3Client.getObjectAsBytes(
                GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key(sessionId))
                    .build());
        } catch (NoSuchKeyException noSuchKeyException) {
            return null;
        }

        StoredSession document = jsonMapper.readValue(response.asByteArray(), StoredSession.class);

        return new Loaded(document, response.response()
            .eTag());
    }

    private List<StoredSession> loadAll() {
        List<StoredSession> documents = new ArrayList<>();

        List<S3Object> objects = new ArrayList<>();

        for (S3Object s3Object : s3Client.listObjectsV2Paginator(ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix(keyPrefix)
            .build())
            .contents()) {

            objects.add(s3Object);
        }

        for (S3Object object : objects) {
            String objectKey = object.key();

            if (!objectKey.endsWith(SUFFIX)) {
                continue;
            }

            try {
                ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .build());

                documents.add(jsonMapper.readValue(response.asByteArray(), StoredSession.class));
            } catch (NoSuchKeyException noSuchKeyException) {
                // object deleted between list and get — skip
                continue;
            }
        }

        return documents;
    }

    private void put(StoredSession document, @Nullable String ifMatchEtag) {
        PutObjectRequest.Builder request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key(document.id()))
            .contentType("application/json");

        if (ifMatchEtag != null) {
            request.ifMatch(ifMatchEtag);
        }

        s3Client.putObject(request.build(), RequestBody.fromBytes(jsonMapper.writeValueAsBytes(document)));
    }

    private boolean tryPut(StoredSession document, @Nullable String ifMatchEtag) {
        try {
            put(document, ifMatchEtag);

            return true;
        } catch (S3Exception s3Exception) {
            if (s3Exception.statusCode() == 412) {
                return false;
            }

            throw s3Exception;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private record Loaded(StoredSession document, @Nullable String etag) {
    }

    public static final class Builder {

        private S3Client s3Client;
        private String bucketName;
        private String keyPrefix = "";
        private JsonMapper jsonMapper = JsonMapper.builder()
            .build();

        private Builder() {
        }

        @SuppressFBWarnings("EI_EXPOSE_REP2")
        public Builder s3Client(S3Client s3Client) {
            this.s3Client = s3Client;

            return this;
        }

        public Builder bucketName(String bucketName) {
            this.bucketName = bucketName;

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

        public S3SessionRepository build() {
            Assert.notNull(s3Client, "s3Client must not be null");
            Assert.hasText(bucketName, "bucketName must not be null or empty");

            return new S3SessionRepository(this);
        }
    }
}
