
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.coordinator.config;

import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * @author Ivica Cardic
 */
@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfiguration {

    @Bean("workflowRepositoryCacheManager")
    @ConditionalOnProperty(
        prefix = "bytechef.workflow",
        name = "workflow-repository.cache.provider",
        havingValue = "no-op")
    public CacheManager noOpworkflowRepositoryCacheManager() {
        return new NoOpCacheManager();
    }

    @Bean("workflowRepositoryCacheManager")
    @ConditionalOnProperty(
        prefix = "bytechef.workflow",
        name = "workflow-repository.cache.provider",
        havingValue = "redis")
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        return RedisCacheManager.RedisCacheManagerBuilder.fromConnectionFactory(connectionFactory)
            .build();
    }
}
