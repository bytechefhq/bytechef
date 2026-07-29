/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.connection.remote.grpc.config.ConnectionGrpcServerConfiguration;
import com.bytechef.ee.platform.connection.remote.grpc.interceptor.InternalServiceTokenClientInterceptor;
import com.bytechef.ee.platform.connection.remote.grpc.interceptor.TenantContextClientInterceptor;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Integration test proving that the spring-grpc server auto-configuration boots under Spring Boot 4 and serves the
 * Connection gRPC service (with its auth + tenant interceptors), by starting a real Netty gRPC server via the starter
 * and calling it through a Netty client channel.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@org.springframework.boot.test.context.SpringBootTest(
    classes = ConnectionGrpcServerAutoConfigIntTest.TestApp.class,
    properties = {
        "bytechef.remote.transport=grpc",
        "bytechef.internal.service-token=test-token",
        "spring.grpc.server.enabled=true",
        "spring.grpc.server.ssl.enabled=false",
        "spring.grpc.server.observation.enabled=false",
        "spring.grpc.server.health.enabled=false",
        "spring.grpc.server.reflection.enabled=false"
    })
class ConnectionGrpcServerAutoConfigIntTest {

    private static final int PORT = findFreePort();

    @Autowired
    private ConnectionService connectionService;

    private ManagedChannel channel;

    @DynamicPropertySource
    static void grpcServerPort(DynamicPropertyRegistry registry) {
        registry.add("spring.grpc.server.port", () -> PORT);
    }

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();

        if (channel != null) {
            channel.shutdownNow();
        }
    }

    @Test
    void testSpringGrpcServerServesConnectionServiceWithInterceptors() throws Exception {
        AtomicReference<String> tenantSeenOnServer = new AtomicReference<>();

        when(connectionService.getConnection(42L)).thenAnswer(invocation -> {
            tenantSeenOnServer.set(TenantContext.getCurrentTenantId());

            Connection connection = new Connection();

            connection.setId(42L);
            connection.setName("Prod Slack");
            connection.setType(PlatformType.AUTOMATION);

            return connection;
        });

        channel = ManagedChannelBuilder.forAddress("localhost", PORT)
            .usePlaintext()
            .intercept(
                new TenantContextClientInterceptor(),
                new InternalServiceTokenClientInterceptor("test-token"))
            .build();

        TenantContext.setCurrentTenantId("acme");

        ConnectionProto proto = ConnectionRemoteServiceGrpc.newBlockingStub(channel)
            .getConnection(
                GetConnectionRequest.newBuilder()
                    .setId(42L)
                    .build());

        assertThat(proto.getId()).isEqualTo(42L);
        assertThat(proto.getName()).isEqualTo("Prod Slack");
        assertThat(tenantSeenOnServer.get()).isEqualTo("acme");

        channel.shutdown()
            .awaitTermination(5, TimeUnit.SECONDS);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(ConnectionGrpcServerConfiguration.class)
    static class TestApp {

        @Bean
        ConnectionService connectionService() {
            return mock(ConnectionService.class);
        }
    }

    @SuppressFBWarnings("UNENCRYPTED_SERVER_SOCKET")
    private static int findFreePort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
