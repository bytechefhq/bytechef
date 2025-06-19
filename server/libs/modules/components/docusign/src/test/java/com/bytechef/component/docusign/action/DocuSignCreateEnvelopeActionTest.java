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

package com.bytechef.component.docusign.action;

import static com.bytechef.component.docusign.constant.DocuSignConstants.ACCOUNT_ID;
import static com.bytechef.component.docusign.constant.DocuSignConstants.CARBON_COPIES;
import static com.bytechef.component.docusign.constant.DocuSignConstants.DOCUMENTS;
import static com.bytechef.component.docusign.constant.DocuSignConstants.EMAIL_SUBJECT;
import static com.bytechef.component.docusign.constant.DocuSignConstants.RECIPIENTS;
import static com.bytechef.component.docusign.constant.DocuSignConstants.SIGNERS;
import static com.bytechef.component.docusign.constant.DocuSignConstants.STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.docusign.constant.DocuSignConstants.DocumentRecord;
import com.bytechef.component.docusign.util.DocuSignUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Nikolina Spehar
 */
class DocuSignCreateEnvelopeActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    private final ArgumentCaptor<List<DocumentRecord>> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(
        DOCUMENTS, List.of(), STATUS, "sent", EMAIL_SUBJECT, "emailSubject", SIGNERS, List.of(), CARBON_COPIES,
        List.of(),
        ACCOUNT_ID, List.of()));
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final Map<String, Object> responseMap = Map.of();

    @Test
    void testPerform() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        try (MockedStatic<DocuSignUtils> mockedDocuSignUtils = Mockito.mockStatic(DocuSignUtils.class)) {

            mockedDocuSignUtils.when(
                () -> DocuSignUtils.getDocumentsList(listArgumentCaptor.capture(), contextArgumentCaptor.capture()))
                .thenReturn(List.of());

            Map<String, Object> result = DocuSignCreateEnvelopeAction.perform(
                mockedParameters, mockedParameters, mockedContext);

            assertEquals(Map.of(), result);

            Body body = bodyArgumentCaptor.getValue();

            Map<String, Object> expectedBody = Map.of(
                STATUS, "sent", EMAIL_SUBJECT, "emailSubject", DOCUMENTS, List.of(), RECIPIENTS,
                Map.of(SIGNERS, List.of(), CARBON_COPIES, List.of()));

            assertEquals(expectedBody, body.getContent());

            assertEquals(List.of(), listArgumentCaptor.getValue());
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
        }
    }
}
