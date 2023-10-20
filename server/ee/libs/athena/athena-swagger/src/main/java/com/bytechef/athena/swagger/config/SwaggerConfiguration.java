/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.athena.swagger.config;

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
    public GroupedOpenApi embeddedOpenApi() {
        return GroupedOpenApi.builder()
            .group("embedded")
            .displayName("Embedded API")
            .pathsToMatch(new String[] {
                "/api/embedded/**"
            })
            .build();
    }
}
