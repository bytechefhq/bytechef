/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserAuthenticationsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testFetchCurrentReturnsEmptyForPlatformUser() {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("admin", "", List.of()));

        assertThat(ConnectedUserAuthentications.fetchCurrent()).isEmpty();
    }

    @Test
    void testFetchCurrentReturnsEmptyWhenNoAuthenticationIsSet() {
        assertThat(ConnectedUserAuthentications.fetchCurrent()).isEmpty();
    }

    @Test
    void testFetchCurrentReturnsEmptyForAnUnauthenticatedConnectedUserToken() {
        TestConnectedUserAuthentication authentication =
            new TestConnectedUserAuthentication("external-user", 42L, 2L, false);

        SecurityContextHolder.getContext()
            .setAuthentication(authentication);

        assertThat(ConnectedUserAuthentications.fetchCurrent()).isEmpty();
    }

    @Test
    void testFetchCurrentReturnsTheConnectedUserAuthentication() {
        TestConnectedUserAuthentication authentication =
            new TestConnectedUserAuthentication("external-user", 42L, 2L, true);

        SecurityContextHolder.getContext()
            .setAuthentication(authentication);

        assertThat(ConnectedUserAuthentications.fetchCurrent()).contains(authentication);
    }

    @Test
    void testGetCurrentExternalUserIdReturnsTheConnectedUsersExternalId() {
        SecurityContextHolder.getContext()
            .setAuthentication(new TestConnectedUserAuthentication("external-user", 42L, 2L, true));

        assertThat(ConnectedUserAuthentications.getCurrentExternalUserId()).isEqualTo("external-user");
    }

    @Test
    void testGetCurrentExternalUserIdRefusesAPlatformUser() {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("external-user", "", List.of()));

        assertThatThrownBy(ConnectedUserAuthentications::getCurrentExternalUserId)
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testRequireCurrentExternalUserIdAcceptsOnlyTheCallersOwnExternalId() {
        SecurityContextHolder.getContext()
            .setAuthentication(TestConnectedUserAuthentication.of("alice"));

        assertThat(ConnectedUserAuthentications.requireCurrentExternalUserId("alice")).isEqualTo("alice");
        assertThatThrownBy(() -> ConnectedUserAuthentications.requireCurrentExternalUserId("bob"))
            .isInstanceOf(AccessDeniedException.class);

        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("bob", "", List.of()));

        assertThatThrownBy(() -> ConnectedUserAuthentications.requireCurrentExternalUserId("bob"))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testIsConnectedUserIsTrueOnlyForAnAuthenticatedConnectedUserToken() {
        assertThat(ConnectedUserAuthentications.isConnectedUser()).isFalse();

        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("admin", "", List.of()));

        assertThat(ConnectedUserAuthentications.isConnectedUser()).isFalse();

        SecurityContextHolder.getContext()
            .setAuthentication(new TestConnectedUserAuthentication("external-user", 42L, 2L, false));

        assertThat(ConnectedUserAuthentications.isConnectedUser()).isFalse();

        SecurityContextHolder.getContext()
            .setAuthentication(new TestConnectedUserAuthentication("external-user", 42L, 2L, true));

        assertThat(ConnectedUserAuthentications.isConnectedUser()).isTrue();
    }

    @Test
    void testIsCurrentConnectedUserMatchesOnlyTheAuthenticatedTokensConnectedUserId() {
        assertThat(ConnectedUserAuthentications.isCurrentConnectedUser(5L)).isFalse();

        SecurityContextHolder.getContext()
            .setAuthentication(new TestConnectedUserAuthentication("external-user", 5L, 2L, true));

        assertThat(ConnectedUserAuthentications.isCurrentConnectedUser(5L)).isTrue();
        assertThat(ConnectedUserAuthentications.isCurrentConnectedUser(6L)).isFalse();

        SecurityContextHolder.getContext()
            .setAuthentication(new TestConnectedUserAuthentication("external-user", 5L, 2L, false));

        assertThat(ConnectedUserAuthentications.isCurrentConnectedUser(5L)).isFalse();

        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("admin", "", List.of()));

        assertThat(ConnectedUserAuthentications.isCurrentConnectedUser(5L)).isFalse();
    }
}
