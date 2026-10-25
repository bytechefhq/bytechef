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

package com.bytechef.platform.coordinator.lock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Keeps at most one coordinator running against a database in the Community Edition by holding a PostgreSQL session
 * level advisory lock on a dedicated connection.
 *
 * @author Ivica Cardic
 */
public class SingleCoordinatorLock implements InitializingBean, DisposableBean {

    static final long LOCK_KEY = 0x4279746543686566L;
    static final String ANOTHER_COORDINATOR_MESSAGE =
        "Another coordinator is already running. Running more than one coordinator requires ByteChef Enterprise " +
            "Edition (bytechef.edition=ee, BYTECHEF_EDITION=ee).";

    private static final Logger log = LoggerFactory.getLogger(SingleCoordinatorLock.class);

    private final boolean skipped;
    private final LockConnectionFactory lockConnectionFactory;
    private final Runnable shutdownAction;

    private Connection connection;
    private boolean held;
    private boolean shutdownTriggered;

    public SingleCoordinatorLock(
        String jdbcUrl, LockConnectionFactory lockConnectionFactory, Runnable shutdownAction) {

        this.skipped = jdbcUrl != null && jdbcUrl.startsWith("jdbc:h2:");
        this.lockConnectionFactory = lockConnectionFactory;
        this.shutdownAction = shutdownAction;
    }

    @Override
    public synchronized void afterPropertiesSet() throws SQLException {
        if (skipped) {
            log.debug("Single coordinator lock is skipped for an H2 database");

            return;
        }

        Connection openedConnection = lockConnectionFactory.getConnection();

        boolean acquired;

        try {
            acquired = tryAcquire(openedConnection);
        } catch (SQLException sqlException) {
            closeQuietly(openedConnection);

            throw sqlException;
        }

        if (!acquired) {
            closeQuietly(openedConnection);

            throw new IllegalStateException(ANOTHER_COORDINATOR_MESSAGE);
        }

        connection = openedConnection;

        held = true;
    }

    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
    public synchronized void verifyLock() {
        if (skipped || connection == null || shutdownTriggered) {
            return;
        }

        try {
            if (connection.isValid(5)) {
                return;
            }
        } catch (SQLException sqlException) {
            log.warn("Single coordinator lock connection check failed", sqlException);
        }

        held = false;

        closeQuietly(connection);

        Connection reopenedConnection = null;

        try {
            reopenedConnection = lockConnectionFactory.getConnection();

            connection = reopenedConnection;

            if (tryAcquire(reopenedConnection)) {
                held = true;

                log.warn("Single coordinator lock was re-acquired after the connection was lost");
            } else {
                log.error(
                    "Another coordinator took the lock while this node's connection was down; shutting down");

                shutdownTriggered = true;

                closeQuietly(reopenedConnection);

                shutdownAction.run();
            }
        } catch (SQLException sqlException) {
            if (reopenedConnection != null) {
                closeQuietly(reopenedConnection);
            }

            log.warn("Single coordinator lock could not be re-acquired, retrying on the next check", sqlException);
        }
    }

    @Override
    public synchronized void destroy() {
        if (skipped || connection == null) {
            return;
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT pg_advisory_unlock(?)")) {
            preparedStatement.setLong(1, LOCK_KEY);

            preparedStatement.execute();
        } catch (SQLException sqlException) {
            log.debug("Single coordinator lock could not be released explicitly", sqlException);
        }

        held = false;

        closeQuietly(connection);

        connection = null;
    }

    synchronized boolean isHeld() {
        return held;
    }

    private static boolean tryAcquire(Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
            preparedStatement.setLong(1, LOCK_KEY);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next() && resultSet.getBoolean(1);
            }
        }
    }

    private static void closeQuietly(Connection connection) {
        try {
            connection.close();
        } catch (SQLException sqlException) {
            log.debug("Single coordinator lock connection could not be closed", sqlException);
        }
    }

    @FunctionalInterface
    public interface LockConnectionFactory {

        Connection getConnection() throws SQLException;
    }
}
