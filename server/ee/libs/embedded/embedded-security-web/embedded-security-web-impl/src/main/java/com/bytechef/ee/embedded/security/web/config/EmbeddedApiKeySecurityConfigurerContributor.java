/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.config;

import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.ee.embedded.security.web.configurer.EmbeddedApiKeySecurityConfigurer;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.config.SecurityConfigurerContributor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
public class EmbeddedApiKeySecurityConfigurerContributor implements SecurityConfigurerContributor {

    private final ApiKeyService apiKeyService;
    private final ConnectedUserService connectedUserService;
    private final EnvironmentService environmentService;
    private final JwtTokenService jwtTokenService;
    private final SigningKeyService signingKeyService;

    @SuppressFBWarnings("EI")
    public EmbeddedApiKeySecurityConfigurerContributor(
        ApiKeyService apiKeyService, ConnectedUserService connectedUserService, EnvironmentService environmentService,
        JwtTokenService jwtTokenService,
        SigningKeyService signingKeyService) {

        this.apiKeyService = apiKeyService;
        this.connectedUserService = connectedUserService;
        this.environmentService = environmentService;
        this.jwtTokenService = jwtTokenService;
        this.signingKeyService = signingKeyService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>> T getSecurityConfigurerAdapter() {
        return (T) new EmbeddedApiKeySecurityConfigurer(
            apiKeyService, connectedUserService, environmentService, jwtTokenService, signingKeyService);
    }
}
