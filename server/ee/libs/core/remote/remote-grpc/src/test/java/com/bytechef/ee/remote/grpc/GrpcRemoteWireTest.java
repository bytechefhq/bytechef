/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.remote.grpc.interceptor.ServiceTokenClientInterceptor;
import com.bytechef.ee.remote.grpc.interceptor.ServiceTokenServerInterceptor;
import com.bytechef.ee.remote.grpc.interceptor.TenantContextClientInterceptor;
import com.bytechef.ee.remote.grpc.interceptor.TenantContextServerInterceptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.protobuf.services.HealthStatusManager;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class GrpcRemoteWireTest {

    @Test
    void testHealthIsServingThroughBothInterceptors() throws Exception {
        try (InProcessGrpc inProcessGrpc = start("secret")) {
            HealthCheckResponse healthCheckResponse = HealthGrpc.newBlockingStub(inProcessGrpc.channel())
                .check(HealthCheckRequest.getDefaultInstance());

            assertThat(healthCheckResponse.getStatus()).isEqualTo(HealthCheckResponse.ServingStatus.SERVING);
        }
    }

    @Test
    void testWrongTokenIsUnauthenticated() throws Exception {
        try (InProcessGrpc inProcessGrpc = new InProcessGrpc(
            new EchoService(),
            List.of(new TenantContextServerInterceptor(), new ServiceTokenServerInterceptor("secret")),
            List.of(new TenantContextClientInterceptor(), new ServiceTokenClientInterceptor("wrong")))) {

            assertThatThrownBy(() -> EchoService.echoTenant(inProcessGrpc.channel()))
                .isInstanceOfSatisfying(StatusRuntimeException.class, exception -> assertThat(exception.getStatus()
                    .getCode()).isEqualTo(Status.Code.UNAUTHENTICATED));
        }
    }

    private static InProcessGrpc start(String clientToken) throws Exception {
        HealthStatusManager healthStatusManager = new HealthStatusManager();

        return new InProcessGrpc(
            healthStatusManager.getHealthService(),
            List.of(new TenantContextServerInterceptor(), new ServiceTokenServerInterceptor("secret")),
            List.of(new TenantContextClientInterceptor(), new ServiceTokenClientInterceptor(clientToken)));
    }
}
