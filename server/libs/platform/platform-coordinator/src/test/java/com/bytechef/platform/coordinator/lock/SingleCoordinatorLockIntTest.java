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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.coordinator.config.SingleCoordinatorLockConfiguration;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author Ivica Cardic
 */
@Testcontainers
class SingleCoordinatorLockIntTest {

    @Container
    private static final PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:16-alpine");

    private final AtomicInteger shutdownCount = new AtomicInteger();
    private final AtomicReference<Connection> lastOpenedConnection = new AtomicReference<>();

    @Test
    void testFirstCoordinatorAcquiresLock() throws Exception {
        SingleCoordinatorLock lock = newLock();

        try {
            lock.afterPropertiesSet();

            assertThat(lock.isHeld()).isTrue();

            assertLockedInDatabase();
        } finally {
            lock.destroy();
        }
    }

    @Test
    void testSecondCoordinatorIsRefusedWithEditionMessage() throws Exception {
        SingleCoordinatorLock firstLock = newLock();
        SingleCoordinatorLock secondLock = newLock();

        try {
            firstLock.afterPropertiesSet();

            assertThatThrownBy(secondLock::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(SingleCoordinatorLock.ANOTHER_COORDINATOR_MESSAGE);
        } finally {
            secondLock.destroy();
            firstLock.destroy();
        }
    }

    @Test
    void testLockIsReleasedOnDestroy() throws Exception {
        SingleCoordinatorLock firstLock = newLock();
        SingleCoordinatorLock secondLock = newLock();

        try {
            firstLock.afterPropertiesSet();

            firstLock.destroy();

            secondLock.afterPropertiesSet();

            assertThat(secondLock.isHeld()).isTrue();
        } finally {
            secondLock.destroy();
            firstLock.destroy();
        }
    }

    @Test
    void testLockIsFreedWhenHolderConnectionDropsWithoutUnlock() throws Exception {
        SingleCoordinatorLock firstLock = newLock();
        SingleCoordinatorLock secondLock = newLock();

        try {
            firstLock.afterPropertiesSet();

            terminateLockHolderBackend();

            secondLock.afterPropertiesSet();

            assertThat(secondLock.isHeld()).isTrue();
        } finally {
            secondLock.destroy();
            firstLock.destroy();
        }
    }

    @Test
    void testShutsDownAsSoonAsTheLockConnectionIsLost() throws Exception {
        SingleCoordinatorLock lock = newLock();

        try {
            lock.afterPropertiesSet();

            terminateLockHolderBackend();

            lock.verifyLock();

            assertThat(lock.isHeld()).isFalse();
            assertThat(shutdownCount.get()).isEqualTo(1);

            lock.verifyLock();

            assertThat(shutdownCount.get()).isEqualTo(1);
        } finally {
            lock.destroy();
        }
    }

    @Test
    void testShutsDownWhenLockWasTakenWhileDisconnected() throws Exception {
        SingleCoordinatorLock firstLock = newLock();
        SingleCoordinatorLock secondLock = newLock();

        try {
            firstLock.afterPropertiesSet();

            terminateLockHolderBackend();

            secondLock.afterPropertiesSet();

            firstLock.verifyLock();

            assertThat(shutdownCount.get()).isEqualTo(1);
            assertThat(firstLock.isHeld()).isFalse();
        } finally {
            secondLock.destroy();
            firstLock.destroy();
        }
    }

    @Test
    void testStartupWaitsForTheLockHeldByAPreviousCoordinator() throws Exception {
        SingleCoordinatorLock firstLock = newLock();
        SingleCoordinatorLock secondLock = newLock(Duration.ofSeconds(30));

        AtomicReference<Throwable> failure = new AtomicReference<>();

        firstLock.afterPropertiesSet();

        Thread waitingThread = Thread.ofPlatform()
            .start(() -> {
                try {
                    secondLock.afterPropertiesSet();
                } catch (Exception exception) {
                    failure.set(exception);
                }
            });

        try {
            Thread.sleep(1000);

            assertThat(secondLock.isHeld()).isFalse();

            firstLock.destroy();

            waitingThread.join(15_000);

            assertThat(waitingThread.isAlive()).isFalse();
            assertThat(failure.get()).isNull();
            assertThat(secondLock.isHeld()).isTrue();
        } finally {
            waitingThread.interrupt();

            secondLock.destroy();
            firstLock.destroy();
        }
    }

    @Test
    void testRefusalHappensAfterTheLockWaitElapses() throws Exception {
        SingleCoordinatorLock firstLock = newLock();
        SingleCoordinatorLock secondLock = newLock(Duration.ofSeconds(2));

        try {
            firstLock.afterPropertiesSet();

            long startedAt = System.nanoTime();

            assertThatThrownBy(secondLock::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(SingleCoordinatorLock.ANOTHER_COORDINATOR_MESSAGE);

            assertThat(Duration.ofNanos(System.nanoTime() - startedAt)).isGreaterThanOrEqualTo(Duration.ofSeconds(2));
        } finally {
            secondLock.destroy();
            firstLock.destroy();
        }
    }

    @Test
    void testLockLossDoesNotWaitForTheLockWait() throws Exception {
        SingleCoordinatorLock firstLock = newLock(Duration.ofSeconds(30));
        SingleCoordinatorLock secondLock = newLock();

        try {
            firstLock.afterPropertiesSet();

            terminateLockHolderBackend();

            secondLock.afterPropertiesSet();

            long startNanos = System.nanoTime();

            firstLock.verifyLock();

            assertThat(Duration.ofNanos(System.nanoTime() - startNanos)).isLessThan(Duration.ofSeconds(10));
            assertThat(firstLock.isHeld()).isFalse();
            assertThat(shutdownCount.get()).isEqualTo(1);
        } finally {
            secondLock.destroy();
            firstLock.destroy();
        }
    }

    @Test
    void testLockConnectionUsesServerSideKeepalives() throws Exception {
        SingleCoordinatorLock lock = newLock();

        try {
            lock.afterPropertiesSet();

            Connection lockConnection = lastOpenedConnection.get();

            assertThat(showSetting(lockConnection, "tcp_keepalives_idle")).isEqualTo("10");
            assertThat(showSetting(lockConnection, "tcp_keepalives_interval")).isEqualTo("5");
            assertThat(showSetting(lockConnection, "tcp_keepalives_count")).isEqualTo("3");
        } finally {
            lock.destroy();
        }
    }

    @Test
    void testH2UrlIsSkipped() throws Exception {
        SingleCoordinatorLock lock = new SingleCoordinatorLock(
            "jdbc:h2:mem:test",
            () -> {
                throw new AssertionError("No connection must be opened for H2");
            },
            shutdownCount::incrementAndGet, Duration.ZERO);

        lock.afterPropertiesSet();
        lock.verifyLock();
        lock.destroy();

        assertThat(lock.isHeld()).isFalse();
        assertThat(shutdownCount.get()).isZero();
    }

    private SingleCoordinatorLock newLock() {
        return newLock(Duration.ZERO);
    }

    private SingleCoordinatorLock newLock(Duration lockWait) {
        return new SingleCoordinatorLock(
            container.getJdbcUrl(),
            () -> {
                Connection openedConnection = DriverManager.getConnection(
                    container.getJdbcUrl(),
                    SingleCoordinatorLockConfiguration.lockConnectionProperties(
                        container.getUsername(), container.getPassword()));

                lastOpenedConnection.set(openedConnection);

                return openedConnection;
            },
            shutdownCount::incrementAndGet, lockWait);
    }

    private void assertLockedInDatabase() {
        SingleCoordinatorLock contender = newLock();

        assertThatThrownBy(contender::afterPropertiesSet)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(SingleCoordinatorLock.ANOTHER_COORDINATOR_MESSAGE);
    }

    private static String showSetting(Connection connection, String settingName) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT current_setting(?)")) {
            preparedStatement.setString(1, settingName);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new AssertionError("No value for setting " + settingName);
                }

                return resultSet.getString(1);
            }
        }
    }

    private static void terminateLockHolderBackend() throws SQLException {
        try (Connection adminConnection = DriverManager.getConnection(
            container.getJdbcUrl(), container.getUsername(), container.getPassword());
            Statement statement = adminConnection.createStatement()) {

            statement.execute(
                "SELECT pg_terminate_backend(pid, 5000) FROM pg_stat_activity WHERE pid <> pg_backend_pid() " +
                    "AND datname = current_database() AND query LIKE '%advisory%'");
        }
    }
}
