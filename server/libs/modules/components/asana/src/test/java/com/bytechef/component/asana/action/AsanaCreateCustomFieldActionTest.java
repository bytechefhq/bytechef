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

package com.bytechef.component.asana.action;

import static com.bytechef.component.asana.constant.AsanaConstants.COLOR;
import static com.bytechef.component.asana.constant.AsanaConstants.CURRENCY_CODE;
import static com.bytechef.component.asana.constant.AsanaConstants.DATE_VALUE;
import static com.bytechef.component.asana.constant.AsanaConstants.ENABLED;
import static com.bytechef.component.asana.constant.AsanaConstants.ENUM_OPTIONS;
import static com.bytechef.component.asana.constant.AsanaConstants.FORMAT;
import static com.bytechef.component.asana.constant.AsanaConstants.INPUT_RESTRICTIONS;
import static com.bytechef.component.asana.constant.AsanaConstants.NAME;
import static com.bytechef.component.asana.constant.AsanaConstants.NUMBER_VALUE;
import static com.bytechef.component.asana.constant.AsanaConstants.PEOPLE_VALUE;
import static com.bytechef.component.asana.constant.AsanaConstants.PRECISION;
import static com.bytechef.component.asana.constant.AsanaConstants.REFERENCE_VALUE;
import static com.bytechef.component.asana.constant.AsanaConstants.RESOURCE_SUBTYPE;
import static com.bytechef.component.asana.constant.AsanaConstants.TEXT_VALUE;
import static com.bytechef.component.asana.constant.AsanaConstants.WORKSPACE;
import static com.bytechef.component.definition.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockContextSetupExtension.class)
class AsanaCreateCustomFieldActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Http.Body.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.ofEntries(
            Map.entry(WORKSPACE, "1234567890"),
            Map.entry(NAME, "test"),
            Map.entry(RESOURCE_SUBTYPE, "enum"),
            Map.entry(TEXT_VALUE, "text"),
            Map.entry(ENUM_OPTIONS, List.of(
                Map.of(
                    NAME, "option1",
                    ENABLED, true,
                    COLOR, "red"),
                Map.of(
                    NAME, "option2",
                    ENABLED, false,
                    COLOR, "blue"))),
            Map.entry(NUMBER_VALUE, 100.25),
            Map.entry(PRECISION, 2),
            Map.entry(DATE_VALUE, "2026-03-04"),
            Map.entry(PEOPLE_VALUE, List.of("user1", "user2")),
            Map.entry(REFERENCE_VALUE, List.of("reference1", "reference2")),
            Map.entry(INPUT_RESTRICTIONS, List.of("input_restriction1", "input_restriction2")),
            Map.entry(FORMAT, "currency"),
            Map.entry(CURRENCY_CODE, "USD")));
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
            .thenReturn(Map.of(OK, true));

        Object result = AsanaCreateCustomFieldAction.perform(mockedParameters, null, mockedContext);

        assertEquals(Map.of(OK, true), result);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        Http.Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/custom_fields", stringArgumentCaptor.getValue());

        Map<String, Object> expectedBody = Map.of(
            "data",
            Map.ofEntries(
                Map.entry(WORKSPACE, "1234567890"),
                Map.entry(NAME, "test"),
                Map.entry(RESOURCE_SUBTYPE, "enum"),
                Map.entry(TEXT_VALUE, "text"),
                Map.entry(ENUM_OPTIONS, List.of(
                    Map.of(
                        NAME, "option1",
                        ENABLED, true,
                        COLOR, "red"),
                    Map.of(
                        NAME, "option2",
                        ENABLED, false,
                        COLOR, "blue"))),
                Map.entry(NUMBER_VALUE, 100.25),
                Map.entry(PRECISION, 2),
                Map.entry(DATE_VALUE, "2026-03-04"),
                Map.entry(PEOPLE_VALUE, List.of("user1", "user2")),
                Map.entry(REFERENCE_VALUE, List.of("reference1", "reference2")),
                Map.entry(INPUT_RESTRICTIONS, List.of("input_restriction1", "input_restriction2")),
                Map.entry(FORMAT, "currency"),
                Map.entry(CURRENCY_CODE, "USD")));
        assertEquals(
            Http.Body.of(expectedBody,
                Http.BodyContentType.JSON),
            bodyArgumentCaptor.getValue());
    }

}
