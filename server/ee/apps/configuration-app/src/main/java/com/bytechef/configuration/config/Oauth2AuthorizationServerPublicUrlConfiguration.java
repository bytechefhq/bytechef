/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.configuration.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.oauth2.authorizationserver.annotation.ConditionalOnOauth2AuthorizationServerEnabled;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnOauth2AuthorizationServerEnabled
final class Oauth2AuthorizationServerPublicUrlConfiguration {

    Oauth2AuthorizationServerPublicUrlConfiguration(ApplicationProperties applicationProperties) {
        String publicUrl = applicationProperties.getPublicUrl();

        if (publicUrl == null || publicUrl.isBlank()) {
            throw new IllegalStateException(
                "bytechef.public-url must be set when bytechef.oauth2.authorization-server.enabled is true, " +
                    "otherwise the consent page and the issuer point at configuration-app instead of the gateway");
        }
    }
}
