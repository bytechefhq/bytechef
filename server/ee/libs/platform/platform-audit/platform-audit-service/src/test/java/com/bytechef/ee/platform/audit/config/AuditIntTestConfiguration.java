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

package com.bytechef.ee.platform.audit.config;

import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

/**
 * Test sources share the scanned {@code com.bytechef.ee.platform.audit} tree with production ones, so the exclude
 * filter keeps the classes nested inside a sibling integration test out of this context.
 * {@code PermissionAuditAspectIntTest.Config} is the one that matters: it is a {@code @SpringBootConfiguration}
 * contributing its own mock {@code AuditEventService} bean, sharing the same default bean name ("auditEventService") as
 * the real scanned {@code @Service}. That silently overrides the real bean instead of producing an ambiguity error, so
 * {@link com.bytechef.ee.platform.audit.aspect.PermissionAuditAspect} — also reached by this scan, and also
 * constructor-injected with {@code AuditEventService} by type — would resolve the mock instead of the real,
 * DB-persisting service. Nothing in this module currently exercises that aspect through this context, which is why the
 * swap has stayed invisible.
 *
 * <p>
 * The pattern stops at nested classes on purpose: {@code AuditIntTestConfiguration} itself lives in this tree and its
 * own nested {@code AuditIntTestJdbcConfiguration} must still be reachable.
 *
 * @author Ivica Cardic
 */
@ComponentScan(
    basePackages = "com.bytechef.ee.platform.audit",
    excludeFilters = @Filter(type = FilterType.REGEX, pattern = ".*IntTest\\$.*"))
@EnableAutoConfiguration
@Import(LiquibaseConfiguration.class)
@Configuration
public class AuditIntTestConfiguration {

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class AuditIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}
