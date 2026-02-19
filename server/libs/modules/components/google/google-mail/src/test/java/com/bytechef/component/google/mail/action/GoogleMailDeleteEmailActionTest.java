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

import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author J. Iamsamang
 */
class GoogleMailDeleteEmailActionTest {

    private final Gmail.Users.Messages.Delete mockedDelete = mock(Gmail.Users.Messages.Delete.class);
    private final Gmail mockedGmail = mock(Gmail.class);
    private final Gmail.Users.Messages mockedMessages = mock(Gmail.Users.Messages.class);
    private final Gmail.Users mockedUsers = mock(Gmail.Users.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);

    @Test
    void testPerform() throws IOException {
        Parameters parameters = MockParametersFactory.create(Map.of(ID, "123"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic.when(() -> GoogleServices.getMail(parametersArgumentCaptor.capture()))
                .thenReturn(mockedGmail);
            when(mockedGmail.users())
                .thenReturn(mockedUsers);
            when(mockedUsers.messages())
                .thenReturn(mockedMessages);
            when(mockedMessages.delete(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(mockedDelete);

            GoogleMailDeleteEmailAction.perform(parameters, parameters, mock(ActionContext.class));

            assertEquals(parameters, parametersArgumentCaptor.getValue());
            assertEquals(List.of(ME, "123"), stringArgumentCaptor.getAllValues());
        }
    }
}
