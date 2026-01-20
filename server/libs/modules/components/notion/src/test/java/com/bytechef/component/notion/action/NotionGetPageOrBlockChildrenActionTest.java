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

package com.bytechef.component.notion.action;

import static com.bytechef.component.notion.constant.NotionConstants.ID;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class NotionGetPageOrBlockChildrenActionTest {

    private final ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor =
        forClass(ConfigurationBuilder.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http mockedHttp = mock(Http.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(ID, "parent-id"));
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = forClass(Object[].class);

    @Test
    @SuppressWarnings("unchecked")
    void testPerform() {
        Map<String, Object> child1 = new HashMap<>(Map.of("id", "child-1", "has_children", true));
        Map<String, Object> child2 = Map.of("id", "child-2", "has_children", false);
        Map<String, Object> grandchild = Map.of("id", "grandchild-1", "has_children", false);

        when(mockedContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Http, Http.Executor> value = httpFunctionArgumentCaptor.getValue();

                return value.apply(mockedHttp);
            });
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of("results", List.of(child1, child2)),
                Map.of("results", List.of(grandchild)));

        List<Object> result = NotionGetPageOrBlockChildrenAction.perform(mockedParameters, null, mockedContext);

        child1.put("children", List.of(grandchild));
        assertEquals(List.of(child1, child2), result);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals(
            List.of("/blocks/parent-id/children", "/blocks/child-1/children"),
            stringArgumentCaptor.getAllValues());

        Object[] objects = {
            "page_size", 100, "start_cursor", null
        };
        assertArrayEquals(objects, objectsArgumentCaptor.getValue());
    }
}
