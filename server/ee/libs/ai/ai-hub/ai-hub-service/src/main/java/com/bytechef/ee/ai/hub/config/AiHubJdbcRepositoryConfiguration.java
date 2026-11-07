/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import com.bytechef.ee.ai.hub.usage.CostEstimationProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@AutoConfiguration(afterName = "org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration")
@ConditionalOnBean(AbstractJdbcConfiguration.class)
@EnableConfigurationProperties(CostEstimationProperties.class)
@EnableJdbcRepositories(
    basePackages = {
        "com.bytechef.ee.ai.hub.chat.repository", "com.bytechef.ee.ai.hub.approval.repository",
        "com.bytechef.ee.ai.hub.mcpserver.repository"
    })
public class AiHubJdbcRepositoryConfiguration {
}
