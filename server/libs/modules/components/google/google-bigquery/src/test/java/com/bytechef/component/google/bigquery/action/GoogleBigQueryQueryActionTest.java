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
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Context.Http.ResponseType.Type;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
@ExtendWith(MockContextSetupExtension.class)
class GoogleBigQueryQueryActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final Object mockedObject = mock(Object.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            PROJECT_ID, "projectId", QUERY, "query", MAX_RESULT, 10, TIMEOUT_MS, 10, DRY_RUN, true,
            CREATION_SESSION, true));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void perform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(mockedObject);

        Object result = GoogleBigQueryQueryAction.perform(mockedParameters, null, mockedContext);

        assertEquals(mockedObject, result);

        Map<String, Object> expectedBodyContent = Map.of(
            QUERY, "query", MAX_RESULT, 10, TIMEOUT_MS, 10, DRY_RUN, true, CREATION_SESSION, true);

        assertEquals(Http.Body.of(expectedBodyContent, Http.BodyContentType.JSON), bodyArgumentCaptor.getValue());

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();
        ResponseType responseType = configuration.getResponseType();

        assertEquals(Type.JSON, responseType.getType());
        assertEquals("/projects/projectId/queries", stringArgumentCaptor.getValue());
    }
}
