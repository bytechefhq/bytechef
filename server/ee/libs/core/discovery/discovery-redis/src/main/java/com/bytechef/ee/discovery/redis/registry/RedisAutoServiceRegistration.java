/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.discovery.redis.registry;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.cloud.client.serviceregistry.AbstractAutoServiceRegistration;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationProperties;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.context.ApplicationContext;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class RedisAutoServiceRegistration extends AbstractAutoServiceRegistration<RedisRegistration> {

    private final RedisRegistration redisRegistration;

    @SuppressFBWarnings("EI2")
    public RedisAutoServiceRegistration(
        ApplicationContext applicationContext, ServiceRegistry<RedisRegistration> serviceRegistry,
        AutoServiceRegistrationProperties properties, RedisRegistration redisRegistration) {

        super(applicationContext, serviceRegistry, properties);

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
