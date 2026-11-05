/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.tenant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @version ee
 */
final class PublicSearchPathScope implements AutoCloseable {

    private static final String RESTORE_SEARCH_PATH_STATEMENT = "SELECT set_config('search_path', ?, false)";
    private static final String SHOW_AND_SWITCH_SEARCH_PATH_STATEMENT = "SHOW search_path; SET search_path TO public";

    private final Connection connection;
    private final String originalSearchPath;

    private PublicSearchPathScope(Connection connection, String originalSearchPath) {
        this.connection = connection;
        this.originalSearchPath = originalSearchPath;
    }

    static PublicSearchPathScope enter(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            if (!statement.execute(SHOW_AND_SWITCH_SEARCH_PATH_STATEMENT)) {
                throw new SQLException("SHOW search_path returned no result");
            }

            try (ResultSet resultSet = statement.getResultSet()) {
                if (!resultSet.next()) {
                    throw new SQLException("SHOW search_path returned no row");
                }

                return new PublicSearchPathScope(connection, resultSet.getString(1));
            }
        }
    }

    Connection connection() {
        return connection;
    }

    @Override
    public void close() throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(RESTORE_SEARCH_PATH_STATEMENT)) {
            preparedStatement.setString(1, originalSearchPath);

            preparedStatement.execute();
        }
    }
}
