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
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@EnableCaching
public class CacheConfiguration implements CachingConfigurer {

    private static final String REENTRANT_LOCK_CACHE =
        "com.bytechef.platform.component.aspect.TokenRefreshHandler.reentrantLock";

    private static final String WEBHOOK_RESUME_URL_CACHE =
        "com.bytechef.ee.ai.hub.agent.WebhookResumeRegistry.resumeUrls";

    private static final String WORKFLOW_CHAT_LAST_TURN_CACHE =
        "com.bytechef.ee.ai.hub.agent.WorkflowChatGuard.lastTurn";

    @Bean
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
            WORKFLOW_CHAT_LAST_TURN_CACHE,
            Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build());

        return caffeineCacheManager;
    }

    @Override
    public KeyGenerator keyGenerator() {
        return new TenantKeyGenerator();
    }
}
