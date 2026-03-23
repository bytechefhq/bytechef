/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.configurer;

import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;

import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationProvider;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.configurer.AbstractApiKeyHttpConfigurer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedApiKeySecurityConfigurer extends AbstractApiKeyHttpConfigurer {

    @SuppressFBWarnings("EI")
    public EmbeddedApiKeySecurityConfigurer(
        ApiKeyService apiKeyService, ConnectedUserService connectedUserService, JwtTokenService jwtTokenService,
        SigningKeyService signingKeyService) {

        super(
            request -> regexMatcher("^/api/embedded/v[0-9]+/.+").matches(request) ||
                (regexMatcher("^/api/(?:automation|embedded|platform)/internal/.+").matches(request) &&
                    request.getHeader("Authorization") != null),
            new EmbeddedApiKeyAuthenticationConverter(jwtTokenService, signingKeyService),
            new EmbeddedApiKeyAuthenticationProvider(apiKeyService, connectedUserService));
    }

    @Override
    protected void registerCsrfOverride(CsrfConfigurer<?> csrf) {
        csrf.ignoringRequestMatchers(regexMatcher("^/api/embedded/v[0-9]+/.+"));
        // For internal calls from the embedded workflow builder
        csrf.ignoringRequestMatchers(
            request -> regexMatcher("^/api/(?:automation|embedded|platform)/internal/.+").matches(request) &&
                request.getHeader("Authorization") != null);
    }
}
