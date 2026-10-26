/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.user.remote.grpc.AuthorityRemoteServiceGrpcServer;
import com.bytechef.ee.platform.user.remote.grpc.UserRemoteServiceGrpcServer;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.exception.UserNotFoundException;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 */
class UserRemoteServiceGrpcWireTest {

    private final AuthorityService authorityService = mock(AuthorityService.class);
    private ManagedChannel managedChannel;
    private RemoteAuthorityServiceClient remoteAuthorityServiceClient;
    private RemoteUserServiceClient remoteUserServiceClient;
    private Server server;
    private final UserService userService = mock(UserService.class);

    @BeforeEach
    void beforeEach() throws Exception {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(new UserRemoteServiceGrpcServer(userService))
            .addService(new AuthorityRemoteServiceGrpcServer(authorityService))
            .build()
            .start();
        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteAuthorityServiceClient = new RemoteAuthorityServiceClient(serviceId -> managedChannel);
        remoteUserServiceClient = new RemoteUserServiceClient(serviceId -> managedChannel);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        managedChannel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void testGetUserByLogin() {
        when(userService.fetchUserByLogin("admin@localhost.com"))
            .thenReturn(Optional.of(user(1L, "admin@localhost.com")));

        assertThat(remoteUserServiceClient.getUser("admin@localhost.com")
            .getId()).isEqualTo(1L);
    }

    @Test
    void testGetUserThrowsWhenMissing() {
        when(userService.fetchUserByLogin("missing@localhost.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> remoteUserServiceClient.getUser("missing@localhost.com"))
            .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void testFetchUserByLoginReturnsEmptyWhenMissing() {
        when(userService.fetchUserByLogin("missing@localhost.com")).thenReturn(Optional.empty());

        assertThat(remoteUserServiceClient.fetchUserByLogin("missing@localhost.com")).isEmpty();
    }

    @Test
    void testFetchUserById() {
        when(userService.fetchUser(7L)).thenReturn(Optional.of(user(7L, "seven@localhost.com")));

        assertThat(remoteUserServiceClient.fetchUser(7L)).map(User::getLogin)
            .contains("seven@localhost.com");
    }

    @Test
    void testFetchUserByEmail() {
        when(userService.fetchUserByEmail("seven@localhost.com"))
            .thenReturn(Optional.of(user(7L, "seven@localhost.com")));

        assertThat(remoteUserServiceClient.fetchUserByEmail("seven@localhost.com")).map(User::getId)
            .contains(7L);
    }

    @Test
    void testGetCurrentUserUsesCallerLogin() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("admin@localhost.com", null, List.of()));

        when(userService.fetchUserByLogin("admin@localhost.com"))
            .thenReturn(Optional.of(user(1L, "admin@localhost.com")));

        assertThat(remoteUserServiceClient.getCurrentUser()
            .getLogin()).isEqualTo("admin@localhost.com");
    }

    @Test
    void testFetchAuthority() {
        when(authorityService.fetchAuthority(1L)).thenReturn(Optional.of(authority(1L, "ROLE_ADMIN")));

        assertThat(remoteAuthorityServiceClient.fetchAuthority(1L)).map(Authority::getName)
            .contains("ROLE_ADMIN");
    }

    @Test
    void testGetAuthorities() {
        when(authorityService.getAuthorities()).thenReturn(
            List.of(authority(1L, "ROLE_ADMIN"), authority(2L, "ROLE_USER")));

        assertThat(remoteAuthorityServiceClient.getAuthorities()).extracting(Authority::getName)
            .containsExactly("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void testUnportedMethodStillThrowsUnsupported() {
        assertThatThrownBy(() -> remoteUserServiceClient.registerUser(new AdminUserDTO(), "password"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    private static Authority authority(long id, String name) {
        Authority authority = new Authority();

        authority.setId(id);
        authority.setName(name);

        return authority;
    }

    private static User user(long id, String login) {
        User user = new User();

        user.setId(id);
        user.setLogin(login);

        return user;
    }
}
