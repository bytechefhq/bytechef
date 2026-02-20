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

package com.bytechef.component.ai.agent.chat.memory.builtin.config;

import com.bytechef.component.ai.agent.chat.memory.builtin.repository.RedisChatMemoryRepository;
import com.bytechef.config.ApplicationProperties;
import java.time.Duration;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.agent.memory", name = "provider", havingValue = "redis")
class RedisChatMemoryConfiguration {

    private final ApplicationProperties applicationProperties;

    RedisChatMemoryConfiguration(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Bean
    ChatMemoryRepository chatMemoryRepository() {
        ApplicationProperties.Ai.Agent.Memory.Redis redisProperties =
            applicationProperties.getAi()
                .getAgent()
                .getMemory()
                .getRedis();

        JedisPooled jedisClient = new JedisPooled(redisProperties.getHost(), redisProperties.getPort());

        RedisChatMemoryRepository.Builder builder = RedisChatMemoryRepository.builder()
            .jedisClient(jedisClient)
            .keyPrefix(redisProperties.getKeyPrefix());

        String timeToLive = redisProperties.getTimeToLive();

        if (timeToLive != null && !timeToLive.isBlank()) {
            builder.timeToLive(parseDuration(timeToLive));
        }

        return builder.build();
    }

    private Duration parseDuration(String timeToLive) {
        timeToLive = timeToLive.trim()
            .toLowerCase();

        if (timeToLive.endsWith("d")) {
            return Duration.ofDays(Long.parseLong(timeToLive.substring(0, timeToLive.length() - 1)));
        } else if (timeToLive.endsWith("h")) {
            return Duration.ofHours(Long.parseLong(timeToLive.substring(0, timeToLive.length() - 1)));
        } else if (timeToLive.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(timeToLive.substring(0, timeToLive.length() - 1)));
        } else if (timeToLive.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(timeToLive.substring(0, timeToLive.length() - 1)));
        }

        return Duration.parse(timeToLive);
    }
}
