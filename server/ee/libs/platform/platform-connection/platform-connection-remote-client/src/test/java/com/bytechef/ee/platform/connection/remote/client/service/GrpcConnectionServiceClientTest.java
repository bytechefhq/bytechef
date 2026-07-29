/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.client.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

/**
 * Verifies the gRPC client's channel wiring and its plaintext security guard. Constructing the client builds a real
 * {@code ManagedChannel}, which requires a concrete gRPC transport ({@code grpc-netty-shaded}) on the runtime
 * classpath; a missing transport would surface here as a {@code ProviderNotFoundException} rather than only at first
 * call.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class GrpcConnectionServiceClientTest {

    @Test
    void testTlsChannelBuildsWithConfiguredTransport() {
        assertThatCode(() -> new GrpcConnectionServiceClient("dns:///connection-app:9090", "token", false))
            .doesNotThrowAnyException();
    }

    @Test
    void testPlaintextAllowedForLoopbackTarget() {
        assertThatCode(() -> new GrpcConnectionServiceClient("localhost:9090", "token", true))
            .doesNotThrowAnyException();
    }

    @Test
    void testPlaintextRejectedForNonLoopbackTarget() {
        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> new GrpcConnectionServiceClient("connection-app:9090", "token", true));
    }
}
