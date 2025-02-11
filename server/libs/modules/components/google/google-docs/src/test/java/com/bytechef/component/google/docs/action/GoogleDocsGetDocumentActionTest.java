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

import static com.bytechef.component.google.docs.constant.GoogleDocsConstants.DOCUMENT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.Document;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleDocsGetDocumentActionTest {

    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Docs mockedDocs = mock(Docs.class);
    private final Document mockedDocument = mock(Document.class);
    private final Docs.Documents mockedDocuments = mock(Docs.Documents.class);
    private final Docs.Documents.Get mockedGet = mock(Docs.Documents.Get.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(DOCUMENT_ID, "documentId"));
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);

    @Test
    void perform() throws IOException {
        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {

            googleServicesMockedStatic
                .when(() -> GoogleServices.getDocs(parametersArgumentCaptor.capture()))
                .thenReturn(mockedDocs);

            when(mockedDocs.documents())
                .thenReturn(mockedDocuments);
            when(mockedDocuments.get(stringArgumentCaptor.capture()))
                .thenReturn(mockedGet);
            when(mockedGet.execute())
                .thenReturn(mockedDocument);

            Object result =
                GoogleDocsGetDocumentAction.perform(mockedParameters, mockedParameters, mockedActionContext);

            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals(mockedDocument, result);
            assertEquals("documentId", stringArgumentCaptor.getValue());
        }
    }
}
