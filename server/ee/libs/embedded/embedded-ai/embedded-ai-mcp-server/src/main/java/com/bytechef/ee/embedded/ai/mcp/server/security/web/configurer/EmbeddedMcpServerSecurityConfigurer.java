/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.configurer;

import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpServerApiKeyAuthenticationProvider;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.platform.security.web.configurer.AbstractApiKeyHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedMcpServerSecurityConfigurer extends AbstractApiKeyHttpConfigurer {

    private static final String PATH_PATTERN = "^/api/embedded/.+/mcp";

    public EmbeddedMcpServerSecurityConfigurer(
        ConnectedUserService connectedUserService, SigningKeyService signingKeyService) {

        super(
            PATH_PATTERN, new EmbeddedMcpServerApiKeyAuthenticationConverter(signingKeyService),
            new EmbeddedMcpServerApiKeyAuthenticationProvider(connectedUserService));
    }

    @Override
    protected void registerCsrfOverride(CsrfConfigurer<?> csrf) {
        csrf.ignoringRequestMatchers(RegexRequestMatcher.regexMatcher(PATH_PATTERN));
    }
}
