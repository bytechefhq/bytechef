/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.repository;

import com.bytechef.ee.automation.app.domain.AppFavorite;
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
public class AppFavoriteRepository {

    private final JdbcClient jdbcClient;
    private final boolean onConflictSupported;

    @SuppressFBWarnings("EI")
    public AppFavoriteRepository(Dialect dialect, JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
        this.onConflictSupported = dialect instanceof PostgresDialect;
    }

    public void delete(long userId, long appId) {
        jdbcClient.sql("DELETE FROM app_favorite WHERE user_id = :userId AND app_id = :appId")
            .param("userId", userId)
            .param("appId", appId)
            .update();
    }

    public void deleteAllByUserIdAndAppIdIn(long userId, Collection<Long> appIds) {
        jdbcClient.sql("DELETE FROM app_favorite WHERE user_id = :userId AND app_id IN (:appIds)")
            .param("userId", userId)
            .param("appIds", appIds)
            .update();
    }

    public List<AppFavorite> findAllByUserIdAndAppIdIn(long userId, Collection<Long> appIds) {
        return jdbcClient
            .sql(
                "SELECT user_id, app_id, created_date FROM app_favorite WHERE user_id = :userId " +
                    "AND app_id IN (:appIds) ORDER BY app_id")
            .param("userId", userId)
            .param("appIds", appIds)
            .query(AppFavoriteRepository::toAppFavorite)
            .list();
    }

    public void insertIfAbsent(long userId, long appId, Instant createdDate) {
        String sql = onConflictSupported
            ? """
                INSERT INTO app_favorite (user_id, app_id, created_date)
                     VALUES (:userId, :appId, :createdDate)
                ON CONFLICT (user_id, app_id) DO NOTHING
                """
            : """
                INSERT INTO app_favorite (user_id, app_id, created_date)
                SELECT :userId, :appId, :createdDate
                 WHERE NOT EXISTS (SELECT 1 FROM app_favorite WHERE user_id = :userId AND app_id = :appId)
                """;

        jdbcClient.sql(sql)
            .param("userId", userId)
            .param("appId", appId)
            .param("createdDate", Timestamp.from(createdDate))
            .update();
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static AppFavorite toAppFavorite(ResultSet resultSet, int rowNumber) throws SQLException {
        Timestamp createdDate = resultSet.getTimestamp("created_date");

        return new AppFavorite(resultSet.getLong("user_id"), resultSet.getLong("app_id"), createdDate.toInstant());
    }
}
