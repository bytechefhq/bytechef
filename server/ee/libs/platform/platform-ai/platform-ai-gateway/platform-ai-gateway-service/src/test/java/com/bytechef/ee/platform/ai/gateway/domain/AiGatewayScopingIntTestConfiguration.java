/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.domain;

import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 */
@Import({
    LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class
})
@EnableAutoConfiguration
@Configuration
class AiGatewayScopingIntTestConfiguration {
}
