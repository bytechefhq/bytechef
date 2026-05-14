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
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.EVENT_TYPE;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.IDENTIFIER;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.KEY;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.PLATFORM;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.USER_PROPERTIES;
import static com.bytechef.component.amplitude.constant.AmplitudeConstants.VALUE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.amplitude.util.AmplitudeUtils;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
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
class AmplitudeCreateAttributionEventActionTest {

    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            API_KEY, "api_key", EVENT_TYPE, "eventType", PLATFORM, "platform",
            IDENTIFIER, Map.of(KEY, "identifierKey", VALUE, "identifierValue"),
            USER_PROPERTIES, List.of(Map.of(KEY, "userPropertyKey", VALUE, "userPropertyValue"))));
    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = forClass(Object[].class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        String jsonString = "jsonString";
        String responseString = "response";

        try (MockedStatic<AmplitudeUtils> amplitudeUtilsMockedStatic = mockStatic(AmplitudeUtils.class)) {
            amplitudeUtilsMockedStatic
                .when(() -> AmplitudeUtils.getEventJson(
                    parametersArgumentCaptor.capture(), contextArgumentCaptor.capture()))
                .thenReturn(jsonString);

            when(mockedHttp.post(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedResponse.getBody(String.class))
                .thenReturn(responseString);

            String response = AmplitudeCreateAttributionEventAction.perform(
                mockedParameters, mockedParameters, mockedContext);

            assertEquals(responseString, response);
            assertNotNull(httpFunctionArgumentCaptor.getValue());
            assertEquals("/attribution", stringArgumentCaptor.getValue());

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Configuration configuration = configurationBuilder.build();

            assertEquals(ResponseType.TEXT, configuration.getResponseType());
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals(mockedContext, contextArgumentCaptor.getValue());

            Object[] queryParameters = {
                API_KEY, "api_key", "event", jsonString
            };

            assertArrayEquals(queryParameters, objectsArgumentCaptor.getValue());
        }
    }
}
