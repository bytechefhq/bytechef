/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.openapi.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration("embeddedOpenApiConfiguration")
@Profile("api-docs")
public class OpenApiConfiguration {

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
