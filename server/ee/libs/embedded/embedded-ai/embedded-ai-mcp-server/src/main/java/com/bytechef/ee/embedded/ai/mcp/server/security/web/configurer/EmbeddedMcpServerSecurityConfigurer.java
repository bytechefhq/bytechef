/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.configurer;

import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpServerApiKeyAuthenticationProvider;
import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpServerApiKeyAuthenticationToken;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.security.web.configurer.AbstractApiKeyHttpConfigurer;
import com.bytechef.platform.security.web.filter.AbstractApiKeyAuthenticationConverter;
import com.bytechef.tenant.domain.TenantKey;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedMcpServerSecurityConfigurer extends AbstractApiKeyHttpConfigurer {

    private static final String PATH_PATTERN = "^/api/embedded/.+/mcp";

    public EmbeddedMcpServerSecurityConfigurer(McpServerService mcpServerService) {
        super(
            PATH_PATTERN, new EmbeddedMcpServerApiKeyAuthenticationConverter(),
            new EmbeddedMcpServerApiKeyAuthenticationProvider(mcpServerService));
    }

    @Override
    protected void registerCsrfOverride(CsrfConfigurer<?> csrf) {
        csrf.ignoringRequestMatchers(RegexRequestMatcher.regexMatcher(PATH_PATTERN));
    }

    private static class EmbeddedMcpServerApiKeyAuthenticationConverter
        extends AbstractApiKeyAuthenticationConverter {

        @Override
        public Authentication convert(HttpServletRequest request) {
            String authToken = fetchAuthToken(request);
            String servletPath = request.getServletPath();

            String mcpServerSecretKey = servletPath.replace("/api/embedded/", "")
                .replace("/mcp", "");

            String externalUserId = request.getParameter("externalUserId");

            TenantKey tenantKey = TenantKey.parse(mcpServerSecretKey);

            return new EmbeddedMcpServerApiKeyAuthenticationToken(
                mcpServerSecretKey, externalUserId, authToken, tenantKey.getTenantId());
        }
    }
}
