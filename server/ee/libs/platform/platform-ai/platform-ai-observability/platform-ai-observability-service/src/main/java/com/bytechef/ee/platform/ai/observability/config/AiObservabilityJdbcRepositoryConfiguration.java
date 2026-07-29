/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.config;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * Activates Spring Data JDBC repository scanning for the ai_observability_* family — sessions, traces, spans, alert
 * rules + events, export jobs, webhook subscriptions + deliveries. Every table, including its {@code workspace_id}
 * scoping column, is owned by platform-ai-observability so the gateway-side OTLP ingest, alerting, exporter,
 * dispatcher, cleanup, and GraphQL surfaces can depend on a single CRUD layer rather than reach across modules.
 * Discovered via {@code AutoConfiguration.imports} when this module is on the classpath, gated on a
 * {@link AbstractJdbcConfiguration} bean so app variants without JDBC start cleanly.
 *
 * @author Ivica Cardic
 * @version ee
 */
@AutoConfiguration(afterName = "org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration")
@ConditionalOnEEVersion
@ConditionalOnBean(AbstractJdbcConfiguration.class)
@EnableJdbcRepositories(basePackages = "com.bytechef.ee.platform.ai.observability.repository")
public class AiObservabilityJdbcRepositoryConfiguration {
}
