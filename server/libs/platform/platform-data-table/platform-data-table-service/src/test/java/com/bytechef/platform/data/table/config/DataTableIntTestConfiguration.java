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

package com.bytechef.platform.data.table.config;

import static org.mockito.Mockito.mock;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.data.table.configuration.audit.DataTableAuditPublisher;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

/**
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = "com.bytechef.platform.data.table")
@EnableAutoConfiguration
@EnableConfigurationProperties(ApplicationProperties.class)
@Import(LiquibaseConfiguration.class)
@Configuration
public class DataTableIntTestConfiguration {

    @Bean
    DataTableAuditPublisher dataTableAuditPublisher() {
        return mock(DataTableAuditPublisher.class);
    }

    /**
     * Reached only by {@code DataTableTagServiceImpl}, which these tests do not exercise. Mocked rather than scanned so
     * tagging does not drag its own schema and services into a data-table context.
     */
    @Bean
    TagService tagService() {
        return mock(TagService.class);
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class DataTableIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}
