/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.session.config.boot;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.tenant.multi.sql.MultiTenantDataSource;
import com.bytechef.tenant.TenantContext;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.session.autoconfigure.SessionAutoConfiguration;
import org.springframework.boot.session.data.redis.autoconfigure.SessionDataRedisAutoConfiguration;
import org.springframework.boot.session.jdbc.autoconfigure.JdbcSessionAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Testcontainers
class SessionStoreEnvironmentPostProcessorIntTest {

    private static final String AUTOCONFIGURE_EXCLUDE = String.join(
        ",", "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration",
        "org.springframework.boot.session.data.redis.autoconfigure.SessionDataRedisAutoConfiguration",
        "org.springframework.boot.session.jdbc.autoconfigure.JdbcSessionAutoConfiguration");

    private static final String COUNT_PUBLIC_SESSIONS = "SELECT count(*) FROM public.spring_session";
    private static final String COUNT_TENANT_SESSIONS = "SELECT count(*) FROM bytechef_one.spring_session";
    private static final String CREATE_TENANT_SCHEMA = "CREATE SCHEMA bytechef_one";
    private static final String CREATE_TENANT_SESSION_TABLE =
        "CREATE TABLE bytechef_one.spring_session (primary_id CHAR(36))";

    @Nested
    class RedisStore {

        @Container
        private static final GenericContainer<?> redis =
            new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

        @Test
        void testSessionCreatedOnOneNodeIsReadOnAnother() {
            WebApplicationContextRunner webApplicationContextRunner = createRunner();

            webApplicationContextRunner.run(nodeA -> webApplicationContextRunner.run(nodeB -> {
                SessionRepository<Session> nodeARepository = getSessionRepository(nodeA);

                Session session = nodeARepository.createSession();

                session.setAttribute("user", "admin@localhost.com");

                nodeARepository.save(session);

                SessionRepository<Session> nodeBRepository = getSessionRepository(nodeB);

                Session foundSession = nodeBRepository.findById(session.getId());

                assertThat(foundSession).isNotNull();
                assertThat(foundSession.<String>getAttribute("user")).isEqualTo("admin@localhost.com");

                StringRedisTemplate stringRedisTemplate = nodeB.getBean(StringRedisTemplate.class);

                assertThat(stringRedisTemplate.keys("bytechef:session:*")).isNotEmpty();
            }));
        }

        private WebApplicationContextRunner createRunner() {
            return new WebApplicationContextRunner()
                .withConfiguration(
                    AutoConfigurations.of(
                        DataRedisAutoConfiguration.class, SessionAutoConfiguration.class,
                        SessionDataRedisAutoConfiguration.class))
                .withInitializer(
                    context -> new SessionStoreEnvironmentPostProcessor().postProcessEnvironment(
                        context.getEnvironment(), new SpringApplication()))
                .withPropertyValues(
                    "bytechef.edition=ee", "bytechef.session.store=redis",
                    "spring.autoconfigure.exclude=" + AUTOCONFIGURE_EXCLUDE,
                    "spring.data.redis.host=" + redis.getHost(),
                    "spring.data.redis.port=" + redis.getMappedPort(6379));
        }
    }

    @Nested
    class JdbcStore {

        @Container
        private static final PostgreSQLContainer<?> postgreSql = new PostgreSQLContainer<>("postgres:16-alpine");

        @Test
        void testSessionWrittenInTenantContextIsReadWithoutTenant() throws Exception {
            String tenantSchema = TenantContext.callWithTenantId("one", () -> TenantContext.getCurrentDatabaseSchema());

            assertThat(tenantSchema).isEqualTo("bytechef_one");

            try (HikariDataSource hikariDataSource = createHikariDataSource()) {
                prepareSchema(hikariDataSource);

                new WebApplicationContextRunner()
                    .withBean(DataSource.class, () -> new MultiTenantDataSource(hikariDataSource))
                    .withConfiguration(
                        AutoConfigurations.of(
                            DataSourceTransactionManagerAutoConfiguration.class, SessionAutoConfiguration.class,
                            JdbcSessionAutoConfiguration.class))
                    .withInitializer(
                        context -> new SessionStoreEnvironmentPostProcessor().postProcessEnvironment(
                            context.getEnvironment(), new SpringApplication()))
                    .withPropertyValues(
                        "bytechef.edition=ee", "bytechef.session.store=jdbc",
                        "spring.autoconfigure.exclude=" + AUTOCONFIGURE_EXCLUDE)
                    .run(context -> {
                        SessionRepository<Session> sessionRepository = getSessionRepository(context);

                        Session session = sessionRepository.createSession();

                        session.setAttribute("user", "admin@localhost.com");

                        TenantContext.runWithTenantId("one", () -> sessionRepository.save(session));

                        Session foundSession = sessionRepository.findById(session.getId());

                        assertThat(foundSession).isNotNull();
                        assertThat(foundSession.<String>getAttribute("user")).isEqualTo("admin@localhost.com");
                        assertThat(count(hikariDataSource, COUNT_PUBLIC_SESSIONS)).isEqualTo(1);
                        assertThat(count(hikariDataSource, COUNT_TENANT_SESSIONS)).isZero();
                    });
            }
        }

        private static HikariDataSource createHikariDataSource() {
            HikariConfig hikariConfig = new HikariConfig();

            hikariConfig.setJdbcUrl(postgreSql.getJdbcUrl());
            hikariConfig.setUsername(postgreSql.getUsername());
            hikariConfig.setPassword(postgreSql.getPassword());

            return new HikariDataSource(hikariConfig);
        }

        private static void prepareSchema(DataSource dataSource) throws Exception {
            SpringLiquibase springLiquibase = new SpringLiquibase();

            springLiquibase.setChangeLog(
                "classpath:config/liquibase/changelog/session/20261025000000_spring_session_init.xml");
            springLiquibase.setDataSource(dataSource);
            springLiquibase.setResourceLoader(new DefaultResourceLoader());

            springLiquibase.afterPropertiesSet();

            try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
                statement.execute(CREATE_TENANT_SCHEMA);
                statement.execute(CREATE_TENANT_SESSION_TABLE);
            }
        }

        private static int count(DataSource dataSource, String countQuery) throws SQLException {
            try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(countQuery)) {

                if (!resultSet.next()) {
                    throw new IllegalStateException("Count query returned no row");
                }

                return resultSet.getInt(1);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static SessionRepository<Session> getSessionRepository(AssertableWebApplicationContext context) {
        return context.getBean(SessionRepository.class);
    }
}
