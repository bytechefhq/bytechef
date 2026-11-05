/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.tenant;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A mocked PostgreSQL {@link Connection} that keeps track of its search_path the way the server does, so tests can
 * assert on the search_path itself rather than on the statements used to change it. It understands
 * {@code SHOW search_path}, {@code SET search_path TO ...} (also several of them separated by {@code ;} in one
 * {@link Statement#execute(String)}) and {@code SELECT set_config('search_path', ?, false)}.
 *
 * @version ee
 */
@SuppressFBWarnings(value = {
    "OBL", "ODR", "SQL_INJECTION_JDBC"
}, justification = "Connections, statements and result sets are Mockito mocks being stubbed, no SQL is run")
final class SearchPathConnection {

    private static final String SET_CONFIG_SEARCH_PATH = "SELECT set_config('search_path', ?, false)";
    private static final Pattern SET_SEARCH_PATH_PATTERN = Pattern.compile("SET search_path TO (.+)");
    private static final String SHOW_SEARCH_PATH = "SHOW search_path";

    private final Connection connection = mock(Connection.class);
    private final Map<Integer, SQLException> searchPathChangeFailures = new HashMap<>();
    private final List<AutoCloseable> resources = new ArrayList<>();
    private int executedStatementCount;
    private String searchPath;
    private int searchPathChangeCount;

    SearchPathConnection(String searchPath) throws SQLException {
        this.searchPath = searchPath;

        when(connection.createStatement()).thenAnswer(invocation -> createStatement());
        when(connection.prepareStatement(anyString())).thenAnswer(
            invocation -> createPreparedStatement(invocation.getArgument(0)));
    }

    Connection getConnection() {
        return connection;
    }

    int getExecutedStatementCount() {
        return executedStatementCount;
    }

    String getSearchPath() {
        return searchPath;
    }

    /**
     * Makes the given search_path change (1 for the first one on this connection) fail with the given exception,
     * leaving the search_path as it was.
     */
    void failSearchPathChange(int searchPathChangeNumber, SQLException sqlException) {
        searchPathChangeFailures.put(searchPathChangeNumber, sqlException);
    }

    void verifyEveryStatementAndResultSetClosed() throws Exception {
        for (AutoCloseable resource : resources) {
            verify(resource).close();
        }
    }

    private void changeSearchPath(String newSearchPath) throws SQLException {
        searchPathChangeCount++;

        SQLException sqlException = searchPathChangeFailures.get(searchPathChangeCount);

        if (sqlException != null) {
            throw sqlException;
        }

        searchPath = newSearchPath;
    }

    private PreparedStatement createPreparedStatement(String sql) throws SQLException {
        if (!SET_CONFIG_SEARCH_PATH.equals(sql)) {
            throw new SQLException("Unexpected prepared statement: " + sql);
        }

        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        AtomicReference<String> parameterReference = new AtomicReference<>();

        doAnswer(invocation -> {
            parameterReference.set(invocation.getArgument(1));

            return null;
        }).when(preparedStatement)
            .setString(anyInt(), anyString());

        when(preparedStatement.execute()).thenAnswer(invocation -> {
            executedStatementCount++;

            changeSearchPath(parameterReference.get());

            return true;
        });

        resources.add(preparedStatement);

        return preparedStatement;
    }

    @SuppressWarnings("PMD.CheckResultSet")
    private ResultSet createResultSet(String value) throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);

        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString(1)).thenReturn(value);

        resources.add(resultSet);

        return resultSet;
    }

    private Statement createStatement() throws SQLException {
        Statement statement = mock(Statement.class);
        AtomicReference<ResultSet> resultSetReference = new AtomicReference<>();

        when(statement.execute(anyString())).thenAnswer(invocation -> {
            executedStatementCount++;

            String sql = invocation.getArgument(0);

            boolean firstResultIsResultSet = false;
            String[] sqlCommands = sql.split(";");

            for (int index = 0; index < sqlCommands.length; index++) {
                String sqlCommand = sqlCommands[index].trim();

                if (sqlCommand.equalsIgnoreCase(SHOW_SEARCH_PATH)) {
                    resultSetReference.compareAndSet(null, createResultSet(searchPath));

                    firstResultIsResultSet = firstResultIsResultSet || index == 0;

                    continue;
                }

                Matcher matcher = SET_SEARCH_PATH_PATTERN.matcher(sqlCommand);

                if (!matcher.matches()) {
                    throw new SQLException("Unexpected statement: " + sqlCommand);
                }

                changeSearchPath(matcher.group(1));
            }

            return firstResultIsResultSet;
        });

        when(statement.getResultSet()).thenAnswer(invocation -> resultSetReference.get());

        resources.add(statement);

        return statement;
    }
}
