/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.remote.grpc.discovery.DiscoveryClientNameResolverProvider;
import com.bytechef.ee.remote.grpc.interceptor.DeadlineClientInterceptor;
import com.bytechef.ee.remote.grpc.interceptor.ServiceTokenClientInterceptor;
import com.bytechef.ee.remote.grpc.interceptor.TenantContextClientInterceptor;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * One channel per service id, using TLS unless the trusted-network switch {@code bytechef.remote.grpc.plaintext} is on.
 * A service id without a fixed target in {@code bytechef.remote.grpc.targets} is found through discovery.
 *
 * @version ee
 */
@Component
public class ManagedGrpcChannels implements AutoCloseable, GrpcChannels {

    private final Duration deadline;
    private final Map<String, ManagedChannel> managedChannels = new ConcurrentHashMap<>();
    private final boolean plaintext;
    private final String serviceToken;
    private final Map<String, String> targets;

    public ManagedGrpcChannels(ApplicationProperties applicationProperties) {
        ApplicationProperties.Remote remote = applicationProperties.getRemote();

        ApplicationProperties.Remote.Grpc grpc = remote.getGrpc();

        Map<String, String> configuredTargets = grpc.getTargets();

        this.deadline = grpc.getDeadline();
        this.plaintext = grpc.isPlaintext();
        this.serviceToken = grpc.getServiceToken();
        this.targets = configuredTargets == null ? Map.of() : Map.copyOf(configuredTargets);
    }

    @Override
    public void close() {
        for (ManagedChannel managedChannel : managedChannels.values()) {
            managedChannel.shutdownNow();
        }

        managedChannels.clear();
    }

    @Override
    public Channel getChannel(String serviceId) {
        if (StringUtils.isBlank(serviceToken)) {
            throw new IllegalStateException(
                "bytechef.remote.grpc.service-token must be set to call another microservice");
        }

        return managedChannels.computeIfAbsent(serviceId, this::createManagedChannel);
    }

    public String getTarget(String serviceId) {
        return targets.getOrDefault(serviceId, DiscoveryClientNameResolverProvider.SCHEME + ":///" + serviceId);
    }

    private ManagedChannel createManagedChannel(String serviceId) {
        ManagedChannelBuilder<?> managedChannelBuilder = ManagedChannelBuilder.forTarget(getTarget(serviceId))
            .defaultLoadBalancingPolicy("round_robin")
            .intercept(new TenantContextClientInterceptor(), new ServiceTokenClientInterceptor(serviceToken));

        if (deadline != null) {
            managedChannelBuilder.intercept(new DeadlineClientInterceptor(deadline));
        }

        if (plaintext) {
            managedChannelBuilder.usePlaintext();
        } else {
            managedChannelBuilder.useTransportSecurity();
        }

        return managedChannelBuilder.build();
    }
}
