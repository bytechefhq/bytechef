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

package com.bytechef.platform.security.web.mcp;

import java.util.List;
import java.util.regex.Pattern;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationToken;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

/**
 * @author Ivica Cardic
 */
public class McpApiKeyHttpConfigurer extends AbstractHttpConfigurer<McpApiKeyHttpConfigurer, HttpSecurity> {

    private final AuthenticationConverter authenticationConverter;
    private final AuthenticationProvider authenticationProvider;
    private final Pattern mcpServerSecretPattern;
    private final String pathPatternRegex;

    public McpApiKeyHttpConfigurer(
        String pathPatternRegex, Pattern mcpServerSecretPattern, AuthenticationConverter authenticationConverter,
        AuthenticationProvider authenticationProvider) {

        this.authenticationConverter = authenticationConverter;
        this.authenticationProvider = authenticationProvider;
        this.mcpServerSecretPattern = mcpServerSecretPattern;
        this.pathPatternRegex = pathPatternRegex;
    }

    @Override
    public void init(HttpSecurity http) {
        http.authenticationProvider(authenticationProvider);

        CsrfConfigurer<?> csrf = http.getConfigurer(CsrfConfigurer.class);

        if (csrf != null) {
            csrf.ignoringRequestMatchers(RegexRequestMatcher.regexMatcher(pathPatternRegex));
        }
    }

    @Override
    public void configure(HttpSecurity http) {
        AuthenticationManager authenticationManager = http.getSharedObject(AuthenticationManager.class);

        TenantAwareApiKeyAuthenticationFilter tenantAwareApiKeyAuthenticationFilter =
            new TenantAwareApiKeyAuthenticationFilter(
                RegexRequestMatcher.regexMatcher(pathPatternRegex), mcpServerSecretPattern, authenticationManager,
                authenticationConverter);

        http.addFilterBefore(tenantAwareApiKeyAuthenticationFilter, BasicAuthenticationFilter.class);

        McpAuthenticationGuardFilter mcpAuthenticationGuardFilter = new McpAuthenticationGuardFilter(
            RegexRequestMatcher.regexMatcher(pathPatternRegex),
            List.of(ApiKeyAuthenticationToken.class, JwtAuthenticationToken.class,
                McpAnonymousAuthenticationToken.class),
            getAuthenticationEntryPoint(http));

        http.addFilterBefore(mcpAuthenticationGuardFilter, AuthorizationFilter.class);
    }

    private static AuthenticationEntryPoint getAuthenticationEntryPoint(HttpSecurity http) {
        McpAuthenticationEntryPoint mcpAuthenticationEntryPoint = http.getSharedObject(
            McpAuthenticationEntryPoint.class);

        if (mcpAuthenticationEntryPoint == null) {
            return new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
        }

        return mcpAuthenticationEntryPoint;
    }
}
