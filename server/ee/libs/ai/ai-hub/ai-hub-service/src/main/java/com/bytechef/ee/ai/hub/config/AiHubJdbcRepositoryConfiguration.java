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
 * Single AI Hub auto-configuration: enables Spring Data JDBC repositories for every CC-owned sub-domain (task,
 * personal-agent) and binds {@code bytechef.ai-hub.cost-estimation.*} configuration properties.
 *
 * <p>
 * Cross-module repositories are NOT scanned here — they're owned by their own module's auto-configuration:
 * </p>
 *
 * <ul>
 * <li>{@code com.bytechef.platform.ai.auto.memory.repository} — handled by
 * {@code AiAutoMemoryJdbcRepositoryConfiguration} in {@code platform-ai-auto-memory-service}.</li>
 * <li>LLM and tool usage repositories — handled by the corresponding {@code platform-ai-{llm,tool}-usage-service}
 * modules.</li>
 * </ul>
 *
 * <p>
 * Service-layer beans ({@code AiHubTaskServiceImpl}, {@code DefaultCostEstimator}, etc.) are picked up by Spring's
 * component scan via {@code @Component} / {@code @Service} annotations and are not declared here.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@AutoConfiguration(afterName = "org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration")
@ConditionalOnBean(AbstractJdbcConfiguration.class)
@EnableConfigurationProperties(CostEstimationProperties.class)
@EnableJdbcRepositories(
    basePackages = {
        "com.bytechef.ee.ai.hub.mcpserver.repository",
        "com.bytechef.ee.ai.hub.personalagent.repository",
        "com.bytechef.ee.ai.hub.task.repository",
        "com.bytechef.ee.ai.hub.workspacesettings.repository"
    })
public class AiHubJdbcRepositoryConfiguration {
}
