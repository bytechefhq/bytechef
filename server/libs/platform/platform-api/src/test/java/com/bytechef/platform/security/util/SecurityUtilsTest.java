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

package com.bytechef.platform.security.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.security.constant.AuthorityConstants;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Test class for the {@link SecurityUtils} utility class.
 */
class SecurityUtilsTest {

    @BeforeEach
    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testFetchCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("admin", "admin"));

        SecurityContextHolder.setContext(securityContext);

        Optional<String> login = SecurityUtils.fetchCurrentUserLogin();

        assertThat(login).contains("admin");
    }

    @Test
    void testIsAuthenticated() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("admin", "admin"));

        SecurityContextHolder.setContext(securityContext);

        boolean isAuthenticated = SecurityUtils.isAuthenticated();

        assertThat(isAuthenticated).isTrue();
    }

    @Test
    void testAnonymousIsNotAuthenticated() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        authorities.add(new SimpleGrantedAuthority(AuthorityConstants.ANONYMOUS));

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken("anonymous", "anonymous", authorities));

        SecurityContextHolder.setContext(securityContext);

        boolean isAuthenticated = SecurityUtils.isAuthenticated();

        assertThat(isAuthenticated).isFalse();
    }

    @Test
    void testHasCurrentUserThisAuthority() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        authorities.add(new SimpleGrantedAuthority(AuthorityConstants.USER));

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("user", "user", authorities));

        SecurityContextHolder.setContext(securityContext);

        assertThat(SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.USER)).isTrue();
        assertThat(SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN)).isFalse();
    }

    @Test
    void testHasCurrentUserAnyOfAuthorities() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        authorities.add(new SimpleGrantedAuthority(AuthorityConstants.USER));

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("user", "user", authorities));

        SecurityContextHolder.setContext(securityContext);

        assertThat(
            SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthorityConstants.USER, AuthorityConstants.ADMIN)).isTrue();
        assertThat(
            SecurityUtils.hasCurrentUserAnyOfAuthorities(
                AuthorityConstants.ANONYMOUS, AuthorityConstants.ADMIN)).isFalse();
    }

    @Test
    void testRunAsTemporarilyOverridesContextAndRestores() {
        SecurityContext originalContext = SecurityContextHolder.createEmptyContext();

        originalContext.setAuthentication(new UsernamePasswordAuthenticationToken("original-user", ""));

        SecurityContextHolder.setContext(originalContext);

        Collection<GrantedAuthority> overrideAuthorities = new ArrayList<>();

        overrideAuthorities.add(new SimpleGrantedAuthority(AuthorityConstants.ADMIN));

        String result = SecurityUtils.runAs("override-user", overrideAuthorities, () -> {
            assertThat(SecurityUtils.getCurrentUserLogin()).isEqualTo("override-user");
            assertThat(SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN)).isTrue();

            return "ran-with-override";
        });

        assertThat(result).isEqualTo("ran-with-override");
        // The original context must be restored once runAs returns - otherwise a thread-pool reuse would
        // leak the override user's authorities to whoever runs on this thread next.
        assertThat(SecurityUtils.getCurrentUserLogin()).isEqualTo("original-user");
        assertThat(SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN)).isFalse();
    }

    @Test
    void testRunAsAuthenticationTemporarilyOverridesContextAndRestores() {
        SecurityContext originalContext = SecurityContextHolder.createEmptyContext();

        originalContext.setAuthentication(new UsernamePasswordAuthenticationToken("original-user", ""));

        SecurityContextHolder.setContext(originalContext);

        Collection<GrantedAuthority> authorities = new ArrayList<>();

        authorities.add(new SimpleGrantedAuthority(AuthorityConstants.ADMIN));

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken("captured-user", "", authorities);

        String result = SecurityUtils.runAs(authentication, () -> {
            assertThat(SecurityContextHolder.getContext()
                .getAuthentication()).isSameAs(authentication);
            assertThat(SecurityUtils.getCurrentUserLogin()).isEqualTo("captured-user");

            return "ran-with-authentication";
        });

        assertThat(result).isEqualTo("ran-with-authentication");
        assertThat(SecurityUtils.getCurrentUserLogin()).isEqualTo("original-user");
    }

    @Test
    void testRunAsAuthenticationRestoresContextEvenWhenSupplierThrows() {
        SecurityContext originalContext = SecurityContextHolder.createEmptyContext();

        originalContext.setAuthentication(new UsernamePasswordAuthenticationToken("original-user", ""));

        SecurityContextHolder.setContext(originalContext);

        try {
            SecurityUtils.runAs(new UsernamePasswordAuthenticationToken("captured-user", ""), () -> {
                throw new RuntimeException("supplier failure");
            });
        } catch (RuntimeException ignored) {
            // expected
        }

        assertThat(SecurityUtils.getCurrentUserLogin()).isEqualTo("original-user");
    }

    @Test
    void testRunAsRestoresContextEvenWhenSupplierThrows() {
        SecurityContext originalContext = SecurityContextHolder.createEmptyContext();

        originalContext.setAuthentication(new UsernamePasswordAuthenticationToken("original-user", ""));

        SecurityContextHolder.setContext(originalContext);

        try {
            SecurityUtils.runAs("override-user", new ArrayList<>(), () -> {
                throw new RuntimeException("supplier failure");
            });
        } catch (RuntimeException ignored) {
            // expected
        }

        assertThat(SecurityUtils.getCurrentUserLogin()).isEqualTo("original-user");
    }

    @Test
    void testHasCurrentUserNoneOfAuthorities() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        authorities.add(new SimpleGrantedAuthority(AuthorityConstants.USER));

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("user", "user", authorities));

        SecurityContextHolder.setContext(securityContext);

        assertThat(
            SecurityUtils.hasCurrentUserNoneOfAuthorities(AuthorityConstants.USER, AuthorityConstants.ADMIN)).isFalse();
        assertThat(
            SecurityUtils.hasCurrentUserNoneOfAuthorities(
                AuthorityConstants.ANONYMOUS, AuthorityConstants.ADMIN)).isTrue();
    }
}
