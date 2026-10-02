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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.session.store.AbstractDocumentSessionRepository;
import org.springframework.ai.session.store.ConditionalWriteRetry;
import org.springframework.ai.session.store.StoredSession;
import org.springframework.ai.session.store.UnreadableSessionDocumentException;
import org.springframework.util.Assert;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
public final class RedisSessionRepository
    extends AbstractDocumentSessionRepository<RedisSessionRepository.RedisCasToken> {

    static final String DOCUMENT_FIELD = "document";
    static final String EXPIRES_AT_FIELD = "expiresAt";
    static final String REVISION_FIELD = "revision";

    static final String CAS_SCRIPT = """
        local keyType = redis.call('TYPE', KEYS[1])['ok']
        if keyType == 'none' then
          if ARGV[2] ~= '-1' then
            return 0
          end
        elseif keyType == 'hash' then
          local revision = redis.call('HGET', KEYS[1], 'revision')
          if not revision then
            return -1
          end
          if revision ~= ARGV[2] then
            return 0
          end
        elseif keyType == 'string' then
          if ARGV[5] == '' or redis.call('GET', KEYS[1]) ~= ARGV[5] then
            return 0
          end
          redis.call('DEL', KEYS[1])
        else
          return keyType
        end
        redis.call('HSET', KEYS[1], 'revision', ARGV[3], 'expiresAt', ARGV[4], 'document', ARGV[1])
        return 1
        """;

    static final String DELETE_IF_EXPIRED_SCRIPT = """
        local keyType = redis.call('TYPE', KEYS[1])['ok']
        if keyType == 'hash' then
          local expiresAt = tonumber(redis.call('HGET', KEYS[1], 'expiresAt'))
          if expiresAt ~= nil and expiresAt < tonumber(ARGV[1]) then
            redis.call('DEL', KEYS[1])
            return 1
          end
          return 0
        end
        if keyType == 'string' and ARGV[2] ~= '' and redis.call('GET', KEYS[1]) == ARGV[2] then
          redis.call('DEL', KEYS[1])
          return 1
        end
        return 0
        """;

    private static final int SCAN_BATCH_SIZE = 500;
    private static final String EXPECT_ABSENT = "-1";
    private static final String GLOB_METACHARACTERS = "*?[]\\";
    private static final Long MISSING_REVISION_RESULT = -1L;
    private static final String NO_LEGACY_DOCUMENT = "";
    private static final String NO_EXPIRY = "";
    private static final String UNCHECKED_REVISION = "";
    private static final String WRONG_TYPE_ERROR_PREFIX = "WRONGTYPE";

    private final UnifiedJedis jedis;
    private final String keyPrefix;

    private RedisSessionRepository(Builder builder) {
        super(builder.jsonMapper, builder.conditionalWriteRetry, builder.maxArchivedEvents);

        this.jedis = builder.jedis;
        this.keyPrefix = builder.keyPrefix;
    }

    @Override
    protected String documentKey(String sessionId) {
        return keyPrefix + sessionId;
    }

    @Override
    protected List<String> listDocumentKeys() {
        Set<String> keys = new LinkedHashSet<>();
        ScanParams scanParams = new ScanParams()
            .match(keyPrefix + "*")
            .count(SCAN_BATCH_SIZE);
        String cursor = ScanParams.SCAN_POINTER_START;

        do {
            ScanResult<String> scanResult = jedis.scan(cursor, scanParams);

            keys.addAll(scanResult.getResult());

            cursor = scanResult.getCursor();
        } while (!ScanParams.SCAN_POINTER_START.equals(cursor));

        return List.copyOf(keys);
    }

    @Override
    @Nullable
    protected LoadedSession<RedisCasToken> loadDocument(String documentKey) {
        try {
            return loadHash(documentKey);
        } catch (JedisDataException jedisDataException) {
            requireWrongType(jedisDataException);
        }

        String legacyDocument;

        try {
            legacyDocument = jedis.get(documentKey);
        } catch (JedisDataException jedisDataException) {
            requireWrongType(jedisDataException);

            return loadRetypedHash(documentKey);
        }

        if (legacyDocument == null) {
            return null;
        }

        StoredSession document = jsonMapper().readValue(legacyDocument, StoredSession.class);

        return new LoadedSession<>(document, new RedisCasToken(UNCHECKED_REVISION, legacyDocument));
    }

    @Override
    protected boolean tryPut(StoredSession document, @Nullable LoadedSession<RedisCasToken> current) {
        RedisCasToken casToken = current == null ? null : current.casToken();

        String expectedRevision = casToken == null ? EXPECT_ABSENT : casToken.expectedRevision();
        String legacyDocument = casToken == null ? NO_LEGACY_DOCUMENT : casToken.legacyDocument();

        Long expiresAtEpochMilli = document.expiresAtEpochMilli();

        String expiresAt = expiresAtEpochMilli == null ? NO_EXPIRY : Long.toString(expiresAtEpochMilli);

        String key = documentKey(document.id());

        Object result = jedis.eval(
            CAS_SCRIPT, List.of(key),
            List.of(
                jsonMapper().writeValueAsString(document), expectedRevision, Long.toString(document.revision()),
                expiresAt, legacyDocument));

        if (result instanceof String keyType) {
            throw new IllegalStateException(
                "Redis key " + key + " holds a " + keyType + " instead of a session hash or JSON string");
        }

        if (MISSING_REVISION_RESULT.equals(result)) {
            throw new IllegalStateException(
                "Redis session hash " + key + " has no " + REVISION_FIELD + " field; conditional writes require one");
        }

        return toScriptOutcome(key, result);
    }

    @Override
    protected boolean tryDeleteIfExpired(LoadedSession<RedisCasToken> loadedSession, Instant before) {
        RedisCasToken casToken = loadedSession.casToken();

        String legacyDocument = casToken.legacyDocument();

        StoredSession document = loadedSession.document();

        String key = documentKey(document.id());

        Object result = jedis.eval(
            DELETE_IF_EXPIRED_SCRIPT, List.of(key), List.of(Long.toString(before.toEpochMilli()), legacyDocument));

        return toScriptOutcome(key, result);
    }

    @Override
    protected void deleteDocument(String documentKey) {
        jedis.del(documentKey);
    }

    @Nullable
    private LoadedSession<RedisCasToken> loadHash(String key) {
        List<String> fieldValues = jedis.hmget(key, DOCUMENT_FIELD, REVISION_FIELD);

        String document = fieldValues.get(0);

        if (document == null) {
            if (fieldValues.get(1) != null) {
                throw new UnreadableSessionDocumentException(
                    "Redis session hash " + key + " has a " + REVISION_FIELD + " field but no " + DOCUMENT_FIELD +
                        " field");
            }

            return null;
        }

        StoredSession storedSession = jsonMapper().readValue(document, StoredSession.class);

        return new LoadedSession<>(
            storedSession, new RedisCasToken(Long.toString(storedSession.revision()), NO_LEGACY_DOCUMENT));
    }

    @Nullable
    private LoadedSession<RedisCasToken> loadRetypedHash(String key) {
        try {
            return loadHash(key);
        } catch (JedisDataException jedisDataException) {
            requireWrongType(jedisDataException);

            throw new UnreadableSessionDocumentException(
                "Redis key " + key + " holds neither a session hash nor a JSON string", jedisDataException);
        }
    }

    private static boolean toScriptOutcome(String key, @Nullable Object result) {
        if (result instanceof Long resultValue) {
            if (resultValue == 1L) {
                return true;
            }

            if (resultValue == 0L) {
                return false;
            }
        }

        throw new IllegalStateException("Unexpected session script result " + result + " for Redis key " + key);
    }

    private static boolean containsGlobMetacharacter(String keyPrefix) {
        for (char character : keyPrefix.toCharArray()) {
            if (GLOB_METACHARACTERS.indexOf(character) >= 0) {
                return true;
            }
        }

        return false;
    }

    private static void requireWrongType(JedisDataException jedisDataException) {
        String message = jedisDataException.getMessage();

        if (message == null || !message.startsWith(WRONG_TYPE_ERROR_PREFIX)) {
            throw jedisDataException;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    record RedisCasToken(String expectedRevision, String legacyDocument) {
    }

    public static final class Builder {

        private UnifiedJedis jedis;
        private String keyPrefix = "spring-ai-session:";
        private JsonMapper jsonMapper = JsonMapper.builder()
            .build();
        private ConditionalWriteRetry conditionalWriteRetry = ConditionalWriteRetry.defaults();
        private int maxArchivedEvents = StoredSession.DEFAULT_MAX_ARCHIVED_EVENTS;

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

        public Builder maxArchivedEvents(int maxArchivedEvents) {
            this.maxArchivedEvents = maxArchivedEvents;

            return this;
        }

        Builder conditionalWriteRetry(ConditionalWriteRetry conditionalWriteRetry) {
            this.conditionalWriteRetry = conditionalWriteRetry;

            return this;
        }

        public RedisSessionRepository build() {
            Assert.notNull(jedis, "jedis must not be null");
            Assert.hasText(keyPrefix, "keyPrefix must not be null or empty");
            Assert.isTrue(
                !containsGlobMetacharacter(keyPrefix),
                () -> "keyPrefix " + keyPrefix + " must not contain any of the Redis glob characters " +
                    GLOB_METACHARACTERS);
            Assert.notNull(jsonMapper, "jsonMapper must not be null");
            Assert.notNull(conditionalWriteRetry, "conditionalWriteRetry must not be null");
            Assert.isTrue(maxArchivedEvents >= 0, "maxArchivedEvents must not be negative");

            return new RedisSessionRepository(this);
        }
    }
}
