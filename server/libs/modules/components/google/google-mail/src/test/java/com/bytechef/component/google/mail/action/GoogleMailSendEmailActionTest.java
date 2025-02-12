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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.mail.util.GoogleMailUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import jakarta.mail.MessagingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleMailSendEmailActionTest {

    private final ArgumentCaptor<ActionContext> actionContextArgumentCaptor =
        ArgumentCaptor.forClass(ActionContext.class);
    private final ArgumentCaptor<Gmail> gmailArgumentCaptor = ArgumentCaptor.forClass(Gmail.class);
    private final ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Gmail mockedGmail = mock(Gmail.class);
    private final Message mockedMessage = mock(Message.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);

    @Test
    void testPerform() throws IOException, MessagingException {
        Parameters parameters = MockParametersFactory.create(Map.of());

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleMailUtils> googleMailUtilsMockedStatic = mockStatic(GoogleMailUtils.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getMail(parametersArgumentCaptor.capture()))
                .thenReturn(mockedGmail);
            googleMailUtilsMockedStatic
                .when(() -> GoogleMailUtils.getEncodedEmail(
                    parametersArgumentCaptor.capture(), actionContextArgumentCaptor.capture(),
                    messageArgumentCaptor.capture()))
                .thenReturn("encodedMail");
            googleMailUtilsMockedStatic
                .when(() -> GoogleMailUtils.sendMail(gmailArgumentCaptor.capture(), messageArgumentCaptor.capture()))
                .thenReturn(mockedMessage);

            Message result = GoogleMailSendEmailAction.perform(parameters, parameters, mockedActionContext);

            assertEquals(mockedMessage, result);

            assertEquals(List.of(parameters, parameters), parametersArgumentCaptor.getAllValues());
            assertEquals(mockedGmail, gmailArgumentCaptor.getValue());
            assertEquals(mockedActionContext, actionContextArgumentCaptor.getValue());

            Message expecteMessage = new Message().setRaw("encodedMail");

            ArrayList<Object> messages = new ArrayList<>();

            messages.add(null);
            messages.add(expecteMessage);

            assertEquals(messages, messageArgumentCaptor.getAllValues());
        }
    }
}
