/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import io.grpc.Status;
import io.grpc.StatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;
import org.springframework.security.access.AccessDeniedException;

/**
 * Logs every exception a gRPC service throws and returns it to the caller with its message, so another microservice
 * sees why a call failed instead of a bare UNKNOWN status.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class GrpcRemoteExceptionHandler implements GrpcExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GrpcRemoteExceptionHandler.class);

    @Override
    public StatusException handleException(Throwable throwable) {
        log.error(throwable.getMessage(), throwable);

        Class<? extends Throwable> throwableClass = throwable.getClass();
        String message = throwable.getMessage();

        if (throwable instanceof AccessDeniedException) {
            return Status.PERMISSION_DENIED.withDescription(message == null ? throwableClass.getSimpleName() : message)
                .withCause(throwable)
                .asException();
        }

        if (isOptimisticLockingFailure(throwable)) {
            return Status.ABORTED.withDescription(message == null ? throwableClass.getSimpleName() : message)
                .withCause(throwable)
                .asException();
        }

        if (throwable instanceof IllegalArgumentException) {
            return Status.INVALID_ARGUMENT.withDescription(message == null ? throwableClass.getSimpleName() : message)
                .withCause(throwable)
                .asException();
        }

        return Status.INTERNAL.withDescription(
            message == null ? throwableClass.getSimpleName() : throwableClass.getSimpleName() + ": " + message)
            .withCause(throwable)
            .asException();
    }

    private static boolean isOptimisticLockingFailure(Throwable throwable) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof OptimisticLockingFailureException) {
                return true;
            }
        }

        return false;
    }
}
