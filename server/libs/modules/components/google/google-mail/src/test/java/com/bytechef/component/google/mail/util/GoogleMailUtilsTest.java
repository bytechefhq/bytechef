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
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FORMAT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FULL_MESSAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.METADATA_HEADERS;
import static com.bytechef.component.google.mail.util.GoogleMailUtils.METADATA_MESSAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.google.mail.util.GoogleMailUtils.MINIMAL_MESSAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.google.mail.util.GoogleMailUtils.RAW_MESSAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.google.mail.util.GoogleMailUtils.SIMPLE_MESSAGE_OUTPUT_PROPERTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.mail.definition.Format;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.ListThreadsResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.Thread;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleMailUtilsTest {

    private final ArgumentCaptor<Long> longArgumentCaptor = ArgumentCaptor.forClass(Long.class);
    private final ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Gmail mockedGmail = mock(Gmail.class);
    private final Gmail.Users.Labels mockedLabels = mock(Gmail.Users.Labels.class);
    private final Gmail.Users.Labels.List mockedLabelsList = mock(Gmail.Users.Labels.List.class);
    private final Gmail.Users.Messages.List mockedMesagesList = mock(Gmail.Users.Messages.List.class);
    private Parameters parameters;
    private final Gmail.Users.Threads mockedThreads = mock(Gmail.Users.Threads.class);
    private final Gmail.Users.Threads.List mockedThreadsList = mock(Gmail.Users.Threads.List.class);
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
    void testGetMessageIdOptions() throws IOException {
        parameters = MockParametersFactory.create(Map.of(ACCESS_TOKEN, "id"));

        List<Message> messages = List.of(new Message().setId("id1"), new Message().setId("id2"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getMail(parametersArgumentCaptor.capture()))
                .thenReturn(mockedGmail);
            when(mockedGmail.users())
                .thenReturn(mockedUsers);
            when(mockedUsers.messages())
                .thenReturn(mockedMessages);
            when(mockedMessages.list(stringArgumentCaptor.capture()))
                .thenReturn(mockedMesagesList);
            when(mockedMesagesList.setMaxResults(longArgumentCaptor.capture()))
                .thenReturn(mockedMesagesList);
            when(mockedMesagesList.setPageToken(stringArgumentCaptor.capture()))
                .thenReturn(mockedMesagesList);
            when(mockedMesagesList.execute())
                .thenReturn(new ListMessagesResponse().setMessages(messages));

            List<Option<String>> messageIdOptions = GoogleMailUtils.getMessageIdOptions(
                parameters, parameters, Map.of(), anyString(), mockedActionContext);

            List<Option<String>> expectedOptions = List.of(option("id1", "id1"), option("id2", "id2"));

            assertEquals(expectedOptions, messageIdOptions);
            assertEquals(parameters, parametersArgumentCaptor.getValue());

            List<String> strings = new ArrayList<>();

            strings.add(ME);
            strings.add(null);

            assertEquals(strings, stringArgumentCaptor.getAllValues());
            assertEquals(500L, longArgumentCaptor.getValue());
        }
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
                List.of(), "https://mail.google.com/mail/u/0/#all/id");

        assertEquals(expectedSimpleMessage, result);
    }

    @Test
    void testGetThreadIdOptions() throws IOException {
        parameters = MockParametersFactory.create(Map.of(ACCESS_TOKEN, "id"));

        List<Thread> threads = List.of(new Thread().setId("id1"), new Thread().setId("id2"));
        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getMail(parametersArgumentCaptor.capture()))
                .thenReturn(mockedGmail);
            when(mockedGmail.users())
                .thenReturn(mockedUsers);
            when(mockedUsers.threads())
                .thenReturn(mockedThreads);
            when(mockedThreads.list(stringArgumentCaptor.capture()))
                .thenReturn(mockedThreadsList);
            when(mockedThreadsList.setMaxResults(longArgumentCaptor.capture()))
                .thenReturn(mockedThreadsList);
            when(mockedThreadsList.setPageToken(stringArgumentCaptor.capture()))
                .thenReturn(mockedThreadsList);
            when(mockedThreadsList.execute())
                .thenReturn(new ListThreadsResponse().setThreads(threads));

            List<Option<String>> threadIdOptions = GoogleMailUtils.getThreadIdOptions(
                parameters, parameters, Map.of(), anyString(), mockedActionContext);

            List<Option<String>> expectedOptions = List.of(option("id1", "id1"), option("id2", "id2"));

            assertEquals(expectedOptions, threadIdOptions);
            assertEquals(parameters, parametersArgumentCaptor.getValue());
            List<String> strings = new ArrayList<>();

            strings.add(ME);
            strings.add(null);

            assertEquals(strings, stringArgumentCaptor.getAllValues());
            assertEquals(500L, longArgumentCaptor.getValue());
        }
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
}
