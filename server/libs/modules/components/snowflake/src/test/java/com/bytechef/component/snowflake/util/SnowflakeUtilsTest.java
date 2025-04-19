/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.snowflake.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.DATABASE;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.SCHEMA;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class SnowflakeUtilsTest {
    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of());
    private final Response mockedResponse = mock(Response.class);
    private final List<Map<String, Object>> reponseList = List.of(
        Map.of("name", "test1"),
        Map.of("name", "test2"));

    @Test
    void getColumnOptions() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(DATABASE, "db", SCHEMA, "schema", TABLE, "table"));
        Map<String, Object> resposneMap = Map.of(
            "columns", List.of(Map.of("name", "col1"), Map.of("name", "col2")));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(resposneMap);

        List<Option<String>> result = SnowflakeUtils.getColumnOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expected = List.of(option("col1", "col1"), option("col2", "col2"));

        assertEquals(expected, result);
    }

    @Test
    void getColumnUpdateStatementValuesValid() {
        String testColumns = "col1,col2,col3";
        String testValues = "4,2,5";

        String result = SnowflakeUtils.getColumnUpdateStatement(testColumns, testValues);

        String expected = "col1=4,col2=2,col3=5";

        assertEquals(expected, result);
    }

    @Test
    void getColumnUpdateStatementValuesInvalid() {
        String testColumns = "col1,col2,col3";
        String testValues = "4,2";

        assertThrows(IllegalArgumentException.class,
            () -> SnowflakeUtils.getColumnUpdateStatement(testColumns, testValues));
    }

    @Test
    void getDatabaseNameOptions() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(reponseList);

        List<Option<String>> result = SnowflakeUtils.getDatabaseNameOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expected = List.of(
            option("test1", "test1"),
            option("test2", "test2"));

        assertEquals(expected, result);
    }

    @Test
    void getSchemaNameOptions() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(reponseList);

        List<Option<String>> result = SnowflakeUtils.getSchemaNameOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expected = List.of(
            option("test1", "test1"),
            option("test2", "test2"));

        assertEquals(expected, result);
    }

    @Test
    void getTableColumns() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(DATABASE, "db", SCHEMA, "schema", TABLE, "table"));
        Map<String, Object> resposneMap = Map.of(
            "columns", List.of(Map.of("name", "col1"), Map.of("name", "col2")));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(resposneMap);

        String result = SnowflakeUtils.getTableColumns(mockedParameters, mockedContext);

        assertEquals("col1,col2", result);
    }

    @Test
    void getTableNameOptions() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(reponseList);

        List<Option<String>> result = SnowflakeUtils.getTableNameOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expected = List.of(
            option("test1", "test1"),
            option("test2", "test2"));

        assertEquals(expected, result);
    }
}
