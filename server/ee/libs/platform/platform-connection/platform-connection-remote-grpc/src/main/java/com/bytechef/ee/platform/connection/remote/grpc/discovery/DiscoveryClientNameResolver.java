/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.grpc.discovery;

import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.Status;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * A gRPC {@link NameResolver} backed by the Spring Cloud {@link DiscoveryClient} (which {@code discovery-redis}
 * implements), so a {@code discovery:///<serviceId>} channel target resolves to the live service instances registered
 * in the same registry the REST {@code LoadBalancedRestClient} uses. Each instance becomes an
 * {@link EquivalentAddressGroup}; combined with a {@code round_robin} load-balancing policy on the channel, calls
 * distribute across instances — matching the REST discovery/LB behavior.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class DiscoveryClientNameResolver extends NameResolver {

    private final String serviceId;
    private final DiscoveryClient discoveryClient;

    private @Nullable Listener2 listener;

    public DiscoveryClientNameResolver(String serviceId, DiscoveryClient discoveryClient) {
        this.serviceId = serviceId;
        this.discoveryClient = discoveryClient;
    }

    @Override
    public String getServiceAuthority() {
        return serviceId;
    }

    @Override
    public void start(Listener2 listener) {
        this.listener = listener;

        resolve();
    }

    @Override
    public void refresh() {
        resolve();
    }

    @Override
    public void shutdown() {
        listener = null;
    }

    private void resolve() {
        Listener2 currentListener = listener;

        if (currentListener == null) {
            return;
        }

        List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceId);

        if (serviceInstances == null || serviceInstances.isEmpty()) {
            currentListener.onError(
                Status.UNAVAILABLE.withDescription("No instances discovered for service: " + serviceId));

            return;
        }

        List<EquivalentAddressGroup> addressGroups = new ArrayList<>();

        for (ServiceInstance serviceInstance : serviceInstances) {
            addressGroups.add(
                new EquivalentAddressGroup(
                    new InetSocketAddress(serviceInstance.getHost(), serviceInstance.getPort())));
        }

        currentListener.onResult(
            ResolutionResult.newBuilder()
                .setAddresses(addressGroups)
                .build());
    }
}
