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

package com.bytechef.component.google.slides.util;

import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.slides.constant.GoogleSlidesConstants.PLACEHOLDER_FORMAT;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class GoogleSlidesUtilsTest {

    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(FILE_ID, "presentationId", PLACEHOLDER_FORMAT, "[[]]"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testCreatePropertiesForPlaceholderVariables(
        Context mockedContext, Http.Executor mockedExecutor, Http.Response mockedResponse, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, Object> presentationBody = Map.of(
            "slides", List.of(
                Map.of(
                    "pageElements", List.of(
                        Map.of(
                            "shape", Map.of(
                                "text", Map.of(
                                    "textElements", List.of(
                                        Map.of(
                                            "textRun", Map.of(
                                                "content", "Hello [[ name ]]!"))))))))));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(presentationBody);

        List<ModifiableValueProperty<?, ?>> properties = GoogleSlidesUtils.createPropertiesForPlaceholderVariables(
            mockedParameters, mock(Parameters.class), Map.of(), mockedContext);

        assertEquals(
            List.of(
                string("name")
                    .label("name")
                    .description("Value for \"[[name]]\"")
                    .required(false)),
            properties);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Http.Configuration configuration = configurationBuilder.build();

        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/presentations/presentationId", stringArgumentCaptor.getValue());
    }
}
