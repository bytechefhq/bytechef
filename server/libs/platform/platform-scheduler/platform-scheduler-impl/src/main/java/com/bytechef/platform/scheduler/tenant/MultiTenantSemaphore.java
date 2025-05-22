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

package com.bytechef.platform.scheduler.tenant;

import com.bytechef.tenant.TenantContext;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Function;
import org.quartz.impl.jdbcjobstore.LockException;
import org.quartz.impl.jdbcjobstore.Semaphore;

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
