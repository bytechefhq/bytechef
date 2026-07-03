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

package com.bytechef.cache.config;

import com.bytechef.cache.interceptor.TenantKeyGenerator;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @author Ivica Cardic
 */
@Configuration
@EnableCaching
public class CacheConfiguration implements CachingConfigurer {

    private static final String REENTRANT_LOCK_CACHE =
        "com.bytechef.platform.component.aspect.TokenRefreshHandler.reentrantLock";

    /**
     * EE workflow-chat resume URL cache. Registered here so {@code WebhookResumeRegistry} (in
     * {@code ai-copilot-service}) gets a TTL-bounded, multi-instance-safe store without depending on this module —
     * declaring the constant in the central CacheConfiguration is the same pattern as {@link #REENTRANT_LOCK_CACHE}.
     * The TTL needs to outlive a typical "user reads question + types answer" cycle but expire well before the workflow
     * engine's own pause-window timeout, hence 30 minutes.
     */
    private static final String WEBHOOK_RESUME_URL_CACHE =
        "com.bytechef.ee.ai.hub.agent.WebhookResumeRegistry.resumeUrls";

    /**
     * EE workflow-chat in-flight jobId cache. Maps conversationId → jobId so the cancel-turn mutation can resolve back
     * to the right {@code JobFacade.stopJob} target. Same TTL as the resume URL cache (30 minutes) — both protect
     * ephemeral per-turn state with identical lifetime requirements.
     */
    private static final String WORKFLOW_CHAT_JOB_CACHE =
        "com.bytechef.ee.ai.hub.agent.WorkflowChatJobRegistry.jobIds";

    /**
     * EE workflow-chat last-turn timestamp cache (rate-limit). Stores epoch-millis of the last turn that started for
     * each conversation, so {@code WorkflowChatGuard.tryAdmit} can refuse turns that arrive within the cooldown window.
     * 5-minute TTL is well above the 2-second cooldown — entries naturally expire long before they could spuriously
     * rate-limit a follow-up turn.
     */
    private static final String WORKFLOW_CHAT_LAST_TURN_CACHE =
        "com.bytechef.ee.ai.hub.agent.WorkflowChatGuard.lastTurn";

    /**
     * EE workflow-chat in-flight conversation lock (concurrency). Held for the duration of a turn so a second message
     * arriving on the same conversation gets a "previous turn still running" error rather than racing two workflow
     * executions. 30-minute TTL bounds orphaned locks if a JVM crashes mid-turn — long enough for normal workflows,
     * short enough that a stuck conversation auto-recovers without admin intervention.
     */
    private static final String WORKFLOW_CHAT_IN_FLIGHT_CACHE =
        "com.bytechef.ee.ai.hub.agent.WorkflowChatGuard.inFlight";

    @Bean
    @ConditionalOnProperty(prefix = "bytechef", name = "cache.provider", havingValue = "redis")
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        Class<?> clazz = getClass();

        return (builder) -> {
            ClassLoader classLoader = clazz.getClassLoader();

            builder.cacheDefaults(getCacheConfiguration(10, classLoader))
                .withCacheConfiguration(REENTRANT_LOCK_CACHE, getCacheConfiguration(1, classLoader))
                .withCacheConfiguration(WEBHOOK_RESUME_URL_CACHE, getCacheConfiguration(30, classLoader))
                .withCacheConfiguration(WORKFLOW_CHAT_JOB_CACHE, getCacheConfiguration(30, classLoader))
                .withCacheConfiguration(WORKFLOW_CHAT_LAST_TURN_CACHE, getCacheConfiguration(5, classLoader))
                .withCacheConfiguration(WORKFLOW_CHAT_IN_FLIGHT_CACHE, getCacheConfiguration(30, classLoader));
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef", name = "cache.provider", havingValue = "caffeine")
    public CacheManager cacheManager() {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();

        caffeineCacheManager.setCaffeine(
            Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(10, TimeUnit.MINUTES));

        caffeineCacheManager.registerCustomCache(
            REENTRANT_LOCK_CACHE,
            Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats()
                .build());

        caffeineCacheManager.registerCustomCache(
            WEBHOOK_RESUME_URL_CACHE,
            Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats()
                .build());

        caffeineCacheManager.registerCustomCache(
            WORKFLOW_CHAT_JOB_CACHE,
            Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats()
                .build());

        caffeineCacheManager.registerCustomCache(
            WORKFLOW_CHAT_LAST_TURN_CACHE,
            Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build());

        caffeineCacheManager.registerCustomCache(
            WORKFLOW_CHAT_IN_FLIGHT_CACHE,
            Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats()
                .build());

        return caffeineCacheManager;
    }

    @Override
    public KeyGenerator keyGenerator() {
        return new TenantKeyGenerator();
    }

    private static RedisCacheConfiguration getCacheConfiguration(int minutes, ClassLoader clazz) {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(minutes))
            .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(SerializationPair.fromSerializer(new JdkSerializationRedisSerializer(clazz)));
    }
}
