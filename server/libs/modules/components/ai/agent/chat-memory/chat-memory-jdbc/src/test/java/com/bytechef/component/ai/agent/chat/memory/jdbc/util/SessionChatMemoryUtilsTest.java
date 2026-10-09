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

package com.bytechef.component.ai.agent.chat.memory.jdbc.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.ClientCacheSettings;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.DataSourceFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Scheduler;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * @author Ivica Cardic
 */
class SessionChatMemoryUtilsTest {

    private final List<Runnable> deferredCloseTasks = new ArrayList<>();
    private final List<Runnable> scheduledTasks = new ArrayList<>();
    private final AtomicLong tickerNanos = new AtomicLong();

    private final Scheduler manualScheduler = (executor, command, delay, timeUnit) -> {
        scheduledTasks.add(() -> executor.execute(command));

        return new CompletableFuture<>();
    };

    private DriverManagerDataSource dataSource;

    @BeforeEach
    void setUp() {
        dataSource = new DriverManagerDataSource(
            "jdbc:h2:mem:" + System.nanoTime() + ";DB_CLOSE_DELAY=-1", "sa", "");
    }

    @Test
    void testGetDataSourceWithoutDataSourceConnectionNamesTheDataSourceNode() throws Exception {
        ClusterElementDefinitionService clusterElementDefinitionService = mock(ClusterElementDefinitionService.class);

        DataSourceFunction dataSourceFunction =
            (inputParameters, connectionParameters, extensions, componentConnections) -> dataSource;

        when(clusterElementDefinitionService.<DataSourceFunction>getClusterElement(
            eq("postgresql"), eq(1), eq("dataSource"))).thenReturn(dataSourceFunction);

        Parameters extensions = MockParametersFactory.create(
            Map.of(
                "clusterElements",
                Map.of(
                    "dataSource",
                    Map.of(
                        "name", "dataSource_1",
                        "type", "postgresql/v1/dataSource",
                        "parameters", Map.of()))));

        assertThatThrownBy(
            () -> SessionChatMemoryUtils.getDataSource(extensions, Map.of(), clusterElementDefinitionService))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                    "The Data Source dataSource_1 of JDBC Chat Memory has no connection; select a database " +
                        "connection for it.");
    }

    @Test
    void testCreateSessionRepositoryRejectsAnOracleDataSource() throws SQLException {
        DataSource oracleDataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);

        when(oracleDataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("Oracle");

        assertThatThrownBy(() -> SessionChatMemoryUtils.createSessionRepository(oracleDataSource))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("JDBC Chat Memory v2 does not support Oracle; use JDBC Chat Memory v1");
    }

    @Test
    void testGetSessionRepositoryCreatesSchemaFromLibraryScript() {
        SessionChatMemoryUtils.getSessionRepository(dataSource);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        List<String> columnNames = jdbcTemplate.queryForList(
            "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'AI_SESSION_EVENT'", String.class);

        assertThat(columnNames).contains("ID", "SESSION_ID", "SEQ", "ARCHIVED", "SYNTHETIC", "METADATA");
    }

    @Test
    void testInitializeSchemaRerunsWithoutLosingEvents() {
        SessionChatMemoryUtils.initializeSchema(dataSource);

        SessionRepository sessionRepository = SessionChatMemoryUtils.createSessionRepository(dataSource);

        sessionRepository.save(Session.builder()
            .id("conversation-1")
            .userId("user-1")
            .createdAt(Instant.now())
            .build());

        sessionRepository.appendEvent(SessionEvent.builder()
            .sessionId("conversation-1")
            .message(new UserMessage("hello"))
            .build());

        SessionChatMemoryUtils.initializeSchema(dataSource);

        List<SessionEvent> sessionEvents = sessionRepository.findEvents("conversation-1", EventFilter.all());

        assertThat(sessionEvents)
            .extracting(SessionEvent::getMessage)
            .extracting(Message::getText)
            .containsExactly("hello");
    }

    @Test
    void testGetSessionRepositoryReusesOneRepositoryPerDatabase() {
        String url = dataSource.getUrl();

        SessionRepository firstSessionRepository = SessionChatMemoryUtils.getSessionRepository(
            new SingleConnectionDataSource(url, "sa", "", false));
        SessionRepository secondSessionRepository = SessionChatMemoryUtils.getSessionRepository(
            new SingleConnectionDataSource(url, "sa", "", false));
        SessionRepository otherDatabaseSessionRepository = SessionChatMemoryUtils.getSessionRepository(
            new SingleConnectionDataSource(
                "jdbc:h2:mem:" + System.nanoTime() + ";DB_CLOSE_DELAY=-1", "sa", "", false));

        assertThat(secondSessionRepository).isSameAs(firstSessionRepository);
        assertThat(otherDatabaseSessionRepository).isNotSameAs(firstSessionRepository);
    }

    @Test
    void testGetSessionRepositoryUsesASeparateRepositoryForAnotherUsername() {
        String url = dataSource.getUrl();

        SessionRepository adminSessionRepository = SessionChatMemoryUtils.getSessionRepository(
            new SingleConnectionDataSource(url, "sa", "", false));

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("CREATE USER memory_user PASSWORD 'secret' ADMIN");

        SessionRepository otherUserSessionRepository = SessionChatMemoryUtils.getSessionRepository(
            new SingleConnectionDataSource(url, "memory_user", "secret", false));

        assertThat(otherUserSessionRepository).isNotSameAs(adminSessionRepository);
    }

    @Test
    void testGetSessionRepositoryUsesASeparateRepositoryForAnotherPassword() {
        String url = dataSource.getUrl();

        SessionRepository originalPasswordSessionRepository = SessionChatMemoryUtils.getSessionRepository(
            new SingleConnectionDataSource(url, "sa", "", false));

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("ALTER USER sa SET PASSWORD 'rotated'");

        SessionRepository rotatedPasswordSessionRepository = SessionChatMemoryUtils.getSessionRepository(
            new SingleConnectionDataSource(url, "sa", "rotated", false));

        assertThat(rotatedPasswordSessionRepository).isNotSameAs(originalPasswordSessionRepository);
    }

    @Test
    void testGetSessionRepositoryClosesTheSuppliedSingleConnection() throws SQLException {
        SingleConnectionDataSource singleConnectionDataSource = new SingleConnectionDataSource(
            dataSource.getUrl(), "sa", "", false);

        try (Connection connection = singleConnectionDataSource.getConnection()) {
            SessionChatMemoryUtils.getSessionRepository(singleConnectionDataSource);

            assertThat(connection.isClosed()).isTrue();
        }
    }

    @Test
    void testGetSessionRepositoryStoresAndReadsEvents() {
        SessionRepository sessionRepository = SessionChatMemoryUtils.getSessionRepository(dataSource);

        sessionRepository.save(Session.builder()
            .id("conversation-1")
            .userId("user-1")
            .createdAt(Instant.now())
            .build());

        sessionRepository.appendEvent(SessionEvent.builder()
            .sessionId("conversation-1")
            .message(new UserMessage("hello"))
            .build());

        List<SessionEvent> sessionEvents = sessionRepository.findEvents("conversation-1", EventFilter.all());

        assertThat(sessionEvents)
            .extracting(SessionEvent::getMessage)
            .extracting(Message::getText)
            .containsExactly("hello");
    }

    @Test
    void testRepositoryObtainedBeforeASizeEvictionKeepsWorkingUntilTheGracePeriodEnds() {
        Cache<SessionChatMemoryUtils.DataSourceKey, SessionChatMemoryUtils.PooledSessionRepository> sessionRepositories =
            SessionChatMemoryUtils.createSessionRepositoryCache(clientCacheSettings());

        SessionRepository firstSessionRepository = SessionChatMemoryUtils.getSessionRepository(
            createDataSource(), sessionRepositories);
        SessionRepository secondSessionRepository = SessionChatMemoryUtils.getSessionRepository(
            createDataSource(), sessionRepositories);

        sessionRepositories.cleanUp();

        assertThat(sessionRepositories.asMap()).hasSize(1);

        saveAndReadEvent(firstSessionRepository);
        saveAndReadEvent(secondSessionRepository);

        boolean firstSessionRepositoryKept = containsSessionRepository(sessionRepositories, firstSessionRepository);

        SessionRepository evictedSessionRepository =
            firstSessionRepositoryKept ? secondSessionRepository : firstSessionRepository;
        SessionRepository keptSessionRepository =
            firstSessionRepositoryKept ? firstSessionRepository : secondSessionRepository;

        runTasks(deferredCloseTasks);

        assertThatThrownBy(() -> saveAndReadEvent(evictedSessionRepository))
            .hasRootCauseMessage("HikariDataSource HikariDataSource (session-chat-memory) has been closed.");

        saveAndReadEvent(keptSessionRepository);
    }

    @Test
    void testIdleDataSourcePoolIsClosedWhenTheSchedulerExpiresIt() {
        Cache<SessionChatMemoryUtils.DataSourceKey, SessionChatMemoryUtils.PooledSessionRepository> sessionRepositories =
            SessionChatMemoryUtils.createSessionRepositoryCache(clientCacheSettings());

        SessionRepository sessionRepository = SessionChatMemoryUtils.getSessionRepository(
            createDataSource(), sessionRepositories);

        Duration expiredIdleTime = ClientCacheSettings.IDLE_TIMEOUT.plusSeconds(1);

        tickerNanos.addAndGet(expiredIdleTime.toNanos());

        runTasks(scheduledTasks);

        assertThat(sessionRepositories.asMap()).isEmpty();
        assertThat(deferredCloseTasks).isEmpty();

        assertThatThrownBy(() -> saveAndReadEvent(sessionRepository))
            .hasRootCauseMessage("HikariDataSource HikariDataSource (session-chat-memory) has been closed.");
    }

    @Test
    void testDataSourceKeyDescriptionOmitsCredentials() {
        SessionChatMemoryUtils.DataSourceKey dataSourceKey = new SessionChatMemoryUtils.DataSourceKey(
            "jdbc:mysql://admin:url-secret@localhost:3306/memory?password=query-secret", "admin", "secret");

        assertThat(dataSourceKey.toString())
            .isEqualTo("DataSourceKey{url=jdbc:mysql:localhost:3306/memory, username=admin}");
    }

    private ClientCacheSettings clientCacheSettings() {
        return new ClientCacheSettings(1, tickerNanos::get, manualScheduler, Runnable::run, deferredCloseTasks::add);
    }

    private static boolean containsSessionRepository(
        Cache<SessionChatMemoryUtils.DataSourceKey, SessionChatMemoryUtils.PooledSessionRepository> sessionRepositories,
        SessionRepository sessionRepository) {

        Map<SessionChatMemoryUtils.DataSourceKey, SessionChatMemoryUtils.PooledSessionRepository> cachedRepositories =
            sessionRepositories.asMap();

        return cachedRepositories.values()
            .stream()
            .anyMatch(pooledSessionRepository -> pooledSessionRepository.sessionRepository() == sessionRepository);
    }

    private static DriverManagerDataSource createDataSource() {
        return new DriverManagerDataSource("jdbc:h2:mem:" + System.nanoTime() + ";DB_CLOSE_DELAY=-1", "sa", "");
    }

    private static void runTasks(List<Runnable> tasks) {
        List<Runnable> pendingTasks = new ArrayList<>(tasks);

        tasks.clear();

        pendingTasks.forEach(Runnable::run);
    }

    private static void saveAndReadEvent(SessionRepository sessionRepository) {
        String sessionId = "conversation-" + System.nanoTime();

        sessionRepository.save(Session.builder()
            .id(sessionId)
            .userId("user-1")
            .createdAt(Instant.now())
            .build());

        sessionRepository.appendEvent(SessionEvent.builder()
            .sessionId(sessionId)
            .message(new UserMessage("hello"))
            .build());

        assertThat(sessionRepository.findEvents(sessionId, EventFilter.all())).hasSize(1);
    }
}
