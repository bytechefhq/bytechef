/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.ee.discovery.metadata.ServiceMetadataRegistry;
import io.grpc.Server;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;
import org.springframework.grpc.server.lifecycle.GrpcServerStartedEvent;

/**
 * @version ee
 */
class GrpcPortMetadataRegistrarTest {

    private final Registration registration = mock(Registration.class);
    private final ServiceMetadataRegistry serviceMetadataRegistry = mock(ServiceMetadataRegistry.class);

    @SuppressWarnings("unchecked")
    private final ServiceRegistry<Registration> serviceRegistry = mock(ServiceRegistry.class);

    @Test
    void testPortIsAddedAndTheInstanceReRegistered() {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory(
            Map.of(
                "registration", registration, "serviceMetadataRegistry", serviceMetadataRegistry,
                "serviceRegistry", serviceRegistry));

        new GrpcPortMetadataRegistrar(
            beanFactory.getBeanProvider(ServiceMetadataRegistry.class), beanFactory.getBeanProvider(Registration.class),
            beanFactory.getBeanProvider(ServiceRegistry.class)).onGrpcServerStarted(startedEvent(17111));

        InOrder inOrder = inOrder(serviceMetadataRegistry, serviceRegistry);

        inOrder.verify(serviceMetadataRegistry)
            .registerMetadata(Map.of("grpc-port", "17111"));
        inOrder.verify(serviceRegistry)
            .register(registration);
    }

    @Test
    void testNoDiscoveryDoesNothing() {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory(Map.of());

        new GrpcPortMetadataRegistrar(
            beanFactory.getBeanProvider(ServiceMetadataRegistry.class), beanFactory.getBeanProvider(Registration.class),
            beanFactory.getBeanProvider(ServiceRegistry.class)).onGrpcServerStarted(startedEvent(17111));

        verify(serviceRegistry, never()).register(any());
    }

    private static GrpcServerStartedEvent startedEvent(int port) {
        return new GrpcServerStartedEvent(mock(GrpcServerLifecycle.class), mock(Server.class), "localhost", port);
    }
}
