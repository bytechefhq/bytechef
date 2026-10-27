/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.discovery.redis.registry;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.connection.RedisListCommands;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Keeps one discovery entry per instance: registering an instance again after its metadata changed replaces the entry
 * it registered before.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class RedisServiceRegistry implements ServiceRegistry<RedisRegistration> {

    private static final Logger log = LoggerFactory.getLogger(RedisServiceRegistry.class);

    private RedisRegistration redisRegistration;
    private final RedisTemplate<String, RedisRegistration> redisTemplate;
    private boolean stopped;

    @SuppressFBWarnings("EI2")
    public RedisServiceRegistry(
        RedisTemplate<String, RedisRegistration> redisTemplate, TaskExecutor taskExecutor) {
        this.redisTemplate = redisTemplate;

        taskExecutor.execute(this::periodicallyRegisterService);
    }

    @Override
    @SuppressFBWarnings("EI2")
    public void register(@Nullable RedisRegistration redisRegistration) {
        if (redisRegistration == null) {
            return;
        }

        registerService(redisRegistration);

        this.redisRegistration = redisRegistration;
    }

    @Override
    public synchronized void deregister(@Nullable RedisRegistration redisRegistration) {
        if (redisRegistration == null) {
            return;
        }

        this.redisRegistration = null;

        removeRegistrations(redisRegistration);
    }

    @Override
    public void close() {
        stopped = true;

        log.info("Redis Service Registry is closed");
    }

    @Override
    public void setStatus(RedisRegistration registration, String status) {
    }

    @Override
    public <T> T getStatus(RedisRegistration registration) {
        return null;
    }

    private void periodicallyRegisterService() {
        while (!stopped) {
            try {
                if (redisRegistration == null) {
                    TimeUnit.SECONDS.sleep(1);

                    continue;
                }

                registerService(redisRegistration);

                TimeUnit.SECONDS.sleep(10);
            } catch (Exception e) {
                if (!stopped) {
                    log.error(e.getMessage(), e);

                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException ex) {
                        if (log.isTraceEnabled()) {
                            log.trace(e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

    private static String getKey(RedisRegistration redisRegistration) {
        return "discovery/" + redisRegistration.getServiceId();
    }

    @SuppressFBWarnings("RCN")
    private synchronized void registerService(RedisRegistration redisRegistration) {
        String key = getKey(redisRegistration);

        ListOperations<String, RedisRegistration> listOperations = redisTemplate.opsForList();

        Long index = listOperations.indexOf(key, redisRegistration);

        if (index == null) {
            removeRegistrations(redisRegistration);

            listOperations.leftPush(key, redisRegistration);
        }

        redisTemplate.expire(key, 15, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unchecked")
    private void removeRegistrations(RedisRegistration redisRegistration) {
        RedisSerializer<String> keySerializer = (RedisSerializer<String>) redisTemplate.getKeySerializer();
        RedisSerializer<RedisRegistration> valueSerializer =
            (RedisSerializer<RedisRegistration>) redisTemplate.getValueSerializer();

        byte[] rawKey = Objects.requireNonNull(keySerializer.serialize(getKey(redisRegistration)), "rawKey");

        redisTemplate.execute((RedisCallback<Void>) redisConnection -> {
            RedisListCommands redisListCommands = redisConnection.listCommands();

            List<byte[]> rawRegistrations = redisListCommands.lRange(rawKey, 0, -1);

            if (rawRegistrations == null) {
                return null;
            }

            for (byte[] rawRegistration : rawRegistrations) {
                RedisRegistration registeredRedisRegistration = valueSerializer.deserialize(rawRegistration);

                if (redisRegistration.equals(registeredRedisRegistration)) {
                    redisListCommands.lRem(rawKey, 0, rawRegistration);
                }
            }

            return null;
        });
    }
}
