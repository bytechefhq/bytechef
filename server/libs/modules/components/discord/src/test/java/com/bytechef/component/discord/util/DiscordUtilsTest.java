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

package com.bytechef.component.discord.util;

import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.discord.constant.DiscordConstants.RECIPIENT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Domiter
 */
class DiscordUtilsTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Map<String, Object> mockedMap = Map.of("key", "value");
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @Test
    void testGetChannelIdOptions() {
        List<Map<String, Object>> channels = new ArrayList<>();
        Map<String, Object> channel = new LinkedHashMap<>();

        channel.put("name", "name");
        channel.put("id", "id");

        channels.add(channel);

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(channels);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("name", "id"));

        assertEquals(
            expectedOptions,
            DiscordUtils.getChannelIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));
    }

    @Test
    void testGetDMChannel() {
        when(mockedParameters.getRequired(RECIPIENT_ID))
            .thenReturn("id");

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

        Map<String, Object> result = DiscordUtils.getDMChannel(mockedParameters, mockedContext);

        assertEquals(mockedMap, result);

        Http.Body bodyMessage = bodyArgumentCaptor.getValue();

        assertEquals(Map.of(RECIPIENT_ID, "id"), bodyMessage.getContent());
    }

    @Test
    void testGetGuildIdOptions() {
        List<Map<String, Object>> guilds = new ArrayList<>();
        Map<String, Object> guild = new LinkedHashMap<>();

        guild.put("name", "name");
        guild.put("id", "id");

        guilds.add(guild);

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(guilds);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("name", "id"));

        assertEquals(
            expectedOptions,
            DiscordUtils.getGuildIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));
    }

    @Test
    void testGetGuildMemberIdOptions() {
        List<Map<String, Object>> guildMembers = new ArrayList<>();
        Map<String, Object> member = new LinkedHashMap<>();
        Map<String, Object> user = new LinkedHashMap<>();

        user.put("username", "username");
        user.put("id", "id");

        guildMembers.add(member);

        member.put("user", user);

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter("limit", "1000"))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(guildMembers);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("username", "id"));

        assertEquals(
            expectedOptions,
            DiscordUtils.getGuildMemberIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));
    }
}
