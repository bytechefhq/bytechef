/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.grpc.interceptor;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import org.jspecify.annotations.Nullable;

/**
 * gRPC client interceptor that attaches the internal service token to every outgoing call, mirroring the
 * {@code X-Bytechef-Internal-Token} header the REST client sends.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class InternalServiceTokenClientInterceptor implements ClientInterceptor {

    private final @Nullable String serviceToken;

    public InternalServiceTokenClientInterceptor(@Nullable String serviceToken) {
        this.serviceToken = serviceToken;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
        MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(
            channel.newCall(methodDescriptor, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata metadata) {
                if (serviceToken != null && !serviceToken.isBlank()) {
                    metadata.put(GrpcMetadataKeys.INTERNAL_SERVICE_TOKEN, serviceToken);
                }

                super.start(responseListener, metadata);
            }
        };
    }
}
