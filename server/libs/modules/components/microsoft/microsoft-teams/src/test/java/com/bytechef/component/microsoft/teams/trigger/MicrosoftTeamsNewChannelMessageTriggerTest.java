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

import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CHANNEL_ID;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.TEAM_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.microsoft.teams.util.MicrosoftTeamsUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class MicrosoftTeamsNewChannelMessageTriggerTest extends AbstractMicrosoftTeamsTriggerTest {

    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(TEAM_ID, "teamId", CHANNEL_ID, "channelId"));

    @Test
    void testPoll() {
        microsoftTeamsUtilsMockedStatic.when(
            () -> MicrosoftTeamsUtils.pollMicrosoftTeamsMessage(
                stringArgumentCaptor.capture(),
                parametersArgumentCaptor.capture(),
                triggerContextArgumentCaptor.capture()))
            .thenReturn(mockedPollOutput);

        PollOutput result = MicrosoftTeamsNewChannelMessageTrigger.poll(
            mockedParameters, null, mockedParameters, mockedTriggerContext);

        assertEquals(mockedPollOutput, result);
        assertEquals("/teams/teamId/channels/channelId/messages", stringArgumentCaptor.getValue());
        assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
        assertEquals(mockedTriggerContext, triggerContextArgumentCaptor.getValue());
    }
}
