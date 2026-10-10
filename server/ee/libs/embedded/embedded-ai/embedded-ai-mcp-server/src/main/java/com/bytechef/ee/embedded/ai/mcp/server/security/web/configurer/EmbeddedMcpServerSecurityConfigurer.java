/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.configurer;

import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpServerApiKeyAuthenticationProvider;
import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpServerApiKeyAuthenticationToken;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.security.web.configurer.AbstractApiKeyHttpConfigurer;
import com.bytechef.platform.security.web.mcp.McpAnonymousAuthenticationToken;
import com.bytechef.platform.security.web.mcp.McpAuthenticationGuardFilter;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedMcpServerSecurityConfigurer extends AbstractApiKeyHttpConfigurer {

    private static final String PATH_PATTERN = "^/api/embedded/.+/mcp";

    public EmbeddedMcpServerSecurityConfigurer(
        ConnectedUserService connectedUserService, McpServerService mcpServerService,
        SigningKeyService signingKeyService) {

        super(
            PATH_PATTERN,
            new EmbeddedMcpServerApiKeyAuthenticationConverter(signingKeyService),
            new EmbeddedMcpServerApiKeyAuthenticationProvider(connectedUserService, mcpServerService));
    }

    @Override
    public void configure(HttpSecurity http) {
        super.configure(http);

        McpAuthenticationGuardFilter mcpAuthenticationGuardFilter = new McpAuthenticationGuardFilter(
            RegexRequestMatcher.regexMatcher(PATH_PATTERN),
            List.of(EmbeddedMcpServerApiKeyAuthenticationToken.class, McpAnonymousAuthenticationToken.class),
            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));

        http.addFilterBefore(mcpAuthenticationGuardFilter, AuthorizationFilter.class);
    }

    @Override
    protected void registerCsrfOverride(CsrfConfigurer<?> csrf) {
        csrf.ignoringRequestMatchers(RegexRequestMatcher.regexMatcher(PATH_PATTERN));
    }
}
