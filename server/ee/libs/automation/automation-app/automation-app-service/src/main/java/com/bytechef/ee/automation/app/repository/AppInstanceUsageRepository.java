/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.repository;

import com.bytechef.ee.automation.app.domain.AppInstanceUsage;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.dialect.PostgresDialect;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
@ConditionalOnEEVersion
public class AppInstanceUsageRepository {

    private final JdbcClient jdbcClient;
    private final boolean onConflictSupported;

    @SuppressFBWarnings("EI")
    public AppInstanceUsageRepository(Dialect dialect, JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
        this.onConflictSupported = dialect instanceof PostgresDialect;
    }

    public void deleteAllByUserIdAndAppInstanceIdIn(long userId, Collection<Long> appInstanceIds) {
        jdbcClient
            .sql(
                "DELETE FROM app_instance_usage WHERE user_id = :userId AND app_instance_id IN (:appInstanceIds)")
            .param("userId", userId)
            .param("appInstanceIds", appInstanceIds)
            .update();
    }

    public List<AppInstanceUsage> findAllByUserIdAndAppInstanceIdIn(long userId, Collection<Long> appInstanceIds) {
        return jdbcClient
            .sql(
                "SELECT user_id, app_instance_id, last_used_date, use_count FROM app_instance_usage " +
                    "WHERE user_id = :userId AND app_instance_id IN (:appInstanceIds) ORDER BY app_instance_id")
            .param("userId", userId)
            .param("appInstanceIds", appInstanceIds)
            .query(AppInstanceUsageRepository::toAppInstanceUsage)
            .list();
    }

    public void recordLaunch(long userId, long appInstanceId, Instant now) {
        if (onConflictSupported) {
            statement(
                """
                    INSERT INTO app_instance_usage (user_id, app_instance_id, last_used_date, use_count)
                         VALUES (:userId, :appInstanceId, :now, 1)
                    ON CONFLICT (user_id, app_instance_id) DO UPDATE
                       SET last_used_date = EXCLUDED.last_used_date, use_count = app_instance_usage.use_count + 1
                    """, userId, appInstanceId, now)
                    .update();

            return;
        }

        int updatedRows = statement(
            """
                UPDATE app_instance_usage SET last_used_date = :now, use_count = use_count + 1
                 WHERE user_id = :userId AND app_instance_id = :appInstanceId
                """, userId, appInstanceId, now)
                .update();

        if (updatedRows == 0) {
            insertIfAbsent(userId, appInstanceId, now, 1);
        }
    }

    /**
     * Writes only when the stored last-used date is older than {@code cutoff}, or when there is no row yet. A throttled
     * call changes nothing.
     */
    public void touchLastUsedDate(long userId, long appInstanceId, Instant now, Instant cutoff) {
        if (onConflictSupported) {
            statement(
                """
                    INSERT INTO app_instance_usage (user_id, app_instance_id, last_used_date, use_count)
                         VALUES (:userId, :appInstanceId, :now, 0)
                    ON CONFLICT (user_id, app_instance_id) DO UPDATE
                       SET last_used_date = EXCLUDED.last_used_date
                     WHERE app_instance_usage.last_used_date < :cutoff
                    """, userId, appInstanceId, now)
                    .param("cutoff", Timestamp.from(cutoff))
                    .update();

            return;
        }

        int updatedRows = statement(
            """
                UPDATE app_instance_usage SET last_used_date = :now
                 WHERE user_id = :userId AND app_instance_id = :appInstanceId AND last_used_date < :cutoff
                """, userId, appInstanceId, now)
                .param("cutoff", Timestamp.from(cutoff))
                .update();

        if (updatedRows == 0) {
            insertIfAbsent(userId, appInstanceId, now, 0);
        }
    }

    private void insertIfAbsent(long userId, long appInstanceId, Instant now, long useCount) {
        statement(
            """
                INSERT INTO app_instance_usage (user_id, app_instance_id, last_used_date, use_count)
                SELECT :userId, :appInstanceId, :now, :useCount
                 WHERE NOT EXISTS (
                       SELECT 1 FROM app_instance_usage
                        WHERE user_id = :userId AND app_instance_id = :appInstanceId)
                """, userId, appInstanceId, now)
                .param("useCount", useCount)
                .update();
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static AppInstanceUsage toAppInstanceUsage(ResultSet resultSet, int rowNumber) throws SQLException {
        Timestamp lastUsedDate = resultSet.getTimestamp("last_used_date");

        return new AppInstanceUsage(
            resultSet.getLong("user_id"), resultSet.getLong("app_instance_id"), lastUsedDate.toInstant(),
            resultSet.getLong("use_count"));
    }

    private JdbcClient.StatementSpec statement(String sql, long userId, long appInstanceId, Instant now) {
        return jdbcClient.sql(sql)
            .param("userId", userId)
            .param("appInstanceId", appInstanceId)
            .param("now", Timestamp.from(now));
    }
}
