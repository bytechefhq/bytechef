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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * The one place the "confined principal vs. session principal" rule is implemented, so both the authorization gate and
 * the execution that follows it resolve the same environment.
 */
class PrincipalEnvironmentTest {

    private static final long DEVELOPMENT_ORDINAL = 0L;
    private static final long STAGING_ORDINAL = 1L;
    private static final long PRODUCTION_ORDINAL = 2L;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testNoAuthenticationCarriesNoEnvironment() {
        assertThat(PrincipalEnvironment.fetchCurrentPrincipalEnvironmentId()).isEmpty();
        assertThat(PrincipalEnvironment.resolveEffectiveEnvironmentId(STAGING_ORDINAL)).isEqualTo(STAGING_ORDINAL);
    }

    @Test
    void testSessionPrincipalCarriesNoEnvironmentAndKeepsTheRequestedOne() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        assertThat(PrincipalEnvironment.fetchCurrentPrincipalEnvironmentId()).isEmpty();
        assertThat(PrincipalEnvironment.resolveEffectiveEnvironmentId(DEVELOPMENT_ORDINAL))
            .isEqualTo(DEVELOPMENT_ORDINAL);
    }

    /**
     * The rule this class exists for: for a principal confined to one environment the request parameter is inert, so
     * the two cannot disagree and there is no mismatch left to deny.
     */
    @Test
    void testConfinedPrincipalOverridesTheRequestedEnvironment() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        assertThat(PrincipalEnvironment.fetchCurrentPrincipalEnvironmentId()).contains(PRODUCTION_ORDINAL);
        assertThat(PrincipalEnvironment.resolveEffectiveEnvironmentId(DEVELOPMENT_ORDINAL))
            .isEqualTo(PRODUCTION_ORDINAL);
        assertThat(PrincipalEnvironment.resolveEffectiveEnvironmentId(STAGING_ORDINAL)).isEqualTo(PRODUCTION_ORDINAL);
        assertThat(PrincipalEnvironment.resolveEffectiveEnvironmentId(null)).isEqualTo(PRODUCTION_ORDINAL);
    }

    /**
     * An api-key token whose provider built it without an environment reports "unknown", not a confident DEVELOPMENT.
     * Were it to report ordinal 0, this helper would silently confine every such caller to DEVELOPMENT.
     */
    @Test
    void testApiKeyTokenBuiltWithoutAnEnvironmentCarriesNone() {
        authenticate(new TestApiKeyAuthenticationToken(user()));

        assertThat(PrincipalEnvironment.fetchCurrentPrincipalEnvironmentId()).isEmpty();
        assertThat(PrincipalEnvironment.resolveEffectiveEnvironmentId(STAGING_ORDINAL)).isEqualTo(STAGING_ORDINAL);
    }

    private static void authenticate(org.springframework.security.core.Authentication authentication) {
        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
    }

    private static User user() {
        return new User("connected-user-1", "", List.of());
    }

    private static final class TestApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

        private TestApiKeyAuthenticationToken(long environmentId, User user) {
            super(environmentId, user);
        }

        private TestApiKeyAuthenticationToken(User user) {
            super(user);
        }
    }
}
