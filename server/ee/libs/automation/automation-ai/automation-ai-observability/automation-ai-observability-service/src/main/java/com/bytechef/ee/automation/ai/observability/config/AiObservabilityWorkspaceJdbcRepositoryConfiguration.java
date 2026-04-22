/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.config;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * Activates Spring Data JDBC repository scanning for the workspace_ai_observability_* family of relation tables that
 * live alongside the gateway tenant surface (alert rules, export jobs, notification channels, sessions, traces, webhook
 * subscriptions). Discovered via {@code AutoConfiguration.imports} when this module is on the classpath, gated on a
 * {@link AbstractJdbcConfiguration} bean so app variants without JDBC start cleanly.
 *
 * @author Ivica Cardic
 * @version ee
 */
@AutoConfiguration(afterName = "org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration")
@ConditionalOnEEVersion
@ConditionalOnBean(AbstractJdbcConfiguration.class)
@EnableJdbcRepositories(basePackages = "com.bytechef.ee.automation.ai.observability.repository")
public class AiObservabilityWorkspaceJdbcRepositoryConfiguration {
}
