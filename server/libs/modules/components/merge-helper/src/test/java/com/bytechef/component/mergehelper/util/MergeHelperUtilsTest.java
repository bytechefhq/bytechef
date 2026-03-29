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

package com.bytechef.component.mergehelper.util;

import static com.bytechef.component.mergehelper.util.MergeHelperUtils.flatten;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.util.HashMap;
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
class MergeHelperUtilsTest {

    private final Connection connection = mock(Connection.class);
    private final Statement statement = mock(Statement.class);
    private final PreparedStatement ps = mock(PreparedStatement.class);
    private final ResultSet rs = mock(ResultSet.class);
    private final ResultSetMetaData rsMetaData = mock(ResultSetMetaData.class);

    @Test
    void testFlattenSimpleMap() {
        Map<String, Object> input = new HashMap<>();

        input.put("a", 1);
        input.put("b", "test");

        List<Map<String, Object>> result = flatten(input);

        assertEquals(List.of(Map.of("a", 1, "b", "test")), result);
    }

    @Test
    void testFlattenNestedMap() {
        Map<String, Object> nested = new HashMap<>();

        nested.put("inner", "value");

        Map<String, Object> input = new HashMap<>();

        input.put("a", nested);

        List<Map<String, Object>> result = flatten(input);

        assertEquals(List.of(Map.of("a_inner", "value")), result);
    }

    @Test
    void testFlattenIterable() {
        List<Object> input = List.of(Map.of("a", 1), Map.of("a", 2));

        List<Map<String, Object>> result = flatten(input);

        assertEquals(List.of(Map.of("a", 1), Map.of("a", 2)), result);
    }

    @Test
    void testFlattenNestedIterableInsideMap() {
        Map<String, Object> input = new HashMap<>();

        input.put("items", List.of(Map.of("a", 1), Map.of("a", 2)));

        List<Map<String, Object>> result = flatten(input);

        assertEquals(List.of(Map.of("items", List.of(Map.of("a", 1), Map.of("a", 2)))), result);

    }

    @Test
    void testFlattenPrimitiveValue() {
        List<Map<String, Object>> result = flatten(42);

        assertEquals(List.of(Map.of("value", 42)), result);
    }

    @Test
    void testFlattenNull() {
        List<Map<String, Object>> result = flatten(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void testFlattenMixedStructure() {
        Map<String, Object> inner = new HashMap<>();

        inner.put("b", 2);

        Map<String, Object> input = new HashMap<>();

        input.put("a", inner);
        input.put("c", List.of(Map.of("d", 3), Map.of("d", 4)));

        List<Map<String, Object>> result = flatten(input);

        assertEquals(List.of(Map.of("a_b", 2, "c", List.of(Map.of("d", 3), Map.of("d", 4)))), result);
    }

    @Test
    void testCreateTableWithRows() throws Exception {
        when(connection.createStatement()).thenReturn(statement);

        List<Map<String, Object>> rows = List.of(Map.of("id", 1, "name", "Alice"));

        MergeHelperUtils.createTable(connection, "people", rows);

        verify(connection).createStatement();
        verify(statement).execute(contains("CREATE TABLE people"));
        verify(statement).close();
    }

    @Test
    void testCreateTableWithEmptyRows() throws Exception {
        when(connection.createStatement()).thenReturn(statement);

        MergeHelperUtils.createTable(connection, "empty_table", List.of());

        verify(statement).execute("CREATE TABLE empty_table (dummy INTEGER);");
        verify(statement).close();
    }

    @Test
    void testInsertData() throws Exception {
        when(connection.prepareStatement(anyString())).thenReturn(ps);

        List<Map<String, Object>> rows = List.of(Map.of("id", 1, "name", "Alice"), Map.of("id", 2, "name", "Bob"));

        MergeHelperUtils.insertData(connection, "people", rows);

        verify(connection).prepareStatement(contains("INSERT INTO people"));
        verify(ps, times(2)).addBatch();
        verify(ps).executeBatch();
        verify(ps).close();
    }

    @Test
    void testInsertDataEmpty() throws Exception {
        MergeHelperUtils.insertData(connection, "people", List.of());

        verify(connection, never()).prepareStatement(anyString());
    }

    @Test
    void testValidateSQL() throws Exception {
        when(connection.createStatement()).thenReturn(statement);

        MergeHelperUtils.validateSQL(connection, "SELECT * FROM people");

        verify(statement).execute("EXPLAIN SELECT * FROM people");
        verify(statement).close();
    }

    @SuppressWarnings("PMD.CheckResultSet")
    @Test
    void testExecuteQuery() throws Exception {
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(rs);

        when(rs.getMetaData()).thenReturn(rsMetaData);
        when(rsMetaData.getColumnCount()).thenReturn(2);
        when(rsMetaData.getColumnName(1)).thenReturn("id");
        when(rsMetaData.getColumnName(2)).thenReturn("name");

        when(rs.next()).thenReturn(true, false);
        when(rs.getObject(1)).thenReturn(1);
        when(rs.getObject(2)).thenReturn("Alice");

        List<Map<String, Object>> result = MergeHelperUtils.executeQuery(connection, "SELECT id, name FROM people");

        assertEquals(1, result.size());

        Map<String, Object> row = result.getFirst();

        assertEquals(1, row.get("id"));
        assertEquals("Alice", row.get("name"));

        verify(statement).executeQuery("SELECT id, name FROM people");
        verify(rs).close();
        verify(statement).close();
    }
}
