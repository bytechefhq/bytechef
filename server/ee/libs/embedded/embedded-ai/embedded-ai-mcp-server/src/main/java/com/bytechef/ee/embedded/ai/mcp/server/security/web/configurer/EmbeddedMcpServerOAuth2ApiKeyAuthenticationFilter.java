/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.configurer;

import com.bytechef.platform.security.web.filter.ApiKeyAuthenticationFilter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedMcpServerOAuth2ApiKeyAuthenticationFilter extends ApiKeyAuthenticationFilter {

    EmbeddedMcpServerOAuth2ApiKeyAuthenticationFilter(
        RequestMatcher requestMatcher, AuthenticationConverter authenticationConverter,
        AuthenticationManager authenticationManager, AuthenticationFailureHandler authenticationFailureHandler) {

        super(requestMatcher, authenticationConverter, authenticationManager, authenticationFailureHandler);
    }
}
