/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.google.docs.action;

import static com.bytechef.component.google.docs.constant.GoogleDocsConstants.IMAGES;
import static com.bytechef.component.google.docs.constant.GoogleDocsConstants.VALUES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.docs.util.GoogleDocsUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.ReplaceAllTextRequest;
import com.google.api.services.docs.v1.model.ReplaceImageRequest;
import com.google.api.services.docs.v1.model.Request;
import com.google.api.services.docs.v1.model.SubstringMatchCriteria;
import com.google.api.services.drive.model.File;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleDocsCreateDocumentFromTemplateActionTest {

    private final ArgumentCaptor<Docs> docsArgumentCaptor = ArgumentCaptor.forClass(Docs.class);
    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Docs mockedDocs = mock(Docs.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(VALUES, Map.of("textKey1", "textValue1"), IMAGES, Map.of("imageId1", "url1")));
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    @SuppressWarnings("unchecked")
    void perform() throws Exception {
        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleDocsUtils> googleDocsUtilsMockedStatic = mockStatic(GoogleDocsUtils.class);
            MockedStatic<GoogleUtils> googleUtilsMockedStatic = mockStatic(GoogleUtils.class)) {

            googleServicesMockedStatic
                .when(() -> GoogleServices.getDocs(parametersArgumentCaptor.capture()))
                .thenReturn(mockedDocs);
            googleUtilsMockedStatic
                .when(() -> GoogleUtils.copyFileOnGoogleDrive(
                    parametersArgumentCaptor.capture(), parametersArgumentCaptor.capture()))
                .thenReturn(new File().setId("destinationFile"));

            googleDocsUtilsMockedStatic
                .when(() -> GoogleDocsUtils.writeToDocument(
                    docsArgumentCaptor.capture(), stringArgumentCaptor.capture(), listArgumentCaptor.capture()))
                .thenAnswer(Answers.RETURNS_DEFAULTS);

            Object result = GoogleDocsCreateDocumentFromTemplateAction.perform(
                mockedParameters, mockedParameters, mockedActionContext);

            assertNull(result);
            assertEquals(
                List.of(mockedParameters, mockedParameters, mockedParameters),
                parametersArgumentCaptor.getAllValues());
            assertEquals(mockedDocs, docsArgumentCaptor.getValue());
            assertEquals("destinationFile", stringArgumentCaptor.getValue());

            Request replaceAllText = new Request()
                .setReplaceAllText(
                    new ReplaceAllTextRequest()
                        .setContainsText(
                            new SubstringMatchCriteria()
                                .setText("[[textKey1]]")
                                .setMatchCase(true))
                        .setReplaceText("textValue1"));

            Request replaceImage = new Request()
                .setReplaceImage(
                    new ReplaceImageRequest()
                        .setImageObjectId("imageId1")
                        .setUri("url1"));

            assertEquals(List.of(replaceAllText, replaceImage), listArgumentCaptor.getValue());
        }
    }
}
