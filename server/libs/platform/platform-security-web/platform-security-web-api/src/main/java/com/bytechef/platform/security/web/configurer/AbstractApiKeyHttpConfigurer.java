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

package com.bytechef.platform.security.web.configurer;

import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;

import com.bytechef.platform.security.web.filter.ApiKeyAuthenticationFilter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * @author Ivica Cardic
 */
public class AbstractApiKeyHttpConfigurer
    extends AbstractHttpConfigurer<AbstractApiKeyHttpConfigurer, HttpSecurity> {

    private final AuthenticationConverter authenticationConverter;
    private final AuthenticationProvider authenticationProvider;
    private final Object pathPattern;

    public AbstractApiKeyHttpConfigurer(
        String pathPatternRegex, AuthenticationConverter authenticationConverter,
        AuthenticationProvider authenticationProvider) {

        this.authenticationConverter = authenticationConverter;
        this.authenticationProvider = authenticationProvider;
        this.pathPattern = pathPatternRegex;
    }

    public AbstractApiKeyHttpConfigurer(
        RequestMatcher requestMatcher, AuthenticationConverter authenticationConverter,
        AuthenticationProvider authenticationProvider) {

        this.authenticationConverter = authenticationConverter;
        this.authenticationProvider = authenticationProvider;
        this.pathPattern = requestMatcher;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init(HttpSecurity http) {
        http.authenticationProvider(authenticationProvider);

        CsrfConfigurer<?> csrf = http.getConfigurer(CsrfConfigurer.class);

        if (csrf != null) {
            registerCsrfOverride(csrf);
        }
    }

    @Override
    public void configure(HttpSecurity http) {
        var authenticationManager = http.getSharedObject(AuthenticationManager.class);

        RequestMatcher requestMatcher;

        if (pathPattern instanceof String) {
            requestMatcher = regexMatcher((String) pathPattern);
        } else {
            requestMatcher = (RequestMatcher) pathPattern;
        }

        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(
            requestMatcher, authenticationConverter, authenticationManager);

        http.addFilterBefore(filter, BasicAuthenticationFilter.class);
    }

    protected void registerCsrfOverride(CsrfConfigurer<?> csrf) {
    }
}
