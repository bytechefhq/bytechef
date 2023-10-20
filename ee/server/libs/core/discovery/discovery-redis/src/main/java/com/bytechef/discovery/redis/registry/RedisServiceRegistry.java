/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.discovery.redis.registry;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class RedisServiceRegistry implements ServiceRegistry<RedisRegistration> {

    private static final Logger logger = LoggerFactory.getLogger(RedisServiceRegistry.class);

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
    public void register(RedisRegistration redisRegistration) {
        registerService(redisRegistration);

        this.redisRegistration = redisRegistration;
    }

    @Override
    public void deregister(RedisRegistration redisRegistration) {
        ListOperations<String, RedisRegistration> listOperations = redisTemplate.opsForList();

        listOperations.remove(redisRegistration.getServiceId(), 1, redisRegistration);

        this.redisRegistration = null;
    }

    @Override
    public void close() {
        stopped = true;

        logger.info("Redis Service Registry is closed");
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
                    logger.error(e.getMessage(), e);

                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException ex) {
                        // ignore
                    }
                }
            }
        }
    }

    @SuppressFBWarnings("RCN")
    private void registerService(RedisRegistration redisRegistration) {
        String serviceId = "discovery/" + redisRegistration.getServiceId();

        ListOperations<String, RedisRegistration> listOperations = redisTemplate.opsForList();

        Long index = listOperations.indexOf(serviceId, redisRegistration);

        if (index == null) {
            listOperations.leftPush(serviceId, redisRegistration);
        }

        redisTemplate.expire(serviceId, 15, TimeUnit.SECONDS);
    }
}
