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
import com.bytechef.ee.remote.grpc.security.RemotePrincipalCodec;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.CredentialType;
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
import org.springframework.security.core.Authentication;
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
    void testCallWithoutIdentityDoesNotSeeAContextLeftOnTheServerThread() throws Exception {
        try (InProcessGrpc inProcessGrpc = start(metadata -> {})) {
            inProcessGrpc.serverExecutorService()
                .submit(() -> SecurityContextHolder.getContext()
                    .setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(
                            "stale@localhost.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))))
                .get();

            assertThat(EchoService.echoUser(inProcessGrpc.channel())).isEmpty();
        }
    }

    @Test
    void testApiKeyUserKeepsEnvironmentAndAuthorities() throws Exception {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new RemoteApiKeyAuthenticationToken(
                    1L, new User("api@localhost.com", "", List.of(new SimpleGrantedAuthority("ROLE_USER"))),
                    CredentialType.AUTOMATION_API_KEY, PlatformType.AUTOMATION));

        try (InProcessGrpc inProcessGrpc = start()) {
            assertThat(EchoService.echoPrincipal(inProcessGrpc.channel()))
                .isEqualTo("api-key|api@localhost.com|1|AUTOMATION_API_KEY|AUTOMATION|ROLE_USER");
        }
    }

    @Test
    void testApiKeyUserWithoutAPlatformKeepsItsCredentialType() throws Exception {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new RemoteApiKeyAuthenticationToken(
                    1L, new User("api@localhost.com", "", List.of()), CredentialType.PLATFORM_API_KEY, null));

        try (InProcessGrpc inProcessGrpc = start()) {
            assertThat(EchoService.echoPrincipal(inProcessGrpc.channel()))
                .isEqualTo("api-key|api@localhost.com|1|PLATFORM_API_KEY|null|");
        }
    }

    @Test
    void testApiKeyUserWithoutACredentialTypeBindsNoIdentity() throws Exception {
        try (InProcessGrpc inProcessGrpc = start(metadata -> {
            metadata.put(GrpcMetadataKeys.PRINCIPAL_KIND, "api-key");
            metadata.put(GrpcMetadataKeys.USER_LOGIN, "admin@localhost.com".getBytes(StandardCharsets.UTF_8));
            metadata.put(GrpcMetadataKeys.ENVIRONMENT_ID, "1");
        })) {
            assertThat(EchoService.echoUser(inProcessGrpc.channel())).isEmpty();
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
    void testMalformedApiKeyEnvironmentIdBindsNoIdentity() throws Exception {
        try (InProcessGrpc inProcessGrpc = start(metadata -> {
            metadata.put(GrpcMetadataKeys.PRINCIPAL_KIND, "api-key");
            metadata.put(GrpcMetadataKeys.USER_LOGIN, "admin@localhost.com".getBytes(StandardCharsets.UTF_8));
            metadata.put(GrpcMetadataKeys.ENVIRONMENT_ID, "production");
        })) {
            assertThat(EchoService.echoUser(inProcessGrpc.channel())).isEmpty();
        }
    }

    @Test
    void testCodecPrincipalIsRebuiltByTheCodecOfItsKind() throws Exception {
        SecurityContextHolder.getContext()
            .setAuthentication(new TestingAuthenticationToken("robot-1", null, "ROLE_ROBOT"));

        RobotRemotePrincipalCodec robotRemotePrincipalCodec = new RobotRemotePrincipalCodec();

        try (InProcessGrpc inProcessGrpc = new InProcessGrpc(
            new EchoService(), List.of(new UserContextServerInterceptor(List.of(robotRemotePrincipalCodec))),
            List.of(new UserContextClientInterceptor(List.of(robotRemotePrincipalCodec))))) {

            assertThat(EchoService.echoUser(inProcessGrpc.channel())).isEqualTo("robot:robot-1|true|");
        }
    }

    @Test
    void testCodecPrincipalIsNotSentWhenTheCodecDeclinesToEncode() throws Exception {
        SecurityContextHolder.getContext()
            .setAuthentication(new TestingAuthenticationToken("declined", null, "ROLE_ROBOT"));

        RobotRemotePrincipalCodec robotRemotePrincipalCodec = new RobotRemotePrincipalCodec();

        try (InProcessGrpc inProcessGrpc = new InProcessGrpc(
            new EchoService(), List.of(new UserContextServerInterceptor(List.of(robotRemotePrincipalCodec))),
            List.of(new UserContextClientInterceptor(List.of(robotRemotePrincipalCodec))))) {

            assertThat(EchoService.echoUser(inProcessGrpc.channel())).isEmpty();
        }
    }

    @Test
    void testCodecKindBindsNoIdentityWhenTheCodecRejectsTheMetadata() throws Exception {
        try (InProcessGrpc inProcessGrpc = new InProcessGrpc(
            new EchoService(), List.of(new UserContextServerInterceptor(List.of(new RobotRemotePrincipalCodec()))),
            List.of(metadataInterceptor(metadata -> {
                metadata.put(GrpcMetadataKeys.PRINCIPAL_KIND, "robot");
                metadata.put(GrpcMetadataKeys.USER_LOGIN, "rejected".getBytes(StandardCharsets.UTF_8));
            })))) {

            assertThat(EchoService.echoUser(inProcessGrpc.channel())).isEmpty();
        }
    }

    private static ClientInterceptor metadataInterceptor(Consumer<Metadata> metadataWriter) {
        return new ClientInterceptor() {

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
    }

    private static InProcessGrpc start(Consumer<Metadata> metadataWriter) throws Exception {
        return new InProcessGrpc(
            new EchoService(), List.of(new UserContextServerInterceptor()),
            List.of(metadataInterceptor(metadataWriter)));
    }

    private static InProcessGrpc start() throws Exception {
        return new InProcessGrpc(
            new EchoService(), List.of(new UserContextServerInterceptor()),
            List.of(new UserContextClientInterceptor()));
    }

    private static final class RobotRemotePrincipalCodec implements RemotePrincipalCodec {

        @Override
        public String getPrincipalKind() {
            return "robot";
        }

        @Override
        public boolean supports(Authentication authentication) {
            return authentication instanceof TestingAuthenticationToken;
        }

        @Override
        public boolean encode(Authentication authentication, Metadata metadata) {
            if ("declined".equals(authentication.getName())) {
                return false;
            }

            metadata.put(GrpcMetadataKeys.USER_LOGIN, authentication.getName()
                .getBytes(StandardCharsets.UTF_8));

            return true;
        }

        @Override
        public Authentication decode(String login, Metadata metadata) {
            if ("rejected".equals(login)) {
                return null;
            }

            return new TestingAuthenticationToken("robot:" + login, null, List.of());
        }
    }
}
