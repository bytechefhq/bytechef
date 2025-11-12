/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.handler.security.web.config;

import com.bytechef.ee.automation.apiplatform.configuration.service.ApiClientService;
import com.bytechef.ee.automation.apiplatform.handler.security.web.configurer.ApiPlatformApiKeySecurityConfigurer;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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

    private final ApiClientService apiClientService;

    @SuppressFBWarnings("EI")
    public ApiPlatformApiKeySecurityConfigurerContributor(ApiClientService apiClientService) {
        this.apiClientService = apiClientService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>> T getSecurityConfigurerAdapter() {
        return (T) new ApiPlatformApiKeySecurityConfigurer(apiClientService);
    }
}
