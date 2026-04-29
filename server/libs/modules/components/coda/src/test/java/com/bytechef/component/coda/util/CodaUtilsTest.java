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

package com.bytechef.component.coda.util;

import static com.bytechef.component.coda.constant.CodaConstants.DOC_ID;
import static com.bytechef.component.coda.constant.CodaConstants.TABLE_ID;
import static com.bytechef.component.definition.ComponentDsl.option;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
@ExtendWith(MockContextSetupExtension.class)
class CodaUtilsTest {

    private Parameters mockedParameters = mock(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    Map<String, Object> mockResponse = Map.of(
        "items", List.of(
            Map.of("name", "One", "id", "1"),
            Map.of("name", "Two", "id", "2")));

    List<Option<String>> expected = List.of(
        option("One", "1"),
        option("Two", "2"));

    @BeforeEach
    void beforeEach(Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockResponse);
    }

    @Test
    void testGetTableIdOptions(
        Context mockedContext, ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        mockedParameters = MockParametersFactory.create(Map.of(DOC_ID, "id"));

        List<Option<String>> result = CodaUtils.getTableIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(expected, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/docs/id/tables", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetDocIdOptions(
        Context mockedContext, ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        List<Option<String>> result = CodaUtils.getDocIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(expected, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/docs", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetRowIdOptions(
        Context mockedContext, ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        mockedParameters = MockParametersFactory.create(Map.of(DOC_ID, "id", TABLE_ID, "table"));

        List<Option<String>> result = CodaUtils.getRowIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(expected, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/docs/id/tables/table/rows", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetColumnOptions(
        Context mockedContext, ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        mockedParameters = MockParametersFactory.create(Map.of(DOC_ID, "id", TABLE_ID, "table"));

        List<Option<String>> result = CodaUtils.getColumnOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(expected, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/docs/id/tables/table/columns", stringArgumentCaptor.getValue());
    }
}
