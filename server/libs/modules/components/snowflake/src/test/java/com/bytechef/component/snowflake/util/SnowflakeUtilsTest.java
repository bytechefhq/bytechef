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

package com.bytechef.component.snowflake.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.DATABASE;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.DATATYPE;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.NAME;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.SCHEMA;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
@ExtendWith(MockContextSetupExtension.class)
class SnowflakeUtilsTest {

    private Parameters mockedParameters = mock(Parameters.class);
    private final List<Map<String, Object>> responseList = List.of(
        Map.of("name", "test1"),
        Map.of("name", "test2"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void getDatabaseNameOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseList);

        List<Option<String>> result = SnowflakeUtils.getDatabaseNameOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(List.of(option("test1", "test1"), option("test2", "test2")), result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/databases", stringArgumentCaptor.getValue());
    }

    @Test
    void getSchemaNameOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        mockedParameters = MockParametersFactory.create(Map.of(DATABASE, "db"));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseList);

        List<Option<String>> result = SnowflakeUtils.getSchemaNameOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(List.of(option("test1", "test1"), option("test2", "test2")), result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/databases/db/schemas", stringArgumentCaptor.getValue());
    }

    @Test
    void getTableColumns(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        mockedParameters = MockParametersFactory.create(Map.of(DATABASE, "db", SCHEMA, "sc", TABLE, "tb"));

        Map<String, Object> responseMap = Map.of(
            "columns", List.of(
                Map.of(NAME, "col1", DATATYPE, "NUMBER"),
                Map.of(NAME, "col2", DATATYPE, "NUMBER")));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        String result = SnowflakeUtils.getTableColumns(mockedParameters, mockedContext)
            .stream()
            .map(value -> value.get(NAME))
            .collect(Collectors.joining(","));

        assertEquals("col1,col2", result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/databases/db/schemas/sc/tables/tb", stringArgumentCaptor.getValue());
    }

    @Test
    void getTableNameOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        mockedParameters = MockParametersFactory.create(Map.of(DATABASE, "db", SCHEMA, "sc"));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseList);

        List<Option<String>> result = SnowflakeUtils.getTableNameOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(List.of(option("test1", "test1"), option("test2", "test2")), result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/databases/db/schemas/sc/tables", stringArgumentCaptor.getValue());
    }
}
