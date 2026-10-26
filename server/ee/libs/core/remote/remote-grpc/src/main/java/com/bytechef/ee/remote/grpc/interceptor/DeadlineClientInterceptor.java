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
import io.grpc.MethodDescriptor;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Gives every call without its own deadline a default one, so a hung microservice cannot hold the caller's thread
 * forever.
 *
 * @version ee
 */
public class DeadlineClientInterceptor implements ClientInterceptor {

    private final long deadlineMillis;

    public DeadlineClientInterceptor(Duration deadline) {
        this.deadlineMillis = deadline.toMillis();
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
        MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {

        if (callOptions.getDeadline() == null) {
            return channel.newCall(
                methodDescriptor, callOptions.withDeadlineAfter(deadlineMillis, TimeUnit.MILLISECONDS));
        }

        return channel.newCall(methodDescriptor, callOptions);
    }
}
