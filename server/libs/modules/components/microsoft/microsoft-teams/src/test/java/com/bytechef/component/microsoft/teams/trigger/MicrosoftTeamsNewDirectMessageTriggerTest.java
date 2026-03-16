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

package com.bytechef.component.microsoft.teams.trigger;

import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CHAT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.microsoft.commons.MicrosoftTriggerUtils;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class MicrosoftTeamsNewDirectMessageTriggerTest extends AbstractMicrosoftTeamsTriggerTest {

    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(CHAT_ID, "chatId"));

    @Test
    void testPoll() {
        microsoftTriggerUtilsMockedStatic.when(
            () -> MicrosoftTriggerUtils.poll(
                stringArgumentCaptor.capture(),
                stringArgumentCaptor.capture(),
                parametersArgumentCaptor.capture(),
                triggerContextArgumentCaptor.capture()))
            .thenReturn(mockedPollOutput);

        PollOutput result = MicrosoftTeamsNewDirectMessageTrigger.poll(
            mockedParameters, null, mockedParameters, mockedTriggerContext);

        assertEquals(mockedPollOutput, result);
        assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
        assertEquals(mockedTriggerContext, triggerContextArgumentCaptor.getValue());
        assertEquals(
            List.of("/me/chats/chatId/messages", "messageType"), stringArgumentCaptor.getAllValues());
    }
}
