/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.discovery.redis.registry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationProperties;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @version ee
 */
class RedisAutoServiceRegistrationTest {

    private final ApplicationContext applicationContext = mock(ApplicationContext.class);
    private final RedisRegistration redisRegistration = new RedisRegistration();
    @SuppressWarnings("unchecked")
    private final ServiceRegistry<RedisRegistration> serviceRegistry = mock(ServiceRegistry.class);
    private RedisAutoServiceRegistration redisAutoServiceRegistration;

    @BeforeEach
    void beforeEach() {
        ReflectionTestUtils.setField(redisRegistration, "host", "localhost");
        ReflectionTestUtils.setField(redisRegistration, "instanceId", "worker-app:1");
        ReflectionTestUtils.setField(redisRegistration, "port", 8080);
        ReflectionTestUtils.setField(redisRegistration, "serviceId", "worker-app");

        redisAutoServiceRegistration = new RedisAutoServiceRegistration(
            applicationContext, serviceRegistry, new AutoServiceRegistrationProperties(), redisRegistration);

        redisAutoServiceRegistration.start();
    }

    @Test
    void testClosingTheApplicationContextDeregistersBeforeBeansAreDestroyed() {
        redisAutoServiceRegistration.onContextClosed(new ContextClosedEvent(applicationContext));

        verify(serviceRegistry).deregister(redisRegistration);
    }

    @Test
    void testClosingAnotherApplicationContextKeepsTheRegistration() {
        redisAutoServiceRegistration.onContextClosed(new ContextClosedEvent(mock(ApplicationContext.class)));

        verify(serviceRegistry, never()).deregister(any());
    }
}
