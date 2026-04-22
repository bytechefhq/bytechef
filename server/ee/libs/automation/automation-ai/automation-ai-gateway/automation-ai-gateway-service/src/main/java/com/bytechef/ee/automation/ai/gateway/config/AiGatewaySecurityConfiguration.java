/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.config;

import com.bytechef.ee.automation.ai.gateway.security.web.configurer.AiGatewaySecurityConfigurer;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.config.SecurityConfigurerContributor;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

/**
 * @version ee
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class AiGatewaySecurityConfiguration {

    @Bean
    SecurityConfigurerContributor aiGatewaySecurityConfigurerContributor(
        ApiKeyService apiKeyService, AuthorityService authorityService, UserService userService) {

        return new SecurityConfigurerContributor() {

            @Override
            @SuppressWarnings("unchecked")
            public <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>> T
                getSecurityConfigurerAdapter() {

                return (T) new AiGatewaySecurityConfigurer(apiKeyService, authorityService, userService);
            }
        };
    }

}
