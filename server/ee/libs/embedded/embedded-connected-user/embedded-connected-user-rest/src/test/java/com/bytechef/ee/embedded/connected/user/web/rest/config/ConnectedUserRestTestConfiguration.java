/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.web.rest.config;

import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.platform.configuration.web.rest.mapper.EnvironmentMapper;
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
    basePackages = {
        "com.bytechef.ee.embedded.connected.user.web.rest",
        "com.bytechef.ee.embedded.configuration.web.rest.adapter",
        "com.bytechef.web.rest.error",
        "com.bytechef.web.rest.mapper"
    },
    excludeFilters = @Filter(type = FilterType.REGEX, pattern = ".*Test\\$.*"))
@Configuration
@Import({
    EnvironmentMapper.class, JacksonConfiguration.class
})
public class ConnectedUserRestTestConfiguration {
}
