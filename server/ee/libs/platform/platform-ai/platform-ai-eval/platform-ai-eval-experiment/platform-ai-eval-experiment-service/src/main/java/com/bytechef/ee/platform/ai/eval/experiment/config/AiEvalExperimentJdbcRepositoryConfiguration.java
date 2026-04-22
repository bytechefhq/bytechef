/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.experiment.config;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * Activates Spring Data JDBC repository scanning for {@code ai_eval_experiment} and {@code ai_eval_experiment_run}. The
 * {@code workspace_ai_eval_experiment} membership table is owned by automation-ai-eval-experiment's
 * {@code WorkspaceAiEvalExperimentJdbcRepositoryConfiguration}. Discovered via {@code AutoConfiguration.imports} when
 * this module is on the classpath, gated on a {@link AbstractJdbcConfiguration} bean so app variants without JDBC start
 * cleanly.
 *
 * @author Ivica Cardic
 * @version ee
 */
@AutoConfiguration(afterName = "org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration")
@ConditionalOnEEVersion
@ConditionalOnBean(AbstractJdbcConfiguration.class)
@EnableJdbcRepositories(basePackages = "com.bytechef.ee.platform.ai.eval.experiment.repository")
public class AiEvalExperimentJdbcRepositoryConfiguration {
}
