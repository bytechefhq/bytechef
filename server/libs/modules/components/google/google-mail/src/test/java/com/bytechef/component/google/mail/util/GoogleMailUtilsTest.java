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

package com.bytechef.component.google.mail.util;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FORMAT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FULL;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FULL_MESSAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.METADATA;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.METADATA_HEADERS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.MINIMAL;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.RAW;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SIMPLE;
import static com.bytechef.component.google.mail.util.GoogleMailUtils.METADATA_MESSAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.google.mail.util.GoogleMailUtils.MINIMAL_MESSAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.google.mail.util.GoogleMailUtils.RAW_MESSAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.google.mail.util.GoogleMailUtils.SIMPLE_MESSAGE_OUTPUT_PROPERTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleMailUtilsTest {

    private MockedStatic<GoogleServices> googleServicesMockedStatic;
    private final ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
    protected ActionContext mockedContext = mock(ActionContext.class);
    protected Gmail mockedGmail = mock(Gmail.class);
    protected Gmail.Users.Labels mockedLabels = mock(Gmail.Users.Labels.class);
    protected Gmail.Users.Labels.List mockedLabelsList = mock(Gmail.Users.Labels.List.class);
    protected Gmail.Users.Messages.List mockedMesagesList = mock(Gmail.Users.Messages.List.class);
    protected Parameters parameters;
    protected Gmail.Users.Threads mockedThreads = mock(Gmail.Users.Threads.class);
    protected Gmail.Users.Threads.List mockedThreadsList = mock(Gmail.Users.Threads.List.class);
    protected Gmail.Users mockedUsers = mock(Gmail.Users.class);
    private final ArgumentCaptor<String> userIdArgumentCaptor = ArgumentCaptor.forClass(String.class);

    private final ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> formatArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<List> metadataArgumentCaptor = ArgumentCaptor.forClass(List.class);
    private final Gmail.Users.Messages.Get mockedGet = mock(Gmail.Users.Messages.Get.class);
    private final Message mockedMessage = mock(Message.class);
    private final Gmail.Users.Messages.Send mockedSend = mock(Gmail.Users.Messages.Send.class);
    private final Gmail.Users.Messages.Delete mockedDelete = mock(Gmail.Users.Messages.Delete.class);
    private final Gmail.Users.Messages mockedMessages = mock(Gmail.Users.Messages.class);

    @BeforeEach
    void beforeEach() {
        googleServicesMockedStatic = mockStatic(GoogleServices.class);

        googleServicesMockedStatic
            .when(() -> GoogleServices.getMail(any(Parameters.class)))
            .thenReturn(mockedGmail);
    }

    @AfterEach
    public void afterEach() {
        googleServicesMockedStatic.close();
    }

    @Test
    void testGetLabelIdOptions() throws IOException {
        parameters = MockParametersFactory.create(Map.of(ACCESS_TOKEN, "id"));

        List<Label> labels = List.of(new Label().setName("label1"), new Label().setName("label2"));

        when(mockedGmail.users())
            .thenReturn(mockedUsers);
        when(mockedUsers.labels())
            .thenReturn(mockedLabels);
        when(mockedLabels.list(userIdArgumentCaptor.capture()))
            .thenReturn(mockedLabelsList);
        when(mockedLabelsList.execute())
            .thenReturn(new ListLabelsResponse().setLabels(labels));

        List<Option<String>> result = GoogleMailUtils.getLabelIdOptions(
            parameters, parameters, Map.of(), anyString(), mockedContext);

        assertEquals("me", userIdArgumentCaptor.getValue());

        assertNotNull(result);
        assertEquals(2, result.size());

        Option<String> labelIdOptionFirst = result.getFirst();

        assertEquals("label1", labelIdOptionFirst.getLabel());
        assertEquals("label1", labelIdOptionFirst.getValue());

        Option<String> option = result.get(1);

        assertEquals("label2", option.getLabel());
        assertEquals("label2", option.getValue());
    }

    @Test
    void testGetMessage() throws IOException {
        parameters = MockParametersFactory.create(
            Map.of(FORMAT, FULL, ID, "id", METADATA_HEADERS, List.of("metadata")));

        Message message = new Message().setId("id");

        when(mockedGmail.users())
            .thenReturn(mockedUsers);
        when(mockedUsers.messages())
            .thenReturn(mockedMessages);
        when(mockedMessages.get(userIdArgumentCaptor.capture(), idArgumentCaptor.capture()))
            .thenReturn(mockedGet);
        when(mockedGet.setFormat(formatArgumentCaptor.capture()))
            .thenReturn(mockedGet);
        when(mockedGet.setMetadataHeaders(metadataArgumentCaptor.capture()))
            .thenReturn(mockedGet);
        when(mockedGet.execute())
            .thenReturn(message);

        Message result = GoogleMailUtils.getMessage(parameters, mockedGmail);

        assertEquals(message, result);
        assertEquals(ME, userIdArgumentCaptor.getValue());
        assertEquals(FULL, formatArgumentCaptor.getValue());
        assertEquals(List.of("metadata"), metadataArgumentCaptor.getValue());
    }

    @Test
    void testGetMessageIdOptions() throws IOException {
        parameters = MockParametersFactory.create(Map.of(ACCESS_TOKEN, "id"));

        List<Message> messages = List.of(new Message().setId("id1"), new Message().setId("id2"));

        when(mockedGmail.users())
            .thenReturn(mockedUsers);
        when(mockedUsers.messages())
            .thenReturn(mockedMessages);
        when(mockedMessages.list(userIdArgumentCaptor.capture()))
            .thenReturn(mockedMesagesList);
        when(mockedMesagesList.execute())
            .thenReturn(new ListMessagesResponse().setMessages(messages));

        List<Option<String>> messageIdOptions = GoogleMailUtils.getMessageIdOptions(
            parameters, parameters, Map.of(), anyString(), mockedContext);

        assertEquals("me", userIdArgumentCaptor.getValue());

        assertNotNull(messageIdOptions);
        assertEquals(2, messageIdOptions.size());

        Option<String> messageIdOptionsFirst = messageIdOptions.getFirst();

        assertEquals("id1", messageIdOptionsFirst.getLabel());
        assertEquals("id1", messageIdOptionsFirst.getValue());

        Option<String> option = messageIdOptions.get(1);

        assertEquals("id2", option.getLabel());
        assertEquals("id2", option.getValue());
    }

    @Test
    void testGetSimpleMessage() throws IOException {
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

        GoogleMailUtils.SimpleMessage result = GoogleMailUtils.getSimpleMessage(message, mockedContext, mockedGmail);

        GoogleMailUtils.SimpleMessage expectedSimpleMessage =
            new GoogleMailUtils.SimpleMessage("id", "threadId", new BigInteger("123"),
                "email subject", null, List.of(), List.of(), List.of(), "email body", "",
                List.of());
        assertEquals(expectedSimpleMessage, result);
    }

    @Test
    void testGetThreadIdOptions() throws IOException {
        parameters = MockParametersFactory.create(Map.of(ACCESS_TOKEN, "id"));

        List<Thread> threads = List.of(new Thread().setId("id1"), new Thread().setId("id2"));

        when(mockedGmail.users())
            .thenReturn(mockedUsers);
        when(mockedUsers.threads())
            .thenReturn(mockedThreads);
        when(mockedThreads.list(userIdArgumentCaptor.capture()))
            .thenReturn(mockedThreadsList);
        when(mockedThreadsList.execute())
            .thenReturn(new ListThreadsResponse().setThreads(threads));

        List<Option<String>> threadIdOptions = GoogleMailUtils.getThreadIdOptions(
            parameters, parameters, Map.of(), anyString(), mockedContext);

        assertEquals("me", userIdArgumentCaptor.getValue());

        assertNotNull(threadIdOptions);
        assertEquals(2, threadIdOptions.size());

        Option<String> threadIdOptionsFirst = threadIdOptions.getFirst();

        assertEquals("id1", threadIdOptionsFirst.getLabel());
        assertEquals("id1", threadIdOptionsFirst.getValue());

        Option<String> option = threadIdOptions.get(1);

        assertEquals("id2", option.getLabel());
        assertEquals("id2", option.getValue());
    }

    @Test
    void testGetOutputForSimpleFormat() {
        ModifiableObjectProperty messageOutputProperty = GoogleMailUtils.getMessageOutputProperty(SIMPLE);

        assertEquals(SIMPLE_MESSAGE_OUTPUT_PROPERTY, messageOutputProperty);
    }

    @Test
    void testGetOutputForRawFormat() {
        ModifiableObjectProperty messageOutputProperty = GoogleMailUtils.getMessageOutputProperty(RAW);

        assertEquals(RAW_MESSAGE_OUTPUT_PROPERTY, messageOutputProperty);
    }

    @Test
    void testGetOutputForMinimalFormat() {
        ModifiableObjectProperty messageOutputProperty = GoogleMailUtils.getMessageOutputProperty(MINIMAL);

        assertEquals(MINIMAL_MESSAGE_OUTPUT_PROPERTY, messageOutputProperty);
    }

    @Test
    void testGetOutputForMetadataFormat() {
        ModifiableObjectProperty messageOutputProperty = GoogleMailUtils.getMessageOutputProperty(METADATA);

        assertEquals(METADATA_MESSAGE_OUTPUT_PROPERTY, messageOutputProperty);
    }

    @Test
    void testGetOutputForFullFormat() {
        ModifiableObjectProperty messageOutputProperty = GoogleMailUtils.getMessageOutputProperty(FULL);

        assertEquals(FULL_MESSAGE_OUTPUT_PROPERTY, messageOutputProperty);
    }

    @Test
    void testSendMail() throws IOException {
        when(mockedGmail.users())
            .thenReturn(mockedUsers);
        when(mockedUsers.messages())
            .thenReturn(mockedMessages);
        when(mockedMessages.send(userIdArgumentCaptor.capture(), messageArgumentCaptor.capture()))
            .thenReturn(mockedSend);
        when(mockedSend.execute())
            .thenReturn(mockedMessage);

        Message result = GoogleMailUtils.sendMail(mockedGmail, mockedMessage);

        assertEquals(mockedMessage, result);
        assertEquals(ME, userIdArgumentCaptor.getValue());
    }

    @Test
    void testDeleteMail() throws IOException {
        parameters = MockParametersFactory.create(Map.of(ID, "id"));

        when(mockedGmail.users())
            .thenReturn(mockedUsers);
        when(mockedUsers.messages())
            .thenReturn(mockedMessages);
        when(mockedMessages.delete(userIdArgumentCaptor.capture(), idArgumentCaptor.capture()))
            .thenReturn(mockedDelete);

        GoogleMailUtils.deleteMail(parameters, mockedGmail);
        assertEquals(ME, userIdArgumentCaptor.getValue());
        assertEquals("id", idArgumentCaptor.getValue());
    }
}
