/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import com.bytechef.ee.discovery.metadata.ServiceMetadataRegistry;
import com.bytechef.ee.remote.grpc.discovery.DiscoveryClientNameResolver;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.grpc.server.lifecycle.GrpcServerStartedEvent;
import org.springframework.stereotype.Component;

/**
 * Publishes the started gRPC server's port in service discovery, so other microservices dial it instead of the HTTP
 * port. The instance is registered again, since discovery may have written it before the gRPC server started.
 *
 * @version ee
 */
@Component
public class GrpcPortMetadataRegistrar {

    private final ObjectProvider<Registration> registrationProvider;
    private final ObjectProvider<ServiceMetadataRegistry> serviceMetadataRegistryProvider;

    @SuppressWarnings("rawtypes")
    private final ObjectProvider<ServiceRegistry> serviceRegistryProvider;

    @SuppressWarnings("rawtypes")
    public GrpcPortMetadataRegistrar(
        ObjectProvider<ServiceMetadataRegistry> serviceMetadataRegistryProvider,
        ObjectProvider<Registration> registrationProvider, ObjectProvider<ServiceRegistry> serviceRegistryProvider) {

        this.registrationProvider = registrationProvider;
        this.serviceMetadataRegistryProvider = serviceMetadataRegistryProvider;
        this.serviceRegistryProvider = serviceRegistryProvider;
    }

    @EventListener
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    public void onGrpcServerStarted(GrpcServerStartedEvent grpcServerStartedEvent) {
        ServiceMetadataRegistry serviceMetadataRegistry = serviceMetadataRegistryProvider.getIfAvailable();

        if (serviceMetadataRegistry == null) {
            return;
        }

        serviceMetadataRegistry.registerMetadata(
            Map.of(
                DiscoveryClientNameResolver.GRPC_PORT_METADATA_KEY, String.valueOf(grpcServerStartedEvent.getPort())));

        Registration registration = registrationProvider.getIfAvailable();
        ServiceRegistry serviceRegistry = serviceRegistryProvider.getIfAvailable();

        if (registration != null && serviceRegistry != null) {
            serviceRegistry.register(registration);
        }
    }
}
