/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.discovery.redis.client;

import com.bytechef.ee.discovery.redis.registry.RedisRegistration;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.data.redis.core.RedisTemplate;
import reactor.core.publisher.Flux;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class RedisReactiveDiscoveryClient implements ReactiveDiscoveryClient {

    private final RedisDiscoveryClient redisDiscoveryClient;

    public RedisReactiveDiscoveryClient(RedisTemplate<String, RedisRegistration> redisTemplate) {
        this.redisDiscoveryClient = new RedisDiscoveryClient(redisTemplate);
    }

    @Override
    public String description() {
        return null;
    }

    @Override
    public Flux<ServiceInstance> getInstances(String serviceId) {
        return Flux.fromIterable(redisDiscoveryClient.getInstances(serviceId));
    }

    @Override
    public Flux<String> getServices() {
        return Flux.fromIterable(redisDiscoveryClient.getServices());
    }
}
