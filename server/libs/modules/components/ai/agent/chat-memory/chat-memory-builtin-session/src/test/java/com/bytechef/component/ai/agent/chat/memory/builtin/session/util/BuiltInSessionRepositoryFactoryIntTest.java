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

package com.bytechef.component.ai.agent.chat.memory.builtin.session.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.sql.BaseDataSource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.env.MockEnvironment;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author Ivica Cardic
 */
@Testcontainers
class BuiltInSessionRepositoryFactoryIntTest {

    private static final String CHANGELOG =
        "classpath:config/liquibase/changelog/platform/ai/chat_memory/20261002000001_spring_ai_session_schema.xml";
    private static final String CONVERSATION_ID = "conversation-1";
    private static final String FIRST_TENANT_ID = "000001";
    private static final String FIRST_TENANT_SCHEMA = "bytechef_000001";
    private static final String SECOND_TENANT_ID = "000002";
    private static final String SECOND_TENANT_SCHEMA = "bytechef_000002";

    @Container
    private static final PostgreSQLContainer<?> POSTGRE_SQL_CONTAINER = new PostgreSQLContainer<>(
        "postgres:16-alpine");

    private static DriverManagerDataSource driverManagerDataSource;

    @BeforeAll
    static void beforeAll() throws LiquibaseException {
        driverManagerDataSource = new DriverManagerDataSource(
            POSTGRE_SQL_CONTAINER.getJdbcUrl(), POSTGRE_SQL_CONTAINER.getUsername(),
            POSTGRE_SQL_CONTAINER.getPassword());

        JdbcTemplate jdbcTemplate = new JdbcTemplate(driverManagerDataSource);

        jdbcTemplate.execute("CREATE SCHEMA " + FIRST_TENANT_SCHEMA);
        jdbcTemplate.execute("CREATE SCHEMA " + SECOND_TENANT_SCHEMA);

        runChangelog(FIRST_TENANT_SCHEMA);
        runChangelog(SECOND_TENANT_SCHEMA);
    }

    @Test
    void testTenantSchemasFollowTheTenantContextNaming() {
        assertThat(getDatabaseSchema(FIRST_TENANT_ID)).isEqualTo(FIRST_TENANT_SCHEMA);
        assertThat(getDatabaseSchema(SECOND_TENANT_ID)).isEqualTo(SECOND_TENANT_SCHEMA);
    }

    @Test
    void testJdbcProviderKeepsEachTenantsSessionsInItsOwnSchema() {
        SessionRepository sessionRepository = createJdbcSessionRepository();

        TenantContext.runWithTenantId(
            FIRST_TENANT_ID, () -> saveSessionWithMessage(sessionRepository, "first tenant message"));
        TenantContext.runWithTenantId(
            SECOND_TENANT_ID, () -> saveSessionWithMessage(sessionRepository, "second tenant message"));

        assertThat(findMessageTexts(sessionRepository, FIRST_TENANT_ID)).containsExactly("first tenant message");
        assertThat(findMessageTexts(sessionRepository, SECOND_TENANT_ID)).containsExactly("second tenant message");
        assertThat(countSessions(FIRST_TENANT_ID)).isEqualTo(1);
        assertThat(countSessions(SECOND_TENANT_ID)).isEqualTo(1);
    }

    @Test
    void testJdbcProviderDoesNotCreateTablesInThePublicSchema() {
        createJdbcSessionRepository();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(driverManagerDataSource);

        Integer publicTableCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' " +
                "AND table_name IN ('ai_session', 'ai_session_event')",
            Integer.class);

        assertThat(publicTableCount).isZero();
    }

    private static int countSessions(String tenantId) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(createTenantDataSource());

        Integer sessionCount = TenantContext.callWithTenantId(
            tenantId, () -> jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_session", Integer.class));

        return sessionCount == null ? 0 : sessionCount;
    }

    private static SessionRepository createJdbcSessionRepository() {
        BuiltInSessionRepositoryFactory.BuiltInSessionStore builtInSessionStore =
            BuiltInSessionRepositoryFactory.create(
                new MockEnvironment().withProperty("bytechef.ai.memory.provider", "jdbc"),
                new JdbcTemplate(createTenantDataSource()));

        return builtInSessionStore.sessionRepository();
    }

    private static List<String> findMessageTexts(SessionRepository sessionRepository, String tenantId) {
        return findMessageTexts(sessionRepository, tenantId, CONVERSATION_ID);
    }

    private static List<String> findMessageTexts(
        SessionRepository sessionRepository, String tenantId, String sessionId) {

        List<String> messageTexts = new ArrayList<>();

        TenantContext.runWithTenantId(tenantId, () -> {
            for (SessionEvent sessionEvent : sessionRepository.findEvents(sessionId, EventFilter.all())) {
                Message message = sessionEvent.getMessage();

                messageTexts.add(message.getText());
            }
        });

        return messageTexts;
    }

    private static String getDatabaseSchema(String tenantId) {
        List<String> databaseSchemas = new ArrayList<>();

        TenantContext.runWithTenantId(tenantId, () -> databaseSchemas.add(TenantContext.getCurrentDatabaseSchema()));

        return databaseSchemas.getFirst();
    }

    private static void saveSessionWithMessage(SessionRepository sessionRepository, String text) {
        saveSessionWithMessage(sessionRepository, CONVERSATION_ID, text);
    }

    private static void saveSessionWithMessage(SessionRepository sessionRepository, String sessionId, String text) {
        sessionRepository.save(Session.builder()
            .id(sessionId)
            .userId("bytechef")
            .createdAt(Instant.now())
            .build());

        sessionRepository.appendEvent(SessionEvent.builder()
            .sessionId(sessionId)
            .message(new UserMessage(text))
            .build());
    }

    private static DataSource createTenantDataSource() {
        return new BaseDataSource(driverManagerDataSource) {};
    }

    private static void runChangelog(String databaseSchema) throws LiquibaseException {
        SpringLiquibase springLiquibase = new SpringLiquibase();

        springLiquibase.setChangeLog(CHANGELOG);
        springLiquibase.setDataSource(driverManagerDataSource);
        springLiquibase.setDefaultSchema(databaseSchema);
        springLiquibase.setResourceLoader(new DefaultResourceLoader());

        springLiquibase.afterPropertiesSet();
    }
}
