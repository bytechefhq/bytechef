
/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.execution.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("api-docs")
public class SwaggerConfiguration {

    @Bean
    public GroupedOpenApi automationOpenApi() {
        return GroupedOpenApi.builder()
            .group("automation")
            .displayName("Automation API")
            .pathsToMatch(new String[] {
                "/api/automation/**"
            })
            .build();
    }

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
