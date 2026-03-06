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

package com.bytechef.component.ai.agent.chat.memory.redis.util;

import static com.bytechef.component.ai.agent.chat.memory.redis.constant.RedisChatMemoryConstants.HOST;
import static com.bytechef.component.ai.agent.chat.memory.redis.constant.RedisChatMemoryConstants.KEY_PREFIX;
import static com.bytechef.component.ai.agent.chat.memory.redis.constant.RedisChatMemoryConstants.PASSWORD;
import static com.bytechef.component.ai.agent.chat.memory.redis.constant.RedisChatMemoryConstants.PORT;
import static com.bytechef.component.ai.agent.chat.memory.redis.constant.RedisChatMemoryConstants.TIME_TO_LIVE;
import static com.bytechef.component.ai.agent.chat.memory.redis.constant.RedisChatMemoryConstants.USERNAME;

import com.bytechef.component.definition.Parameters;
import java.time.Duration;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import redis.clients.jedis.JedisPooled;

/**
 * @author Ivica Cardic
 */
public class RedisChatMemoryUtils {

    private static final String DEFAULT_KEY_PREFIX = "bytechef-chat-memory:";

    public static ChatMemoryRepository getChatMemoryRepository(Parameters connectionParameters) {
        JedisPooled jedisClient = getJedisClient(connectionParameters);
        String keyPrefix = connectionParameters.getString(KEY_PREFIX, DEFAULT_KEY_PREFIX);

        RedisChatMemoryRepository.Builder builder = RedisChatMemoryRepository.builder()
            .jedisClient(jedisClient)
            .keyPrefix(keyPrefix);

        String timeToLive = connectionParameters.getString(TIME_TO_LIVE);

        if (timeToLive != null && !timeToLive.isBlank()) {
            builder.timeToLive(parseDuration(timeToLive));
        }

        return builder.build();
    }

    public static JedisPooled getJedisClient(Parameters connectionParameters) {
        String host = connectionParameters.getRequiredString(HOST);
        int port = connectionParameters.getRequiredInteger(PORT);
        String username = connectionParameters.getString(USERNAME);
        String password = connectionParameters.getString(PASSWORD);

        if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
            return new JedisPooled(host, port, username, password);
        } else if (password != null && !password.isBlank()) {
            return new JedisPooled(host, port, null, password);
        }

        return new JedisPooled(host, port);
    }

    private static Duration parseDuration(String timeToLive) {
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

    private RedisChatMemoryUtils() {
    }
}
