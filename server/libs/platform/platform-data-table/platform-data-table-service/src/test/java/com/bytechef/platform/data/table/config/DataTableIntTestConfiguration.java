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
import com.bytechef.platform.data.table.domain.DataTableWorkspaceResolver;
import com.bytechef.platform.data.table.execution.listener.DataTableWebhookEventListener;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

/**
 * Everything under {@code com.bytechef.platform.data.table} except the webhook delivery listener.
 *
 * <p>
 * That one is excluded because its default constructor builds a real
 * {@link org.springframework.web.client.RestTemplate} and it is reached by an ordinary row insert, so any test here
 * that both registers a webhook and writes a row would POST to whatever URL the registration names and then retry for a
 * minute. A test that wants delivery registers the listener itself, wired to a transport it can observe.
 *
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = "com.bytechef.platform.data.table",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = DataTableWebhookEventListener.class))
@EnableAutoConfiguration
@EnableConfigurationProperties(ApplicationProperties.class)
@Import(LiquibaseConfiguration.class)
@Configuration
public class DataTableIntTestConfiguration {

    @Bean
    DataTableWorkspaceResolver dataTableWorkspaceResolver() {
        return mock(DataTableWorkspaceResolver.class);
    }

    @Bean
    TagService tagService() {
        return mock(TagService.class);
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class DataTableIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}
