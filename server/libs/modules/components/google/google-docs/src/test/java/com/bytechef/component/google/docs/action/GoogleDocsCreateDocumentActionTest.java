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

import static com.bytechef.component.google.docs.constant.GoogleDocsConstants.BODY;
import static com.bytechef.component.google.docs.constant.GoogleDocsConstants.TITLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.google.docs.util.GoogleDocsUtils;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.Document;
import com.google.api.services.docs.v1.model.InsertTextRequest;
import com.google.api.services.docs.v1.model.Request;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
class GoogleDocsCreateDocumentActionTest extends AbstractGoogleDocsActionTest {

    private final ArgumentCaptor<String> documentIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> requestsArgumentCaptor = ArgumentCaptor.forClass(List.class);
    private final ArgumentCaptor<String> titleArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    @SuppressWarnings("unchecked")
    void perform() throws IOException {
        when(mockedParameters.getRequiredString(TITLE))
            .thenReturn("title");
        when(mockedParameters.getRequiredString(BODY))
            .thenReturn("text");

        Document document = new Document().setDocumentId("123");

        try (MockedStatic<GoogleDocsUtils> googleDocsUtilsMockedStatic = mockStatic(GoogleDocsUtils.class)) {
            googleDocsUtilsMockedStatic
                .when(() -> GoogleDocsUtils.createDocument(titleArgumentCaptor.capture(), any(Docs.class)))
                .thenReturn(document);
            googleDocsUtilsMockedStatic
                .when(() -> GoogleDocsUtils.writeToDocument(any(Docs.class), documentIdArgumentCaptor.capture(), requestsArgumentCaptor.capture()))
                .thenAnswer(Answers.RETURNS_DEFAULTS);

            Object result = GoogleDocsCreateDocumentAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertNull(result);

            assertEquals("title", titleArgumentCaptor.getValue());
            assertEquals("123", documentIdArgumentCaptor.getValue());

            List<Request> requests = requestsArgumentCaptor.getValue();

            assertEquals(1, requests.size());

            InsertTextRequest insertTextRequest = requests.getFirst().getInsertText();

            assertEquals("text", insertTextRequest.getText());
            assertTrue(insertTextRequest.getEndOfSegmentLocation().isEmpty());
        }
    }
}
