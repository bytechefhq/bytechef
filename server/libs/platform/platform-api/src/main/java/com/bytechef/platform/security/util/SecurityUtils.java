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

import com.bytechef.platform.security.constant.AuthorityConstants;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Utility class for Spring Security.
 *
 * @author Ivica Cardic
 */
public final class SecurityUtils {

    /**
     * Login of the synthetic principal used by background flows (Quartz scheduler jobs, message consumers) that run
     * without an originating HTTP request and therefore have no authenticated user of their own.
     */
    public static final String SYSTEM_LOGIN = "system";

    private SecurityUtils() {
    }

    /**
     * Fetch the login of the current user if logged in, otherwise null.
     *
     * @return the login of the current user.
     */
    public static Optional<String> fetchCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        return Optional.ofNullable(extractPrincipal(securityContext.getAuthentication()));
    }

    /**
     * Retrieves the login of the current user. Throws an {@link IllegalStateException} if no user is currently
     * authenticated.
     *
     * @return the login of the current user.
     * @throws IllegalStateException if the current user is not set.
     */
    public static String getCurrentUserLogin() {
        return fetchCurrentUserLogin().orElseThrow(() -> new IllegalStateException("Current user is not set!"));
    }

    /**
     * Check if a user is authenticated.
     *
     * @return true if the user is authenticated, false otherwise.
     */
    public static boolean isAuthenticated() {
        SecurityContext context = SecurityContextHolder.getContext();

        Authentication authentication = context.getAuthentication();

        return authentication != null && getAuthorities(authentication).noneMatch(AuthorityConstants.ANONYMOUS::equals);
    }

    /**
     * Checks if the current user has any of the authorities.
     *
     * @param authorities the authorities to check.
     * @return true if the current user has any of the authorities, false otherwise.
     */
    public static boolean hasCurrentUserAnyOfAuthorities(String... authorities) {
        SecurityContext context = SecurityContextHolder.getContext();

        Authentication authentication = context.getAuthentication();

        List<String> authoritiesList = Arrays.asList(authorities);

        return authentication != null && getAuthorities(authentication).anyMatch(authoritiesList::contains);
    }

    /**
     * Checks if the current user has none of the authorities.
     *
     * @param authorities the authorities to check.
     * @return true if the current user has none of the authorities, false otherwise.
     */
    public static boolean hasCurrentUserNoneOfAuthorities(String... authorities) {
        return !hasCurrentUserAnyOfAuthorities(authorities);
    }

    /**
     * Checks if the current user has a specific authority.
     *
     * @param authority the authority to check.
     * @return true if the current user has the authority, false otherwise.
     */
    public static boolean hasCurrentUserThisAuthority(String authority) {
        return hasCurrentUserAnyOfAuthorities(authority);
    }

    /**
     * Executes the supplied {@code supplier} with a temporary {@link SecurityContext} populated from {@code login} +
     * {@code authorities}, restoring the original context on completion. Use when a background flow (e.g. a Spring AI
     * tool callback running on a Reactor scheduler thread) must invoke code that calls {@link #getCurrentUserLogin()}
     * or {@link #hasCurrentUserThisAuthority(String)} but the original HTTP request's security context did not
     * propagate to the executing thread.
     */
    public static <T> T runAs(
        String login, Collection<? extends GrantedAuthority> authorities, Supplier<T> supplier) {

        SecurityContext originalContext = SecurityContextHolder.getContext();

        try {
            SecurityContext newContext = SecurityContextHolder.createEmptyContext();

            newContext.setAuthentication(new UsernamePasswordAuthenticationToken(login, "", authorities));

            SecurityContextHolder.setContext(newContext);

            return supplier.get();
        } finally {
            SecurityContextHolder.setContext(originalContext);
        }
    }

    /**
     * Executes the supplied {@code supplier} as the system principal ({@link #SYSTEM_LOGIN}) with
     * {@link AuthorityConstants#ADMIN} authority, restoring the original context on completion. Use for background
     * flows that own no user identity yet must invoke code guarded by authenticated/owner-or-admin checks (e.g. a
     * Quartz scheduler job refreshing an OAuth2 connection token).
     */
    public static <T> T runAsSystem(Supplier<T> supplier) {
        return runAs(SYSTEM_LOGIN, List.of(new SimpleGrantedAuthority(AuthorityConstants.ADMIN)), supplier);
    }

    /**
     * Executes the supplied {@code supplier} with a temporary {@link SecurityContext} holding the given
     * {@code authentication}, restoring the original context on completion. Use to restore an already-resolved
     * principal (e.g. an embedded API-key principal that has no backing platform user) on a background thread that did
     * not inherit the request's thread-local SecurityContext.
     */
    public static <T> T runAs(Authentication authentication, Supplier<T> supplier) {
        SecurityContext originalContext = SecurityContextHolder.getContext();

        try {
            SecurityContext newContext = SecurityContextHolder.createEmptyContext();

            newContext.setAuthentication(authentication);

            SecurityContextHolder.setContext(newContext);

            return supplier.get();
        } finally {
            SecurityContextHolder.setContext(originalContext);
        }
    }

    private static String extractPrincipal(Authentication authentication) {
        if (authentication == null) {
            return null;
        } else if (authentication.getPrincipal() instanceof UserDetails springSecurityUser) {
            return springSecurityUser.getUsername();
        } else if (authentication.getPrincipal() instanceof String s) {
            return s;
        }

        return null;
    }

    private static Stream<String> getAuthorities(Authentication authentication) {
        return authentication.getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority);
    }
}
