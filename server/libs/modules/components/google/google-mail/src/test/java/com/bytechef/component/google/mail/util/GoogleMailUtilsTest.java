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

package com.bytechef.component.google.mail.util;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.BODY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.BODY_TYPE;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FORMAT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FULL_MESSAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.METADATA_HEADERS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SUBJECT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.TO;
import static com.bytechef.component.google.mail.util.GoogleMailUtils.METADATA_MESSAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.google.mail.util.GoogleMailUtils.MINIMAL_MESSAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.google.mail.util.GoogleMailUtils.RAW_MESSAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.google.mail.util.GoogleMailUtils.SIMPLE_MESSAGE_OUTPUT_PROPERTY;
import static java.util.Base64.getUrlDecoder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.mail.definition.Format;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.Gmail.Users.Messages.Attachments;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import jakarta.mail.BodyPart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleMailUtilsTest {

    private final ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Gmail mockedGmail = mock(Gmail.class);
    private final Gmail.Users.Labels mockedLabels = mock(Gmail.Users.Labels.class);
    private final Gmail.Users.Labels.List mockedLabelsList = mock(Gmail.Users.Labels.List.class);
    private Parameters parameters;
    private final Gmail.Users mockedUsers = mock(Gmail.Users.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<List> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
    private final Gmail.Users.Messages.Get mockedGet = mock(Gmail.Users.Messages.Get.class);
    private final Message mockedMessage = mock(Message.class);
    private final Gmail.Users.Messages.Send mockedSend = mock(Gmail.Users.Messages.Send.class);
    private final Gmail.Users.Messages mockedMessages = mock(Gmail.Users.Messages.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);

    @Test
    void testGetLabelOptions() throws IOException {
        parameters = MockParametersFactory.create(Map.of(ACCESS_TOKEN, "id"));

        List<Label> labels = List.of(new Label().setName("label1")
            .setId("label1"),
            new Label().setName("label2")
                .setId("label2"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getMail(parametersArgumentCaptor.capture()))
                .thenReturn(mockedGmail);
            when(mockedGmail.users())
                .thenReturn(mockedUsers);
            when(mockedUsers.labels())
                .thenReturn(mockedLabels);
            when(mockedLabels.list(stringArgumentCaptor.capture()))
                .thenReturn(mockedLabelsList);
            when(mockedLabelsList.execute())
                .thenReturn(new ListLabelsResponse().setLabels(labels));

            List<Option<String>> result = GoogleMailUtils.getLabelOptions(
                parameters, parameters, Map.of(), anyString(), mockedActionContext);

            List<Option<String>> expectedOptions = List.of(option("label1", "label1"), option("label2", "label2"));

            assertEquals(expectedOptions, result);
            assertEquals(parameters, parametersArgumentCaptor.getValue());
            assertEquals(ME, stringArgumentCaptor.getValue());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetMessage() throws IOException {
        parameters = MockParametersFactory.create(
            Map.of(FORMAT, Format.FULL, ID, "id", METADATA_HEADERS, List.of("metadata")));

        Message message = new Message().setId("id");

        when(mockedGmail.users())
            .thenReturn(mockedUsers);
        when(mockedUsers.messages())
            .thenReturn(mockedMessages);
        when(mockedMessages.get(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedGet);
        when(mockedGet.setFormat(stringArgumentCaptor.capture()))
            .thenReturn(mockedGet);
        when(mockedGet.setMetadataHeaders(listArgumentCaptor.capture()))
            .thenReturn(mockedGet);
        when(mockedGet.execute())
            .thenReturn(message);

        Message result = GoogleMailUtils.getMessage(parameters, mockedGmail);

        assertEquals(message, result);
        assertEquals(List.of(ME, "id", Format.FULL.getMapping()), stringArgumentCaptor.getAllValues());
        assertEquals(List.of("metadata"), listArgumentCaptor.getValue());
    }

    @Test
    void testGetSimpleMessage() {
        Message message = new Message()
            .setId("id")
            .setThreadId("threadId")
            .setHistoryId(new BigInteger("123"))
            .setPayload(
                new MessagePart()
                    .setBody(new MessagePartBody().setData("ZW1haWwgYm9keQ=="))
                    .setHeaders(List.of(new MessagePartHeader().setName("Subject")
                        .setValue("email subject")))
                    .setParts(List.of(new MessagePart()
                        .setMimeType("text/plain"))));

        GoogleMailUtils.SimpleMessage result = GoogleMailUtils.getSimpleMessage(
            message, mockedActionContext, mockedGmail);

        GoogleMailUtils.SimpleMessage expectedSimpleMessage =
            new GoogleMailUtils.SimpleMessage("id", "threadId", new BigInteger("123"),
                "email subject", null, List.of(), List.of(), List.of(), "email body", "",
                List.of(), List.of(), "https://mail.google.com/mail/u/0/#all/id");

        assertEquals(expectedSimpleMessage, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetSimpleMessageWithAttachments() throws Exception {
        Message message = new Message()
            .setId("id")
            .setThreadId("threadId")
            .setHistoryId(new BigInteger("123"))
            .setPayload(
                new MessagePart()
                    .setMimeType("multipart/mixed")
                    .setHeaders(List.of(
                        new MessagePartHeader()
                            .setName("Subject")
                            .setValue("email subject"),
                        new MessagePartHeader()
                            .setName("Content-Type")
                            .setValue("multipart/mixed")))
                    .setParts(List.of(
                        new MessagePart()
                            .setMimeType("text/plain")
                            .setBody(new MessagePartBody().setData("ZW1haWwgYm9keQ==")),
                        new MessagePart()
                            .setMimeType("application/pdf")
                            .setFilename(null)
                            .setBody(new MessagePartBody().setAttachmentId("attachmentId1")),
                        new MessagePart()
                            .setMimeType("application/octet-stream")
                            .setFilename("noextension")
                            .setBody(new MessagePartBody().setAttachmentId("attachmentId2")))));

        Attachments mockedAttachments = mock(Attachments.class);
        Attachments.Get mockedAttachmentsGet = mock(Attachments.Get.class);

        when(mockedGmail.users()).thenReturn(mockedUsers);
        when(mockedUsers.messages()).thenReturn(mockedMessages);
        when(mockedMessages.attachments()).thenReturn(mockedAttachments);
        when(mockedAttachments.get(anyString(), anyString(), anyString())).thenReturn(mockedAttachmentsGet);
        when(mockedAttachmentsGet.execute()).thenReturn(new MessagePartBody().setData("YXRhY2htZW50IGNvbnRlbnQ="));

        Context.File mockedFile = mock(Context.File.class);

        when(mockedActionContext.file(any())).thenAnswer(invocation -> {
            Context.ContextFunction<Context.File, ?> function = invocation.getArgument(0);

            return function.apply(mockedFile);
        });

        FileEntry mockedFileEntry1 = mock(FileEntry.class);
        FileEntry mockedFileEntry2 = mock(FileEntry.class);

        when(mockedFile.storeContent(anyString(), any(InputStream.class))).thenReturn(
            mockedFileEntry1, mockedFileEntry2);

        GoogleMailUtils.SimpleMessage result = GoogleMailUtils.getSimpleMessage(
            message, mockedActionContext, mockedGmail);

        assertNotNull(result);

        List<FileEntry> attachments = result.attachments();

        assertEquals(2, attachments.size());

        ArgumentCaptor<String> fileNameCaptor = ArgumentCaptor.forClass(String.class);

        verify(mockedFile, times(2)).storeContent(fileNameCaptor.capture(), any(InputStream.class));

        List<String> capturedFileNames = fileNameCaptor.getAllValues();

        assertEquals("attachment", capturedFileNames.get(0));
        assertEquals("noextension", capturedFileNames.get(1));
    }

    @Test
    void tesGetMessageOutputForSimpleFormat() {
        ModifiableObjectProperty messageOutputProperty = GoogleMailUtils.getMessageOutputProperty(Format.SIMPLE);

        assertEquals(SIMPLE_MESSAGE_OUTPUT_PROPERTY, messageOutputProperty);
    }

    @Test
    void testGetMessageOutputForRawFormat() {
        ModifiableObjectProperty messageOutputProperty = GoogleMailUtils.getMessageOutputProperty(Format.RAW);

        assertEquals(RAW_MESSAGE_OUTPUT_PROPERTY, messageOutputProperty);
    }

    @Test
    void testGetMessageOutputForMinimalFormat() {
        ModifiableObjectProperty messageOutputProperty = GoogleMailUtils.getMessageOutputProperty(Format.MINIMAL);

        assertEquals(MINIMAL_MESSAGE_OUTPUT_PROPERTY, messageOutputProperty);
    }

    @Test
    void testGetMessageOutputForMetadataFormat() {
        ModifiableObjectProperty messageOutputProperty = GoogleMailUtils.getMessageOutputProperty(Format.METADATA);

        assertEquals(METADATA_MESSAGE_OUTPUT_PROPERTY, messageOutputProperty);
    }

    @Test
    void testGetMessageOutputForFullFormat() {
        ModifiableObjectProperty messageOutputProperty = GoogleMailUtils.getMessageOutputProperty(Format.FULL);

        assertEquals(FULL_MESSAGE_OUTPUT_PROPERTY, messageOutputProperty);
    }

    @Test
    void testSendMail() throws IOException {
        when(mockedGmail.users())
            .thenReturn(mockedUsers);
        when(mockedUsers.messages())
            .thenReturn(mockedMessages);
        when(mockedMessages.send(stringArgumentCaptor.capture(), messageArgumentCaptor.capture()))
            .thenReturn(mockedSend);
        when(mockedSend.execute())
            .thenReturn(mockedMessage);

        Message result = GoogleMailUtils.sendMail(mockedGmail, mockedMessage);

        assertEquals(mockedMessage, result);
        assertEquals(ME, stringArgumentCaptor.getValue());
    }

    @Test
    void testGetEncodedEmailWithoutReplyAndAttachments() throws Exception {
        String croatianText = "Pozdrav - šđ!";
        String body = "Mail s hrvatskim slovima: š, č, ć, đ, Ž.";

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(TO, List.of("to@example.com"), SUBJECT, croatianText, BODY, body, BODY_TYPE, "plain"));

        String encodedEmail = GoogleMailUtils.getEncodedEmail(mockedParameters, mockedActionContext, null);

        byte[] raw = getUrlDecoder().decode(encodedEmail);

        MimeMessage mimeMessage = new MimeMessage(
            Session.getDefaultInstance(new Properties()), new ByteArrayInputStream(raw));

        assertEquals(croatianText, mimeMessage.getSubject());

        Object content = mimeMessage.getContent();

        if (!(content instanceof MimeMultipart mimeMultipart)) {
            throw new AssertionError("Expected MimeMultipart content but was: " + content);
        }

        BodyPart bodyPart = mimeMultipart.getBodyPart(0);

        String partContentType = bodyPart.getContentType();

        assertNotNull(partContentType);

        String upperCT = partContentType.toUpperCase();

        assertTrue(upperCT.contains("UTF-8"), "Content-Type should include UTF-8 but was: " + partContentType);
        assertEquals(body, bodyPart.getContent());
    }

    @Test
    void testGetEncodedEmailWithReplyMessageNoAttachments() throws Exception {
        String replyMessageId = "<message-id-123@example.com>";
        String originalSubject = "Re: Tema s đ i ž";
        String newBody = "Odgovor s dijakritičkim znakovima: čćžšđ.";

        MessagePartHeader messagePartHeader = new MessagePartHeader()
            .setName("Message-ID")
            .setValue(replyMessageId);

        MessagePartHeader messagePartHeader1 = new MessagePartHeader()
            .setName("Subject")
            .setValue(originalSubject);
        MessagePart messagePart = new MessagePart()
            .setHeaders(List.of(messagePartHeader, messagePartHeader1));
        Message toReply = new Message().setPayload(messagePart);

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(
                TO, List.of("to@example.com"), SUBJECT, "ignored subject when replying", BODY, newBody,
                BODY_TYPE, "plain"));

        String encodedEmail = GoogleMailUtils.getEncodedEmail(mockedParameters, mockedActionContext, toReply);

        byte[] raw = getUrlDecoder().decode(encodedEmail);
        MimeMessage parsed = new MimeMessage(
            Session.getDefaultInstance(new Properties()), new ByteArrayInputStream(raw));

        assertEquals(originalSubject, parsed.getSubject());
        assertEquals(replyMessageId, parsed.getHeader("In-Reply-To", null));
        assertEquals(replyMessageId, parsed.getHeader("References", null));

        MimeMultipart mimeMultipart = (MimeMultipart) parsed.getContent();
        BodyPart bodyPart = mimeMultipart.getBodyPart(0);

        assertEquals(newBody, bodyPart.getContent());
    }
}
