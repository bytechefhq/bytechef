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

import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.google.api.services.gmail.Gmail;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class GoogleMailDeleteMailActionTest extends AbstractGoogleMailActionTest {

    private final Gmail.Users mockedUsers = mock(Gmail.Users.class);
    private final Gmail.Users.Messages mockedMessages = mock(Gmail.Users.Messages.class);
    private final Gmail.Users.Messages.Delete mockedDelete = mock(Gmail.Users.Messages.Delete.class);
    private final ArgumentCaptor<String> userIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform() throws IOException {
        Parameters parameters = MockParametersFactory.create(Map.of(ID, "id"));

        when(mockedGmail.users())
            .thenReturn(mockedUsers);
        when(mockedUsers.messages())
            .thenReturn(mockedMessages);
        when(mockedMessages.delete(userIdArgumentCaptor.capture(), idArgumentCaptor.capture()))
            .thenReturn(mockedDelete);

        GoogleMailDeleteMailAction.perform(parameters, parameters, mockedActionContext);
        assertEquals(ME, userIdArgumentCaptor.getValue());
        assertEquals("id", idArgumentCaptor.getValue());
    }
}
