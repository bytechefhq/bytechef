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

import static com.bytechef.component.google.docs.constant.GoogleDocsConstants.DOCUMENT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.docs.util.GoogleDocsUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.Document;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleDocsGetDocumentActionTest {

    private final ArgumentCaptor<Docs> docsArgumentCaptor = forClass(Docs.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Parameters mockedConnectionParameters = mock(Parameters.class);
    private final Docs mockedDocs = mock(Docs.class);
    private final Document mockedDocument = mock(Document.class);
    private final Parameters mockedInputParameters = MockParametersFactory.create(Map.of(DOCUMENT_ID, "xy"));
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform() {
        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleDocsUtils> googleDocsUtilsMockedStatic = mockStatic(GoogleDocsUtils.class)) {

            googleServicesMockedStatic
                .when(() -> GoogleServices.getDocs(parametersArgumentCaptor.capture()))
                .thenReturn(mockedDocs);
            googleDocsUtilsMockedStatic
                .when(() -> GoogleDocsUtils.getDocument(docsArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(mockedDocument);

            Document result = GoogleDocsGetDocumentAction.perform(
                mockedInputParameters, mockedConnectionParameters, mockedActionContext);

            assertEquals(mockedDocument, result);
            assertEquals(mockedDocs, docsArgumentCaptor.getValue());
            assertEquals(mockedConnectionParameters, parametersArgumentCaptor.getValue());
            assertEquals(mockedDocument, result);
            assertEquals("xy", stringArgumentCaptor.getValue());
        }
    }
}
