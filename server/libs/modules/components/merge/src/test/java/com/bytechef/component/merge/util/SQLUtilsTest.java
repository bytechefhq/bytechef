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

package com.bytechef.component.merge.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivona Pavela
 */
@SuppressFBWarnings({
    "ODR_OPEN_DATABASE_RESOURCE", "OBL_UNSATISFIED_OBLIGATION", "SQL_INJECTION_JDBC"
})
@SuppressWarnings({
    "SqlStatementUsage"
})
class SQLUtilsTest {

    private final Connection conn = mock(Connection.class);
    private final Statement statement = mock(Statement.class);
    private final PreparedStatement preparedStatement = mock(PreparedStatement.class);
    private final ResultSet resultSet = mock(ResultSet.class);
    private final ResultSetMetaData metaData = mock(ResultSetMetaData.class);

    @Test
    void testCreateTableWithRows() throws Exception {
        when(conn.createStatement()).thenReturn(statement);

        List<Map<String, Object>> rows = List.of(
            Map.of("id", 1, "name", "Alice"));

        SQLUtils.createTable(conn, "people", rows);

        verify(conn).createStatement();
        verify(statement).execute(contains("CREATE TABLE people"));
        verify(statement).close();
    }

    @Test
    void testCreateTableWithEmptyRows() throws Exception {
        when(conn.createStatement()).thenReturn(statement);

        SQLUtils.createTable(conn, "empty_table", List.of());

        verify(statement).execute("CREATE TABLE empty_table (dummy INTEGER);");
        verify(statement).close();
    }

    @Test
    void testInsertData() throws Exception {
        when(conn.prepareStatement(anyString())).thenReturn(preparedStatement);

        List<Map<String, Object>> rows = List.of(
            Map.of("id", 1, "name", "Alice"),
            Map.of("id", 2, "name", "Bob"));

        SQLUtils.insertData(conn, "people", rows);

        verify(conn).prepareStatement(contains("INSERT INTO people"));
        verify(preparedStatement, times(2)).addBatch();
        verify(preparedStatement).executeBatch();
        verify(preparedStatement).close();
    }

    @Test
    void testInsertDataEmpty() throws Exception {
        SQLUtils.insertData(conn, "people", List.of());

        verify(conn, never()).prepareStatement(anyString());
    }

    @Test
    void testValidateSQL() throws Exception {
        when(conn.createStatement()).thenReturn(statement);

        SQLUtils.validateSQL(conn, "SELECT * FROM people");

        verify(statement).execute("EXPLAIN SELECT * FROM people");
        verify(statement).close();
    }

    @SuppressWarnings("PMD.CheckResultSet")
    @Test
    void testExecuteQuery() throws Exception {
        when(conn.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);

        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnName(1)).thenReturn("id");
        when(metaData.getColumnName(2)).thenReturn("name");

        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getObject(1)).thenReturn(1);
        when(resultSet.getObject(2)).thenReturn("Alice");

        List<Map<String, Object>> result =
            SQLUtils.executeQuery(conn, "SELECT id, name FROM people");

        assertEquals(1, result.size());

        Map<String, Object> row = result.getFirst();
        assertEquals(1, row.get("id"));
        assertEquals("Alice", row.get("name"));

        verify(statement).executeQuery("SELECT id, name FROM people");
        verify(resultSet).close();
        verify(statement).close();
    }
}
