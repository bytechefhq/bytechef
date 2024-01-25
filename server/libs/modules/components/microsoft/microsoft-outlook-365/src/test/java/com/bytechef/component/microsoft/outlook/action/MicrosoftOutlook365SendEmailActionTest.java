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

import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SAVE_TO_SENT_ITEMS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.UserSendMailParameterSet;
import com.microsoft.graph.requests.UserSendMailRequest;
import com.microsoft.graph.requests.UserSendMailRequestBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Domiter
 */
class MicrosoftOutlook365SendEmailActionTest extends AbstractMicrosoftOutlook365ActionTest {

    private final Message mockedMessage = mock(Message.class);
    private final UserSendMailRequest mockedUserSendMailRequest = mock(UserSendMailRequest.class);
    private final UserSendMailRequestBuilder mockedUserSendMailRequestBuilder = mock(UserSendMailRequestBuilder.class);
    private final ArgumentCaptor<UserSendMailParameterSet> userSendMailParameterSetArgumentCaptor =
        ArgumentCaptor.forClass(UserSendMailParameterSet.class);

    @Test
    void testPerform() {
        microsoftOutlook365UtilsMockedStatic
            .when(() -> MicrosoftOutlook365Utils.createMessage(mockedParameters))
            .thenReturn(mockedMessage);
        when(mockedParameters.getBoolean(SAVE_TO_SENT_ITEMS))
            .thenReturn(true);
        when(mockedUserRequestBuilder.sendMail(userSendMailParameterSetArgumentCaptor.capture()))
            .thenReturn(mockedUserSendMailRequestBuilder);
        when(mockedUserSendMailRequestBuilder.buildRequest())
            .thenReturn(mockedUserSendMailRequest);

        MicrosoftOutlook365SendEmailAction.perform(mockedParameters, mockedParameters, mockedContext);

        UserSendMailParameterSet userSendMailParameterSet = userSendMailParameterSetArgumentCaptor.getValue();

        assertEquals(true, userSendMailParameterSet.saveToSentItems);
        assertEquals(mockedMessage, userSendMailParameterSet.message);
    }
}
