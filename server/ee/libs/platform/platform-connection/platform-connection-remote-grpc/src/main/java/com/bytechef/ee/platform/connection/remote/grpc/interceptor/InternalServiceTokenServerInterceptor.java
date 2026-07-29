/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.grpc.interceptor;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.jspecify.annotations.Nullable;

/**
 * gRPC server interceptor mirroring {@code RemoteServiceAuthenticationFilter}: authenticates internal calls with a
 * shared service token, constant-time compared, fail-closed (a missing/blank/mismatched token, or no configured token,
 * closes the call with {@link Status#UNAUTHENTICATED}).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class InternalServiceTokenServerInterceptor implements ServerInterceptor {

    private final @Nullable String serviceToken;

    public InternalServiceTokenServerInterceptor(@Nullable String serviceToken) {
        this.serviceToken = serviceToken;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
        ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {

        String providedToken = metadata.get(GrpcMetadataKeys.INTERNAL_SERVICE_TOKEN);

        if (serviceToken == null || serviceToken.isBlank() || providedToken == null || providedToken.isBlank() ||
            !MessageDigest.isEqual(
                serviceToken.getBytes(StandardCharsets.UTF_8), providedToken.getBytes(StandardCharsets.UTF_8))) {

            serverCall.close(
                Status.UNAUTHENTICATED.withDescription("Invalid or missing internal service token"), new Metadata());

            return new ServerCall.Listener<>() {};
        }

        return serverCallHandler.startCall(serverCall, metadata);
    }
}
