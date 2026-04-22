/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.prompt.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * Activates Spring Data JDBC repository scanning for the {@code ai_prompt} and {@code ai_prompt_version} tables. The
 * {@code workspace_ai_prompt} membership table is owned by automation-ai-prompt's
 * {@code WorkspaceAiPromptJdbcRepositoryConfiguration}. Discovered via {@code AutoConfiguration.imports} when this
 * module is on the classpath, gated on a {@link AbstractJdbcConfiguration} bean so app variants without JDBC start
 * cleanly.
 *
 * @author Ivica Cardic
 * @version ee
 */
@AutoConfiguration(afterName = "org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration")
@ConditionalOnBean(AbstractJdbcConfiguration.class)
@EnableJdbcRepositories(basePackages = "com.bytechef.ee.platform.ai.prompt.repository")
public class AiPromptJdbcRepositoryConfiguration {
}
