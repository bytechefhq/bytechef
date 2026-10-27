/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.remote.grpc.EchoService;
import com.bytechef.ee.remote.grpc.InProcessGrpc;
import io.grpc.ClientInterceptor;
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
class ServiceTokenServerInterceptorTest {

    @Test
    void testMatchingTokenPasses() throws Exception {
        try (InProcessGrpc inProcessGrpc = start("secret", List.of(new ServiceTokenClientInterceptor("secret")))) {
            assertThat(EchoService.echoTenant(inProcessGrpc.channel())).isNotNull();
        }
    }

    @Test
    void testMissingTokenIsUnauthenticated() throws Exception {
        try (InProcessGrpc inProcessGrpc = start("secret", List.of())) {
            assertUnauthenticated(inProcessGrpc);
        }
    }

    @Test
    void testWrongTokenIsUnauthenticated() throws Exception {
        try (InProcessGrpc inProcessGrpc = start("secret", List.of(new ServiceTokenClientInterceptor("wrong")))) {
            assertUnauthenticated(inProcessGrpc);
        }
    }

    @Test
    void testBlankConfiguredTokenRejectsEverything() throws Exception {
        try (InProcessGrpc inProcessGrpc = start("", List.of(new ServiceTokenClientInterceptor("")))) {
            assertUnauthenticated(inProcessGrpc);
        }
    }

    @Test
    void testHealthCheckNeedsNoToken() throws Exception {
        HealthStatusManager healthStatusManager = new HealthStatusManager();

        try (InProcessGrpc inProcessGrpc = new InProcessGrpc(
            healthStatusManager.getHealthService(), List.of(new ServiceTokenServerInterceptor("secret")), List.of())) {

            HealthCheckResponse healthCheckResponse = HealthGrpc.newBlockingStub(inProcessGrpc.channel())
                .check(HealthCheckRequest.getDefaultInstance());

            assertThat(healthCheckResponse.getStatus()).isEqualTo(HealthCheckResponse.ServingStatus.SERVING);
        }
    }

    private static void assertUnauthenticated(InProcessGrpc inProcessGrpc) {
        assertThatThrownBy(() -> EchoService.echoTenant(inProcessGrpc.channel()))
            .isInstanceOfSatisfying(StatusRuntimeException.class, exception -> assertThat(exception.getStatus()
                .getCode()).isEqualTo(Status.Code.UNAUTHENTICATED));
    }

    private static InProcessGrpc start(String serverToken, List<ClientInterceptor> clientInterceptors)
        throws Exception {

        return new InProcessGrpc(
            new EchoService(), List.of(new ServiceTokenServerInterceptor(serverToken)), clientInterceptors);
    }
}
