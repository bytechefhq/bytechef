/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.ratelimit;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayRateLimitResult;
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

/**
 * Redis-backed rate limiter for distributed deployments.
 *
 * <p>
 * Uses Redis INCR + EXPIRE for atomic fixed-window counting. On the first call within a window, the counter is set to 1
 * and a TTL equal to {@code windowSeconds} is applied. Subsequent calls simply increment the counter and check it
 * against the limit; the counter resets automatically when the key expires.
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
// Activates only when rate limiting is enabled AND provider=redis. Requiring both avoids a dangling Redis bean
// that Spring would instantiate when provider=redis but rate-limiting.enabled=false.
@ConditionalOnExpression("'${bytechef.ai.gateway.rate-limiting.enabled:false}'=='true' "
    + "and '${bytechef.ai.gateway.rate-limiting.provider:in-memory}'=='redis'")
public class RedisAiGatewayRateLimiter implements AiGatewayRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(RedisAiGatewayRateLimiter.class);

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
            logger.error("Redis rate limiter unavailable for key {}", key, exception);

            throw new RateLimiterUnavailableException(
                "Redis rate limiter unavailable for key " + key, exception);
        }

        if (count == null) {
            logger.error("Redis INCR returned null for key {} — misconfiguration or pipelined call", key);

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
            logger.error("Failed to reset Redis rate limit key {}", key, exception);

            throw new RateLimiterUnavailableException(
                "Failed to reset Redis rate limit key " + key, exception);
        }
    }
}
