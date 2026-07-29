/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.connection.remote.grpc.interceptor.InternalServiceTokenClientInterceptor;
import com.bytechef.ee.platform.connection.remote.grpc.interceptor.InternalServiceTokenServerInterceptor;
import com.bytechef.ee.platform.connection.remote.grpc.interceptor.TenantContextClientInterceptor;
import com.bytechef.ee.platform.connection.remote.grpc.interceptor.TenantContextServerInterceptor;
import com.bytechef.ee.platform.connection.remote.grpc.server.ConnectionRemoteServiceGrpcServer;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.tenant.TenantContext;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * End-to-end in-process gRPC test proving the transport + interceptor parity: a real server (service impl + auth/tenant
 * server interceptors) is called through a real client (blocking stub + auth/tenant client interceptors). Asserts the
 * mapped result, that the tenant id propagates from the caller's {@link TenantContext} to the server handler thread,
 * and that a call without a valid internal token is rejected with {@code UNAUTHENTICATED}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectionRemoteServiceGrpcWireTest {

    private static final String SERVICE_TOKEN = "s3cret-internal-token";

    private ConnectionService connectionService;
    private Server server;

    @BeforeEach
    void beforeEach() throws Exception {
        connectionService = mock(ConnectionService.class);

        server = InProcessServerBuilder.forName("connection-grpc-wire-test")
            .addService(
                ServerInterceptors.intercept(
                    new ConnectionRemoteServiceGrpcServer(connectionService),
                    new TenantContextServerInterceptor(),
                    new InternalServiceTokenServerInterceptor(SERVICE_TOKEN)))
            .build()
            .start();

        TenantContext.resetCurrentTenantId();
    }

    @AfterEach
    void afterEach() throws Exception {
        TenantContext.resetCurrentTenantId();

        server.shutdownNow()
            .awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void testGetConnectionMapsResultAndPropagatesTenant() {
        AtomicReference<String> tenantSeenOnServer = new AtomicReference<>();

        when(connectionService.getConnection(42L)).thenAnswer(invocation -> {
            tenantSeenOnServer.set(TenantContext.getCurrentTenantId());

            return buildConnection();
        });

        TenantContext.setCurrentTenantId("acme");

        ConnectionProto proto = blockingStub(SERVICE_TOKEN).getConnection(
            GetConnectionRequest.newBuilder()
                .setId(42L)
                .build());

        assertThat(proto.getId()).isEqualTo(42L);
        assertThat(proto.getName()).isEqualTo("Prod Slack");
        assertThat(proto.getComponentName()).isEqualTo("slack");
        assertThat(tenantSeenOnServer.get()).isEqualTo("acme");
    }

    @Test
    void testGetConnectionsReturnsList() {
        when(connectionService.getConnections(PlatformType.AUTOMATION)).thenReturn(List.of(buildConnection()));

        TenantContext.setCurrentTenantId("acme");

        ConnectionListProto list = blockingStub(SERVICE_TOKEN).getConnections(
            GetConnectionsRequest.newBuilder()
                .setType(PlatformTypeProto.AUTOMATION)
                .build());

        assertThat(list.getConnectionsList()).hasSize(1);
        assertThat(list.getConnections(0)
            .getName()).isEqualTo("Prod Slack");
    }

    @Test
    void testCallWithoutValidTokenIsRejected() {
        assertThatThrownBy(
            () -> blockingStub("wrong-token").getConnection(
                GetConnectionRequest.newBuilder()
                    .setId(42L)
                    .build()))
                        .isInstanceOfSatisfying(
                            StatusRuntimeException.class,
                            exception -> assertThat(exception.getStatus()
                                .getCode()).isEqualTo(Status.Code.UNAUTHENTICATED));
    }

    private ConnectionRemoteServiceGrpc.ConnectionRemoteServiceBlockingStub blockingStub(String token) {
        ManagedChannel channel = InProcessChannelBuilder.forName("connection-grpc-wire-test")
            .intercept(
                new TenantContextClientInterceptor(),
                new InternalServiceTokenClientInterceptor(token))
            .build();

        return ConnectionRemoteServiceGrpc.newBlockingStub(channel);
    }

    private static Connection buildConnection() {
        Connection connection = new Connection();

        connection.setId(42L);
        connection.setName("Prod Slack");
        connection.setComponentName("slack");
        connection.setType(PlatformType.AUTOMATION);

        return connection;
    }
}
