
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

package com.bytechef.discovery.redis.registry;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Ivica Cardic
 */
public class RedisServiceRegistry implements ServiceRegistry<RedisRegistration> {

    private static final Logger logger = LoggerFactory.getLogger(RedisServiceRegistry.class);

    private final ExecutorService executorService = Executors.newFixedThreadPool(1);
    private RedisRegistration redisRegistration;
    private final RedisTemplate<String, RedisRegistration> redisTemplate;
    private boolean stopped;

    @SuppressFBWarnings("EI2")
    public RedisServiceRegistry(RedisTemplate<String, RedisRegistration> redisTemplate) {
        this.redisTemplate = redisTemplate;

        executorService.submit(this::periodicallyRegisterService);
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

        executorService.shutdownNow();

        try {
            executorService.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // ignore
        }

        logger.info("Redis Service Registry is closed.");
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
        String serviceId = redisRegistration.getServiceId();

        ListOperations<String, RedisRegistration> listOperations = redisTemplate.opsForList();

        Long index = listOperations.indexOf(serviceId, redisRegistration);

        if (index == null) {
            listOperations.leftPush(serviceId, redisRegistration);
        }

        redisTemplate.expire(serviceId, 15, TimeUnit.SECONDS);
    }
}
