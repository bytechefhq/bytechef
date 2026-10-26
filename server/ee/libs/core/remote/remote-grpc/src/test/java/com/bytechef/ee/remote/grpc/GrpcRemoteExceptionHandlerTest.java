/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.Status;
import io.grpc.StatusException;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class GrpcRemoteExceptionHandlerTest {

    private final GrpcRemoteExceptionHandler grpcRemoteExceptionHandler = new GrpcRemoteExceptionHandler();

    @Test
    void testIllegalArgumentBecomesInvalidArgumentWithItsMessage() {
        StatusException statusException = grpcRemoteExceptionHandler.handleException(
            new IllegalArgumentException("No worker instance offers component missing"));

        assertThat(statusException.getStatus()
            .getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(statusException.getStatus()
            .getDescription()).isEqualTo("No worker instance offers component missing");
    }

    @Test
    void testAccessDeniedBecomesPermissionDenied() {
        StatusException statusException = grpcRemoteExceptionHandler.handleException(
            new AccessDeniedException("Only the connection creator or an admin can modify connection 7"));

        assertThat(statusException.getStatus()
            .getCode()).isEqualTo(Status.Code.PERMISSION_DENIED);
        assertThat(statusException.getStatus()
            .getDescription()).isEqualTo("Only the connection creator or an admin can modify connection 7");
    }

    @Test
    void testOtherExceptionsBecomeInternalWithTheirMessage() {
        StatusException statusException = grpcRemoteExceptionHandler.handleException(
            new IllegalStateException("Component version 3 not found"));

        assertThat(statusException.getStatus()
            .getCode()).isEqualTo(Status.Code.INTERNAL);
        assertThat(statusException.getStatus()
            .getDescription()).isEqualTo("IllegalStateException: Component version 3 not found");
    }

    @Test
    void testExceptionWithoutMessageIsNamedByItsType() {
        StatusException statusException =
            grpcRemoteExceptionHandler.handleException(new UnsupportedOperationException());

        assertThat(statusException.getStatus()
            .getDescription()).isEqualTo("UnsupportedOperationException");
    }
}
