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

package com.bytechef.component.google.slides.action;

import static com.bytechef.component.google.slides.constant.GoogleSlidesConstants.NAME;
import static com.bytechef.component.google.slides.constant.GoogleSlidesConstants.PLACEHOLDER_FORMAT;
import static com.bytechef.component.google.slides.constant.GoogleSlidesConstants.VALUES;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.drive.model.File;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class GoogleSlidesCreatePresentationFromTemplateActionTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = forClass(Http.Body.class);
    private final Object mockedObject = mock(Object.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(
        FILE_ID, "123", NAME, "newFileName", PLACEHOLDER_FORMAT, "[[]]",
        VALUES, Map.of("key1", "value1", "key2", "value2")));
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Http.Executor mockedExecutor, Http.Response mockedResponse, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(mockedObject);

        try (MockedStatic<GoogleUtils> googleUtilsMockedStatic = mockStatic(GoogleUtils.class)) {
            googleUtilsMockedStatic
                .when(() -> GoogleUtils.copyFileOnGoogleDrive(
                    parametersArgumentCaptor.capture(), parametersArgumentCaptor.capture()))
                .thenReturn(new File().setId("destinationFile"));

            Object result = GoogleSlidesCreatePresentationFromTemplateAction.perform(
                mockedParameters, mockedParameters, mockedContext);

            assertEquals(mockedObject, result);
            assertEquals(List.of(mockedParameters, mockedParameters), parametersArgumentCaptor.getAllValues());

            ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

            assertNotNull(capturedFunction);

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

            Http.Configuration configuration = configurationBuilder.build();

            Http.ResponseType responseType = configuration.getResponseType();

            assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
            assertEquals("/presentations/destinationFile:batchUpdate", stringArgumentCaptor.getValue());

            Map<String, List<Map<String, Map<String, Object>>>> expectedBody = Map.of("requests", List.of(
                Map.of(
                    "replaceAllText", Map.of(
                        "replaceText", "value1",
                        "containsText", Map.of("text", "[[key1]]", "matchCase", true))),
                Map.of(
                    "replaceAllText", Map.of(
                        "replaceText", "value2",
                        "containsText", Map.of("text", "[[key2]]", "matchCase", true)))));

            assertEquals(Http.Body.of(expectedBody, Http.BodyContentType.JSON), bodyArgumentCaptor.getValue());
        }
    }
}
