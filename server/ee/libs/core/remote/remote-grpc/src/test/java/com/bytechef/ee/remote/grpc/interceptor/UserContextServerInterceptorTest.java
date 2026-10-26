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
import java.util.List;
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

    private static InProcessGrpc start() throws Exception {
        return new InProcessGrpc(
            new EchoService(), List.of(new UserContextServerInterceptor()),
            List.of(new UserContextClientInterceptor()));
    }
}
