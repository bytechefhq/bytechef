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
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
class GoogleBigQueryUtilsTest {

    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of());
    private final Response mockedResponse = mock(Response.class);
    private final Map<String, List<Map<String, Object>>> responseMap = Map.of(
        "projects", List.of(
            Map.of("friendlyName", "name1",
                ID, "id1"),
            Map.of("friendlyName", "name2",
                ID, "id2")));
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testGetProjectIdOptions() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        List<Option<String>> result = GoogleBigQueryUtils.getProjectIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(List.of(NEXT_PAGE_TOKEN, ""), stringArgumentCaptor.getAllValues());

        List<Option<String>> expectedProjectIdOptions = List.of(
            option("name1", "id1"),
            option("name2", "id2"));

        assertEquals(expectedProjectIdOptions, result);
    }
}
