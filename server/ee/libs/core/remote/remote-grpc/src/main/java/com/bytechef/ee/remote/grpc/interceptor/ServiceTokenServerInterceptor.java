/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc.interceptor;

import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.apache.commons.lang3.StringUtils;

/**
 * Rejects every call that does not carry the internal service token. A blank configured token rejects all calls.
 *
 * @version ee
 */
public class ServiceTokenServerInterceptor implements ServerInterceptor {

    private static final String HEALTH_SERVICE_NAME = "grpc.health.v1.Health";

    private final String serviceToken;

    public ServiceTokenServerInterceptor(String serviceToken) {
        this.serviceToken = serviceToken;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
        ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {

        MethodDescriptor<ReqT, RespT> methodDescriptor = serverCall.getMethodDescriptor();

        if (HEALTH_SERVICE_NAME.equals(methodDescriptor.getServiceName())) {
            return serverCallHandler.startCall(serverCall, metadata);
        }

        String providedToken = metadata.get(GrpcMetadataKeys.SERVICE_TOKEN);

        if (StringUtils.isBlank(serviceToken) || StringUtils.isBlank(providedToken) ||
            !MessageDigest.isEqual(
                serviceToken.getBytes(StandardCharsets.UTF_8), providedToken.getBytes(StandardCharsets.UTF_8))) {

            serverCall.close(
                Status.UNAUTHENTICATED.withDescription("Invalid or missing internal service token"), new Metadata());

            return new ServerCall.Listener<>() {};
        }

        return serverCallHandler.startCall(serverCall, metadata);
    }
}
