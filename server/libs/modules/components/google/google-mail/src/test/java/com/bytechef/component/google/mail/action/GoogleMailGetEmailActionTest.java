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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.mail.definition.Format;
import com.bytechef.component.google.mail.util.GoogleMailUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleMailGetEmailActionTest {

    private final ArgumentCaptor<ActionContext> actionContextArgumentCaptor =
        ArgumentCaptor.forClass(ActionContext.class);
    private final ArgumentCaptor<Gmail> gmailArgumentCaptor = ArgumentCaptor.forClass(Gmail.class);
    private final ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Gmail mockedGmail = mock(Gmail.class);
    private final Message mockedMessage = mock(Message.class);
    private final GoogleMailUtils.SimpleMessage mockedSimpleMessage = mock(GoogleMailUtils.SimpleMessage.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);

    @Test
    void testPerform() throws IOException {
        Parameters parameters = MockParametersFactory.create(Map.of(FORMAT, Format.MINIMAL));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
             MockedStatic<GoogleMailUtils> googleMailUtilsMockedStatic = mockStatic(GoogleMailUtils.class)) {

            googleServicesMockedStatic.when(() -> GoogleServices.getMail(parametersArgumentCaptor.capture()))
                .thenReturn(mockedGmail);
            googleMailUtilsMockedStatic.when(() -> GoogleMailUtils.getMessage(
                    parametersArgumentCaptor.capture(), gmailArgumentCaptor.capture()))
                .thenReturn(mockedMessage);

            Object result = GoogleMailGetEmailAction.perform(parameters, parameters, mockedActionContext);

            assertEquals(mockedMessage, result);
            assertEquals(List.of(parameters, parameters), parametersArgumentCaptor.getAllValues());
            assertEquals(mockedGmail, gmailArgumentCaptor.getValue());
        }
    }

    @Test
    void testPerformForSimpleFormat() throws IOException {
        Parameters parameters = MockParametersFactory.create(Map.of(FORMAT, Format.SIMPLE));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
             MockedStatic<GoogleMailUtils> googleMailUtilsMockedStatic = mockStatic(GoogleMailUtils.class)) {

            googleServicesMockedStatic.when(() -> GoogleServices.getMail(parametersArgumentCaptor.capture()))
                .thenReturn(mockedGmail);
            googleMailUtilsMockedStatic.when(() -> GoogleMailUtils.getMessage(
                    parametersArgumentCaptor.capture(), gmailArgumentCaptor.capture()))
                .thenReturn(mockedMessage);
            googleMailUtilsMockedStatic
                .when(() -> GoogleMailUtils.getSimpleMessage(
                    messageArgumentCaptor.capture(), actionContextArgumentCaptor.capture(),
                    gmailArgumentCaptor.capture()))
                .thenReturn(mockedSimpleMessage);

            Object result = GoogleMailGetEmailAction.perform(parameters, parameters, mockedActionContext);

            assertEquals(mockedSimpleMessage, result);
            assertEquals(List.of(parameters, parameters), parametersArgumentCaptor.getAllValues());
            assertEquals(List.of(mockedGmail, mockedGmail), gmailArgumentCaptor.getAllValues());
            assertEquals(mockedMessage, messageArgumentCaptor.getValue());
            assertEquals(mockedActionContext, actionContextArgumentCaptor.getValue());
        }
    }
}
