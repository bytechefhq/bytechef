/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.tenant;

import static com.bytechef.ee.platform.scheduler.tenant.MultiTenantQuartzTestSupport.TENANT_FALLBACK_SEARCH_PATH;
import static com.bytechef.ee.platform.scheduler.tenant.MultiTenantQuartzTestSupport.TENANT_ID;
import static com.bytechef.ee.platform.scheduler.tenant.MultiTenantQuartzTestSupport.TENANT_SCHEMA;
import static com.bytechef.ee.platform.scheduler.tenant.MultiTenantQuartzTestSupport.openConnection;
import static com.bytechef.ee.platform.scheduler.tenant.MultiTenantQuartzTestSupport.showSearchPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.tenant.TenantContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.quartz.impl.jdbcjobstore.LockException;
import org.quartz.impl.jdbcjobstore.SimpleSemaphore;
import org.quartz.impl.jdbcjobstore.StdRowLockSemaphore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @version ee
 */
@Testcontainers
class MultiTenantSemaphoreIntTest {

    private static final String LOCK_NAME = "TRIGGER_ACCESS";
    private static final String RESTRICTED_ROLE = "quartz_restricted";
    private static final String TABLE_PREFIX = "QRTZ_";

    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16-alpine");

    private static DataSource dataSource;
    private static JdbcTemplate jdbcTemplate;

    private final List<Connection> connections = new ArrayList<>();
    private final List<StdRowLockSemaphore> stdRowLockSemaphores = new ArrayList<>();

    @BeforeAll
    static void beforeAll() {
        dataSource = new DriverManagerDataSource(
            postgreSQLContainer.getJdbcUrl(), postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword());

        jdbcTemplate = new JdbcTemplate(dataSource);

        MultiTenantQuartzTestSupport.initializeDatabase(dataSource);

        jdbcTemplate.execute("CREATE ROLE " + RESTRICTED_ROLE);
        jdbcTemplate.execute("REVOKE EXECUTE ON FUNCTION pg_catalog.set_config(text, text, boolean) FROM PUBLIC");
    }

    @AfterEach
    void afterEach() throws SQLException {
        TenantContext.resetCurrentTenantId();

        for (StdRowLockSemaphore stdRowLockSemaphore : stdRowLockSemaphores) {
            stdRowLockSemaphore.releaseLock(LOCK_NAME);
        }

        for (Connection connection : connections) {
            connection.close();
        }
    }

    @Test
    void testObtainLockHoldsRowLockInPublicFromTenantOnlyConnection() throws Exception {
        jdbcTemplate.update(
            "INSERT INTO public.qrtz_locks (sched_name, lock_name) VALUES (?, ?)", "tenantOnly", LOCK_NAME);

        StdRowLockSemaphore stdRowLockSemaphore = createStdRowLockSemaphore(TABLE_PREFIX, "tenantOnly", null);

        MultiTenantSemaphore multiTenantSemaphore = new MultiTenantSemaphore(stdRowLockSemaphore);

        Connection connection = openTrackedConnection(TENANT_SCHEMA);

        connection.setAutoCommit(false);

        TenantContext.setCurrentTenantId(TENANT_ID);

        assertThat(multiTenantSemaphore.obtainLock(connection, LOCK_NAME)).isTrue();

        assertThat(stdRowLockSemaphore.isLockOwner(LOCK_NAME)).isTrue();
        assertThat(showSearchPath(connection)).isEqualTo(TENANT_SCHEMA);

        Connection otherConnection = openTrackedConnection("public");

        otherConnection.setAutoCommit(false);

        assertThatThrownBy(() -> selectLockRowForUpdateNoWait(otherConnection, "tenantOnly"))
            .isInstanceOf(SQLException.class)
            .hasMessageContaining("could not obtain lock");

        otherConnection.rollback();
        connection.commit();
        multiTenantSemaphore.releaseLock(LOCK_NAME);

        assertThat(stdRowLockSemaphore.isLockOwner(LOCK_NAME)).isFalse();
        assertThat(selectLockRowForUpdateNoWait(otherConnection, "tenantOnly")).isTrue();
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
            "bytechef_000002, public | 000002",
            "bytechef_000002, public | 000005",
            "bytechef_vectorstore_000002, \"$user\", public | 000002"
        })
    void testObtainLockRestoresSearchPathOfConnection(String searchPath, String tenantId) throws Exception {
        MultiTenantSemaphore multiTenantSemaphore = new MultiTenantSemaphore(
            createStdRowLockSemaphore(TABLE_PREFIX, "restoreSearchPath", null));
        Connection connection = openTrackedConnection(searchPath);

        TenantContext.setCurrentTenantId(tenantId);

        assertThat(multiTenantSemaphore.obtainLock(connection, LOCK_NAME)).isTrue();

        assertThat(showSearchPath(connection)).isEqualTo(searchPath);
    }

    @Test
    void testLockExceptionOfSemaphoreIsPropagatedAndSearchPathRestored() throws Exception {
        StdRowLockSemaphore stdRowLockSemaphore = createStdRowLockSemaphore("MISSING_", "lockException", null);

        MultiTenantSemaphore multiTenantSemaphore = new MultiTenantSemaphore(stdRowLockSemaphore);

        Connection connection = openTrackedConnection(TENANT_FALLBACK_SEARCH_PATH);

        TenantContext.setCurrentTenantId(TENANT_ID);

        assertThatThrownBy(() -> multiTenantSemaphore.obtainLock(connection, LOCK_NAME))
            .isExactlyInstanceOf(LockException.class)
            .hasMessageStartingWith("Failure obtaining db row lock")
            .hasMessageContaining("missing_locks")
            .satisfies(throwable -> assertThat(throwable.getSuppressed()).isEmpty());

        assertThat(stdRowLockSemaphore.isLockOwner(LOCK_NAME)).isFalse();
        assertThat(showSearchPath(connection)).isEqualTo(TENANT_FALLBACK_SEARCH_PATH);
    }

    @Test
    void testFailureToRestoreSearchPathIsSuppressedOnLockException() throws Exception {
        StdRowLockSemaphore stdRowLockSemaphore = createStdRowLockSemaphore("MISSING_", "restoreFailure", null);

        MultiTenantSemaphore multiTenantSemaphore = new MultiTenantSemaphore(stdRowLockSemaphore);

        Connection connection = openTrackedConnection(TENANT_FALLBACK_SEARCH_PATH);

        connection.setAutoCommit(false);

        TenantContext.setCurrentTenantId(TENANT_ID);

        assertThatThrownBy(() -> multiTenantSemaphore.obtainLock(connection, LOCK_NAME))
            .isExactlyInstanceOf(LockException.class)
            .hasMessageStartingWith("Failure obtaining db row lock")
            .satisfies(throwable -> assertThat(throwable.getSuppressed())
                .singleElement()
                .satisfies(suppressed -> assertThat(suppressed)
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("current transaction is aborted")));

        assertThat(stdRowLockSemaphore.isLockOwner(LOCK_NAME)).isFalse();

        connection.rollback();
    }

    @Test
    void testFailureToSwitchSearchPathIsThrownAsLockExceptionWithoutRequestingLock() throws Exception {
        StdRowLockSemaphore stdRowLockSemaphore = createStdRowLockSemaphore(TABLE_PREFIX, "switchFailure", null);

        MultiTenantSemaphore multiTenantSemaphore = new MultiTenantSemaphore(stdRowLockSemaphore);

        Connection connection = openTrackedConnection(TENANT_FALLBACK_SEARCH_PATH);

        connection.setAutoCommit(false);

        abortTransaction(connection);

        TenantContext.setCurrentTenantId(TENANT_ID);

        assertThatThrownBy(() -> multiTenantSemaphore.obtainLock(connection, LOCK_NAME))
            .isExactlyInstanceOf(LockException.class)
            .hasMessageStartingWith("Failure switching search_path for lock " + LOCK_NAME)
            .cause()
            .isInstanceOf(SQLException.class)
            .hasMessageContaining("current transaction is aborted");

        assertThat(stdRowLockSemaphore.isLockOwner(LOCK_NAME)).isFalse();

        connection.rollback();

        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public.qrtz_locks WHERE sched_name = 'switchFailure'", Integer.class))
                    .isZero();
    }

    @Test
    void testFailureToRestoreSearchPathAfterLockWasObtainedReleasesLockAndThrowsLockException() throws Exception {
        jdbcTemplate.update(
            "INSERT INTO public.qrtz_locks (sched_name, lock_name) VALUES (?, ?)", "restoreAfterLock", LOCK_NAME);

        StdRowLockSemaphore stdRowLockSemaphore = createStdRowLockSemaphore(
            TABLE_PREFIX, "restoreAfterLock",
            "SELECT *, set_config(''role'', ''" + RESTRICTED_ROLE + "'', false) FROM {0}LOCKS WHERE SCHED_NAME = {1} " +
                "AND LOCK_NAME = ? FOR UPDATE");

        MultiTenantSemaphore multiTenantSemaphore = new MultiTenantSemaphore(stdRowLockSemaphore);

        Connection connection = openTrackedConnection(TENANT_FALLBACK_SEARCH_PATH);

        TenantContext.setCurrentTenantId(TENANT_ID);

        assertThatThrownBy(() -> multiTenantSemaphore.obtainLock(connection, LOCK_NAME))
            .isExactlyInstanceOf(LockException.class)
            .hasMessageStartingWith("Failure switching search_path for lock " + LOCK_NAME)
            .cause()
            .isInstanceOf(SQLException.class)
            .hasMessageContaining("permission denied for function set_config");

        assertThat(stdRowLockSemaphore.isLockOwner(LOCK_NAME))
            .as("JobStoreSupport releases only a lock obtainLock reported as obtained, so the semaphore must forget it")
            .isFalse();
    }

    @Test
    void testSemaphoreThatDoesNotRequireConnectionIsCalledWithoutConnection() throws Exception {
        MultiTenantSemaphore multiTenantSemaphore = new MultiTenantSemaphore(new SimpleSemaphore());

        assertThat(multiTenantSemaphore.requiresConnection()).isFalse();
        assertThat(multiTenantSemaphore.obtainLock(null, LOCK_NAME)).isTrue();

        multiTenantSemaphore.releaseLock(LOCK_NAME);

        MultiTenantSemaphore multiTenantRowLockSemaphore = new MultiTenantSemaphore(
            createStdRowLockSemaphore(TABLE_PREFIX, "requiresConnection", null));

        assertThat(multiTenantRowLockSemaphore.requiresConnection()).isTrue();
    }

    private Connection openTrackedConnection(String searchPath) throws SQLException {
        Connection connection = openConnection(dataSource, searchPath);

        connections.add(connection);

        return connection;
    }

    private StdRowLockSemaphore createStdRowLockSemaphore(
        String tablePrefix, String schedulerName, String selectWithLockSql) {

        StdRowLockSemaphore stdRowLockSemaphore = new StdRowLockSemaphore(
            tablePrefix, schedulerName, selectWithLockSql);

        stdRowLockSemaphore.setMaxRetry(1);
        stdRowLockSemaphore.setRetryPeriod(0);

        stdRowLockSemaphores.add(stdRowLockSemaphore);

        return stdRowLockSemaphore;
    }

    private static void abortTransaction(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SELECT 1 / 0");
        } catch (SQLException sqlException) {
            assertThat(sqlException.getMessage()).contains("division by zero");
        }
    }

    private static boolean selectLockRowForUpdateNoWait(Connection connection, String schedulerName)
        throws SQLException {

        try (PreparedStatement preparedStatement = connection.prepareStatement(
            "SELECT * FROM public.qrtz_locks WHERE sched_name = ? AND lock_name = ? FOR UPDATE NOWAIT")) {

            preparedStatement.setString(1, schedulerName);
            preparedStatement.setString(2, LOCK_NAME);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}
