/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * A gRPC service for tests: methods answer with the tenant or user bound while they run, one never answers.
 *
 * @version ee
 */
public final class EchoService implements BindableService {

    public static final MethodDescriptor<String, String> ECHO_PRINCIPAL =
        MethodDescriptor.<String, String>newBuilder()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName(MethodDescriptor.generateFullMethodName("test.Echo", "EchoPrincipal"))
            .setRequestMarshaller(new StringMarshaller())
            .setResponseMarshaller(new StringMarshaller())
            .build();

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

    public static final MethodDescriptor<String, String> SLOW_ECHO = MethodDescriptor.<String, String>newBuilder()
        .setType(MethodDescriptor.MethodType.UNARY)
        .setFullMethodName(MethodDescriptor.generateFullMethodName("test.Echo", "SlowEcho"))
        .setRequestMarshaller(new StringMarshaller())
        .setResponseMarshaller(new StringMarshaller())
        .build();

    public static final MethodDescriptor<String, String> LARGE = MethodDescriptor.<String, String>newBuilder()
        .setType(MethodDescriptor.MethodType.UNARY)
        .setFullMethodName(MethodDescriptor.generateFullMethodName("test.Echo", "Large"))
        .setRequestMarshaller(new StringMarshaller())
        .setResponseMarshaller(new StringMarshaller())
        .build();

    private final AtomicReference<Authentication> lastAuthentication = new AtomicReference<>();

    public static String echoPrincipal(Channel channel) {
        return ClientCalls.blockingUnaryCall(channel, ECHO_PRINCIPAL, CallOptions.DEFAULT, "");
    }

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

    public Authentication getLastAuthentication() {
        return lastAuthentication.get();
    }

    @Override
    public ServerServiceDefinition bindService() {
        return ServerServiceDefinition.builder("test.Echo")
            .addMethod(ECHO_PRINCIPAL, ServerCalls.asyncUnaryCall((request, responseObserver) -> {
                SecurityContext securityContext = SecurityContextHolder.getContext();

                Authentication authentication = securityContext.getAuthentication();

                lastAuthentication.set(authentication);

                responseObserver.onNext(describePrincipal(authentication));
                responseObserver.onCompleted();
            }))
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
            .addMethod(SLOW_ECHO, ServerCalls.asyncUnaryCall((request, responseObserver) -> {
                try {
                    Thread.sleep(Long.parseLong(request));
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread()
                        .interrupt();
                }

                responseObserver.onNext("done");
                responseObserver.onCompleted();
            }))
            .build();
    }

    public static String describePrincipal(Authentication authentication) {
        if (authentication instanceof AbstractApiKeyAuthenticationToken apiKeyAuthenticationToken) {
            return "api-key|" + apiKeyAuthenticationToken.getName() + "|" +
                apiKeyAuthenticationToken.getEnvironmentId() + "|" + joinAuthorities(apiKeyAuthenticationToken);
        }

        return describeUser(authentication);
    }

    public static String describeUser(Authentication authentication) {
        if (authentication == null) {
            return "";
        }

        return authentication.getName() + "|" + authentication.isAuthenticated() + "|" +
            joinAuthorities(authentication);
    }

    private static String joinAuthorities(Authentication authentication) {
        return authentication.getAuthorities()
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
