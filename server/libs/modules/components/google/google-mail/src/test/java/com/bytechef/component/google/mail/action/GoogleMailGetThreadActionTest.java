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

package com.bytechef.component.google.mail.action;

import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FORMAT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FULL;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.METADATA_HEADERS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.MINIMAL;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SIMPLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.mail.util.GoogleMailUtils;
import com.bytechef.component.google.mail.util.GoogleMailUtils.SimpleMessage;
import com.bytechef.test.component.properties.ParametersFactory;
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
class GoogleMailGetThreadActionTest extends AbstractGoogleMailActionTest {

    private final ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> formatArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<List> metadataArgumentCaptor = ArgumentCaptor.forClass(List.class);
    private final Gmail.Users.Threads.Get mockedGet = mock(Gmail.Users.Threads.Get.class);
    private final Message mockedMessage = mock(Message.class);
    private final SimpleMessage mockedSimpleMessage = mock(SimpleMessage.class);
    private final Thread mockedThread = mock(Thread.class);
    private final Gmail.Users.Threads mockedThreads = mock(Gmail.Users.Threads.class);
    private final Gmail.Users mockedUsers = mock(Gmail.Users.class);
    private final ArgumentCaptor<String> userIdArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform() throws IOException {
        Parameters parameters = ParametersFactory.createParameters(
            Map.of(ID, "id", FORMAT, MINIMAL, METADATA_HEADERS, List.of("metadata")));

        when(mockedGmail.users())
            .thenReturn(mockedUsers);
        when(mockedUsers.threads())
            .thenReturn(mockedThreads);
        when(mockedThreads.get(userIdArgumentCaptor.capture(), idArgumentCaptor.capture()))
            .thenReturn(mockedGet);
        when(mockedGet.setFormat(formatArgumentCaptor.capture()))
            .thenReturn(mockedGet);
        when(mockedGet.setMetadataHeaders(metadataArgumentCaptor.capture()))
            .thenReturn(mockedGet);
        when(mockedGet.execute())
            .thenReturn(mockedThread);

        Object result = GoogleMailGetThreadAction.perform(parameters, parameters, mockedActionContext);

        assertEquals(mockedThread, result);

        assertEquals(ME, userIdArgumentCaptor.getValue());
        assertEquals("id", idArgumentCaptor.getValue());
        assertEquals(MINIMAL, formatArgumentCaptor.getValue());
        assertEquals(parameters.getList(METADATA_HEADERS), metadataArgumentCaptor.getValue());
    }

    @Test
    void testPerformForSimpleFormat() throws IOException {
        Parameters parameters = ParametersFactory.createParameters(
            Map.of(ID, "id", FORMAT, SIMPLE, METADATA_HEADERS, List.of("metadata")));

        when(mockedGmail.users())
            .thenReturn(mockedUsers);
        when(mockedUsers.threads())
            .thenReturn(mockedThreads);
        when(mockedThreads.get(userIdArgumentCaptor.capture(), idArgumentCaptor.capture()))
            .thenReturn(mockedGet);
        when(mockedGet.setFormat(formatArgumentCaptor.capture()))
            .thenReturn(mockedGet);
        when(mockedGet.setMetadataHeaders(metadataArgumentCaptor.capture()))
            .thenReturn(mockedGet);
        when(mockedGet.execute())
            .thenReturn(new Thread().setMessages(List.of(mockedMessage))
                .setId("id")
                .setSnippet("snippet")
                .setHistoryId(new BigInteger("123")));

        try (MockedStatic<GoogleMailUtils> googleMailUtilsMockedStatic = mockStatic(GoogleMailUtils.class)) {
            googleMailUtilsMockedStatic
                .when(() -> GoogleMailUtils.getSimpleMessage(mockedMessage, mockedActionContext, mockedGmail))
                .thenReturn(mockedSimpleMessage);

            Object result = GoogleMailGetThreadAction.perform(parameters, parameters, mockedActionContext);

            GoogleMailGetThreadAction.ThreadCustom expected = new GoogleMailGetThreadAction.ThreadCustom(
                "id", "snippet", new BigInteger("123"), List.of(mockedSimpleMessage));

            assertEquals(expected, result);

            assertEquals(ME, userIdArgumentCaptor.getValue());
            assertEquals("id", idArgumentCaptor.getValue());
            assertEquals(FULL, formatArgumentCaptor.getValue());
            assertEquals(parameters.getList(METADATA_HEADERS), metadataArgumentCaptor.getValue());
        }
    }
}
