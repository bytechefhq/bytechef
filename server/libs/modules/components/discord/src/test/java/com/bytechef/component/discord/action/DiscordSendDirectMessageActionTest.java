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

package com.bytechef.component.discord.action;

import static com.bytechef.component.discord.constant.DiscordConstants.CONTENT;
import static com.bytechef.component.discord.constant.DiscordConstants.RECIPIENT_ID;
import static com.bytechef.component.discord.constant.DiscordConstants.TTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.discord.util.DiscordUtils;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
class DiscordSendDirectMessageActionTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Map<String, Object> mockedMap = Map.of("key", "value");
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @Test
    void testPerform() {
        when(mockedParameters.getRequired(RECIPIENT_ID))
            .thenReturn("id");
        when(mockedParameters.getRequiredString(CONTENT))
            .thenReturn("content");
        when(mockedParameters.getBoolean(TTS))
            .thenReturn(true);

        try (MockedStatic<DiscordUtils> discordUtilsMockedStatic = mockStatic(DiscordUtils.class)) {
            discordUtilsMockedStatic
                .when(() -> DiscordUtils.getDMChannel(mockedParameters, mockedContext))
                .thenReturn(mockedMap);

            when(mockedContext.http(any()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.configuration(any()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.execute())
                .thenReturn(mockedResponse);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(mockedMap);

            Object result = DiscordSendDirectMessageAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(mockedMap, result);

            Http.Body bodyMessage = bodyArgumentCaptor.getValue();

            assertEquals(Map.of(CONTENT, "content", TTS, true), bodyMessage.getContent());
        }
    }

}
