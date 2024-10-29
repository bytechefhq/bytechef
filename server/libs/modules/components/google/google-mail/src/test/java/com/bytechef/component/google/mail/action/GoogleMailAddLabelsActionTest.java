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
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.LABEL_IDS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.ModifyMessageRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author J. Iamsamang
 */
class GoogleMailAddLabelsActionTest extends AbstractGoogleMailActionTest {

    private final Gmail.Users.Messages mockedMessages = mock(Gmail.Users.Messages.class);
    private final Gmail.Users mockedUsers = mock(Gmail.Users.class);
    private final Message mockedMessage = mock(Message.class);
    private final Gmail.Users.Messages.Modify mockedModify = mock(Gmail.Users.Messages.Modify.class);
    private final ArgumentCaptor<String> userIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> messageIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<ModifyMessageRequest> requestArgumentCaptor =
        ArgumentCaptor.forClass(ModifyMessageRequest.class);

    @Test
    void testPerform() throws IOException {
        Parameters parameters =
            MockParametersFactory.create(Map.of(ID, "1", LABEL_IDS, Arrays.asList("1", "2", "3")));

        when(mockedGmail.users())
            .thenReturn(mockedUsers);
        when(mockedUsers.messages())
            .thenReturn(mockedMessages);
        when(mockedMessages.modify(
            userIdArgumentCaptor.capture(),
            messageIdArgumentCaptor.capture(),
            requestArgumentCaptor.capture()))
                .thenReturn(mockedModify);
        when(mockedModify.execute())
            .thenReturn(mockedMessage);

        Message message = GoogleMailAddLabelsAction.perform(parameters, parameters, mockedActionContext);
        assertEquals(mockedMessage, message);
        assertEquals(ME, userIdArgumentCaptor.getValue());
        assertEquals("1", messageIdArgumentCaptor.getValue());
        ModifyMessageRequest request = requestArgumentCaptor.getValue();
        assertArrayEquals(Arrays.asList("1", "2", "3")
            .toArray(),
            request.getAddLabelIds()
                .toArray());
    }
}
