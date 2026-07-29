/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.grpc.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.grpc.NameResolver;
import io.grpc.Status;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class DiscoveryClientNameResolverTest {

    @Test
    void testResolvesInstancesToAddressGroups() {
        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);

        when(discoveryClient.getInstances("connection-app")).thenReturn(
            List.of(
                new DefaultServiceInstance("i1", "connection-app", "host-a", 9090, false),
                new DefaultServiceInstance("i2", "connection-app", "host-b", 9090, false)));

        DiscoveryClientNameResolver resolver = new DiscoveryClientNameResolver("connection-app", discoveryClient);
        CapturingListener listener = new CapturingListener();

        resolver.start(listener);

        assertThat(listener.result).isNotNull();
        assertThat(listener.result.getAddresses()).hasSize(2);
        assertThat(listener.error).isNull();
    }

    @Test
    void testReportsErrorWhenNoInstances() {
        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);

        when(discoveryClient.getInstances("connection-app")).thenReturn(List.of());

        DiscoveryClientNameResolver resolver = new DiscoveryClientNameResolver("connection-app", discoveryClient);
        CapturingListener listener = new CapturingListener();

        resolver.start(listener);

        assertThat(listener.result).isNull();
        assertThat(listener.error).isNotNull();
        assertThat(listener.error.getCode()).isEqualTo(Status.Code.UNAVAILABLE);
    }

    private static final class CapturingListener extends NameResolver.Listener2 {

        private NameResolver.@Nullable ResolutionResult result;
        private @Nullable Status error;

        @Override
        public void onResult(NameResolver.ResolutionResult resolutionResult) {
            this.result = resolutionResult;
        }

        @Override
        public void onError(Status status) {
            this.error = status;
        }
    }
}
