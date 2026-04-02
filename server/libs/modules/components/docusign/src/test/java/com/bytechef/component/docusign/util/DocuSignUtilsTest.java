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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Encoder;
import com.bytechef.component.definition.Context.File;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.docusign.constant.DocuSignConstants.DocumentRecord;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
@ExtendWith(MockContextSetupExtension.class)
class DocuSignUtilsTest {

    private final Context mockedContext = mock(Context.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(
        ACCOUNT_ID, "accountId", FROM_DATE, LocalDate.of(2025, 6, 4), ENVELOPE_ID, "1"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<FileEntry> fileEntryArgumentCaptor = forClass(FileEntry.class);
    private final ArgumentCaptor<byte[]> bytesArgumentCaptor = forClass(byte[].class);

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<File, Executor>> fileFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final File mockedFile = mock(File.class);

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Encoder, Executor>> encoderFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final Encoder mockedEncoder = mock(Encoder.class);

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
    void testGetDocumentIdOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("envelopeDocuments", List.of(Map.of(DOCUMENT_ID, "1", "name", "name"))));

        List<Option<String>> result = DocuSignUtils.getDocumentIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(List.of(option("name", "1")), result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/restapi/v2.1/accounts/accountId/envelopes/1/documents", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetDocumentsList() throws IOException {
        FileEntry mockedFileEntry = mock(FileEntry.class);
        byte[] mockedByteArray = {
            1, 1, 1
        };

        String encodeToString = EncodingUtils.base64EncodeToString(mockedByteArray);

        when(mockedContext.file(fileFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<File, Executor> value = fileFunctionArgumentCaptor.getValue();

                return value.apply(mockedFile);
            });
        when(mockedFile.readAllBytes(fileEntryArgumentCaptor.capture()))
            .thenReturn(mockedByteArray);
        when(mockedContext.encoder(encoderFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Encoder, Executor> value = encoderFunctionArgumentCaptor.getValue();

                return value.apply(mockedEncoder);
            });
        when(mockedEncoder.base64Encode(bytesArgumentCaptor.capture()))
            .thenReturn(encodeToString);

        List<Map<String, Object>> result = DocuSignUtils.getDocumentsList(
            List.of(new DocumentRecord(mockedFileEntry, "name", 1)), mockedContext);

        List<Map<String, Object>> expected = List.of(
            Map.of("documentBase64", encodeToString, "name", "name", "documentId", 1));

        assertEquals(expected, result);
        assertNotNull(fileFunctionArgumentCaptor.getValue());
        assertNotNull(encoderFunctionArgumentCaptor.getValue());
        assertEquals(mockedFileEntry, fileEntryArgumentCaptor.getValue());
        assertEquals(mockedByteArray, bytesArgumentCaptor.getValue());
    }

    @Test
    void testGetEnvelopeIdOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("envelopes", List.of(Map.of(ENVELOPE_ID, "1", EMAIL_SUBJECT, "emailSubject"))));

        List<Option<String>> result = DocuSignUtils.getEnvelopeIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(List.of(option("emailSubject", "1")), result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals(
            List.of("/restapi/v2.1/accounts/accountId/envelopes", "from_date", "2025-06-04"),
            stringArgumentCaptor.getAllValues());
    }
}
