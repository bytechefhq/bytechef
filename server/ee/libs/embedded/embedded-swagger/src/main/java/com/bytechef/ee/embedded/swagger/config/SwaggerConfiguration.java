/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.swagger.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration("embeddedSwaggerConfiguration")
@Profile("api-docs")
public class SwaggerConfiguration {

    @Bean
    public GroupedOpenApi embeddedByConnectedUserTokeOpenApi() {
        return GroupedOpenApi.builder()
            .group("embedded-frontend")
            .displayName("Embedded Frontend V1 API")
            .pathsToMatch("/api/embedded/frontend/v1/**")
            .build();
    }

    @Bean
    public GroupedOpenApi embeddedInternalOpenApi() {
        return GroupedOpenApi.builder()
            .group("embedded-internal")
            .displayName("Embedded Internal API")
            .pathsToMatch("/api/embedded/internal/**")
            .build();
    }

    @Bean
    public GroupedOpenApi embeddedPublicOpenApi() {
        return GroupedOpenApi.builder()
            .group("embedded-public")
            .displayName("Embedded Public V1 API")
            .pathsToMatch("/api/embedded/v1/**")
            .build();
    }
}
