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

package com.bytechef.platform.component.log;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.tenant.multi.sql.MultiTenantDataSource;
import com.bytechef.platform.component.log.JdbcLogFileStorage.Scope;
import com.bytechef.platform.component.log.domain.LogEntry;
import com.bytechef.tenant.TenantContext;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author Ivica Cardic
 */
@Testcontainers
class JdbcLogFileStorageIntTest {

    private static final String CHANGELOG = "classpath:config/liquibase/changelog/platform/execution/" +
        "20261026000000_platform_execution_added_component_log_entry_table.xml";

    @Container
    private static final PostgreSQLContainer<?> postgreSql = new PostgreSQLContainer<>("postgres:16-alpine");

    private static HikariDataSource hikariDataSource;

    @BeforeAll
    static void beforeAll() throws Exception {
        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.setJdbcUrl(postgreSql.getJdbcUrl());
        hikariConfig.setUsername(postgreSql.getUsername());
        hikariConfig.setPassword(postgreSql.getPassword());

        hikariDataSource = new HikariDataSource(hikariConfig);

        migrate(hikariDataSource, "public");

        for (String tenantId : List.of("one", "two")) {
            String schema = getSchema(tenantId);

            execute("CREATE SCHEMA " + schema);

            migrate(hikariDataSource, schema);
        }

        execute("CREATE SCHEMA " + getSchema("unmigrated"));
    }

    @AfterAll
    static void afterAll() {
        hikariDataSource.close();
    }

    @Nested
    class SingleTenant {

        private final JdbcLogFileStorage jobLogFileStorage = createLogFileStorage(hikariDataSource, Scope.JOB);

        @AfterEach
        void afterEach() throws Exception {
            execute("DELETE FROM public.component_log_entry");
        }

        @Test
        void testReadsTheEntriesOfATaskExecutionInTheOrderStored() {
            LogEntry first = createLogEntry(LogEntry.Level.INFO, "first", 7L);
            LogEntry second = LogEntry.builder()
                .timestamp(truncatedNow())
                .level(LogEntry.Level.ERROR)
                .componentName("httpClient")
                .componentOperationName("get")
                .taskExecutionId(7L)
                .triggerExecutionId(3L)
                .message("second")
                .exception(new IllegalStateException("boom"))
                .build();

            jobLogFileStorage.storeLogEntries(1L, 7L, List.of(first, second));
            jobLogFileStorage.storeLogEntry(1L, 8L, createLogEntry(LogEntry.Level.DEBUG, "other task", 8L));

            assertThat(jobLogFileStorage.readLogEntries(1L, 7L)).containsExactly(first, second);
        }

        @Test
        void testReadsTheEntriesOfAJobAcrossTaskExecutions() {
            jobLogFileStorage.storeLogEntry(1L, 7L, createLogEntry(LogEntry.Level.INFO, "a", 7L));
            jobLogFileStorage.storeLogEntry(1L, 8L, createLogEntry(LogEntry.Level.INFO, "b", 8L));
            jobLogFileStorage.storeLogEntry(2L, 9L, createLogEntry(LogEntry.Level.INFO, "other job", 9L));

            assertThat(jobLogFileStorage.readLogEntriesByJobId(1L))
                .extracting(LogEntry::message)
                .containsExactly("a", "b");
        }

        @Test
        void testLogsExistUntilDeleted() {
            assertThat(jobLogFileStorage.logsExist(1L)).isFalse();

            jobLogFileStorage.storeLogEntry(1L, 7L, createLogEntry(LogEntry.Level.INFO, "a", 7L));
            jobLogFileStorage.storeLogEntry(2L, 9L, createLogEntry(LogEntry.Level.INFO, "kept", 9L));

            assertThat(jobLogFileStorage.logsExist(1L)).isTrue();

            jobLogFileStorage.deleteLogEntries(1L);

            assertThat(jobLogFileStorage.logsExist(1L)).isFalse();
            assertThat(jobLogFileStorage.readLogEntriesByJobId(2L)).hasSize(1);
        }

        @Test
        void testScopesDoNotSeeEachOther() {
            JdbcLogFileStorage editorLogFileStorage = createLogFileStorage(hikariDataSource, Scope.EDITOR);

            jobLogFileStorage.storeLogEntry(1L, 7L, createLogEntry(LogEntry.Level.INFO, "job", 7L));
            editorLogFileStorage.storeLogEntry(1L, 7L, createLogEntry(LogEntry.Level.INFO, "editor", 7L));

            assertThat(jobLogFileStorage.readLogEntries(1L, 7L))
                .extracting(LogEntry::message)
                .containsExactly("job");

            editorLogFileStorage.deleteLogEntries(1L);

            assertThat(jobLogFileStorage.logsExist(1L)).isTrue();
        }

        @Test
        void testIgnoresAnEmptyBatch() {
            jobLogFileStorage.storeLogEntries(1L, 7L, List.of());

            assertThat(jobLogFileStorage.logsExist(1L)).isFalse();
        }
    }

    @Nested
    class MultiTenant {

        private final JdbcLogFileStorage jobLogFileStorage =
            createLogFileStorage(new MultiTenantDataSource(hikariDataSource), Scope.JOB);

        @Test
        void testTenantsDoNotSeeEachOthersLogs() {
            TenantContext.runWithTenantId(
                "one",
                () -> jobLogFileStorage.storeLogEntry(1L, 7L, createLogEntry(LogEntry.Level.INFO, "tenant one", 7L)));

            assertThat(TenantContext.callWithTenantId("one", () -> jobLogFileStorage.readLogEntries(1L, 7L)))
                .extracting(LogEntry::message)
                .containsExactly("tenant one");
            assertThat(TenantContext.callWithTenantId("two", () -> jobLogFileStorage.logsExist(1L))).isFalse();
        }

        @Test
        void testFailedWriteDoesNotThrow() {
            TenantContext.runWithTenantId(
                "unmigrated",
                () -> jobLogFileStorage.storeLogEntry(1L, 7L, createLogEntry(LogEntry.Level.INFO, "lost", 7L)));
        }
    }

    private static JdbcLogFileStorage createLogFileStorage(DataSource dataSource, Scope scope) {
        return new JdbcLogFileStorage(new NamedParameterJdbcTemplate(dataSource), scope);
    }

    private static LogEntry createLogEntry(LogEntry.Level level, String message, long taskExecutionId) {
        return LogEntry.builder()
            .timestamp(truncatedNow())
            .level(level)
            .componentName("logger")
            .componentOperationName("info")
            .taskExecutionId(taskExecutionId)
            .message(message)
            .build();
    }

    @SuppressFBWarnings("SQL_INJECTION_JDBC")
    private static void execute(String sql) throws Exception {
        try (Connection connection = hikariDataSource.getConnection();
            Statement statement = connection.createStatement()) {

            statement.execute(sql);
        }
    }

    private static String getSchema(String tenantId) {
        return TenantContext.callWithTenantId(tenantId, TenantContext::getCurrentDatabaseSchema);
    }

    private static void migrate(DataSource dataSource, String schema) throws Exception {
        SpringLiquibase springLiquibase = new SpringLiquibase();

        springLiquibase.setChangeLog(CHANGELOG);
        springLiquibase.setDataSource(dataSource);
        springLiquibase.setDefaultSchema(schema);
        springLiquibase.setResourceLoader(new DefaultResourceLoader());

        springLiquibase.afterPropertiesSet();
    }

    private static Instant truncatedNow() {
        Instant now = Instant.now();

        return now.truncatedTo(ChronoUnit.MILLIS);
    }
}
