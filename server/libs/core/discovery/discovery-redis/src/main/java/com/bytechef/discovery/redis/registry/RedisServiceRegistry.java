
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
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 *
 * @author Ivica Cardic
 */
public class RedisServiceRegistry implements ServiceRegistry<RedisRegistration> {

    private static final Logger logger = LoggerFactory.getLogger(RedisServiceRegistry.class);

    private final StringRedisTemplate redisTemplate;

    @SuppressFBWarnings("EI2")
    public RedisServiceRegistry(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void register(RedisRegistration redisRegistration) {
        String serviceId = redisRegistration.getServiceId();

        ListOperations<String, String> listOperations = redisTemplate.opsForList();

        listOperations.leftPush(serviceId, redisRegistration.getHost() + ":" + redisRegistration.getPort());
    }

    @Override
    public void deregister(RedisRegistration registration) {
        ListOperations<String, String> listOperations = redisTemplate.opsForList();

        listOperations.remove(registration.getServiceId(), 1, registration.getHost() + ":" + registration.getPort());
    }

    @Override
    public void close() {
        logger.info("Redis Service Registry is closed.");
    }

    @Override
    public void setStatus(RedisRegistration registration, String status) {
    }

    @Override
    public <T> T getStatus(RedisRegistration registration) {
        return null;
    }
}
