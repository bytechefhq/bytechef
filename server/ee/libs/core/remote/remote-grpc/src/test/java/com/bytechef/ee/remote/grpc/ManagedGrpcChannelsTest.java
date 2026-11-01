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
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCalls;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.util.unit.DataSize;

/**
 * @version ee
 */
class ManagedGrpcChannelsTest {

    @Test
    void testTargetOverrideIsUsed() {
        try (ManagedGrpcChannels managedGrpcChannels = new ManagedGrpcChannels(
            applicationProperties("token", Map.of("api-gateway-app", "localhost:19090")), List.of())) {

            Channel channel = managedGrpcChannels.getChannel("api-gateway-app");

            assertThat(channel.authority()).isEqualTo("localhost:19090");
        }
    }

    @Test
    void testDefaultTargetIsDiscovery() {
        try (ManagedGrpcChannels managedGrpcChannels = new ManagedGrpcChannels(
            applicationProperties("token", Map.of()), List.of())) {

            assertThat(managedGrpcChannels.getTarget("api-gateway-app")).isEqualTo("discovery:///api-gateway-app");
        }
    }

    @Test
    void testChannelIsCachedPerServiceId() {
        try (ManagedGrpcChannels managedGrpcChannels = new ManagedGrpcChannels(
            applicationProperties("token", Map.of("api-gateway-app", "localhost:19090")), List.of())) {

            assertThat(managedGrpcChannels.getChannel("api-gateway-app"))
                .isSameAs(managedGrpcChannels.getChannel("api-gateway-app"));
        }
    }

    @Test
    void testBlankTokenFailsOnFirstChannel() {
        try (ManagedGrpcChannels managedGrpcChannels = new ManagedGrpcChannels(
            applicationProperties("", Map.of("api-gateway-app", "localhost:19090")), List.of())) {

            assertThatThrownBy(() -> managedGrpcChannels.getChannel("api-gateway-app"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bytechef.remote.grpc.service-token");
        }
    }

    @Test
    void testInstanceChannelIsCachedPerInstanceId() {
        try (ManagedGrpcChannels managedGrpcChannels = new ManagedGrpcChannels(
            applicationProperties("token", Map.of()), List.of())) {

            ServiceInstance first = serviceInstance("worker-app:1", 19091);
            ServiceInstance second = serviceInstance("worker-app:2", 19092);

            assertThat(managedGrpcChannels.getChannel(first)).isSameAs(managedGrpcChannels.getChannel(first));
            assertThat(managedGrpcChannels.getChannel(first)).isNotSameAs(managedGrpcChannels.getChannel(second));
        }
    }

    @Test
    void testInstanceChannelAuthenticatesAsServiceId() {
        try (ManagedGrpcChannels managedGrpcChannels = new ManagedGrpcChannels(
            applicationProperties("token", Map.of()), List.of())) {

            assertThat(managedGrpcChannels.getChannel(serviceInstance("worker-app:1", 19091))
                .authority()).isEqualTo("worker-app");
        }
    }

    @Test
    void testInstanceWithoutGrpcPortFails() {
        try (ManagedGrpcChannels managedGrpcChannels = new ManagedGrpcChannels(
            applicationProperties("token", Map.of()), List.of())) {

            ServiceInstance serviceInstance = new DefaultServiceInstance(
                "worker-app:1", "worker-app", "localhost", 8080, false, Map.of());

            assertThatThrownBy(() -> managedGrpcChannels.getChannel(serviceInstance))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("worker-app:1")
                .hasMessageContaining("grpc-port");
        }
    }

    @Test
    void testChannelAcceptsConfiguredInboundMessageSize() throws IOException {
        Server server = ServerBuilder.forPort(0)
            .addService(new EchoService())
            .build()
            .start();

        try {
            ApplicationProperties applicationProperties = applicationProperties("token", Map.of());

            applicationProperties.getRemote()
                .getGrpc()
                .setMaxInboundMessageSize(DataSize.ofMegabytes(8));

            try (ManagedGrpcChannels managedGrpcChannels = new ManagedGrpcChannels(applicationProperties, List.of())) {
                Channel channel = managedGrpcChannels.getChannel(serviceInstance("echo:1", server.getPort()));

                assertThat(EchoService.large(channel, 6 * 1024 * 1024)).hasSize(6 * 1024 * 1024);
            }

            try (ManagedGrpcChannels managedGrpcChannels = new ManagedGrpcChannels(
                applicationProperties("token", Map.of()), List.of())) {

                Channel channel = managedGrpcChannels.getChannel(serviceInstance("echo:2", server.getPort()));

                assertThatThrownBy(() -> EchoService.large(channel, 6 * 1024 * 1024))
                    .isInstanceOfSatisfying(
                        StatusRuntimeException.class,
                        statusRuntimeException -> assertThat(statusRuntimeException.getStatus()
                            .getCode()).isEqualTo(Status.Code.RESOURCE_EXHAUSTED));
            }
        } finally {
            server.shutdownNow();
        }
    }

    @Test
    void testCloseLetsAnInFlightCallFinish() throws Exception {
        Server server = ServerBuilder.forPort(0)
            .addService(new EchoService())
            .build()
            .start();

        try {
            ManagedGrpcChannels managedGrpcChannels =
                new ManagedGrpcChannels(applicationProperties("token", Map.of()), List.of());

            Channel channel = managedGrpcChannels.getChannel(serviceInstance("echo:1", server.getPort()));

            ListenableFuture<String> responseFuture = ClientCalls.futureUnaryCall(
                channel.newCall(EchoService.SLOW_ECHO, CallOptions.DEFAULT), "300");

            Thread.sleep(50);

            managedGrpcChannels.close();

            assertThat(responseFuture.get(5, TimeUnit.SECONDS)).isEqualTo("done");
        } finally {
            server.shutdownNow();
        }
    }

    private static ServiceInstance serviceInstance(String instanceId, int grpcPort) {
        return new DefaultServiceInstance(
            instanceId, "worker-app", "localhost", 8080, false, Map.of("grpc-port", String.valueOf(grpcPort)));
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
