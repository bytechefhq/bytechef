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

import com.bytechef.cache.SharedCacheSupport;
import com.bytechef.cache.interceptor.TenantKeyGenerator;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * @author Ivica Cardic
 */
@Configuration
@EnableCaching
public class CacheConfiguration implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfiguration.class);

    private static final String REENTRANT_LOCK_CACHE =
        "com.bytechef.platform.component.aspect.TokenRefreshHandler.reentrantLock";
    private static final String REDIS_UNSUPPORTED_MESSAGE =
        "bytechef.cache.provider=redis requires ByteChef Enterprise Edition and an application that supports shared " +
            "caches; using caffeine";

    private final Environment environment;
    private final ObjectProvider<SharedCacheSupport> sharedCacheSupportProvider;

    public CacheConfiguration(
        Environment environment, ObjectProvider<SharedCacheSupport> sharedCacheSupportProvider) {

        this.environment = environment;
        this.sharedCacheSupportProvider = sharedCacheSupportProvider;
    }

    @Bean
    public CacheManager cacheManager() {
        if ("redis".equalsIgnoreCase(environment.getProperty("bytechef.cache.provider")) &&
            sharedCacheSupportProvider.getIfAvailable() == null) {

            log.warn(REDIS_UNSUPPORTED_MESSAGE);
        }

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

        return caffeineCacheManager;
    }

    @Override
    public KeyGenerator keyGenerator() {
        return new TenantKeyGenerator();
    }
}
