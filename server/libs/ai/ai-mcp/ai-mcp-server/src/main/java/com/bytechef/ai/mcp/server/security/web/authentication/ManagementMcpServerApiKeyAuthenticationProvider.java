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

package com.bytechef.ai.mcp.server.security.web.authentication;

import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.exception.UserNotActivatedException;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
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
public class ManagementMcpServerApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final ApiKeyService apiKeyService;
    private final AuthorityService authorityService;
    private final PropertyService propertyService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public ManagementMcpServerApiKeyAuthenticationProvider(
        ApiKeyService apiKeyService, AuthorityService authorityService, PropertyService propertyService,
        UserService userService) {

        this.apiKeyService = apiKeyService;
        this.authorityService = authorityService;
        this.propertyService = propertyService;
        this.userService = userService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ManagementMcpServerApiKeyAuthenticationToken managementMcpServerApiKeyAuthenticationToken =
            (ManagementMcpServerApiKeyAuthenticationToken) authentication;

        Property property = propertyService.getProperty("mcp.server", Property.Scope.PLATFORM, null);

        if (!Objects.equals(
            property.get("secretKey"), managementMcpServerApiKeyAuthenticationToken.getMcpServerSecretKey())) {

            throw new BadCredentialsException("Invalid MCP server secret key");
        }

        if (managementMcpServerApiKeyAuthenticationToken.getAuthSecretKey() == null) {
            return new ManagementMcpServerApiKeyAuthenticationToken();
        } else {
            ApiKey apiKey;

            try {
                apiKey = apiKeyService.getApiKey(managementMcpServerApiKeyAuthenticationToken.getAuthSecretKey());
            } catch (IllegalArgumentException e) {
                throw new BadCredentialsException("Invalid API secret key", e);
            }

            org.springframework.security.core.userdetails.User user = userService.fetchUser(apiKey.getUserId())
                .map(curUser -> createSpringSecurityUser(
                    managementMcpServerApiKeyAuthenticationToken.getAuthSecretKey(), curUser))
                .orElseThrow(() -> new UsernameNotFoundException(
                    "User with token " + managementMcpServerApiKeyAuthenticationToken.getAuthSecretKey() +
                        " was not found in the database"));

            return new ManagementMcpServerApiKeyAuthenticationToken(user);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(ManagementMcpServerApiKeyAuthenticationToken.class);
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
