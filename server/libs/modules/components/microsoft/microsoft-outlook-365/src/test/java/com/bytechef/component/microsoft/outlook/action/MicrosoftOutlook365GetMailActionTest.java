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

package com.bytechef.component.microsoft.outlook.action;

import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.microsoft.graph.models.Message;
import com.microsoft.graph.requests.MessageRequest;
import com.microsoft.graph.requests.MessageRequestBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Domiter
 */
class MicrosoftOutlook365GetMailActionTest extends AbstractMicrosoftOutlook365ActionTest {

    private final ArgumentCaptor<String> messageIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final Message mockedMessage = mock(Message.class);
    private final MessageRequestBuilder mockedMessageRequestBuilder = mock(MessageRequestBuilder.class);
    private final MessageRequest mockedMessageRequest = mock(MessageRequest.class);

    @Test
    void testPerform() {
        when(mockedParameters.getRequiredString(ID))
            .thenReturn("id");
        when(mockedUserRequestBuilder.messages(messageIdArgumentCaptor.capture()))
            .thenReturn(mockedMessageRequestBuilder);
        when(mockedMessageRequestBuilder.buildRequest())
            .thenReturn(mockedMessageRequest);
        when(mockedMessageRequest.get())
            .thenReturn(mockedMessage);

        Message result = MicrosoftOutlook365GetMailAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedMessage, result);
        assertEquals("id", messageIdArgumentCaptor.getValue());
    }
}
