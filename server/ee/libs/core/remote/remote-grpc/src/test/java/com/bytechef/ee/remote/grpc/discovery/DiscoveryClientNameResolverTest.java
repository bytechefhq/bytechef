/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.Status;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * @version ee
 */
class DiscoveryClientNameResolverTest {

    private final DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
    private final NameResolver.Listener2 listener = mock(NameResolver.Listener2.class);

    @Test
    void testResolvesGrpcPortFromMetadata() {
        when(discoveryClient.getInstances("api-gateway-app")).thenReturn(
            List.of(instance("localhost", 8080,
                Map.of(DiscoveryClientNameResolver.GRPC_PORT_METADATA_KEY, "9090"))));

        assertThat(resolve()).containsExactly(new InetSocketAddress("localhost", 9090));
    }

    @Test
    void testInstanceWithoutGrpcPortIsSkipped() {
        when(discoveryClient.getInstances("api-gateway-app")).thenReturn(
            List.of(
                instance("localhost", 8080, Map.of()),
                instance("localhost", 8081,
                    Map.of(DiscoveryClientNameResolver.GRPC_PORT_METADATA_KEY, "9091"))));

        assertThat(resolve()).containsExactly(new InetSocketAddress("localhost", 9091));
    }

    @Test
    void testNoInstancesIsUnavailable() {
        when(discoveryClient.getInstances("api-gateway-app")).thenReturn(
            List.of(instance("localhost", 8080, Map.of())));

        new DiscoveryClientNameResolver("api-gateway-app", discoveryClient, null, null).start(listener);

        ArgumentCaptor<Status> statusArgumentCaptor = ArgumentCaptor.forClass(Status.class);

        verify(listener).onError(statusArgumentCaptor.capture());

        assertThat(statusArgumentCaptor.getValue()
            .getCode()).isEqualTo(Status.Code.UNAVAILABLE);
    }

    @Test
    void testInstancesAddedLaterAreResolvedWithoutAFailure() {
        when(discoveryClient.getInstances("api-gateway-app")).thenReturn(
            List.of(instance("localhost", 8080, Map.of(DiscoveryClientNameResolver.GRPC_PORT_METADATA_KEY, "9090"))),
            List.of(
                instance("localhost", 8080, Map.of(DiscoveryClientNameResolver.GRPC_PORT_METADATA_KEY, "9090")),
                instance("localhost", 8081, Map.of(DiscoveryClientNameResolver.GRPC_PORT_METADATA_KEY, "9091"))));

        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        DiscoveryClientNameResolver discoveryClientNameResolver = new DiscoveryClientNameResolver(
            "api-gateway-app", discoveryClient, scheduledExecutorService, Duration.ofMillis(50));

        try {
            discoveryClientNameResolver.start(listener);

            ArgumentCaptor<NameResolver.ResolutionResult> resolutionResultArgumentCaptor =
                ArgumentCaptor.forClass(NameResolver.ResolutionResult.class);

            verify(listener, timeout(2000).atLeast(2)).onResult(resolutionResultArgumentCaptor.capture());

            assertThat(resolutionResultArgumentCaptor.getValue()
                .getAddresses()).hasSize(2);
        } finally {
            discoveryClientNameResolver.shutdown();
            scheduledExecutorService.shutdownNow();
        }
    }

    private List<Object> resolve() {
        new DiscoveryClientNameResolver("api-gateway-app", discoveryClient, null, null).start(listener);

        ArgumentCaptor<NameResolver.ResolutionResult> resolutionResultArgumentCaptor =
            ArgumentCaptor.forClass(NameResolver.ResolutionResult.class);

        verify(listener).onResult(resolutionResultArgumentCaptor.capture());

        return resolutionResultArgumentCaptor.getValue()
            .getAddresses()
            .stream()
            .map(EquivalentAddressGroup::getAddresses)
            .flatMap(List::stream)
            .map(Object.class::cast)
            .toList();
    }

    private static ServiceInstance instance(String host, int port, Map<String, String> metadata) {
        return new DefaultServiceInstance(host + ":" + port, "api-gateway-app", host, port, false, metadata);
    }
}
