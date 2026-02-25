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

package com.bytechef.component.microsoft.outlook.util;

import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ADDRESS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.BCC_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.BODY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CC_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CONTENT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CONTENT_BYTES;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CONTENT_TYPE;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.EMAIL_ADDRESS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.FROM;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.FULL_MESSAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ID;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.NAME;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SIMPLE_MESSAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SUBJECT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.TO_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.VALUE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
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
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.microsoft.outlook.definition.Format;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils.SimpleMessage;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class MicrosoftOutlook365UtilsTest {

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<File, ?>> fileFunctionArgumentCaptor = forClass(ContextFunction.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Encoder, ?>> encderFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final File mockedFile = mock(File.class);
    private final Encoder mockedEncoder = mock(Encoder.class);
    private final Context mockedContext = mock(Context.class);
    private final ArgumentCaptor<FileEntry> fileEntryArgumentCaptor = forClass(FileEntry.class);
    private final ArgumentCaptor<byte[]> bytesArgumentCaptor = forClass(byte[].class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<InputStream> inputStreamArgumentCaptor = forClass(InputStream.class);

    @Test
    void testCreateRecipientList() {
        List<Map<String, Map<String, String>>> result = MicrosoftOutlook365Utils.createRecipientList(
            List.of("address1", "address2"));

        assertEquals(
            List.of(
                Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "address1")),
                Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "address2"))),
            result);
    }

    @ExtendWith(MockContextSetupExtension.class)
    @Test
    void testCreateSimpleMessage(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) throws IOException {

        byte[] fileContent = new byte[] {
            1, 2, 3
        };
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(VALUE, List.of(
                Map.of(CONTENT_BYTES, "encode", "isInline", false, NAME, "file1"),
                Map.of(CONTENT_BYTES, "encode", "isInline", true, NAME, "file2"))));

        when(mockedContext.encoder(encderFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Encoder, ?> value = encderFunctionArgumentCaptor.getValue();

                return value.apply(mockedEncoder);
            });
        when(mockedEncoder.base64Decode(stringArgumentCaptor.capture()))
            .thenReturn(fileContent);
        when(mockedContext.file(fileFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<File, ?> value = fileFunctionArgumentCaptor.getValue();

                return value.apply(mockedFile);
            });
        when(mockedFile.storeContent(stringArgumentCaptor.capture(), inputStreamArgumentCaptor.capture()))
            .thenReturn(mockedFileEntry);

        Map<String, Object> messageBody = new HashMap<>(
            Map.of(
                ID, "messageId",
                SUBJECT, "Test Subject",
                FROM, Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "test@mail.com")),
                BODY, Map.of(CONTENT, "Hello World!"),
                "hasAttachments", true,
                TO_RECIPIENTS, List.of(Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "address1"))),
                CC_RECIPIENTS, List.of(Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "address2"))),
                BCC_RECIPIENTS, List.of(Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "address3"))),
                "bodyPreview", "Hello World!",
                "webLink", "https://example.com"));

        messageBody.put("conversationId", "conversationId");

        SimpleMessage result = MicrosoftOutlook365Utils.createSimpleMessage(mockedContext, messageBody);

        assertEquals(
            new SimpleMessage(
                "messageId",
                "conversationId",
                "Test Subject",
                "test@mail.com",
                List.of("address1"),
                List.of("address2"),
                List.of("address3"),
                "Hello World!",
                "Hello World!",
                List.of(mockedFileEntry),
                List.of(mockedFileEntry),
                "https://example.com"),
            result);

        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertNotNull(fileFunctionArgumentCaptor.getValue());
        assertNotNull(encderFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals(
            List.of("/me/messages/messageId/attachments", "encode", "file1", "encode", "file2"),
            stringArgumentCaptor.getAllValues());

        List<InputStream> allValues = inputStreamArgumentCaptor.getAllValues();

        for (InputStream inputStream : allValues) {
            assertArrayEquals(fileContent, inputStream.readAllBytes());
        }
    }

    @Test
    void testGetAttachments() throws IOException {
        List<FileEntry> fileEntries = List.of(mockedFileEntry);

        byte[] fileContent = new byte[] {
            1, 2, 3
        };

        when(mockedFileEntry.getName())
            .thenReturn("file.txt");
        when(mockedFileEntry.getMimeType())
            .thenReturn("text/plain");

        when(mockedContext.file(fileFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<File, ?> value = fileFunctionArgumentCaptor.getValue();

                return value.apply(mockedFile);
            });
        when(mockedFile.readAllBytes(fileEntryArgumentCaptor.capture()))
            .thenReturn(fileContent);

        when(mockedContext.encoder(encderFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Encoder, ?> value = encderFunctionArgumentCaptor.getValue();

                return value.apply(mockedEncoder);
            });
        String encodeToString = EncodingUtils.base64EncodeToString(fileContent);
        when(mockedEncoder.base64Encode(bytesArgumentCaptor.capture()))
            .thenReturn(encodeToString);

        List<Map<String, Object>> attachments = MicrosoftOutlook365Utils.getAttachments(mockedContext, fileEntries);

        assertEquals(
            List.of(
                Map.of(
                    "@odata.type", "#microsoft.graph.fileAttachment",
                    NAME, "file.txt",
                    CONTENT_TYPE, "text/plain",
                    CONTENT_BYTES, encodeToString)),
            attachments);

        assertNotNull(fileFunctionArgumentCaptor.getValue());
        assertNotNull(encderFunctionArgumentCaptor.getValue());
        assertEquals(mockedFileEntry, fileEntryArgumentCaptor.getValue());
        assertEquals(fileContent, bytesArgumentCaptor.getValue());
    }

    @ExtendWith(MockContextSetupExtension.class)
    @Test
    void testGetMailboxTImeZone(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(VALUE, "zone"));

        String result = MicrosoftOutlook365Utils.getMailboxTimeZone(mockedContext);

        assertEquals("zone", result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/me/mailboxSettings/timeZone", stringArgumentCaptor.getValue());
    }

    @Test
    void tesGetMessageOutputForSimpleFormat() {
        ModifiableObjectProperty messageOutputProperty = MicrosoftOutlook365Utils.getMessageOutputProperty(
            Format.SIMPLE);

        assertEquals(SIMPLE_MESSAGE_OUTPUT_PROPERTY, messageOutputProperty);
    }

    @Test
    void testGetMessageOutputForFullFormat() {
        ModifiableObjectProperty messageOutputProperty = MicrosoftOutlook365Utils.getMessageOutputProperty(Format.FULL);

        assertEquals(FULL_MESSAGE_OUTPUT_PROPERTY, messageOutputProperty);
    }
}
