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

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
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
        TestConnectedUserAuthentication authentication = new TestConnectedUserAuthentication(false);

        SecurityContextHolder.getContext()
            .setAuthentication(authentication);

        assertThat(ConnectedUserAuthentications.fetchCurrent()).isEmpty();
    }

    @Test
    void testFetchCurrentReturnsTheConnectedUserAuthentication() {
        TestConnectedUserAuthentication authentication = new TestConnectedUserAuthentication(true);

        SecurityContextHolder.getContext()
            .setAuthentication(authentication);

        assertThat(ConnectedUserAuthentications.fetchCurrent()).contains(authentication);
    }

    private static final class TestConnectedUserAuthentication extends AbstractAuthenticationToken
        implements ConnectedUserAuthentication {

        private TestConnectedUserAuthentication(boolean authenticated) {
            super(List.of());

            setAuthenticated(authenticated);
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getPrincipal() {
            return "external-user";
        }

        @Override
        public long connectedUserId() {
            return 42L;
        }

        @Override
        public String externalUserId() {
            return "external-user";
        }

        @Override
        public long environmentId() {
            return 2L;
        }
    }
}
