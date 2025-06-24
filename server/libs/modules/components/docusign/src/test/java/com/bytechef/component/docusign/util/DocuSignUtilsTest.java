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

package com.bytechef.component.docusign.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.docusign.constant.DocuSignConstants.ACCOUNT_ID;
import static com.bytechef.component.docusign.constant.DocuSignConstants.DOCUMENT_ID;
import static com.bytechef.component.docusign.constant.DocuSignConstants.EMAIL_SUBJECT;
import static com.bytechef.component.docusign.constant.DocuSignConstants.ENVELOPE_ID;
import static com.bytechef.component.docusign.constant.DocuSignConstants.FROM_DATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.docusign.constant.DocuSignConstants.DocumentRecord;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
class DocuSignUtilsTest {

    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(
        ACCOUNT_ID, List.of(), FROM_DATE, LocalDate.of(2025, 6, 4), ENVELOPE_ID, "1"));
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testGetAuthorizationUrlForDemoEnvironment() {
        String actualUrl = DocuSignUtils.getAuthorizationUrl("demo");

        assertEquals("https://account-d.docusign.com/oauth/", actualUrl);
    }

    @Test
    void testGetAuthorizationUrlForOtherEnvironment() {
        String actualUrl = DocuSignUtils.getAuthorizationUrl("production");

        assertEquals("https://account.docusign.com/oauth/", actualUrl);
    }

    @Test
    void testGetDocumentsList() {
        FileEntry mockFileEntry = mock(FileEntry.class);
        byte[] mockedByteArray = {
            1, 1, 1
        };

        String encodeToString = EncodingUtils.base64EncodeToString(mockedByteArray);

        List<DocumentRecord> documentRecordList = List.of(new DocumentRecord(mockFileEntry, "name", 1));

        when(mockedContext.file(any()))
            .thenReturn(mockedByteArray);
        when(mockedContext.encoder(any()))
            .thenReturn(encodeToString);

        List<Map<String, Object>> result = DocuSignUtils.getDocumentsList(documentRecordList, mockedContext);

        List<Map<String, Object>> expected = List.of(
            Map.of(
                "documentBase64", encodeToString,
                "name", "name",
                "documentId", 1));

        assertEquals(expected, result);
    }

    @Test
    void testGetEnvelopeIdOptions() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("envelopes", List.of(Map.of(ENVELOPE_ID, "1", EMAIL_SUBJECT, "emailSubject"))));

        List<Option<String>> result = DocuSignUtils.getEnvelopeIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expected = List.of(option("emailSubject", "1"));

        assertEquals(expected, result);
        assertEquals(List.of("from_date", "2025-06-04"), stringArgumentCaptor.getAllValues());
    }

    @Test
    void testGetDocumentIdOptions() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("envelopeDocuments", List.of(Map.of(DOCUMENT_ID, "1", "name", "name"))));

        List<Option<String>> result = DocuSignUtils.getDocumentIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expected = List.of(option("name", "1"));

        assertEquals(expected, result);
    }
}
