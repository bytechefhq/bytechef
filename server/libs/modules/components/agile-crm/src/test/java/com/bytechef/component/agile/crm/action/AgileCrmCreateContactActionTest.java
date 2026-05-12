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

package com.bytechef.component.agile.crm.action;

import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.EMAIL;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.FIRST_NAME;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.PROPERTIES;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.TAGS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.agile.crm.util.AgileCrmUtils;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
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
class AgileCrmCreateContactActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Http.Body.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(FIRST_NAME, "test", EMAIL, "test_email", TAGS, List.of("tag1", "tag2")));
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = forClass(Parameters.class);
    private final Map<String, Object> responseMap = Map.of();
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        try (MockedStatic<AgileCrmUtils> agileCrmUtilsMockedStatic = mockStatic(AgileCrmUtils.class)) {
            agileCrmUtilsMockedStatic
                .when(() -> AgileCrmUtils.getPropertiesList(parametersArgumentCaptor.capture()))
                .thenReturn(List.of());

            Map<String, Object> result = AgileCrmCreateContactAction.perform(
                mockedParameters, mockedParameters, mockedContext);

            assertEquals(responseMap, result);
            assertNotNull(httpFunctionArgumentCaptor.getValue());
            assertEquals("/contacts", stringArgumentCaptor.getValue());

            Map<String, Object> expectedBody = Map.of(TAGS, List.of("tag1", "tag2"), PROPERTIES, List.of());

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Configuration configuration = configurationBuilder.build();

            assertEquals(ResponseType.JSON, configuration.getResponseType());
            assertEquals(Body.of(expectedBody, Http.BodyContentType.JSON), bodyArgumentCaptor.getValue());
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
        }
    }
}
