/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ee.platform.ai.tool.usage.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * Activates Spring Data JDBC repository scanning for the shared {@code ai_tool_usage} table. Gated on the host app
 * declaring an {@link AbstractJdbcConfiguration} so a deployment that doesn't ship Spring Data JDBC at all (e.g. a
 * future tool-usage-only EE app that uses a different persistence backend) won't fail bean creation here.
 *
 * <p>
 * Kept separate from {@link AiToolUsageConfiguration} so the JDBC concern (repository discovery, schema mapping) stays
 * decoupled from the bean wiring (recorder + cost estimator). Splitting on that seam means a host app that wants to
 * override the recorder bean with its own implementation can keep this JDBC config active for the entity mapping
 * without inheriting the default-bean registrations.
 * </p>
 *
 * @author Ivica Cardic
 */
@AutoConfiguration(afterName = "org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration")
@ConditionalOnBean(AbstractJdbcConfiguration.class)
@EnableJdbcRepositories(basePackages = "com.bytechef.ee.platform.ai.tool.usage.repository")
public class AiToolUsageJdbcRepositoryConfiguration {
}
