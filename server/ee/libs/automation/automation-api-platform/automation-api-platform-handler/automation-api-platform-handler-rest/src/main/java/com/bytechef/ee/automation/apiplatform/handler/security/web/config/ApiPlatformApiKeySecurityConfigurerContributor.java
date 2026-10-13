/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.handler.security.web.config;

import com.bytechef.ee.automation.apiplatform.handler.security.web.configurer.ApiPlatformApiKeySecurityConfigurer;
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
public class ApiPlatformApiKeySecurityConfigurerContributor implements SecurityConfigurerContributor {

    private final ApiKeyService apiKeyService;
    private final EnvironmentService environmentService;

    @SuppressFBWarnings("EI")
    public ApiPlatformApiKeySecurityConfigurerContributor(ApiKeyService apiKeyService,
        EnvironmentService environmentService) {
        this.apiKeyService = apiKeyService;
        this.environmentService = environmentService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>> T getSecurityConfigurerAdapter() {
        return (T) new ApiPlatformApiKeySecurityConfigurer(apiKeyService, environmentService);
    }
}
