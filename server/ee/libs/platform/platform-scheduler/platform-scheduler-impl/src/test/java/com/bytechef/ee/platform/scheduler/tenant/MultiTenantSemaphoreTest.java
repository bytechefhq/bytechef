/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.quartz.impl.jdbcjobstore.LockException;
import org.quartz.impl.jdbcjobstore.Semaphore;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings(
    value = {
        "ODR_OPEN_DATABASE_RESOURCE", "OBL_UNSATISFIED_OBLIGATION"
    },
    justification = "connection and statement are Mockito mocks")
class MultiTenantSemaphoreTest {

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testObtainLockQuotesTheSchemaInTheSearchPath() throws SQLException, LockException {
        Connection connection = mock(Connection.class);
        Semaphore semaphore = mock(Semaphore.class);
        Statement statement = mock(Statement.class);

        when(connection.createStatement()).thenReturn(statement);
        when(semaphore.obtainLock(connection, "TRIGGER_ACCESS")).thenReturn(true);

        TenantContext.setCurrentTenantId("000004");

        MultiTenantSemaphore multiTenantSemaphore = new MultiTenantSemaphore(semaphore);

        assertThat(multiTenantSemaphore.obtainLock(connection, "TRIGGER_ACCESS")).isTrue();

        InOrder inOrder = inOrder(statement, semaphore);

        inOrder.verify(statement)
            .execute("SET search_path TO \"public\"");
        inOrder.verify(semaphore)
            .obtainLock(connection, "TRIGGER_ACCESS");
        inOrder.verify(statement)
            .execute("SET search_path TO \"bytechef_000004\"");
    }
}
