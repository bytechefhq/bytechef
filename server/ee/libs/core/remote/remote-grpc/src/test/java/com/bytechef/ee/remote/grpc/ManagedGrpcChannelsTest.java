/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.config.ApplicationProperties;
import io.grpc.Channel;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class ManagedGrpcChannelsTest {

    @Test
    void testTargetOverrideIsUsed() {
        try (ManagedGrpcChannels managedGrpcChannels = new ManagedGrpcChannels(
            applicationProperties("token", Map.of("api-gateway-app", "localhost:19090")))) {

            Channel channel = managedGrpcChannels.getChannel("api-gateway-app");

            assertThat(channel.authority()).isEqualTo("localhost:19090");
        }
    }

    @Test
    void testDefaultTargetIsDiscovery() {
        try (ManagedGrpcChannels managedGrpcChannels = new ManagedGrpcChannels(
            applicationProperties("token", Map.of()))) {

            assertThat(managedGrpcChannels.getTarget("api-gateway-app")).isEqualTo("discovery:///api-gateway-app");
        }
    }

    @Test
    void testChannelIsCachedPerServiceId() {
        try (ManagedGrpcChannels managedGrpcChannels = new ManagedGrpcChannels(
            applicationProperties("token", Map.of("api-gateway-app", "localhost:19090")))) {

            assertThat(managedGrpcChannels.getChannel("api-gateway-app"))
                .isSameAs(managedGrpcChannels.getChannel("api-gateway-app"));
        }
    }

    @Test
    void testBlankTokenFailsOnFirstChannel() {
        try (ManagedGrpcChannels managedGrpcChannels = new ManagedGrpcChannels(
            applicationProperties("", Map.of("api-gateway-app", "localhost:19090")))) {

            assertThatThrownBy(() -> managedGrpcChannels.getChannel("api-gateway-app"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bytechef.remote.grpc.service-token");
        }
    }

    private static ApplicationProperties applicationProperties(String serviceToken, Map<String, String> targets) {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Remote.Grpc grpc = applicationProperties.getRemote()
            .getGrpc();

        grpc.setPlaintext(true);
        grpc.setServiceToken(serviceToken);
        grpc.setTargets(targets);

        return applicationProperties;
    }
}
