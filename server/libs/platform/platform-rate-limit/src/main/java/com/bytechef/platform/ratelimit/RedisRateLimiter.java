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
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Redis-backed {@link RateLimiter} for strict global limits across an HA deployment
 * ({@code bytechef.plan.enforcement.provider=redis}). One atomic Lua token bucket per (key, policy): capacity =
 * sustained rate x burst multiplier, refilled continuously at the sustained per-minute rate — the same semantics as
 * {@link Bucket4jRateLimiter}, but the bucket state lives in Redis so every node draws from one shared budget. Keys
 * expire after the bucket would have fully refilled, so idle tenants leave no residue.
 *
 * <p>
 * Fails open: if Redis is unreachable the request is admitted and the failure logged — an enforcement outage must not
 * become a platform outage.
 * </p>
 *
 * @author Ivica Cardic
 */
public class RedisRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

    private static final String LUA_SCRIPT = """
        local tokens_key = KEYS[1]
        local stamp_key = KEYS[2]
        local capacity = tonumber(ARGV[1])
        local refill_per_ms = tonumber(ARGV[2])
        local now_ms = tonumber(ARGV[3])
        local ttl_ms = tonumber(ARGV[4])

        local tokens = tonumber(redis.call('GET', tokens_key))
        local last_ms = tonumber(redis.call('GET', stamp_key))

        if tokens == nil or last_ms == nil then
            tokens = capacity
            last_ms = now_ms
        end

        local elapsed_ms = now_ms - last_ms

        if elapsed_ms > 0 then
            tokens = math.min(capacity, tokens + elapsed_ms * refill_per_ms)
        end

        local allowed = 0

        if tokens >= 1 then
            tokens = tokens - 1
            allowed = 1
        end

        redis.call('SET', tokens_key, tostring(tokens), 'PX', ttl_ms)
        redis.call('SET', stamp_key, tostring(math.max(last_ms, now_ms)), 'PX', ttl_ms)

        return allowed
        """;

    private static final RedisScript<Long> TOKEN_BUCKET_SCRIPT = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);

    private final StringRedisTemplate redisTemplate;

    @SuppressFBWarnings("EI2")
    public RedisRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryConsume(String key, RateLimitPolicy policy) {
        String bucketKey = "bytechef:rate-limit:" + key + "|" + policy.permitsPerMinute() + "x" +
            policy.burstMultiplier();

        double refillPerMillisecond = policy.permitsPerMinute() / 60000.0;

        // TTL: time to refill the full capacity from empty, plus a minute of slack.
        long timeToLiveMillis = (long) Math.ceil(policy.capacity() / refillPerMillisecond) + 60000;

        try {
            Long allowed = redisTemplate.execute(
                TOKEN_BUCKET_SCRIPT,
                List.of(bucketKey + ":tokens", bucketKey + ":stamp"),
                String.valueOf(policy.capacity()), String.valueOf(refillPerMillisecond),
                String.valueOf(System.currentTimeMillis()), String.valueOf(timeToLiveMillis));

            return allowed == null || allowed == 1;
        } catch (Exception exception) {
            log.warn("Rate-limit check against Redis failed; admitting request (fail-open): {}",
                exception.getMessage());

            return true;
        }
    }
}
