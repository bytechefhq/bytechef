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
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * A gRPC service for tests: methods answer with the tenant or user bound while they run, one never answers.
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

    public static final MethodDescriptor<String, String> ECHO_USER = MethodDescriptor.<String, String>newBuilder()
        .setType(MethodDescriptor.MethodType.UNARY)
        .setFullMethodName(MethodDescriptor.generateFullMethodName("test.Echo", "EchoUser"))
        .setRequestMarshaller(new StringMarshaller())
        .setResponseMarshaller(new StringMarshaller())
        .build();

    public static final MethodDescriptor<String, String> NEVER_ANSWER = MethodDescriptor.<String, String>newBuilder()
        .setType(MethodDescriptor.MethodType.UNARY)
        .setFullMethodName(MethodDescriptor.generateFullMethodName("test.Echo", "NeverAnswer"))
        .setRequestMarshaller(new StringMarshaller())
        .setResponseMarshaller(new StringMarshaller())
        .build();

    public static final MethodDescriptor<String, String> LARGE = MethodDescriptor.<String, String>newBuilder()
        .setType(MethodDescriptor.MethodType.UNARY)
        .setFullMethodName(MethodDescriptor.generateFullMethodName("test.Echo", "Large"))
        .setRequestMarshaller(new StringMarshaller())
        .setResponseMarshaller(new StringMarshaller())
        .build();

    public static String echoTenant(Channel channel) {
        return ClientCalls.blockingUnaryCall(channel, ECHO_TENANT, CallOptions.DEFAULT, "");
    }

    public static String echoUser(Channel channel) {
        return ClientCalls.blockingUnaryCall(channel, ECHO_USER, CallOptions.DEFAULT, "");
    }

    public static String large(Channel channel, int length) {
        return ClientCalls.blockingUnaryCall(channel, LARGE, CallOptions.DEFAULT, String.valueOf(length));
    }

    public static String neverAnswer(Channel channel) {
        return ClientCalls.blockingUnaryCall(channel, NEVER_ANSWER, CallOptions.DEFAULT, "");
    }

    @Override
    public ServerServiceDefinition bindService() {
        return ServerServiceDefinition.builder("test.Echo")
            .addMethod(ECHO_TENANT, ServerCalls.asyncUnaryCall((request, responseObserver) -> {
                responseObserver.onNext(TenantContext.getCurrentTenantId());
                responseObserver.onCompleted();
            }))
            .addMethod(ECHO_USER, ServerCalls.asyncUnaryCall((request, responseObserver) -> {
                responseObserver.onNext(describeUser(SecurityContextHolder.getContext()
                    .getAuthentication()));
                responseObserver.onCompleted();
            }))
            .addMethod(LARGE, ServerCalls.asyncUnaryCall((request, responseObserver) -> {
                responseObserver.onNext("x".repeat(Integer.parseInt(request)));
                responseObserver.onCompleted();
            }))
            .addMethod(NEVER_ANSWER, ServerCalls.asyncUnaryCall((request, responseObserver) -> {}))
            .build();
    }

    public static String describeUser(Authentication authentication) {
        if (authentication == null) {
            return "";
        }

        return authentication.getName() + "|" + authentication.isAuthenticated() + "|" + authentication.getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .sorted()
            .collect(Collectors.joining(","));
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
