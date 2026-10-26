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

package com.bytechef.ee.platform.ai.gateway.ratelimit;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimitResult;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnEEVersion
@ConditionalOnExpression("'${bytechef.ai.gateway.rate-limiting.enabled:false}'=='true' "
    + "and '${bytechef.ai.gateway.rate-limiting.provider:in-memory}'=='redis'")
public class RedisAiGatewayRateLimiter implements AiGatewayRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisAiGatewayRateLimiter.class);

    private static final String KEY_PREFIX = "bytechef:ai-gateway:ratelimit:";

    private final StringRedisTemplate stringRedisTemplate;

    @SuppressFBWarnings("EI2")
    public RedisAiGatewayRateLimiter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public AiGatewayRateLimitResult tryAcquire(String key, int limit, int windowSeconds) {
        String redisKey = KEY_PREFIX + key;

        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();

        Long count;

        try {
            count = valueOperations.increment(redisKey);
        } catch (Exception exception) {
            log.error("Redis rate limiter unavailable for key {}", key, exception);

            throw new RateLimiterUnavailableException(
                "Redis rate limiter unavailable for key " + key, exception);
        }

        if (count == null) {
            log.error("Redis INCR returned null for key {} — misconfiguration or pipelined call", key);

            throw new RateLimiterUnavailableException(
                "Redis INCR returned null for key " + key);
        }

        if (count == 1L) {
            stringRedisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
        }

        Long ttlSeconds = stringRedisTemplate.getExpire(redisKey, TimeUnit.SECONDS);

        long ttlMillis;

        if (ttlSeconds == null || ttlSeconds < 0) {
            ttlMillis = windowSeconds * 1000L;
        } else {
            ttlMillis = ttlSeconds * 1000L;
        }

        long resetAtEpochMs = System.currentTimeMillis() + ttlMillis;
        int remaining = (int) Math.max(0L, limit - count);

        if (count > limit) {
            return AiGatewayRateLimitResult.rejected(remaining, resetAtEpochMs);
        }

        return AiGatewayRateLimitResult.allowed(remaining, resetAtEpochMs);
    }

    @Override
    public void reset(String key) {
        try {
            stringRedisTemplate.delete(KEY_PREFIX + key);
        } catch (Exception exception) {
            log.error("Failed to reset Redis rate limit key {}", key, exception);

            throw new RateLimiterUnavailableException(
                "Failed to reset Redis rate limit key " + key, exception);
        }
    }
}
