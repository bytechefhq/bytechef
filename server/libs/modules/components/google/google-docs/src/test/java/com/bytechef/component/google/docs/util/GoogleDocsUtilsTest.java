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

package com.bytechef.component.google.docs.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.BatchUpdateDocumentRequest;
import com.google.api.services.docs.v1.model.Document;
import com.google.api.services.docs.v1.model.Request;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Domiter
 */
class GoogleDocsUtilsTest {

    private final ArgumentCaptor<BatchUpdateDocumentRequest> batchUpdateDocumentRequestArgumentCaptor =
        ArgumentCaptor.forClass(BatchUpdateDocumentRequest.class);
    private final ArgumentCaptor<Document> documentArgumentCaptor = ArgumentCaptor.forClass(Document.class);
    private final ArgumentCaptor<String> documentIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final Docs.Documents.BatchUpdate mockedBatchUpdate = mock(Docs.Documents.BatchUpdate.class);
    private final Docs.Documents.Create mockedCreate = mock(Docs.Documents.Create.class);
    private final Docs mockedDocs = mock(Docs.class);
    private final Document mockedDocument = mock(Document.class);
    private final Docs.Documents mockedDocuments = mock(Docs.Documents.class);
    @SuppressWarnings("unchecked")
    private final List<Request> mockedList = mock(List.class);

    @Test
    void testCreateDocument() throws IOException {
        when(mockedDocs.documents())
            .thenReturn(mockedDocuments);
        when(mockedDocuments.create(documentArgumentCaptor.capture()))
            .thenReturn(mockedCreate);
        when(mockedCreate.execute())
            .thenReturn(mockedDocument);

        Document result = GoogleDocsUtils.createDocument("Title", mockedDocs);

        assertEquals(mockedDocument, result);
        assertEquals("Title", documentArgumentCaptor.getValue().getTitle());
    }

    @Test
    void testWriteToDocument() throws IOException {
        when(mockedDocs.documents())
            .thenReturn(mockedDocuments);
        when(mockedDocuments.batchUpdate(
            documentIdArgumentCaptor.capture(), batchUpdateDocumentRequestArgumentCaptor.capture()))
            .thenReturn(mockedBatchUpdate);

        GoogleDocsUtils.writeToDocument(mockedDocs, "id", mockedList);

        assertEquals("id", documentIdArgumentCaptor.getValue());
        assertEquals(mockedList, batchUpdateDocumentRequestArgumentCaptor.getValue().getRequests());
    }
}
