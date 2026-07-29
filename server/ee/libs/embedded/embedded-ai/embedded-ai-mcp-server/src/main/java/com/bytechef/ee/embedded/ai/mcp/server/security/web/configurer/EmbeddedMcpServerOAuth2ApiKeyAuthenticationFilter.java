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
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * The OAuth2 credential filter for the embedded MCP endpoint. It is a distinct {@link ApiKeyAuthenticationFilter}
 * subclass purely so it carries its own {@code OncePerRequestFilter} "already filtered" attribute name (derived from
 * the class name). Without a distinct class, the OAuth2 filter and the signing-key filter - both plain
 * {@code ApiKeyAuthenticationFilter} instances - would share one attribute, and the second filter in the chain would be
 * skipped as "already filtered", so a signing-key (or token-less anonymous) request would never reach the signing-key
 * credential path.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedMcpServerOAuth2ApiKeyAuthenticationFilter extends ApiKeyAuthenticationFilter {

    EmbeddedMcpServerOAuth2ApiKeyAuthenticationFilter(
        RequestMatcher requestMatcher, AuthenticationConverter authenticationConverter,
        AuthenticationManager authenticationManager) {

        super(requestMatcher, authenticationConverter, authenticationManager);
    }
}
