/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.remote.grpc.discovery.DiscoveryClientNameResolver;
import com.bytechef.ee.remote.grpc.discovery.DiscoveryClientNameResolverProvider;
import com.bytechef.ee.remote.grpc.interceptor.DeadlineClientInterceptor;
import com.bytechef.ee.remote.grpc.interceptor.ServiceTokenClientInterceptor;
import com.bytechef.ee.remote.grpc.interceptor.TenantContextClientInterceptor;
import com.bytechef.ee.remote.grpc.interceptor.UserContextClientInterceptor;
import com.bytechef.ee.remote.grpc.security.RemotePrincipalCodec;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

/**
 * One channel per service id, using TLS unless the trusted-network switch {@code bytechef.remote.grpc.plaintext} is on.
 * A service id without a fixed target in {@code bytechef.remote.grpc.targets} is found through discovery.
 *
 * @version ee
 */
@Component
public class ManagedGrpcChannels implements AutoCloseable, GrpcChannels {

    private static final Map<String, ?> RETRY_SERVICE_CONFIG = Map.of(
        "methodConfig", List.of(
            Map.of(
                "name", List.of(Map.of()),
                "retryPolicy", Map.of(
                    "maxAttempts", 5.0,
                    "initialBackoff", "0.5s",
                    "maxBackoff", "4s",
                    "backoffMultiplier", 2.0,
                    "retryableStatusCodes", List.of("UNAVAILABLE")))));
    private static final Duration SHUTDOWN_GRACE_PERIOD = Duration.ofSeconds(10);

    private final Duration deadline;
    private final Map<String, ManagedChannel> instanceManagedChannels = new ConcurrentHashMap<>();
    private final Map<String, ManagedChannel> managedChannels = new ConcurrentHashMap<>();
    private final DataSize maxInboundMessageSize;
    private final boolean plaintext;
    private final List<RemotePrincipalCodec> remotePrincipalCodecs;
    private final String serviceToken;
    private final Map<String, String> targets;

    public ManagedGrpcChannels(
        ApplicationProperties applicationProperties, List<RemotePrincipalCodec> remotePrincipalCodecs) {

        ApplicationProperties.Remote remote = applicationProperties.getRemote();

        ApplicationProperties.Remote.Grpc grpc = remote.getGrpc();

        Map<String, String> configuredTargets = grpc.getTargets();

        this.deadline = grpc.getDeadline();
        this.maxInboundMessageSize = grpc.getMaxInboundMessageSize();
        this.plaintext = grpc.isPlaintext();
        this.remotePrincipalCodecs = List.copyOf(remotePrincipalCodecs);
        this.serviceToken = grpc.getServiceToken();
        this.targets = configuredTargets == null ? Map.of() : Map.copyOf(configuredTargets);
    }

    @Override
    public void close() {
        List<ManagedChannel> channels = new ArrayList<>(managedChannels.values());

        channels.addAll(instanceManagedChannels.values());

        managedChannels.clear();
        instanceManagedChannels.clear();

        for (ManagedChannel managedChannel : channels) {
            managedChannel.shutdown();
        }

        long deadlineNanos = System.nanoTime() + SHUTDOWN_GRACE_PERIOD.toNanos();

        for (ManagedChannel managedChannel : channels) {
            awaitTermination(managedChannel, deadlineNanos);
        }
    }

    private static void awaitTermination(ManagedChannel managedChannel, long deadlineNanos) {
        try {
            if (!managedChannel.awaitTermination(
                Math.max(0, deadlineNanos - System.nanoTime()), TimeUnit.NANOSECONDS)) {

                managedChannel.shutdownNow();
            }
        } catch (InterruptedException interruptedException) {
            managedChannel.shutdownNow();

            Thread.currentThread()
                .interrupt();
        }
    }

    @Override
    public Channel getChannel(String serviceId) {
        checkServiceToken();

        return managedChannels.computeIfAbsent(
            serviceId, curServiceId -> createManagedChannel(getTarget(curServiceId), null));
    }

    @Override
    public Channel getChannel(ServiceInstance serviceInstance) {
        checkServiceToken();

        return instanceManagedChannels.computeIfAbsent(
            serviceInstance.getInstanceId(),
            instanceId -> createManagedChannel(getInstanceTarget(serviceInstance), serviceInstance.getServiceId()));
    }

    public String getTarget(String serviceId) {
        return targets.getOrDefault(serviceId, DiscoveryClientNameResolverProvider.SCHEME + ":///" + serviceId);
    }

    private static String getInstanceTarget(ServiceInstance serviceInstance) {
        Map<String, String> metadata = serviceInstance.getMetadata();

        String grpcPort = metadata == null ? null : metadata.get(DiscoveryClientNameResolver.GRPC_PORT_METADATA_KEY);

        if (StringUtils.isBlank(grpcPort)) {
            throw new IllegalStateException(
                "Service instance %s of %s publishes no %s".formatted(
                    serviceInstance.getInstanceId(), serviceInstance.getServiceId(),
                    DiscoveryClientNameResolver.GRPC_PORT_METADATA_KEY));
        }

        return serviceInstance.getHost() + ":" + grpcPort;
    }

    private void checkServiceToken() {
        if (StringUtils.isBlank(serviceToken)) {
            throw new IllegalStateException(
                "bytechef.remote.grpc.service-token must be set to call another microservice");
        }
    }

    private ManagedChannel createManagedChannel(String target, @Nullable String authority) {
        ManagedChannelBuilder<?> managedChannelBuilder = ManagedChannelBuilder.forTarget(target)
            .defaultLoadBalancingPolicy("round_robin")
            .defaultServiceConfig(RETRY_SERVICE_CONFIG)
            .intercept(
                new TenantContextClientInterceptor(), new UserContextClientInterceptor(remotePrincipalCodecs),
                new ServiceTokenClientInterceptor(serviceToken));

        if (deadline != null) {
            managedChannelBuilder.intercept(new DeadlineClientInterceptor(deadline));
        }

        if (authority != null) {
            managedChannelBuilder.overrideAuthority(authority);
        }

        if (maxInboundMessageSize != null) {
            managedChannelBuilder.maxInboundMessageSize(Math.toIntExact(maxInboundMessageSize.toBytes()));
        }

        if (plaintext) {
            managedChannelBuilder.usePlaintext();
        } else {
            managedChannelBuilder.useTransportSecurity();
        }

        return managedChannelBuilder.build();
    }
}
