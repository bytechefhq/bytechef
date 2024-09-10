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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.mail.util.GoogleMailUtils;
import com.bytechef.test.component.properties.ParametersFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import jakarta.mail.MessagingException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleMailReplyToEmailActionTest extends AbstractGoogleMailActionTest {

    private final ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
    private final Message mockedMessage = mock(Message.class);

    @Test
    void testPerform() throws IOException, MessagingException {
        Parameters parameters = ParametersFactory.createParameters(Map.of());
        Message message = new Message().setThreadId("id")
            .setHistoryId(new BigInteger("123"));

        try (MockedStatic<GoogleMailUtils> googleMailUtilsMockedStatic = mockStatic(GoogleMailUtils.class)) {
            googleMailUtilsMockedStatic.when(() -> GoogleMailUtils.getMessage(parameters, mockedGmail))
                .thenReturn(message);
            googleMailUtilsMockedStatic
                .when(() -> GoogleMailUtils.getEncodedEmail(parameters, mockedActionContext, message))
                .thenReturn("encodedMail");
            googleMailUtilsMockedStatic
                .when(() -> GoogleMailUtils.sendMail(any(Gmail.class), messageArgumentCaptor.capture()))
                .thenReturn(mockedMessage);

            Message result = GoogleMailReplyToEmailAction.perform(parameters, parameters, mockedActionContext);

            assertEquals(mockedMessage, result);

            Message value = messageArgumentCaptor.getValue();

            assertEquals("encodedMail", value.getRaw());
            assertEquals("id", value.getThreadId());
            assertEquals(new BigInteger("123"), value.getHistoryId());
        }
    }
}
