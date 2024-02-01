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

import static com.bytechef.component.google.mail.constant.GoogleMailConstants.BODY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FROM;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SUBJECT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.TO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import jakarta.mail.MessagingException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Domiter
 */
class GoogleMailSendEmailActionTest extends AbstractGoogleMailActionTest {

    private final ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
    private final Message mockedMessage = mock(Message.class);
    private final Gmail.Users.Messages mockedMessages = mock(Gmail.Users.Messages.class);
    private final Gmail.Users.Messages.Send mockedSend = mock(Gmail.Users.Messages.Send.class);
    private final Gmail.Users mockedUsers = mock(Gmail.Users.class);
    private final ArgumentCaptor<String> userIdArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform() throws IOException, MessagingException {
        List<String> toList = List.of("to@mail.com");

        when(mockedParameters.getRequiredString(FROM))
            .thenReturn("from@mail.com");
        when(mockedParameters.getRequiredList(TO, String.class))
            .thenReturn(toList);
        when(mockedParameters.getRequiredString(SUBJECT))
            .thenReturn("subject");
        when(mockedParameters.getRequiredString(BODY))
            .thenReturn("body");

        when(mockedGmail.users())
            .thenReturn(mockedUsers);
        when(mockedUsers.messages())
            .thenReturn(mockedMessages);
        when(mockedMessages.send(userIdArgumentCaptor.capture(), messageArgumentCaptor.capture()))
            .thenReturn(mockedSend);
        when(mockedSend.execute())
            .thenReturn(mockedMessage);

        Map<String, Message> handleMap = GoogleMailSendEmailAction.perform(
            mockedParameters, mockedParameters, mockedContext);

        Message message = handleMap.get("message");

        assertEquals(mockedMessage, message);
        assertEquals("me", userIdArgumentCaptor.getValue());
    }
}
