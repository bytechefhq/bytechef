/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest.config;

import com.bytechef.jackson.config.JacksonConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = {
    "com.bytechef.ee.embedded.configuration.public_.web.rest",
    "com.bytechef.platform.configuration.web.rest.adapter",
    "com.bytechef.platform.configuration.web.rest.mapper",
    "com.bytechef.web.rest.mapper"
})
@Configuration
@Import(JacksonConfiguration.class)
public class EmbeddedConfigurationPublicRestTestConfiguration {
}
