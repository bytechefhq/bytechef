/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.session.config.autoconfigure;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.session.data.redis.autoconfigure.SessionDataRedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.session.data.redis.RedisSessionRepository;

/**
 * Opens the Redis connection of the HTTP session store before the web server accepts requests. Opened lazily, the first
 * session lookup would open it on a request thread, where Reactor's automatic context propagation resolves the deferred
 * security context, which looks up the same session again and recurses without end.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@AutoConfiguration(after = SessionDataRedisAutoConfiguration.class)
@ConditionalOnBean(RedisSessionRepository.class)
public class RedisSessionConnectionAutoConfiguration {

    @Bean
    SmartInitializingSingleton redisSessionConnectionInitializer(RedisSessionRepository redisSessionRepository) {
        return () -> {
            RedisOperations<String, Object> sessionRedisOperations = redisSessionRepository.getSessionRedisOperations();

            sessionRedisOperations.execute((RedisCallback<String>) RedisConnection::ping);
        };
    }
}
