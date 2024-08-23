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

import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ATTACHMENTS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FORMAT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FROM;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FULL;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.METADATA_HEADERS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.MINIMAL;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SIMPLE;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SUBJECT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.TO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.test.component.properties.ParametersFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class GoogleMailGetMailActionTest extends AbstractGoogleMailActionTest {

    private final ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> formatArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<List> metadataArgumentCaptor = ArgumentCaptor.forClass(List.class);
    private final Gmail.Users.Messages.Get mockedGet = mock(Gmail.Users.Messages.Get.class);
    private final Message mockedMessage = mock(Message.class);
    private final Gmail.Users.Messages mockedMessages = mock(Gmail.Users.Messages.class);
    private final Gmail.Users mockedUsers = mock(Gmail.Users.class);
    private final ArgumentCaptor<String> userIdArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform() throws IOException {
        Parameters parameters = ParametersFactory.createParameters(
            Map.of(ID, "id", FORMAT, MINIMAL, METADATA_HEADERS, List.of("metadata")));

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
            .thenReturn(mockedMessage);

        Map<String, Object> perform = GoogleMailGetMailAction.perform(parameters, parameters, mockedContext);

        assertEquals(mockedMessage, perform);
        assertEquals("me", userIdArgumentCaptor.getValue());
        assertEquals("id", idArgumentCaptor.getValue());
        assertEquals(MINIMAL, formatArgumentCaptor.getValue());
        assertEquals(parameters.getList(METADATA_HEADERS), metadataArgumentCaptor.getValue());
    }

    @Test
    void testPerformForParsedFormat() throws IOException {
        Parameters parameters = ParametersFactory.createParameters(
            Map.of(ID, "id", FORMAT, SIMPLE, METADATA_HEADERS, List.of("metadata")));

        Message message = new Message().setPayload(
            new MessagePart()
                .setBody(new MessagePartBody().setData("ZW1haWwgYm9keQ=="))
                .setHeaders(List.of(new MessagePartHeader().setName("Subject")
                    .setValue("email subject")))
                .setParts(List.of(new MessagePart()
                    .setMimeType("text/plain"))));

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

        Map<String, Object> result = GoogleMailGetMailAction.perform(parameters, parameters, mockedContext);

        Map<String, Object> expectedResponse = new LinkedHashMap<>();

        expectedResponse.put(SUBJECT, "email subject");
        expectedResponse.put(FROM, "");
        expectedResponse.put(TO, "");
        expectedResponse.put("body_plain", "email body");
        expectedResponse.put("body_html", "");
        expectedResponse.put(ATTACHMENTS, List.of());

        assertEquals(expectedResponse, result);
        assertEquals("me", userIdArgumentCaptor.getValue());
        assertEquals("id", idArgumentCaptor.getValue());
        assertEquals(FULL, formatArgumentCaptor.getValue());
        assertEquals(parameters.getList(METADATA_HEADERS), metadataArgumentCaptor.getValue());
    }
}
