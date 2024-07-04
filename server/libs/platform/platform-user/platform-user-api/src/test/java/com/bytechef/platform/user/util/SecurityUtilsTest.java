/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.user.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.user.constant.AuthorityConstants;
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
    void testGetCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("admin", "admin"));

        SecurityContextHolder.setContext(securityContext);

        Optional<String> login = SecurityUtils.getCurrentUserLogin();

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
