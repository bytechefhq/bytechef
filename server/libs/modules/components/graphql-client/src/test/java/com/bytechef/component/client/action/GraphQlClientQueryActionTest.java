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

import static com.bytechef.component.graphql.client.constant.GraphQlConstants.GRAPHQL_ENDPOINT;
import static com.bytechef.component.graphql.client.constant.GraphQlConstants.HEADERS;
import static com.bytechef.component.graphql.client.constant.GraphQlConstants.QUERY;
import static com.bytechef.component.graphql.client.constant.GraphQlConstants.VARIABLES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.graphql.client.action.GraphQlClientQueryAction;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 **/

@ExtendWith(MockContextSetupExtension.class)
class GraphQlClientQueryActionTest {

    public static final String EXAMPLE_QUERY = """
        query ExampleQuery($characterId: ID!) {
          character(id: $characterId) {
            created
          }
        }""";

    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<Map<String, List<String>>> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final Object mockedObject = mock(Object.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(
                GRAPHQL_ENDPOINT, "/graphql",
                HEADERS, Map.of("Content-Type", "application/json"),
                VARIABLES, Map.of("characterId", 1),
                QUERY, EXAMPLE_QUERY));

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.headers(mapArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(mockedObject);

        Object result = GraphQlClientQueryAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedObject, result);

        ContextFunction<Http, Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Configuration configuration = configurationBuilder.build();

        ResponseType responseType = configuration.getResponseType();

        assertEquals(ResponseType.Type.JSON, responseType.getType());
        assertEquals("/graphql", stringArgumentCaptor.getValue());

        Map<String, List<String>> headers = mapArgumentCaptor.getValue();

        assertEquals(headers, Map.of("Content-Type", List.of("application/json")));

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(Map.of(VARIABLES, Map.of("characterId", 1), QUERY, EXAMPLE_QUERY), body.getContent());
    }
}
