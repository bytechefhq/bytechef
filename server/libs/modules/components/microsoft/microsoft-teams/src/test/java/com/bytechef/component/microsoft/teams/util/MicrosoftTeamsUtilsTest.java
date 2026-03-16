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

package com.bytechef.component.microsoft.teams.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CONTENT_TYPE;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CONTENT_URL;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.DISPLAY_NAME;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.E_TAG;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.ID;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.NAME;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.TEAM_ID;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.VALUE;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.WEB_DAV_URL;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Domiter
 */
@ExtendWith(MockContextSetupExtension.class)
class MicrosoftTeamsUtilsTest {

    private final Parameters mockedParameters = mock(Parameters.class);
    private final ArgumentCaptor<Object[]> queryArgumentCaptor = forClass(Object[].class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testGetAttachmentsList(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(E_TAG, "{eTag},1", WEB_DAV_URL, "webDavUrl", NAME, "name"));

        List<Map<String, String>> result = MicrosoftTeamsUtils.getAttachmentsList(
            List.of("fileId"), mockedContext);

        List<Map<String, String>> expected = List.of(
            Map.of(ID, "eTag", CONTENT_TYPE, "reference", CONTENT_URL, "webDavUrl", NAME, "name"));

        assertEquals(expected, result);

        List<String> expectedStrings = List.of(
            "https://graph.microsoft.com/v1.0/me/drive/items/fileId",
            "$select",
            "id,name,webUrl,webDavUrl,@microsoft.graph.downloadUrl,etag");

        assertEquals(expectedStrings, stringArgumentCaptor.getAllValues());

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        Http.Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();
        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
    }

    @Test
    void testGetChatIdOptions(
        ActionContext mockedActionContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(VALUE, List.of(
                Map.of(
                    "chatType", "type", ID, "id",
                    "members", List.of(Map.of(DISPLAY_NAME, "member1"), Map.of(DISPLAY_NAME, "member2"))))));

        List<Option<String>> result = MicrosoftTeamsUtils.getChatIdOptions(mockedParameters, mockedParameters,
            Map.of(), "", mockedActionContext);

        assertEquals(List.of(option("type chat: member1,member2", "id")), result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/chats", stringArgumentCaptor.getValue());

        Object[] expectedQuery = {
            "$expand", "members"
        };

        assertArrayEquals(expectedQuery, queryArgumentCaptor.getValue());
    }

    @Test
    void testGetChannelIdOptions(
        ActionContext mockedActionContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters mockedInputParameters = MockParametersFactory.create(Map.of(TEAM_ID, "xy"));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(VALUE, List.of(Map.of(DISPLAY_NAME, "name", ID, "id"))));

        List<Option<String>> result = MicrosoftTeamsUtils.getChannelIdOptions(
            mockedInputParameters, mockedParameters, Map.of(), "", mockedActionContext);

        assertEquals(List.of(option("name", "id")), result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/teams/xy/channels", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetTeamIdOptions(
        ActionContext mockedActionContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(VALUE, List.of(Map.of(DISPLAY_NAME, "team", ID, "id"))));

        List<Option<String>> result = MicrosoftTeamsUtils.getTeamIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedActionContext);

        assertEquals(List.of(option("team", "id")), result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/me/joinedTeams", stringArgumentCaptor.getValue());
    }
}
