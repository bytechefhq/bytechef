/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.discovery.redis.registry;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.cloud.client.serviceregistry.AbstractAutoServiceRegistration;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationProperties;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class RedisAutoServiceRegistration extends AbstractAutoServiceRegistration<RedisRegistration> {

    private final RedisRegistration redisRegistration;

    @SuppressFBWarnings("EI2")
    public RedisAutoServiceRegistration(
        ServiceRegistry<RedisRegistration> serviceRegistry, AutoServiceRegistrationProperties properties,
        RedisRegistration redisRegistration) {

        super(serviceRegistry, properties);

        this.redisRegistration = redisRegistration;
    }

    @Override
    protected Object getConfiguration() {
        return null;
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    @Override
    protected RedisRegistration getRegistration() {
        return redisRegistration;
    }

    @Override
    protected RedisRegistration getManagementRegistration() {
        return null;
    }
}
