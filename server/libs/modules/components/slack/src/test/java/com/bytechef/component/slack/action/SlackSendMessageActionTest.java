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

package com.bytechef.component.slack.action;

import static com.bytechef.component.slack.constant.SlackConstants.CHANNEL_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.slack.api.methods.SlackApiException;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * @author Mario Cvjetojevic
 */
class SlackSendMessageActionTest extends AbstractSlackActionTest {

    @Test
     void testPerform() throws SlackApiException, IOException {
        when(mockedParameters.getRequiredString(CHANNEL_ID))
            .thenReturn(CHANNEL_ID);

        SlackSendMessageAction.perform(mockedParameters, mockedParameters, mockedContext);

        verify(mockedMethodsClient, times(1))
            .chatPostMessage(chatPostMessageRequestArgumentCaptor.capture());
        assertEquals(CHANNEL_ID, chatPostMessageRequestArgumentCaptor.getValue()
            .getChannel());
    }

}
