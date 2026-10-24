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

package com.bytechef.platform.component.log.listener;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.execution.event.JobDeletedEvent;
import com.bytechef.platform.component.log.JdbcLogFileStorage;
import com.bytechef.platform.component.log.JdbcLogFileStorage.Scope;
import com.bytechef.platform.component.log.LogFileStorage;
import com.bytechef.platform.component.log.domain.LogEntry;
import com.bytechef.platform.component.log.repository.ComponentLogEntryRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author Ivica Cardic
 */
@Testcontainers
class JobDeletedEventListenerIntTest {

    private static final List<String> CHANGELOGS = List.of(
        "classpath:config/liquibase/changelog/platform/execution/" +
            "20261026000000_platform_execution_added_component_log_entry_table.xml",
        "classpath:config/liquibase/changelog/platform/execution/" +
            "20261030000000_platform_execution_added_component_log_entry_cluster_element_call_columns.xml");

    private static final long DELETED_JOB_ID = 1L;
    private static final long OTHER_JOB_ID = 2L;

    @Container
    private static final PostgreSQLContainer<?> postgreSql = new PostgreSQLContainer<>("postgres:16-alpine");

    private static AnnotationConfigApplicationContext applicationContext;
    private static HikariDataSource hikariDataSource;

    private LogFileStorage logFileStorage;
    private TransactionTemplate transactionTemplate;

    @BeforeAll
    static void beforeAll() throws Exception {
        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.setJdbcUrl(postgreSql.getJdbcUrl());
        hikariConfig.setUsername(postgreSql.getUsername());
        hikariConfig.setPassword(postgreSql.getPassword());

        hikariDataSource = new HikariDataSource(hikariConfig);

        for (String changelog : CHANGELOGS) {
            SpringLiquibase springLiquibase = new SpringLiquibase();

            springLiquibase.setChangeLog(changelog);
            springLiquibase.setDataSource(hikariDataSource);
            springLiquibase.setResourceLoader(new DefaultResourceLoader());

            springLiquibase.afterPropertiesSet();
        }

        applicationContext = new AnnotationConfigApplicationContext();

        applicationContext.registerBean(
            NamedParameterJdbcOperations.class, () -> new NamedParameterJdbcTemplate(hikariDataSource));
        applicationContext.registerBean(
            "transactionManager", PlatformTransactionManager.class,
            () -> new DataSourceTransactionManager(hikariDataSource));
        applicationContext.register(JobDeletedEventListenerIntTestConfiguration.class);
        applicationContext.registerBean(
            LogFileStorage.class,
            () -> new JdbcLogFileStorage(applicationContext.getBean(ComponentLogEntryRepository.class), Scope.JOB));
        applicationContext.register(JobDeletedEventListener.class);

        applicationContext.refresh();
    }

    @AfterAll
    static void afterAll() {
        applicationContext.close();

        hikariDataSource.close();
    }

    @BeforeEach
    void beforeEach() {
        logFileStorage = applicationContext.getBean(LogFileStorage.class);
        transactionTemplate = new TransactionTemplate(applicationContext.getBean(PlatformTransactionManager.class));

        logFileStorage.storeLogEntry(DELETED_JOB_ID, 7L, createLogEntry(7L));
        logFileStorage.storeLogEntry(OTHER_JOB_ID, 8L, createLogEntry(8L));
    }

    @AfterEach
    void afterEach() {
        applicationContext.getBean(ComponentLogEntryRepository.class)
            .deleteAll();
    }

    @Test
    void testDeletesTheJobLogsAfterTheDeletingTransactionCommits() {
        transactionTemplate.executeWithoutResult(transactionStatus -> {
            applicationContext.publishEvent(new JobDeletedEvent(DELETED_JOB_ID));

            assertThat(logFileStorage.logsExist(DELETED_JOB_ID)).isTrue();
        });

        assertThat(logFileStorage.logsExist(DELETED_JOB_ID)).isFalse();
        assertThat(logFileStorage.logsExist(OTHER_JOB_ID)).isTrue();
    }

    @Test
    void testKeepsTheJobLogsWhenTheDeletingTransactionRollsBack() {
        transactionTemplate.executeWithoutResult(transactionStatus -> {
            applicationContext.publishEvent(new JobDeletedEvent(DELETED_JOB_ID));

            transactionStatus.setRollbackOnly();
        });

        assertThat(logFileStorage.logsExist(DELETED_JOB_ID)).isTrue();
    }

    @Test
    void testDeletesTheJobLogsWithoutATransaction() {
        applicationContext.publishEvent(new JobDeletedEvent(DELETED_JOB_ID));

        assertThat(logFileStorage.logsExist(DELETED_JOB_ID)).isFalse();
        assertThat(logFileStorage.logsExist(OTHER_JOB_ID)).isTrue();
    }

    private static LogEntry createLogEntry(long taskExecutionId) {
        return LogEntry.builder()
            .timestamp(Instant.now()
                .truncatedTo(ChronoUnit.MILLIS))
            .level(LogEntry.Level.INFO)
            .componentName("logger")
            .componentOperationName("info")
            .taskExecutionId(taskExecutionId)
            .message("entry")
            .build();
    }

    @Configuration
    @EnableJdbcRepositories(basePackages = "com.bytechef.platform.component.log.repository")
    @EnableTransactionManagement
    static class JobDeletedEventListenerIntTestConfiguration extends AbstractJdbcConfiguration {
    }
}
