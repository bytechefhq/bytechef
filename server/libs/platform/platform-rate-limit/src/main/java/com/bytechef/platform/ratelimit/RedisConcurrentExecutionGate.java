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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Redis-backed {@link ConcurrentExecutionGate} for strict global concurrency limits across an HA deployment
 * ({@code bytechef.plan.enforcement.provider=redis}): one shared per-tenant counter, atomically bounded on acquire and
 * floored at zero on release. Counters carry a 24h TTL (refreshed on every acquire/release, matching the engine's
 * maximum task timeout) so slots orphaned by a crashed node self-heal instead of blocking the tenant forever.
 *
 * <p>
 * Fails open on acquire: if Redis is unreachable the submission is admitted and the failure logged — an enforcement
 * outage must not become a platform outage.
 * </p>
 *
 * @author Ivica Cardic
 */
public class RedisConcurrentExecutionGate implements ConcurrentExecutionGate {

    private static final Logger log = LoggerFactory.getLogger(RedisConcurrentExecutionGate.class);

    private static final Duration COUNTER_TIME_TO_LIVE = Duration.ofHours(24);

    private static final RedisScript<Long> ACQUIRE_SCRIPT = new DefaultRedisScript<>("""
        local current = tonumber(redis.call('GET', KEYS[1]) or '0')

        if current >= tonumber(ARGV[1]) then
            return 0
        end

        redis.call('INCR', KEYS[1])
        redis.call('PEXPIRE', KEYS[1], ARGV[2])

        return 1
        """, Long.class);

    private static final RedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>("""
        local current = tonumber(redis.call('GET', KEYS[1]) or '0')

        if current > 0 then
            redis.call('DECR', KEYS[1])
            redis.call('PEXPIRE', KEYS[1], ARGV[1])
        end

        return math.max(0, current - 1)
        """, Long.class);

    private final StringRedisTemplate redisTemplate;

    @SuppressFBWarnings("EI2")
    public RedisConcurrentExecutionGate(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryAcquire(String key, int limit) {
        try {
            Long acquired = redisTemplate.execute(
                ACQUIRE_SCRIPT, List.of(redisKey(key)), String.valueOf(limit),
                String.valueOf(COUNTER_TIME_TO_LIVE.toMillis()));

            return acquired == null || acquired == 1;
        } catch (Exception exception) {
            log.warn("Concurrency-slot acquire against Redis failed; admitting request (fail-open): {}",
                exception.getMessage());

            return true;
        }
    }

    @Override
    public void release(String key) {
        try {
            redisTemplate.execute(
                RELEASE_SCRIPT, List.of(redisKey(key)), String.valueOf(COUNTER_TIME_TO_LIVE.toMillis()));
        } catch (Exception exception) {
            log.warn("Concurrency-slot release against Redis failed: {}", exception.getMessage());
        }
    }

    @Override
    public int held(String key) {
        try {
            String value = redisTemplate.opsForValue()
                .get(redisKey(key));

            return value == null ? 0 : Integer.parseInt(value);
        } catch (Exception exception) {
            log.warn("Concurrency-slot read against Redis failed: {}", exception.getMessage());

            return 0;
        }
    }

    private static String redisKey(String key) {
        return "bytechef:concurrency:" + key;
    }
}
