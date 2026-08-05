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

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.JedisPooled;

/**
 * Integration tests for {@link RedisSessionRepository} against a real Redis container — unlike the mocked unit test,
 * these exercise the actual Lua compare-and-swap script.
 *
 * @author Ivica Cardic
 */
class RedisSessionRepositoryIntTest {

    private static GenericContainer<?> redis;
    private static JedisPooled jedisPooled;
    private static RedisSessionRepository repository;

    @BeforeAll
    static void beforeAll() {
        redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

        redis.start();

        jedisPooled = new JedisPooled(redis.getHost(), redis.getMappedPort(6379));

        repository = RedisSessionRepository.builder()
            .jedis(jedisPooled)
            .keyPrefix("int-test-session:")
            .build();
    }

    @AfterAll
    static void afterAll() {
        if (jedisPooled != null) {
            jedisPooled.close();
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
    void testCompactEventsCasSucceedsThenFailsOnStaleVersion() {
        newSession("s-cas");

        repository.appendEvent(event("s-cas", "v1"));

        long version = repository.getEventVersion("s-cas");

        assertTrue(repository.compactEvents("s-cas", List.of(), List.of(event("s-cas", "compacted")), version));
        assertFalse(repository.compactEvents("s-cas", List.of(), List.of(event("s-cas", "again")), version));
    }

    @Test
    void testFindByUserIdAndDelete() {
        newSession("s-del");

        assertFalse(repository.findByUserId("user-1")
            .isEmpty());

        repository.delete("s-del");

        assertNull(repository.findById("s-del"));
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
