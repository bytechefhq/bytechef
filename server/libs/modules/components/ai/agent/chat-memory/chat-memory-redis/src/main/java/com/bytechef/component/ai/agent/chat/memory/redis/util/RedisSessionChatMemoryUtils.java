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

import static com.bytechef.component.ai.agent.chat.memory.redis.constant.RedisChatMemoryConstants.DEFAULT_KEY_PREFIX;
import static com.bytechef.component.ai.agent.chat.memory.redis.constant.RedisChatMemoryConstants.HOST;
import static com.bytechef.component.ai.agent.chat.memory.redis.constant.RedisChatMemoryConstants.KEY_PREFIX;
import static com.bytechef.component.ai.agent.chat.memory.redis.constant.RedisChatMemoryConstants.PASSWORD;
import static com.bytechef.component.ai.agent.chat.memory.redis.constant.RedisChatMemoryConstants.PORT;
import static com.bytechef.component.ai.agent.chat.memory.redis.constant.RedisChatMemoryConstants.USERNAME;

import com.bytechef.commons.util.ClientCacheSettings;
import com.bytechef.commons.util.ClientCacheUtils;
import com.bytechef.component.definition.Parameters;
import com.github.benmanes.caffeine.cache.Cache;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.redis.RedisSessionRepository;
import redis.clients.jedis.RedisClient;

/**
 * @author Ivica Cardic
 */
public class RedisSessionChatMemoryUtils {

    private static final Cache<RedisClientKey, RedisClient> REDIS_CLIENTS = createClientCache(
        ClientCacheSettings.defaults());

    private RedisSessionChatMemoryUtils() {
    }

    static <K> Cache<K, RedisClient> createClientCache(ClientCacheSettings clientCacheSettings) {
        return ClientCacheUtils.createClientCache(clientCacheSettings, RedisClient::close);
    }

    public static SessionRepository getSessionRepository(Parameters connectionParameters) {
        return RedisSessionRepository.builder()
            .jedis(getSharedRedisClient(connectionParameters))
            .keyPrefix(getSessionKeyPrefix(connectionParameters))
            .build();
    }

    static String getSessionKeyPrefix(Parameters connectionParameters) {
        return connectionParameters.getString(KEY_PREFIX, DEFAULT_KEY_PREFIX) + "session:";
    }

    static RedisClient getSharedRedisClient(Parameters connectionParameters) {
        return REDIS_CLIENTS.get(toRedisClientKey(connectionParameters), RedisSessionChatMemoryUtils::buildRedisClient);
    }

    static RedisClient getRedisClient(Parameters connectionParameters) {
        return buildRedisClient(toRedisClientKey(connectionParameters));
    }

    private static RedisClient buildRedisClient(RedisClientKey redisClientKey) {
        String username = redisClientKey.username();
        String password = redisClientKey.password();

        if (StringUtils.isNotBlank(username) && StringUtils.isBlank(password)) {
            throw new IllegalArgumentException("Password is required when username is provided");
        }

        if (StringUtils.isBlank(password)) {
            return RedisClient.create(redisClientKey.host(), redisClientKey.port());
        }

        return RedisClient.create(
            redisClientKey.host(), redisClientKey.port(), StringUtils.trimToNull(username), password);
    }

    private static RedisClientKey toRedisClientKey(Parameters connectionParameters) {
        return new RedisClientKey(
            connectionParameters.getRequiredString(HOST), connectionParameters.getRequiredInteger(PORT),
            connectionParameters.getString(USERNAME), connectionParameters.getString(PASSWORD));
    }

    private record RedisClientKey(String host, int port, @Nullable String username, @Nullable String password) {

        @Override
        public String toString() {
            return "RedisClientKey{host=" + host + ", port=" + port + ", username=" + username + "}";
        }
    }
}
