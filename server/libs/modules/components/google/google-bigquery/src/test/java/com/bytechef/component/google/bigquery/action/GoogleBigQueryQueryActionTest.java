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

package com.bytechef.component.google.bigquery.action;

import static com.bytechef.component.google.bigquery.constant.GoogleBigQueryConstants.CREATION_SESSION;
import static com.bytechef.component.google.bigquery.constant.GoogleBigQueryConstants.DRY_RUN;
import static com.bytechef.component.google.bigquery.constant.GoogleBigQueryConstants.MAX_RESULT;
import static com.bytechef.component.google.bigquery.constant.GoogleBigQueryConstants.PROJECT_ID;
import static com.bytechef.component.google.bigquery.constant.GoogleBigQueryConstants.QUERY;
import static com.bytechef.component.google.bigquery.constant.GoogleBigQueryConstants.TIMEOUT_MS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ActionDefinition.SingleConnectionPerformFunction;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType.Type;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
class GoogleBigQueryQueryActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor =
        forClass(ConfigurationBuilder.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Executor mockedExecutor = mock(Http.Executor.class);
    private final Http mockedHttp = mock(Http.class);
    private final Object mockedObject = mock(Object.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(PROJECT_ID, "projectId", QUERY, "query", MAX_RESULT, 10,
            TIMEOUT_MS, 10, DRY_RUN, true, CREATION_SESSION, true));
    private final Response mockedResponse = mock(Response.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void perform() throws Exception {
        Optional<PerformFunction> performFunction = GoogleBigQueryQueryAction.ACTION_DEFINITION.getPerform();

        assertTrue(performFunction.isPresent());

        SingleConnectionPerformFunction singleConnectionPerformFunction =
            (SingleConnectionPerformFunction) performFunction.get();

        when(mockedActionContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Http, Http.Executor> value = httpFunctionArgumentCaptor.getValue();

                return value.apply(mockedHttp);
            });
        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedObject);

        Object result = singleConnectionPerformFunction.apply(
            mockedParameters, null, mockedActionContext);

        assertEquals(mockedObject, result);

        Body body = bodyArgumentCaptor.getValue();

        Map<String, Object> expectedBodyContent = Map.of(
            QUERY, "query",
            MAX_RESULT, 10,
            TIMEOUT_MS, 10,
            DRY_RUN, true,
            CREATION_SESSION, true);

        assertEquals(expectedBodyContent, body.getContent());

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        Http.Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Type.JSON, responseType.getType());
        assertEquals(
            "https://bigquery.googleapis.com/bigquery/v2/projects/projectId/queries",
            stringArgumentCaptor.getValue());
    }
}
