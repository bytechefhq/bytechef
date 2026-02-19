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

package com.bytechef.component.google.mail.action;

import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FORMAT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.METADATA_HEADERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.mail.definition.Format;
import com.bytechef.component.google.mail.util.GoogleMailUtils;
import com.bytechef.component.google.mail.util.GoogleMailUtils.SimpleMessage;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.Thread;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleMailGetThreadActionTest {

    private final ArgumentCaptor<ActionContext> actionContextArgumentCaptor = forClass(ActionContext.class);
    private final ArgumentCaptor<Gmail> gmailArgumentCaptor = forClass(Gmail.class);
    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> listArgumentCaptor = forClass(List.class);
    private final ArgumentCaptor<Message> messageArgumentCaptor = forClass(Message.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Parameters mockedConnectionParameters = mock(Parameters.class);
    private final Gmail mockedGmail = mock(Gmail.class);
    private final Gmail.Users.Threads.Get mockedGet = mock(Gmail.Users.Threads.Get.class);
    private final Message mockedMessage = mock(Message.class);
    private final SimpleMessage mockedSimpleMessage = mock(SimpleMessage.class);
    private final Thread mockedThread = mock(Thread.class);
    private final Gmail.Users.Threads mockedThreads = mock(Gmail.Users.Threads.class);
    private final Gmail.Users mockedUsers = mock(Gmail.Users.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    @SuppressWarnings("unchecked")
    void testPerform() throws IOException {
        Parameters inputParameters = MockParametersFactory.create(
            Map.of(ID, "id", FORMAT, Format.MINIMAL, METADATA_HEADERS, List.of("metadata")));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic.when(() -> GoogleServices.getMail(parametersArgumentCaptor.capture()))
                .thenReturn(mockedGmail);
            when(mockedGmail.users())
                .thenReturn(mockedUsers);
            when(mockedUsers.threads())
                .thenReturn(mockedThreads);
            when(mockedThreads.get(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(mockedGet);
            when(mockedGet.setFormat(stringArgumentCaptor.capture()))
                .thenReturn(mockedGet);
            when(mockedGet.setMetadataHeaders(listArgumentCaptor.capture()))
                .thenReturn(mockedGet);
            when(mockedGet.execute())
                .thenReturn(mockedThread);

            Object result = GoogleMailGetThreadAction.perform(
                inputParameters, mockedConnectionParameters, mockedActionContext);

            assertEquals(mockedThread, result);

            assertEquals(mockedConnectionParameters, parametersArgumentCaptor.getValue());
            assertEquals(List.of(ME, "id", Format.MINIMAL.getMapping()), stringArgumentCaptor.getAllValues());
            assertEquals(inputParameters.getList(METADATA_HEADERS), listArgumentCaptor.getValue());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPerformForSimpleFormat() throws IOException {
        Parameters inputParameters = MockParametersFactory.create(
            Map.of(ID, "id", FORMAT, Format.SIMPLE, METADATA_HEADERS, List.of("metadata")));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleMailUtils> googleMailUtilsMockedStatic = mockStatic(GoogleMailUtils.class)) {
            googleMailUtilsMockedStatic
                .when(() -> GoogleMailUtils.getSimpleMessage(
                    messageArgumentCaptor.capture(), actionContextArgumentCaptor.capture(),
                    gmailArgumentCaptor.capture()))
                .thenReturn(mockedSimpleMessage);

            googleServicesMockedStatic.when(() -> GoogleServices.getMail(parametersArgumentCaptor.capture()))
                .thenReturn(mockedGmail);
            when(mockedGmail.users())
                .thenReturn(mockedUsers);
            when(mockedUsers.threads())
                .thenReturn(mockedThreads);
            when(mockedThreads.get(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(mockedGet);
            when(mockedGet.setFormat(stringArgumentCaptor.capture()))
                .thenReturn(mockedGet);
            when(mockedGet.setMetadataHeaders(listArgumentCaptor.capture()))
                .thenReturn(mockedGet);
            when(mockedGet.execute())
                .thenReturn(new Thread()
                    .setMessages(List.of(mockedMessage))
                    .setId("id")
                    .setSnippet("snippet")
                    .setHistoryId(new BigInteger("123")));

            Object result = GoogleMailGetThreadAction.perform(
                inputParameters, mockedConnectionParameters, mockedActionContext);

            GoogleMailGetThreadAction.ThreadCustom expected = new GoogleMailGetThreadAction.ThreadCustom(
                "id", "snippet", new BigInteger("123"), List.of(mockedSimpleMessage));

            assertEquals(expected, result);

            assertEquals(mockedMessage, messageArgumentCaptor.getValue());
            assertEquals(mockedActionContext, actionContextArgumentCaptor.getValue());
            assertEquals(mockedGmail, gmailArgumentCaptor.getValue());
            assertEquals(mockedConnectionParameters, parametersArgumentCaptor.getValue());
            assertEquals(List.of(ME, "id", Format.FULL.getMapping()), stringArgumentCaptor.getAllValues());
            assertEquals(inputParameters.getList(METADATA_HEADERS), listArgumentCaptor.getValue());
        }
    }
}
