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

package com.bytechef.platform.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Exercises the Redis plan-enforcement providers against a real Redis: the Lua token bucket ({@link RedisRateLimiter})
 * consumes up to burst capacity and then rejects, and the bounded concurrency gate
 * ({@link RedisConcurrentExecutionGate}) enforces its slot limit, releases slots, and floors at zero on over-release.
 * Fail-open behavior on a Redis outage is covered last since it stops the shared container.
 *
 * @author Ivica Cardic
 */
@Testcontainers
@TestMethodOrder(OrderAnnotation.class)
class RedisPlanEnforcementIntTest {

    @Container
    private static final GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>("redis:8-alpine")
        .withExposedPorts(6379);

    private static LettuceConnectionFactory redisConnectionFactory;
    private static RedisRateLimiter redisRateLimiter;
    private static RedisConcurrentExecutionGate redisConcurrentExecutionGate;

    @BeforeAll
    static void beforeAll() {
        redisConnectionFactory = new LettuceConnectionFactory(
            REDIS_CONTAINER.getHost(), REDIS_CONTAINER.getMappedPort(6379));

        redisConnectionFactory.afterPropertiesSet();

        StringRedisTemplate stringRedisTemplate = new StringRedisTemplate(redisConnectionFactory);

        redisRateLimiter = new RedisRateLimiter(stringRedisTemplate);
        redisConcurrentExecutionGate = new RedisConcurrentExecutionGate(stringRedisTemplate);
    }

    @AfterAll
    static void afterAll() {
        if (redisConnectionFactory != null) {
            redisConnectionFactory.destroy();
        }
    }

    @Test
    @Order(1)
    void testRateLimiterConsumesUpToBurstCapacityThenRejects() {
        String key = uniqueKey("rate");

        RateLimitPolicy rateLimitPolicy = new RateLimitPolicy(2, 2);

        long capacity = rateLimitPolicy.capacity();

        for (long consumed = 0; consumed < capacity; consumed++) {
            assertThat(redisRateLimiter.tryConsume(key, rateLimitPolicy))
                .as("permit %d of %d within burst capacity", consumed + 1, capacity)
                .isTrue();
        }

        assertThat(redisRateLimiter.tryConsume(key, rateLimitPolicy))
            .as("permit beyond burst capacity")
            .isFalse();
    }

    @Test
    @Order(2)
    void testRateLimiterKeysAreIndependent() {
        RateLimitPolicy rateLimitPolicy = new RateLimitPolicy(1, 1);

        String exhaustedKey = uniqueKey("rate");

        assertThat(redisRateLimiter.tryConsume(exhaustedKey, rateLimitPolicy)).isTrue();
        assertThat(redisRateLimiter.tryConsume(exhaustedKey, rateLimitPolicy)).isFalse();

        assertThat(redisRateLimiter.tryConsume(uniqueKey("rate"), rateLimitPolicy)).isTrue();
    }

    @Test
    @Order(3)
    void testConcurrencyGateEnforcesSlotLimitAndReleases() {
        String key = uniqueKey("concurrency");

        assertThat(redisConcurrentExecutionGate.tryAcquire(key, 2)).isTrue();
        assertThat(redisConcurrentExecutionGate.tryAcquire(key, 2)).isTrue();
        assertThat(redisConcurrentExecutionGate.tryAcquire(key, 2)).isFalse();

        assertThat(redisConcurrentExecutionGate.held(key)).isEqualTo(2);

        redisConcurrentExecutionGate.release(key);

        assertThat(redisConcurrentExecutionGate.held(key)).isEqualTo(1);
        assertThat(redisConcurrentExecutionGate.tryAcquire(key, 2)).isTrue();
    }

    @Test
    @Order(4)
    void testConcurrencyGateFloorsAtZeroOnOverRelease() {
        String key = uniqueKey("concurrency");

        assertThat(redisConcurrentExecutionGate.tryAcquire(key, 1)).isTrue();

        redisConcurrentExecutionGate.release(key);
        redisConcurrentExecutionGate.release(key);

        assertThat(redisConcurrentExecutionGate.held(key)).isZero();

        assertThat(redisConcurrentExecutionGate.tryAcquire(key, 1)).isTrue();
    }

    @Test
    @Order(5)
    void testFailOpenWhenRedisIsUnreachable() {
        // Ordered last because it stops the shared container: both providers must admit rather than
        // block when Redis is down — plan enforcement degrades open by design.
        REDIS_CONTAINER.stop();

        assertThat(redisRateLimiter.tryConsume(uniqueKey("rate"), new RateLimitPolicy(1, 1))).isTrue();
        assertThat(redisConcurrentExecutionGate.tryAcquire(uniqueKey("concurrency"), 1)).isTrue();
    }

    private static String uniqueKey(String prefix) {
        UUID uuid = UUID.randomUUID();

        return prefix + ":" + uuid;
    }
}
