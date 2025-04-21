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

package com.bytechef.component.client.action;

import static com.bytechef.component.graphql.client.constant.GraphQlConstants.HEADERS;
import static com.bytechef.component.graphql.client.constant.GraphQlConstants.QUERY;
import static com.bytechef.component.graphql.client.constant.GraphQlConstants.VARIABLES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.graphql.client.action.GraphQlClientQueryAction;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 **/

class GraphQlClientQueryActionTest {

    public static final String EXAMPLE_QUERY = """
        query ExampleQuery($characterId: ID!) {
          character(id: $characterId) {
            created
          }
        }""";

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final ArgumentCaptor<Map<String, List<String>>> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            HEADERS, Map.of("Content-Type", "application/json"),
            VARIABLES, Map.of("characterId", 1),
            QUERY, EXAMPLE_QUERY));
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final Object mockedObject = mock(Object.class);

    @Test
    void testPerform() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.headers(mapArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody())
            .thenReturn(mockedObject);

        Object result = GraphQlClientQueryAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedObject, result);

        Map<String, List<String>> headers = mapArgumentCaptor.getValue();

        assertEquals(headers, Map.of("Content-Type", List.of("application/json")));

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(Map.of(VARIABLES, Map.of("characterId", 1), QUERY, EXAMPLE_QUERY), body.getContent());
    }
}
