/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.platform.user.remote.client.config.PlatformUserRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.user.remote.grpc.UserRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.exception.UserNotFoundException;
import com.bytechef.platform.user.repository.AuthorityRepository;
import com.bytechef.platform.user.repository.UserRepository;
import com.bytechef.platform.user.service.UserService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = PlatformUserRemoteClientIntTestConfiguration.class, properties = "spring.liquibase.contexts=user")
class RemoteUserServiceClientIntTest {

    private static final String LOGIN = "jane.doe";

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    private ManagedChannel managedChannel;
    private RemoteUserServiceClient remoteUserServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(new UserRemoteServiceGrpcServer(userService))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteUserServiceClient = new RemoteUserServiceClient(new FixedGrpcChannels(managedChannel));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        managedChannel.shutdownNow();
        server.shutdownNow();

        userRepository.deleteAll();
    }

    @Test
    void testFetchUserByIdReturnsTheStoredUser() {
        User storedUser = createUser();

        Optional<User> user = remoteUserServiceClient.fetchUser(storedUser.getId());

        assertThat(user).hasValueSatisfying(fetchedUser -> assertStoredUser(fetchedUser, storedUser));
        assertThat(remoteUserServiceClient.getUser(storedUser.getId())
            .getLogin()).isEqualTo(LOGIN);
    }

    @Test
    void testFetchUserByIdReturnsEmptyWhenMissing() {
        assertThat(remoteUserServiceClient.fetchUser(Long.MAX_VALUE)).isEmpty();

        assertThatThrownBy(() -> remoteUserServiceClient.getUser(Long.MAX_VALUE))
            .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void testFetchUserByEmailIgnoresCase() {
        User storedUser = createUser();

        Optional<User> user = remoteUserServiceClient.fetchUserByEmail("Jane.Doe@Example.com");

        assertThat(user).hasValueSatisfying(fetchedUser -> assertStoredUser(fetchedUser, storedUser));
        assertThat(remoteUserServiceClient.fetchUserByEmail("missing@example.com")).isEmpty();
    }

    @Test
    void testFetchUserByLoginReturnsTheStoredUser() {
        User storedUser = createUser();

        Optional<User> user = remoteUserServiceClient.fetchUserByLogin(LOGIN);

        assertThat(user).hasValueSatisfying(fetchedUser -> assertStoredUser(fetchedUser, storedUser));
        assertThat(remoteUserServiceClient.getUser(LOGIN)
            .getId()).isEqualTo(storedUser.getId());
    }

    @Test
    void testFetchUserByLoginReturnsEmptyWhenMissing() {
        createUser();

        assertThat(remoteUserServiceClient.fetchUserByLogin("missing")).isEmpty();

        assertThatThrownBy(() -> remoteUserServiceClient.getUser("missing"))
            .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void testGetCurrentUserLooksUpTheCallerLogin() {
        User storedUser = createUser();

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(LOGIN, null, List.of()));

        assertThat(remoteUserServiceClient.getCurrentUser()
            .getId()).isEqualTo(storedUser.getId());
    }

    @Test
    void testGetCurrentUserThrowsWhenTheCallerIsUnknown() {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated("missing", null, List.of()));

        assertThatThrownBy(() -> remoteUserServiceClient.getCurrentUser())
            .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void testUnportedMethodStillThrowsUnsupported() {
        assertThatThrownBy(() -> remoteUserServiceClient.registerUser(new AdminUserDTO(), "password"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    private void assertStoredUser(User fetchedUser, User storedUser) {
        Authority adminAuthority = authorityRepository.findByName("ROLE_ADMIN")
            .orElseThrow();

        assertThat(fetchedUser.getId()).isEqualTo(storedUser.getId());
        assertThat(fetchedUser.getLogin()).isEqualTo(LOGIN);
        assertThat(fetchedUser.getEmail()).isEqualTo("jane.doe@example.com");
        assertThat(fetchedUser.getFirstName()).isEqualTo("Jane");
        assertThat(fetchedUser.getLastName()).isEqualTo("Doe");
        assertThat(fetchedUser.getImageUrl()).isEqualTo("https://example.com/jane.png");
        assertThat(fetchedUser.getLangKey()).isEqualTo("de");
        assertThat(fetchedUser.getAuthProvider()).isEqualTo("LOCAL");
        assertThat(fetchedUser.isActivated()).isTrue();
        assertThat(fetchedUser.isTotpEnabled()).isFalse();
        assertThat(fetchedUser.getUuid()).isEqualTo(storedUser.getUuid());
        assertThat(fetchedUser.getUuid()).isNotNull();
        assertThat(fetchedUser.getCreatedDate()).isEqualTo(storedUser.getCreatedDate());
        assertThat(fetchedUser.getAuthorityIds()).containsExactly(adminAuthority.getId());
        assertThat(fetchedUser.getPassword()).isNull();
        assertThat(fetchedUser.getResetKey()).isNull();
    }

    private User createUser() {
        AdminUserDTO adminUserDTO = new AdminUserDTO();

        adminUserDTO.setAuthorities(Set.of("ROLE_ADMIN"));
        adminUserDTO.setEmail("jane.doe@example.com");
        adminUserDTO.setFirstName("Jane");
        adminUserDTO.setImageUrl("https://example.com/jane.png");
        adminUserDTO.setLangKey("de");
        adminUserDTO.setLastName("Doe");
        adminUserDTO.setLogin(LOGIN);

        User user = userService.create(adminUserDTO);

        return userService.getUser(user.getId());
    }
}
