/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.remote.grpc.EchoService;
import com.bytechef.ee.remote.grpc.InProcessGrpc;
import com.bytechef.ee.remote.grpc.security.RemoteApiKeyAuthenticationToken;
import com.bytechef.ee.remote.grpc.security.RemoteConnectedUserAuthenticationToken;
import com.bytechef.platform.security.web.authentication.TestConnectedUserAuthentication;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 */
class UserContextServerInterceptorTest {

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCallCarriesTheUser() throws Exception {
        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                    "admin@localhost.com", null,
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"))));

        try (InProcessGrpc inProcessGrpc = start()) {
            assertThat(EchoService.echoUser(inProcessGrpc.channel()))
                .isEqualTo("admin@localhost.com|true|ROLE_ADMIN,ROLE_USER");
        }
    }

    @Test
    void testCallWithoutUserCarriesNoIdentity() throws Exception {
        try (InProcessGrpc inProcessGrpc = start()) {
            assertThat(EchoService.echoUser(inProcessGrpc.channel())).isEmpty();

            SecurityContextHolder.getContext()
                .setAuthentication(
                    new AnonymousAuthenticationToken(
                        "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

            assertThat(EchoService.echoUser(inProcessGrpc.channel())).isEmpty();
        }
    }

    @Test
    void testApiKeyPrincipalCarriesNoIdentity() throws Exception {
        TestingAuthenticationToken apiKeyAuthenticationToken = new TestingAuthenticationToken(
            new User("external-user-1", "", List.of(new SimpleGrantedAuthority("ROLE_USER"))), null, "ROLE_USER");

        apiKeyAuthenticationToken.setAuthenticated(true);

        SecurityContextHolder.getContext()
            .setAuthentication(apiKeyAuthenticationToken);

        try (InProcessGrpc inProcessGrpc = start()) {
            assertThat(EchoService.echoUser(inProcessGrpc.channel())).isEmpty();
        }
    }

    @Test
    void testNonAsciiLoginAndCommaAuthoritySurvive() throws Exception {
        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                    "jos\u00e9@example.com", null,
                    List.of(new SimpleGrantedAuthority("ROLE_A,B"), new SimpleGrantedAuthority("ROLE_USER"))));

        try (InProcessGrpc inProcessGrpc = start()) {
            assertThat(EchoService.echoUser(inProcessGrpc.channel()))
                .isEqualTo("jos\u00e9@example.com|true|ROLE_A,B,ROLE_USER");
        }
    }

    @Test
    void testIdentityIsClearedAfterTheCall() throws Exception {
        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                    "user@localhost.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        try (InProcessGrpc inProcessGrpc = start()) {
            assertThat(EchoService.echoUser(inProcessGrpc.channel())).startsWith("user@localhost.com|");
            assertThat(inProcessGrpc.serverExecutorService()
                .submit(() -> EchoService.describeUser(SecurityContextHolder.getContext()
                    .getAuthentication()))
                .get()).isEmpty();
        }
    }

    @Test
    void testConnectedUserIsRebuiltAsConnectedUser() throws Exception {
        SecurityContextHolder.getContext()
            .setAuthentication(new TestConnectedUserAuthentication("external-user-1", 7L, 2L, true));

        try (InProcessGrpc inProcessGrpc = start()) {
            assertThat(EchoService.echoPrincipal(inProcessGrpc.channel()))
                .isEqualTo("connected-user|external-user-1|7|2|false");
        }
    }

    @Test
    void testConnectedUserSurvivesASecondHop() throws Exception {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new RemoteConnectedUserAuthenticationToken(2L, 7L, new User("external-user-1", "", List.of()), true));

        try (InProcessGrpc inProcessGrpc = start()) {
            assertThat(EchoService.echoPrincipal(inProcessGrpc.channel()))
                .isEqualTo("connected-user|external-user-1|7|2|true");
        }
    }

    @Test
    void testUnauthenticatedConnectedUserCarriesNoIdentity() throws Exception {
        SecurityContextHolder.getContext()
            .setAuthentication(new TestConnectedUserAuthentication("external-user-1", 7L, 2L, false));

        try (InProcessGrpc inProcessGrpc = start()) {
            assertThat(EchoService.echoUser(inProcessGrpc.channel())).isEmpty();
        }
    }

    @Test
    void testApiKeyUserKeepsEnvironmentAndAuthorities() throws Exception {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new RemoteApiKeyAuthenticationToken(
                    1L, new User("api@localhost.com", "", List.of(new SimpleGrantedAuthority("ROLE_USER")))));

        try (InProcessGrpc inProcessGrpc = start()) {
            assertThat(EchoService.echoPrincipal(inProcessGrpc.channel()))
                .isEqualTo("api-key|api@localhost.com|1|ROLE_USER");
        }
    }

    @Test
    void testLoginWithoutKindBindsNoIdentity() throws Exception {
        try (InProcessGrpc inProcessGrpc = start(metadata -> metadata.put(
            GrpcMetadataKeys.USER_LOGIN, "admin@localhost.com".getBytes(StandardCharsets.UTF_8)))) {

            assertThat(EchoService.echoUser(inProcessGrpc.channel())).isEmpty();
        }
    }

    @Test
    void testUnknownKindBindsNoIdentity() throws Exception {
        try (InProcessGrpc inProcessGrpc = start(metadata -> {
            metadata.put(GrpcMetadataKeys.PRINCIPAL_KIND, "robot");
            metadata.put(GrpcMetadataKeys.USER_LOGIN, "admin@localhost.com".getBytes(StandardCharsets.UTF_8));
        })) {
            assertThat(EchoService.echoUser(inProcessGrpc.channel())).isEmpty();
        }
    }

    @Test
    void testMalformedConnectedUserIdBindsNoIdentity() throws Exception {
        try (InProcessGrpc inProcessGrpc = start(metadata -> {
            metadata.put(GrpcMetadataKeys.PRINCIPAL_KIND, "connected-user");
            metadata.put(GrpcMetadataKeys.USER_LOGIN, "external-user-1".getBytes(StandardCharsets.UTF_8));
            metadata.put(GrpcMetadataKeys.CONNECTED_USER_ID, "seven");
            metadata.put(GrpcMetadataKeys.ENVIRONMENT_ID, "2");
            metadata.put(GrpcMetadataKeys.API_KEY_AUTHENTICATED, "false");
        })) {
            assertThat(EchoService.echoUser(inProcessGrpc.channel())).isEmpty();
        }
    }

    private static InProcessGrpc start(Consumer<Metadata> metadataWriter) throws Exception {
        ClientInterceptor metadataInterceptor = new ClientInterceptor() {

            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {

                return new ForwardingClientCall.SimpleForwardingClientCall<>(
                    channel.newCall(methodDescriptor, callOptions)) {

                    @Override
                    public void start(Listener<RespT> responseListener, Metadata metadata) {
                        metadataWriter.accept(metadata);

                        super.start(responseListener, metadata);
                    }
                };
            }
        };

        return new InProcessGrpc(
            new EchoService(), List.of(new UserContextServerInterceptor()), List.of(metadataInterceptor));
    }

    private static InProcessGrpc start() throws Exception {
        return new InProcessGrpc(
            new EchoService(), List.of(new UserContextServerInterceptor()),
            List.of(new UserContextClientInterceptor()));
    }
}
