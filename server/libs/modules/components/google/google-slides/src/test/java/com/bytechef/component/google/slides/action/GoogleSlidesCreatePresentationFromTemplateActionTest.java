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
import static com.bytechef.component.google.slides.constant.GoogleSlidesConstants.VALUES;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.FILE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.drive.model.File;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleSlidesCreatePresentationFromTemplateActionTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Object mockedObject = mock(Object.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(
        FILE_ID, "123", NAME, "newFileName",
        VALUES, Map.of("key1", "value1", "key2", "value2")));
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);

    @Test
    void testPerform() throws Exception {
        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedObject);

        try (MockedStatic<GoogleUtils> googleUtilsMockedStatic = mockStatic(GoogleUtils.class)) {
            googleUtilsMockedStatic
                .when(() -> GoogleUtils.copyFileOnGoogleDrive(
                    parametersArgumentCaptor.capture(), parametersArgumentCaptor.capture()))
                .thenReturn(new File().setId("destinationFile"));

            Object result = GoogleSlidesCreatePresentationFromTemplateAction.perform(mockedParameters,
                mockedParameters, mockedActionContext);

            assertEquals(mockedObject, result);
            assertEquals(List.of(mockedParameters, mockedParameters), parametersArgumentCaptor.getAllValues());

            Http.Body body = bodyArgumentCaptor.getValue();

            assertEquals(Map.of("requests", List.of(
                Map.of(
                    "replaceAllText", Map.of(
                        "replaceText", "value1",
                        "containsText", Map.of("text", "[[key1]]", "matchCase", true))),
                Map.of(
                    "replaceAllText", Map.of(
                        "replaceText", "value2",
                        "containsText", Map.of("text", "[[key2]]", "matchCase", true))))),
                body.getContent());
        }
    }
}
