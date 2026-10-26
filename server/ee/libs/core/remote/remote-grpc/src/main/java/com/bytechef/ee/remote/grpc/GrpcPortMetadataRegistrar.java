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
import org.springframework.context.event.EventListener;
import org.springframework.grpc.server.lifecycle.GrpcServerStartedEvent;
import org.springframework.stereotype.Component;

/**
 * Publishes the started gRPC server's port in service discovery, so other microservices dial it instead of the HTTP
 * port.
 *
 * @version ee
 */
@Component
public class GrpcPortMetadataRegistrar {

    private final ObjectProvider<ServiceMetadataRegistry> serviceMetadataRegistryProvider;

    public GrpcPortMetadataRegistrar(ObjectProvider<ServiceMetadataRegistry> serviceMetadataRegistryProvider) {
        this.serviceMetadataRegistryProvider = serviceMetadataRegistryProvider;
    }

    @EventListener
    public void onGrpcServerStarted(GrpcServerStartedEvent grpcServerStartedEvent) {
        serviceMetadataRegistryProvider.ifAvailable(serviceMetadataRegistry -> serviceMetadataRegistry.registerMetadata(
            Map.of(
                DiscoveryClientNameResolver.GRPC_PORT_METADATA_KEY,
                String.valueOf(grpcServerStartedEvent.getPort()))));
    }
}
