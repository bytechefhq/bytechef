/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.webhook.public_.web.rest.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.jackson.config.JacksonConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = "com.bytechef.ee.embedded.webhook.public_.web.rest",
    excludeFilters = @Filter(type = FilterType.REGEX, pattern = ".*Test\\$.*"))
@Configuration
@Import(JacksonConfiguration.class)
public class EmbeddedWebhookPublicRestTestConfiguration {

    @Bean
    ApplicationProperties applicationProperties() {
        return new ApplicationProperties();
    }
}
