/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc.discovery;

import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.Status;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * Resolves a service id to the gRPC addresses of its discovered instances. Each instance's gRPC port comes from its
 * {@value #GRPC_PORT_METADATA_KEY} metadata; instances without it serve no gRPC and are skipped. With a scheduler, the
 * instances are looked up again on a fixed interval, so new ones are used without waiting for a failure, and every
 * lookup runs on that scheduler instead of the channel's thread.
 *
 * @version ee
 */
public class DiscoveryClientNameResolver extends NameResolver {

    public static final String GRPC_PORT_METADATA_KEY = "grpc-port";

    private final DiscoveryClient discoveryClient;
    private @Nullable Listener2 listener;
    private final @Nullable Duration refreshInterval;
    private @Nullable ScheduledFuture<?> refreshScheduledFuture;
    private final @Nullable ScheduledExecutorService scheduledExecutorService;
    private final String serviceId;

    public DiscoveryClientNameResolver(
        String serviceId, DiscoveryClient discoveryClient, @Nullable ScheduledExecutorService scheduledExecutorService,
        @Nullable Duration refreshInterval) {

        this.discoveryClient = discoveryClient;
        this.refreshInterval = refreshInterval;
        this.scheduledExecutorService = scheduledExecutorService;
        this.serviceId = serviceId;
    }

    @Override
    public String getServiceAuthority() {
        return serviceId;
    }

    @Override
    public void refresh() {
        if (scheduledExecutorService == null) {
            resolve();
        } else {
            scheduledExecutorService.execute(this::resolve);
        }
    }

    @Override
    public void shutdown() {
        listener = null;

        ScheduledFuture<?> currentRefreshScheduledFuture = refreshScheduledFuture;

        if (currentRefreshScheduledFuture != null) {
            currentRefreshScheduledFuture.cancel(false);
        }
    }

    @Override
    public void start(Listener2 listener) {
        this.listener = listener;

        if (scheduledExecutorService == null || refreshInterval == null) {
            resolve();

            return;
        }

        long refreshIntervalMillis = refreshInterval.toMillis();

        refreshScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(
            this::resolve, 0, refreshIntervalMillis, TimeUnit.MILLISECONDS);
    }

    private void resolve() {
        Listener2 currentListener = listener;

        if (currentListener == null) {
            return;
        }

        List<EquivalentAddressGroup> equivalentAddressGroups = new ArrayList<>();

        for (ServiceInstance serviceInstance : discoveryClient.getInstances(serviceId)) {
            Map<String, String> metadata = serviceInstance.getMetadata();

            String grpcPort = metadata == null ? null : metadata.get(GRPC_PORT_METADATA_KEY);

            if (grpcPort != null) {
                equivalentAddressGroups.add(
                    new EquivalentAddressGroup(
                        new InetSocketAddress(serviceInstance.getHost(), Integer.parseInt(grpcPort))));
            }
        }

        if (equivalentAddressGroups.isEmpty()) {
            currentListener.onError(
                Status.UNAVAILABLE.withDescription("No gRPC instances discovered for service: " + serviceId));

            return;
        }

        currentListener.onResult(
            ResolutionResult.newBuilder()
                .setAddresses(equivalentAddressGroups)
                .build());
    }
}
