/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.tenant;

import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Function;
import org.quartz.impl.jdbcjobstore.LockException;
import org.quartz.impl.jdbcjobstore.Semaphore;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("SQL_INJECTION_JDBC")
public class MultiTenantSemaphore implements Semaphore {

    private final Semaphore semaphore;

    public MultiTenantSemaphore(Semaphore semaphore) {
        this.semaphore = semaphore;
    }

    @Override
    public boolean obtainLock(Connection conn, String lockName) {
        return execute(conn, (connection) -> {
            try {
                return semaphore.obtainLock(connection, lockName);
            } catch (LockException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void releaseLock(String lockName) throws LockException {
        semaphore.releaseLock(lockName);
    }

    @Override
    public boolean requiresConnection() {
        return semaphore.requiresConnection();
    }

    private <T> T execute(Connection connection, Function<Connection, T> function) {
        try {
            setSearchPath(connection, "public");

            return function.apply(connection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                setSearchPath(connection, TenantContext.getCurrentDatabaseSchema());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void setSearchPath(Connection connection, String databaseSchemaName) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + databaseSchemaName);
        }
    }
}
