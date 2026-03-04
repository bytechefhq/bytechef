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

package com.bytechef.component.microsoft.teams.action;

import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.ATTACHMENTS;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.BODY;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CHANNEL_ID;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CONTENT;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CONTENT_TYPE;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.TEAM_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.microsoft.teams.util.MicrosoftTeamsUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
@ExtendWith(MockContextSetupExtension.class)
class MicrosoftTeamsSendChannelMessageActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Http.Body.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    private final ArgumentCaptor<Object> listArgumentCaptor = forClass(Object.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(ATTACHMENTS, List.of("fileId"), TEAM_ID, "teamId", CHANNEL_ID, "channelId", CONTENT, "content",
            CONTENT_TYPE, "contentType"));
    private final Map<String, Object> responseMap = Map.of("key", "value");
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context context, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        try (
            MockedStatic<MicrosoftTeamsUtils> microsoftTeamsUtilsMockedStatic =
                mockStatic(MicrosoftTeamsUtils.class, CALLS_REAL_METHODS)) {

            microsoftTeamsUtilsMockedStatic.when(
                () -> MicrosoftTeamsUtils.getAttachmentsList(
                    (List<String>) listArgumentCaptor.capture(), contextArgumentCaptor.capture()))
                .thenReturn(List.of());

            when(mockedHttp.post(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(responseMap);

            Object result = MicrosoftTeamsSendChannelMessageAction.perform(mockedParameters, mockedParameters, context);

            assertEquals(responseMap, result);

            Http.Body body = bodyArgumentCaptor.getValue();

            assertEquals(
                Map.of(BODY, Map.of(CONTENT_TYPE, "contentType", CONTENT, "content"), ATTACHMENTS, List.of()),
                body.getContent());
            assertNotNull(httpFunctionArgumentCaptor.getValue());

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Configuration configuration = configurationBuilder.build();

            assertEquals(List.of("fileId"), listArgumentCaptor.getValue());
            assertEquals(ResponseType.JSON, configuration.getResponseType());
            assertEquals(context, contextArgumentCaptor.getValue());
            assertEquals("/teams/teamId/channels/channelId/messages", stringArgumentCaptor.getValue());
        }
    }
}
