/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.connection.config;

import com.bytechef.helios.swagger.util.SwaggerUtils;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@Profile("api-docs")
public class SwaggerConfiguration {

    @Bean
    public GroupedOpenApi automationOpenApi() {
        return SwaggerUtils.AUTOMATION_GROUP_API;
    }
}
