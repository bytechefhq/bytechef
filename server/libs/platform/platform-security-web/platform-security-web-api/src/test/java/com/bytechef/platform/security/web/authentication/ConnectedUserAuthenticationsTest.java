/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
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
}
