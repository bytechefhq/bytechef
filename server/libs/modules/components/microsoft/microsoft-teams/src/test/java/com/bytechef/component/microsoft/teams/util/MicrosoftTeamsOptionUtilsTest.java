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

package com.bytechef.component.microsoft.teams.util;

import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.DISPLAY_NAME;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.ID;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
class MicrosoftTeamsOptionUtilsTest {

    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @Test
    void testGetChatIdOptions() {
        Map<String, List<Map<String, Object>>> map = new LinkedHashMap<>();
        List<Map<String, Object>> chats = new ArrayList<>();
        Map<String, Object> chatMap = new LinkedHashMap<>();

        chatMap.put("chatType", "type");
        chatMap.put(ID, "id");

        chats.add(chatMap);

        map.put(VALUE, chats);

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        try (
            MockedStatic<MicrosoftTeamsUtils> microsoftTeamsUtilsMockedStatic = mockStatic(MicrosoftTeamsUtils.class)) {
            microsoftTeamsUtilsMockedStatic
                .when(() -> MicrosoftTeamsUtils.getChatMembers(mockedContext, chatMap))
                .thenReturn(List.of("member1", "member2"));

            List<Option<String>> expectedOptions = new ArrayList<>();

            expectedOptions.add(option("type chat: member1,member2", "id"));

            assertEquals(
                expectedOptions,
                MicrosoftTeamsOptionUtils.getChatIdOptions(mockedParameters, mockedParameters, Map.of(), "",
                    mockedContext));
        }
    }

    @Test
    void testGetChannelIdOptions() {
        Map<String, List<Map<String, Object>>> map = new LinkedHashMap<>();
        List<Map<String, Object>> channels = new ArrayList<>();
        Map<String, Object> channelMap = new LinkedHashMap<>();

        channelMap.put(DISPLAY_NAME, "name");
        channelMap.put(ID, "id");

        channels.add(channelMap);

        map.put(VALUE, channels);

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("name", "id"));

        assertEquals(
            expectedOptions,
            MicrosoftTeamsOptionUtils.getChannelIdOptions(mockedParameters, mockedParameters, Map.of(), "",
                mockedContext));
    }

    @Test
    void testGetTeamIdOptions() {
        Map<String, List<Map<String, Object>>> map = new LinkedHashMap<>();
        List<Map<String, Object>> teams = new ArrayList<>();
        Map<String, Object> teamMap = new LinkedHashMap<>();

        teamMap.put(DISPLAY_NAME, "team");
        teamMap.put(ID, "id");

        teams.add(teamMap);

        map.put(VALUE, teams);

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("team", "id"));

        assertEquals(
            expectedOptions,
            MicrosoftTeamsOptionUtils.getTeamIdOptions(mockedParameters, mockedParameters, Map.of(), "",
                mockedContext));
    }

}
