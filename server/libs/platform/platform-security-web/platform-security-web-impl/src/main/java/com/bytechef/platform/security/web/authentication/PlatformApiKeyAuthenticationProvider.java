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

import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.exception.UserNotActivatedException;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * @author Ivica Cardic
 */
public class PlatformApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final ApiKeyService apiKeyService;
    private final AuthorityService authorityService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public PlatformApiKeyAuthenticationProvider(
        ApiKeyService apiKeyService, AuthorityService authorityService, UserService userService) {

        this.apiKeyService = apiKeyService;
        this.authorityService = authorityService;
        this.userService = userService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        PlatformApiKeyAuthenticationToken platformApiKeyAuthenticationToken =
            (PlatformApiKeyAuthenticationToken) authentication;

        ApiKey apiKey;

        try {
            apiKey = apiKeyService.getApiKey(
                platformApiKeyAuthenticationToken.getSecretKey(), platformApiKeyAuthenticationToken.getEnvironmentId());
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Unknown API secret key", e);
        }

        // Only admin API keys reach /api/platform/v1. An admin key is the one with no PlatformType:
        // getAdminApiKeys(environmentId) is getApiKeys(environmentId, null), so a non-null type means
        // the caller presented an automation or embedded key. Every operation behind this path is
        // ROLE_ADMIN-guarded at its facade and tenant-wide in effect -- custom components carry no
        // environment column at all -- so accepting an environment-scoped key here promised a
        // containment the endpoint does not provide.
        if (apiKey.getType() != null) {
            throw new BadCredentialsException("Admin API key required");
        }

        org.springframework.security.core.userdetails.User user = userService.fetchUser(apiKey.getUserId())
            .map(curUser -> createSpringSecurityUser(platformApiKeyAuthenticationToken.getSecretKey(), curUser))
            .orElseThrow(() -> new UsernameNotFoundException(
                "User with token " + platformApiKeyAuthenticationToken.getSecretKey()
                    + " was not found in the database"));

        return new PlatformApiKeyAuthenticationToken(user);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(PlatformApiKeyAuthenticationToken.class);
    }

    private org.springframework.security.core.userdetails.User createSpringSecurityUser(String secretKey, User user) {
        if (!user.isActivated()) {
            throw new UserNotActivatedException("User " + secretKey + " was not activated");
        }

        List<SimpleGrantedAuthority> grantedAuthorities = user.getAuthorityIds()
            .stream()
            .map(authorityService::fetchAuthority)
            .map(Optional::get)
            .map(Authority::getName)
            .map(SimpleGrantedAuthority::new)
            .toList();

        return new org.springframework.security.core.userdetails.User(
            user.getLogin(), user.getPassword(), grantedAuthorities);
    }
}
