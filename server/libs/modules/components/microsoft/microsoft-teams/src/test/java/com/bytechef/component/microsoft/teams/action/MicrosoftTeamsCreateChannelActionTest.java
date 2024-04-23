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

package com.bytechef.component.microsoft.teams.action;

import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.DESCRIPTION;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.DISPLAY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
class MicrosoftTeamsCreateChannelActionTest extends AbstractMicrosoftTeamsActionTest {

    @Test
    void testPerform() {
        when(mockedParameters.getRequiredString(DISPLAY_NAME))
            .thenReturn("new channel");
        when(mockedParameters.getString(DESCRIPTION))
            .thenReturn("channel description");

        Object result = MicrosoftTeamsCreateChannelAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responseMap, result);

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(Map.of(DISPLAY_NAME, "new channel", DESCRIPTION, "channel description"), body.getContent());
    }

}
