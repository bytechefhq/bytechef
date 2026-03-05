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

package com.bytechef.component.google.docs.action;

import static com.bytechef.component.google.docs.constant.GoogleDocsConstants.VALUES;
import static com.bytechef.google.commons.constant.GoogleCommonsContants.PLACEHOLDER_FORMAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.docs.util.GoogleDocsUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentResponse;
import com.google.api.services.docs.v1.model.Document;
import com.google.api.services.docs.v1.model.ReplaceAllTextRequest;
import com.google.api.services.docs.v1.model.Request;
import com.google.api.services.docs.v1.model.SubstringMatchCriteria;
import com.google.api.services.drive.model.File;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleDocsCreateDocumentFromTemplateActionTest {

    private final ArgumentCaptor<Docs> docsArgumentCaptor = forClass(Docs.class);
    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> listArgumentCaptor = forClass(List.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Parameters mockedConnectionParameters = mock(Parameters.class);
    private final Docs mockedDocs = mock(Docs.class);
    private final Document mockedDocument = mock(Document.class);
    private final Parameters mockedInputParameters = MockParametersFactory.create(
        Map.of(PLACEHOLDER_FORMAT, "[[]]", VALUES, Map.of("textKey1", "textValue1")));
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    @SuppressWarnings("unchecked")
    void testPerform() {
        BatchUpdateDocumentResponse batchUpdateDocumentResponse =
            new BatchUpdateDocumentResponse().setDocumentId("123");

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
                .thenReturn(batchUpdateDocumentResponse);
            googleDocsUtilsMockedStatic
                .when(() -> GoogleDocsUtils.getDocument(docsArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(mockedDocument);

            Document result = GoogleDocsCreateDocumentFromTemplateAction.perform(
                mockedInputParameters, mockedConnectionParameters, mockedActionContext);

            assertEquals(mockedDocument, result);
            assertEquals(
                List.of(mockedConnectionParameters, mockedConnectionParameters, mockedInputParameters),
                parametersArgumentCaptor.getAllValues());
            assertEquals(List.of(mockedDocs, mockedDocs), docsArgumentCaptor.getAllValues());
            assertEquals(List.of("destinationFile", "123"), stringArgumentCaptor.getAllValues());

            Request replaceAllText = new Request()
                .setReplaceAllText(
                    new ReplaceAllTextRequest()
                        .setContainsText(
                            new SubstringMatchCriteria()
                                .setText("[[textKey1]]")
                                .setMatchCase(true))
                        .setReplaceText("textValue1"));

            assertEquals(List.of(replaceAllText), listArgumentCaptor.getValue());
        }
    }
}
