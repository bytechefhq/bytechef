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

package com.bytechef.jdbc.config;

import com.bytechef.platform.security.util.SecurityUtils;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.auditing.CurrentDateTimeProvider;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * @author Ivica Cardic
 */
@Configuration
@EnableJdbcAuditing(auditorAwareRef = "springSecurityAuditorAware", dateTimeProviderRef = "auditingDateTimeProvider")
@EnableJdbcRepositories(
    basePackages = {
        "com.bytechef.atlas.configuration.repository.jdbc",
        "com.bytechef.atlas.execution.repository.jdbc",
        "com.bytechef.automation.configuration.repository",
        "com.bytechef.automation.data.table.configuration.repository",
        "com.bytechef.automation.knowledgebase.repository",
        "com.bytechef.automation.mcp.repository",
        "com.bytechef.automation.task.repository",
        "com.bytechef.platform.category.repository",
        "com.bytechef.platform.configuration.repository",
        "com.bytechef.platform.connection.repository",
        "com.bytechef.platform.data.storage.jdbc.repository",
        "com.bytechef.platform.mcp.repository",
        "com.bytechef.platform.notification.repository",
        "com.bytechef.platform.security.repository",
        "com.bytechef.platform.tag.repository",
        "com.bytechef.platform.user.repository",
        "com.bytechef.platform.workflow.execution.repository",
        "com.bytechef.ee.ai.copilot.repository",
        "com.bytechef.ee.automation.apiplatform.configuration.repository",
        "com.bytechef.ee.automation.configuration.repository",
        "com.bytechef.ee.embedded.configuration.repository",
        "com.bytechef.ee.embedded.connected.user.repository",
        "com.bytechef.ee.embedded.security.repository",
        "com.bytechef.ee.platform.apiconnector.configuration.repository",
        "com.bytechef.ee.platform.audit.repository",
        "com.bytechef.ee.platform.codeworkflow.configuration.repository",
        "com.bytechef.ee.platform.customcomponent.configuration.repository"
    },
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION, classes = com.bytechef.platform.jdbc.ConditionalJdbcRepository.class))
public class AuditingJdbcConfiguration {

    private static final String SYSTEM = "system";

    @Bean
    DateTimeProvider auditingDateTimeProvider() {
        return CurrentDateTimeProvider.INSTANCE;
    }

    @Bean
    AuditorAware<String> springSecurityAuditorAware() {
        return () -> Optional.of(
            SecurityUtils.fetchCurrentUserLogin()
                .orElse(SYSTEM));
    }
}
