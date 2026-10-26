/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import com.bytechef.tenant.TenantContext;
import io.grpc.BindableService;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.ServerCalls;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * A one-method gRPC service for tests: it answers with the tenant bound while it runs.
 *
 * @version ee
 */
public final class EchoService implements BindableService {

    public static final MethodDescriptor<String, String> ECHO_TENANT = MethodDescriptor.<String, String>newBuilder()
        .setType(MethodDescriptor.MethodType.UNARY)
        .setFullMethodName(MethodDescriptor.generateFullMethodName("test.Echo", "EchoTenant"))
        .setRequestMarshaller(new StringMarshaller())
        .setResponseMarshaller(new StringMarshaller())
        .build();

    public static String echoTenant(Channel channel) {
        return ClientCalls.blockingUnaryCall(channel, ECHO_TENANT, CallOptions.DEFAULT, "");
    }

    @Override
    public ServerServiceDefinition bindService() {
        return ServerServiceDefinition.builder("test.Echo")
            .addMethod(ECHO_TENANT, ServerCalls.asyncUnaryCall((request, responseObserver) -> {
                responseObserver.onNext(TenantContext.getCurrentTenantId());
                responseObserver.onCompleted();
            }))
            .build();
    }

    private static final class StringMarshaller implements MethodDescriptor.Marshaller<String> {

        @Override
        public InputStream stream(String value) {
            return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public String parse(InputStream stream) {
            try {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException ioException) {
                throw new UncheckedIOException(ioException);
            }
        }
    }
}
