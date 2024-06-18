/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.discovery.redis.client;

import com.bytechef.ee.discovery.redis.registry.RedisRegistration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class RedisDiscoveryClient implements DiscoveryClient {

    private final RedisTemplate<String, RedisRegistration> redisTemplate;

    @SuppressFBWarnings("EI2")
    public RedisDiscoveryClient(RedisTemplate<String, RedisRegistration> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String description() {
        return "Redis Discovery Client";
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceId) {
        ListOperations<String, RedisRegistration> listOperations = redisTemplate.opsForList();

        List<RedisRegistration> redisRegistrations = listOperations.range("discovery/" + serviceId, 0, -1);

        return Validate.notNull(redisRegistrations, "redisRegistrations")
            .parallelStream()
            .collect(Collectors.toList());
    }

    @Override
    public List<String> getServices() {
        return Validate.notNull(redisTemplate.keys("discovery/*"), "keys")
            .stream()
            .map(key -> key.replace("discovery/", ""))
            .toList();
    }
}
