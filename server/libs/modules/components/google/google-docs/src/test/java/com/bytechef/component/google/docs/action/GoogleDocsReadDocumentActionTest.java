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
import static org.mockito.Mockito.when;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.Document;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Domiter
 */
class GoogleDocsReadDocumentActionTest extends AbstractGoogleDocsActionTest {

    private final ArgumentCaptor<String> documentIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final Document mockedDocument = mock(Document.class);
    private final Docs.Documents mockedDocuments = mock(Docs.Documents.class);
    private final Docs.Documents.Get mockedGet = mock(Docs.Documents.Get.class);

    @Test
    void perform() throws IOException {
        when(mockedParameters.getRequiredString(DOCUMENT_ID))
            .thenReturn("documentId");

        when(mockedDocs.documents())
            .thenReturn(mockedDocuments);
        when(mockedDocuments.get(documentIdArgumentCaptor.capture()))
            .thenReturn(mockedGet);
        when(mockedGet.execute())
            .thenReturn(mockedDocument);

        Object result = GoogleDocsReadDocumentAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedDocument, result);
        assertEquals("documentId", documentIdArgumentCaptor.getValue());
    }
}
