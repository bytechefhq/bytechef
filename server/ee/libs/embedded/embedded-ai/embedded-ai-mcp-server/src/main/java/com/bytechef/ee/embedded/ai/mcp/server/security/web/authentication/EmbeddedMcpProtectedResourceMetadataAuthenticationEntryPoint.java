/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Answers an unauthenticated embedded MCP request with the RFC 9728 discovery challenge: {@code 401} plus a
 * {@code WWW-Authenticate: Bearer resource_metadata="..."} header pointing at this endpoint's protected-resource
 * metadata. The pointer is built per request by inserting {@code /.well-known/oauth-protected-resource} ahead of the
 * request path, so each tenant's {@code /api/embedded/{secret}/mcp} resource advertises its own metadata document
 * (which names that tenant's identity provider) - the "your IdP runs the show" external-direct discovery path.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedMcpProtectedResourceMetadataAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String WELL_KNOWN_PREFIX = "/.well-known/oauth-protected-resource";

    @Override
    public void commence(
        HttpServletRequest request, HttpServletResponse response, AuthenticationException authenticationException) {

        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        String pathWithinApplication = requestUri.substring(contextPath.length());

        String metadataUrl = UriComponentsBuilder.fromUriString(UrlUtils.buildFullRequestUrl(request))
            .replacePath(contextPath + WELL_KNOWN_PREFIX + pathWithinApplication)
            .replaceQuery(null)
            .fragment(null)
            .build()
            .toUriString();

        response.setHeader("WWW-Authenticate", "Bearer resource_metadata=\"" + metadataUrl + "\"");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
