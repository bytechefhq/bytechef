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

package com.bytechef.component.google.bigquery.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.google.bigquery.constant.GoogleBigQueryConstants.ID;
import static com.bytechef.component.google.bigquery.constant.GoogleBigQueryConstants.NEXT_PAGE_TOKEN;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
@ExtendWith(MockContextSetupExtension.class)
class GoogleBigQueryUtilsTest {

    private final Parameters mockedParameters = MockParametersFactory.create(Map.of());
    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = forClass(Object[].class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testGetProjectIdOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of(
                    "projects", List.of(
                        Map.of("friendlyName", "name1", ID, "id1"),
                        Map.of("friendlyName", "name2", ID, "id2")),
                    NEXT_PAGE_TOKEN, "token"))
            .thenReturn(Map.of("projects", List.of(Map.of("friendlyName", "name 3", ID, "id3"))));

        List<Option<String>> result = GoogleBigQueryUtils.getProjectIdOptions(
            mockedParameters, null, Map.of(), "", mockedContext);

        List<Option<String>> expectedProjectIdOptions = List.of(
            option("name1", "id1"),
            option("name2", "id2"),
            option("name 3", "id3"));

        assertEquals(expectedProjectIdOptions, result);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Configuration configuration = configurationBuilder.build();

        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals(List.of("/projects", "/projects"), stringArgumentCaptor.getAllValues());

        List<Object[]> allQueryParams = objectsArgumentCaptor.getAllValues();

        assertEquals(2, allQueryParams.size());

        Object[] queryParameters1 = {
            "pageToken", null,
            "maxResults", 50
        };
        Object[] queryParameters2 = {
            "pageToken", "token",
            "maxResults", 50
        };

        assertArrayEquals(queryParameters1, allQueryParams.get(0));
        assertArrayEquals(queryParameters2, allQueryParams.get(1));
    }
}
