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

package com.bytechef.component.ai.agent.chat.memory.redis.session.util;

import static com.bytechef.component.ai.agent.chat.memory.redis.session.constant.RedisSessionChatMemoryConstants.DEFAULT_KEY_PREFIX;
import static com.bytechef.component.ai.agent.chat.memory.redis.session.constant.RedisSessionChatMemoryConstants.HOST;
import static com.bytechef.component.ai.agent.chat.memory.redis.session.constant.RedisSessionChatMemoryConstants.KEY_PREFIX;
import static com.bytechef.component.ai.agent.chat.memory.redis.session.constant.RedisSessionChatMemoryConstants.PASSWORD;
import static com.bytechef.component.ai.agent.chat.memory.redis.session.constant.RedisSessionChatMemoryConstants.PORT;
import static com.bytechef.component.ai.agent.chat.memory.redis.session.constant.RedisSessionChatMemoryConstants.USERNAME;

import com.bytechef.component.definition.Parameters;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.redis.RedisSessionRepository;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

/**
 * @author Ivica Cardic
 */
public class RedisSessionChatMemoryUtils {

    private static final Duration CLIENT_IDLE_TIMEOUT = Duration.ofHours(24);

    private static final Cache<JedisPoolKey, JedisPooled> JEDIS_POOLS = Caffeine.newBuilder()
        .expireAfterAccess(CLIENT_IDLE_TIMEOUT)
        .removalListener((JedisPoolKey jedisPoolKey, JedisPooled jedisPooled, RemovalCause removalCause) -> {
            if (jedisPooled != null) {
                jedisPooled.close();
            }
        })
        .build();

    private RedisSessionChatMemoryUtils() {
    }

    public static SessionRepository getSessionRepository(Parameters connectionParameters) {
        return RedisSessionRepository.builder()
            .jedis(getSharedJedisPooled(connectionParameters))
            .keyPrefix(connectionParameters.getString(KEY_PREFIX, DEFAULT_KEY_PREFIX))
            .build();
    }

    static JedisPooled getSharedJedisPooled(Parameters connectionParameters) {
        return JEDIS_POOLS.get(toJedisPoolKey(connectionParameters), RedisSessionChatMemoryUtils::buildJedisPooled);
    }

    public static JedisPooled getJedisPooled(Parameters connectionParameters) {
        return buildJedisPooled(toJedisPoolKey(connectionParameters));
    }

    private static JedisPooled buildJedisPooled(JedisPoolKey jedisPoolKey) {
        String username = jedisPoolKey.username();
        String password = jedisPoolKey.password();

        if (username != null && !username.isBlank() && (password == null || password.isBlank())) {
            throw new IllegalArgumentException("Password is required when username is provided");
        }

        if (password == null || password.isBlank()) {
            return new JedisPooled(jedisPoolKey.host(), jedisPoolKey.port());
        }

        DefaultJedisClientConfig.Builder clientConfigBuilder = DefaultJedisClientConfig.builder()
            .password(password);

        if (username != null && !username.isBlank()) {
            clientConfigBuilder.user(username);
        }

        return new JedisPooled(new HostAndPort(jedisPoolKey.host(), jedisPoolKey.port()), clientConfigBuilder.build());
    }

    private static JedisPoolKey toJedisPoolKey(Parameters connectionParameters) {
        return new JedisPoolKey(
            connectionParameters.getRequiredString(HOST), connectionParameters.getRequiredInteger(PORT),
            connectionParameters.getString(USERNAME), connectionParameters.getString(PASSWORD));
    }

    private record JedisPoolKey(String host, int port, @Nullable String username, @Nullable String password) {

        @Override
        public String toString() {
            return "JedisPoolKey{host=" + host + ", port=" + port + ", username=" + username + "}";
        }
    }
}
