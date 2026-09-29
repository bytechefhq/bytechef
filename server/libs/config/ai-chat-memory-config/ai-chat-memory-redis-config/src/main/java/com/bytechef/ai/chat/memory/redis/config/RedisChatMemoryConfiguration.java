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

package com.bytechef.ai.chat.memory.redis.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import redis.clients.jedis.RedisClient;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.memory", name = "provider", havingValue = "redis")
class RedisChatMemoryConfiguration {

    @Bean(destroyMethod = "close")
    RedisClient redisChatMemoryRedisClient(Environment environment) {
        String host = environment.getRequiredProperty("bytechef.ai.memory.redis.host");
        int port = environment.getRequiredProperty("bytechef.ai.memory.redis.port", Integer.class);
        String username = environment.getProperty("bytechef.ai.memory.redis.username");
        String password = environment.getProperty("bytechef.ai.memory.redis.password");

        if (username != null && !username.isBlank()) {
            if (password == null || password.isBlank()) {
                throw new IllegalArgumentException(
                    "bytechef.ai.memory.redis.password is required when a username is configured");
            }

            return RedisClient.create(host, port, username, password);
        }

        if (password != null && !password.isBlank()) {
            return RedisClient.create(host, port, null, password);
        }

        return RedisClient.create(host, port);
    }

    @Bean
    ChatMemoryRepository redisChatMemoryRepository(RedisClient redisChatMemoryRedisClient) {
        return new TenantRoutingRedisChatMemoryRepository(
            tenantId -> RedisChatMemoryRepository.builder()
                .jedisClient(redisChatMemoryRedisClient)
                .indexName(TenantRoutingRedisChatMemoryRepository.getIndexName(tenantId))
                .keyPrefix(TenantRoutingRedisChatMemoryRepository.getKeyPrefix(tenantId))
                .build());
    }

    @Bean
    ChatMemory redisChatMemory(ChatMemoryRepository redisChatMemoryRepository) {
        return MessageWindowChatMemory.builder()
            .chatMemoryRepository(redisChatMemoryRepository)
            .maxMessages(500)
            .build();
    }
}
