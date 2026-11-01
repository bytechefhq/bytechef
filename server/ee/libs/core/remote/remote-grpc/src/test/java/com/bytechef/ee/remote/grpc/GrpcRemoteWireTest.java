/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.bytechef.ee.remote.grpc.interceptor.ServiceTokenClientInterceptor;
import com.bytechef.ee.remote.grpc.interceptor.ServiceTokenServerInterceptor;
import com.bytechef.ee.remote.grpc.interceptor.TenantContextClientInterceptor;
import com.bytechef.ee.remote.grpc.interceptor.TenantContextServerInterceptor;
import com.bytechef.ee.remote.grpc.interceptor.UserContextClientInterceptor;
import com.bytechef.ee.remote.grpc.interceptor.UserContextServerInterceptor;
import com.bytechef.platform.security.web.authentication.PlatformUserAuthentication;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.protobuf.services.HealthStatusManager;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * @version ee
 */
class GrpcRemoteWireTest {

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testHealthIsServingThroughBothInterceptors() throws Exception {
        try (InProcessGrpc inProcessGrpc = start("secret")) {
            HealthCheckResponse healthCheckResponse = HealthGrpc.newBlockingStub(inProcessGrpc.channel())
                .check(HealthCheckRequest.getDefaultInstance());

            assertThat(healthCheckResponse.getStatus()).isEqualTo(HealthCheckResponse.ServingStatus.SERVING);
        }
    }

    @Test
    void testWrongTokenIsUnauthenticated() throws Exception {
        try (InProcessGrpc inProcessGrpc = new InProcessGrpc(
            new EchoService(),
            List.of(new TenantContextServerInterceptor(), new ServiceTokenServerInterceptor("secret")),
            List.of(new TenantContextClientInterceptor(), new ServiceTokenClientInterceptor("wrong")))) {

            assertThatThrownBy(() -> EchoService.echoTenant(inProcessGrpc.channel()))
                .isInstanceOfSatisfying(StatusRuntimeException.class, exception -> assertThat(exception.getStatus()
                    .getCode()).isEqualTo(Status.Code.UNAUTHENTICATED));
        }
    }

    @Test
    void testValidTokenBindsTheCallersUser() throws Exception {
        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                    "admin@localhost.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        try (InProcessGrpc inProcessGrpc = startEcho("secret", new UserContextServerInterceptor())) {
            assertThat(EchoService.echoUser(inProcessGrpc.channel())).isEqualTo("admin@localhost.com|true|ROLE_ADMIN");
        }
    }

    @Test
    void testPlatformUserOauthTokenBindsTheCallersUser() throws Exception {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));

        SecurityContextHolder.getContext()
            .setAuthentication(
                new PlatformUserJwtAuthenticationToken(
                    jwt(), new User("admin@localhost.com", "", authorities), authorities));

        try (InProcessGrpc inProcessGrpc = startEcho("secret", new UserContextServerInterceptor())) {
            assertThat(EchoService.echoUser(inProcessGrpc.channel())).isEqualTo("admin@localhost.com|true|ROLE_ADMIN");
        }
    }

    @Test
    void testOauthTokenThatIsNotAPlatformUserIsNotForwarded() throws Exception {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));

        SecurityContextHolder.getContext()
            .setAuthentication(
                new JwtAuthenticationToken(jwt(), new User("admin@localhost.com", "", authorities), authorities));

        try (InProcessGrpc inProcessGrpc = startEcho("secret", new UserContextServerInterceptor())) {
            assertThat(EchoService.echoUser(inProcessGrpc.channel())).isEmpty();
        }
    }

    @Test
    void testWrongTokenIsRejectedBeforeTheUserIsRead() throws Exception {
        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                    "admin@localhost.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        UserContextServerInterceptor userContextServerInterceptor = spy(new UserContextServerInterceptor());

        try (InProcessGrpc inProcessGrpc = startEcho("wrong", userContextServerInterceptor)) {
            assertThatThrownBy(() -> EchoService.echoUser(inProcessGrpc.channel()))
                .isInstanceOfSatisfying(StatusRuntimeException.class, exception -> assertThat(exception.getStatus()
                    .getCode()).isEqualTo(Status.Code.UNAUTHENTICATED));
        }

        verify(userContextServerInterceptor, never()).interceptCall(any(), any(), any());
    }

    private static class PlatformUserJwtAuthenticationToken extends JwtAuthenticationToken
        implements PlatformUserAuthentication {

        PlatformUserJwtAuthenticationToken(Jwt jwt, User user, List<GrantedAuthority> authorities) {
            super(jwt, user, authorities);
        }
    }

    private static Jwt jwt() {
        return Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("admin@localhost.com")
            .build();
    }

    private static InProcessGrpc startEcho(
        String clientToken, UserContextServerInterceptor userContextServerInterceptor) throws Exception {

        return new InProcessGrpc(
            new EchoService(),
            List.of(
                userContextServerInterceptor, new TenantContextServerInterceptor(),
                new ServiceTokenServerInterceptor("secret")),
            List.of(
                new TenantContextClientInterceptor(), new UserContextClientInterceptor(),
                new ServiceTokenClientInterceptor(clientToken)));
    }

    private static InProcessGrpc start(String clientToken) throws Exception {
        HealthStatusManager healthStatusManager = new HealthStatusManager();

        return new InProcessGrpc(
            healthStatusManager.getHealthService(),
            List.of(new TenantContextServerInterceptor(), new ServiceTokenServerInterceptor("secret")),
            List.of(new TenantContextClientInterceptor(), new ServiceTokenClientInterceptor(clientToken)));
    }
}
