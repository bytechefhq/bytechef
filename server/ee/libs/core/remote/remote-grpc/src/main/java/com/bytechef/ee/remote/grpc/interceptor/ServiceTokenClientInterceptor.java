/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc.interceptor;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import org.apache.commons.lang3.StringUtils;

/**
 * Adds the internal service token to every outgoing call.
 *
 * @version ee
 */
public class ServiceTokenClientInterceptor implements ClientInterceptor {

    private final String serviceToken;

    public ServiceTokenClientInterceptor(String serviceToken) {
        this.serviceToken = serviceToken;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
        MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(channel.newCall(methodDescriptor, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata metadata) {
                if (StringUtils.isNotBlank(serviceToken)) {
                    metadata.put(GrpcMetadataKeys.SERVICE_TOKEN, serviceToken);
                }

                super.start(responseListener, metadata);
            }
        };
    }
}
