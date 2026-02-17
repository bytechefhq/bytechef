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

package com.bytechef.component.google.forms.action;

import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.FORM_ID;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.RESPONSE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.google.forms.util.GoogleFormsUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Vihar Shah
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class GoogleFormsGetResponseActionTest {

    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<Map> mapArgumentCaptor = forClass(Map.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(FORM_ID, "formId", RESPONSE_ID, "responseId"));
    private final Map<String, Object> map = new HashMap<>();

    @Test
    @SuppressWarnings("unchecked")
    void testPerform(
        ActionContext mockedActionContext, Http.Executor mockedExecutor, Http.Response mockedResponse, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        try (MockedStatic<GoogleFormsUtils> googleFormsUtilsMockedStatic = mockStatic(GoogleFormsUtils.class)) {
            googleFormsUtilsMockedStatic
                .when(() -> GoogleFormsUtils.createCustomResponse(
                    contextArgumentCaptor.capture(), stringArgumentCaptor.capture(), mapArgumentCaptor.capture()))
                .thenReturn(map);

            Map<String, Object> result = GoogleFormsGetResponseAction.perform(
                mockedParameters, null, mockedActionContext);

            assertEquals(map, result);

            ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

            assertNotNull(capturedFunction);

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

            Http.Configuration configuration = configurationBuilder.build();

            Http.ResponseType responseType = configuration.getResponseType();

            assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
            assertEquals(
                List.of("/forms/formId/responses/responseId", "formId"), stringArgumentCaptor.getAllValues());
            assertEquals(mockedActionContext, contextArgumentCaptor.getValue());
            assertEquals(map, mapArgumentCaptor.getValue());
        }
    }
}
