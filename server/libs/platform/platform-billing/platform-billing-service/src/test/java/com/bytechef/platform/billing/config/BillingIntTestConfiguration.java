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

package com.bytechef.platform.billing.config;

import static org.mockito.Mockito.mock;

import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.billing.client.StripeClient;
import com.bytechef.tenant.service.TenantService;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

/**
 * Loads the real billing services, facades and repositories against Postgres. The job and principal_job tables the
 * usage query reads come from the atlas and platform execution changelogs; their repositories are not needed. Stripe
 * and the tenant registry are external to every tested path and stay mocked.
 *
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = {
        "com.bytechef.platform.billing.facade", "com.bytechef.platform.billing.service"
    },
    excludeFilters = @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class))
@EnableAutoConfiguration(excludeName = {
    "com.bytechef.atlas.execution.repository.jdbc.config.AtlasExecutionJdbcRepositoryConfiguration",
    "com.bytechef.platform.workflow.execution.config.PlatformWorkflowExecutionJdbcRepositoryConfiguration"
})
@EnableCaching
@Import(LiquibaseConfiguration.class)
@Configuration
public class BillingIntTestConfiguration {

    @Bean
    CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }

    @Bean
    StripeClient stripeClient() {
        return mock(StripeClient.class);
    }

    @Bean
    TenantService tenantService() {
        return mock(TenantService.class);
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class BillingIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}
