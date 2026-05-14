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

package com.bytechef.component.amplitude.action;

import static com.bytechef.component.amplitude.constant.AmplitudeConstants.API_KEY;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.CONTENT_TYPE;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.CONTENT_TYPE_URLENCODED;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.ID;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.IDENTIFICATION;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.KEY;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.USER_PROPERTIES;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.amplitude.util.AmplitudeUtils;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Context.Json;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
@ExtendWith(MockContextSetupExtension.class)
class AmplitudeCreateOrUpdateUserActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Json, Executor>> jsonFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final Map<String, Object> mockedIdentification = Map.of();
    private final Json mockedJson = mock(Json.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            API_KEY, "api_key", ID, "id", USER_PROPERTIES,
            List.of(Map.of(KEY, "userPropertyKey", VALUE, "userPropertyValue"))));
    private final ArgumentCaptor<Object> objectArgumentCaptor = forClass(Object.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        String identificationJson = "identificationJson";
        String responseString = "response";

        try (MockedStatic<AmplitudeUtils> amplitudeUtilsMockedStatic = mockStatic(AmplitudeUtils.class)) {
            amplitudeUtilsMockedStatic
                .when(() -> AmplitudeUtils.getIdentification(parametersArgumentCaptor.capture()))
                .thenReturn(mockedIdentification);

            when(mockedContext.json(jsonFunctionArgumentCaptor.capture()))
                .thenAnswer(inv -> {
                    ContextFunction<Json, Executor> value = jsonFunctionArgumentCaptor.getValue();

                    return value.apply(mockedJson);
                });

            when(mockedJson.write(objectArgumentCaptor.capture()))
                .thenReturn(identificationJson);
            when(mockedHttp.post(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.header(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedResponse.getBody(String.class))
                .thenReturn(responseString);

            String response = AmplitudeCreateOrUpdateUserAction.perform(
                mockedParameters, mockedParameters, mockedContext);

            assertEquals(responseString, response);
            assertNotNull(jsonFunctionArgumentCaptor.getValue());
            assertEquals(mockedIdentification, objectArgumentCaptor.getValue());
            assertNotNull(httpFunctionArgumentCaptor.getValue());

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Configuration configuration = configurationBuilder.build();

            assertEquals(ResponseType.TEXT, configuration.getResponseType());
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals(
                List.of("/identify", CONTENT_TYPE, CONTENT_TYPE_URLENCODED), stringArgumentCaptor.getAllValues());

            Map<String, Object> expectedBody = Map.of(API_KEY, "api_key", IDENTIFICATION, identificationJson);

            assertEquals(Body.of(expectedBody, BodyContentType.FORM_URL_ENCODED), bodyArgumentCaptor.getValue());
        }
    }
}
