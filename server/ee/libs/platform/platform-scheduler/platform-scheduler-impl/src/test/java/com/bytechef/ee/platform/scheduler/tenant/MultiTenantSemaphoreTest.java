/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.tenant.TenantContext;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.impl.jdbcjobstore.LockException;
import org.quartz.impl.jdbcjobstore.Semaphore;

/**
 * @version ee
 */
class MultiTenantSemaphoreTest {

    private static final String LOCK_NAME = "TRIGGER_ACCESS";
    private static final String PUBLIC_SCHEMA = "public";
    private static final String TENANT_SEARCH_PATH = "bytechef_000002, public";

    private final Semaphore semaphore = mock(Semaphore.class);
    private final MultiTenantSemaphore multiTenantSemaphore = new MultiTenantSemaphore(semaphore);
    private SearchPathConnection searchPathConnection;
    private Connection connection;

    @BeforeEach
    void beforeEach() throws SQLException {
        searchPathConnection = new SearchPathConnection(TENANT_SEARCH_PATH);

        connection = searchPathConnection.getConnection();

        when(semaphore.requiresConnection()).thenReturn(true);
    }

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testObtainLockRunsOnPublicSchemaAndRestoresConnectionSearchPath() throws Exception {
        List<String> searchPathsDuringCall = new ArrayList<>();

        doAnswer(invocation -> searchPathsDuringCall.add(searchPathConnection.getSearchPath()))
            .when(semaphore)
            .obtainLock(connection, LOCK_NAME);

        TenantContext.setCurrentTenantId("000002");

        assertThat(multiTenantSemaphore.obtainLock(connection, LOCK_NAME)).isTrue();

        assertThat(searchPathsDuringCall).containsExactly(PUBLIC_SCHEMA);
        assertThat(searchPathConnection.getSearchPath())
            .as("The public fallback of the tenant connection must survive the lock acquisition")
            .isEqualTo(TENANT_SEARCH_PATH);
        assertThat(searchPathConnection.getExecutedStatementCount()).isEqualTo(2);

        searchPathConnection.verifyEveryStatementAndResultSetClosed();
    }

    @Test
    void testObtainLockRestoresConnectionSearchPathWhenTenantContextDiffers() throws Exception {
        when(semaphore.obtainLock(connection, LOCK_NAME)).thenReturn(true);

        TenantContext.setCurrentTenantId("000005");

        multiTenantSemaphore.obtainLock(connection, LOCK_NAME);

        assertThat(searchPathConnection.getSearchPath()).isEqualTo(TENANT_SEARCH_PATH);
    }

    @Test
    void testObtainLockDeclaresLockExceptionOfSemaphoreInterface() throws Exception {
        Method method = MultiTenantSemaphore.class.getMethod("obtainLock", Connection.class, String.class);

        assertThat(method.getExceptionTypes()).containsExactly(LockException.class);
    }

    @Test
    void testObtainLockPropagatesLockExceptionUnwrappedAndRestoresSearchPath() throws Exception {
        LockException lockException = new LockException("Failure obtaining db row lock");

        doThrow(lockException).when(semaphore)
            .obtainLock(connection, LOCK_NAME);

        TenantContext.setCurrentTenantId("000002");

        assertThatThrownBy(() -> multiTenantSemaphore.obtainLock(connection, LOCK_NAME))
            .isSameAs(lockException);

        assertThat(searchPathConnection.getSearchPath()).isEqualTo(TENANT_SEARCH_PATH);
    }

    @Test
    void testObtainLockPropagatesRuntimeExceptionUnwrappedAndRestoresSearchPath() throws Exception {
        IllegalStateException illegalStateException = new IllegalStateException("unexpected");

        doThrow(illegalStateException).when(semaphore)
            .obtainLock(connection, LOCK_NAME);

        assertThatThrownBy(() -> multiTenantSemaphore.obtainLock(connection, LOCK_NAME))
            .isSameAs(illegalStateException);

        assertThat(searchPathConnection.getSearchPath()).isEqualTo(TENANT_SEARCH_PATH);
    }

    @Test
    void testFailureToSwitchSearchPathIsThrownAsLockExceptionWithoutRequestingLock() throws Exception {
        SQLException sqlException = new SQLException("connection closed");

        searchPathConnection.failSearchPathChange(1, sqlException);

        assertThatThrownBy(() -> multiTenantSemaphore.obtainLock(connection, LOCK_NAME))
            .isInstanceOf(LockException.class)
            .hasCause(sqlException);

        verify(semaphore, never()).obtainLock(connection, LOCK_NAME);

        assertThat(searchPathConnection.getSearchPath()).isEqualTo(TENANT_SEARCH_PATH);
    }

    @Test
    void testFailureToRestoreSearchPathIsSuppressedOnLockException() throws Exception {
        LockException lockException = new LockException("Failure obtaining db row lock");
        SQLException restoreException = new SQLException("current transaction is aborted");

        searchPathConnection.failSearchPathChange(2, restoreException);

        doThrow(lockException).when(semaphore)
            .obtainLock(connection, LOCK_NAME);

        assertThatThrownBy(() -> multiTenantSemaphore.obtainLock(connection, LOCK_NAME))
            .isSameAs(lockException)
            .satisfies(throwable -> assertThat(throwable.getSuppressed()).containsExactly(restoreException));

        verify(semaphore, never()).releaseLock(LOCK_NAME);

        searchPathConnection.verifyEveryStatementAndResultSetClosed();
    }

    @Test
    void testFailureToRestoreSearchPathAfterLockWasObtainedReleasesLockAndThrowsLockException() throws Exception {
        SQLException restoreException = new SQLException("connection reset");

        searchPathConnection.failSearchPathChange(2, restoreException);

        when(semaphore.obtainLock(connection, LOCK_NAME)).thenReturn(true);

        assertThatThrownBy(() -> multiTenantSemaphore.obtainLock(connection, LOCK_NAME))
            .isInstanceOf(LockException.class)
            .hasCause(restoreException);

        // JobStoreSupport releases a lock only when obtainLock returned true, so the semaphore has to forget the lock
        // here, or the thread would later believe it still owned it without holding the database row lock
        verify(semaphore).releaseLock(LOCK_NAME);
    }

    @Test
    void testFailureToReleaseLockAfterFailedRestoreIsSuppressed() throws Exception {
        SQLException restoreException = new SQLException("connection reset");
        LockException releaseException = new LockException("release failed");

        searchPathConnection.failSearchPathChange(2, restoreException);

        when(semaphore.obtainLock(connection, LOCK_NAME)).thenReturn(true);
        doThrow(releaseException).when(semaphore)
            .releaseLock(LOCK_NAME);

        assertThatThrownBy(() -> multiTenantSemaphore.obtainLock(connection, LOCK_NAME))
            .isInstanceOf(LockException.class)
            .hasCause(restoreException)
            .satisfies(throwable -> assertThat(throwable.getSuppressed()).containsExactly(releaseException));
    }

    @Test
    void testLockHandlerThatDoesNotRequireConnectionIsCalledWithoutSwitchingSearchPath() throws Exception {
        Semaphore inMemorySemaphore = mock(Semaphore.class);

        when(inMemorySemaphore.requiresConnection()).thenReturn(false);
        when(inMemorySemaphore.obtainLock(null, LOCK_NAME)).thenReturn(true);

        MultiTenantSemaphore multiTenantInMemorySemaphore = new MultiTenantSemaphore(inMemorySemaphore);

        assertThat(multiTenantInMemorySemaphore.obtainLock(null, LOCK_NAME)).isTrue();

        verify(inMemorySemaphore).obtainLock(null, LOCK_NAME);
    }

    @Test
    void testReleaseLockPropagatesLockExceptionUnwrapped() throws Exception {
        LockException lockException = new LockException("release failed");

        doThrow(lockException).when(semaphore)
            .releaseLock(LOCK_NAME);

        assertThatThrownBy(() -> multiTenantSemaphore.releaseLock(LOCK_NAME))
            .isSameAs(lockException);

        verifyNoInteractions(connection);
    }

    @Test
    void testRequiresConnectionIsForwarded() {
        assertThat(multiTenantSemaphore.requiresConnection()).isTrue();

        when(semaphore.requiresConnection()).thenReturn(false);

        assertThat(multiTenantSemaphore.requiresConnection()).isFalse();
    }
}
