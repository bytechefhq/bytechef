/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.athena.configuration.web.rest.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = {
    "com.bytechef.athena.configuration.web.rest",
    "com.bytechef.category.web.rest.mapper",
    "com.bytechef.hermes.workflow.web.rest",
    "com.bytechef.tag.web.rest.mapper"
})
@Configuration
public class IntegrationRestTestConfiguration {
}
