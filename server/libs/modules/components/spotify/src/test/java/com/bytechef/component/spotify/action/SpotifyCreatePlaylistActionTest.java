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

package com.bytechef.component.spotify.action;

import static com.bytechef.component.spotify.action.SpotifyCreatePlaylistAction.COLLABORATIVE;
import static com.bytechef.component.spotify.action.SpotifyCreatePlaylistAction.DESCRIPTION;
import static com.bytechef.component.spotify.action.SpotifyCreatePlaylistAction.PUBLIC;
import static com.bytechef.component.spotify.constant.SpotifyConstants.NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.spotify.util.SpotifyUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class SpotifyCreatePlaylistActionTest {

    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = forClass(Http.Body.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        String mockedCurrentUserId = "userId";

        Map<String, Object> map = Map.of(
            NAME, "name", DESCRIPTION, "desc", PUBLIC, true, COLLABORATIVE, true);
        Parameters parameters = MockParametersFactory.create(map);

        try (MockedStatic<SpotifyUtils> spotifyUtilsMockedStatic = mockStatic(SpotifyUtils.class)) {
            spotifyUtilsMockedStatic.when(() -> SpotifyUtils.getCurrentUserId(contextArgumentCaptor.capture()))
                .thenReturn(mockedCurrentUserId);

            when(mockedHttp.post(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(Map.of());

            Map<String, Object> result = SpotifyCreatePlaylistAction.perform(parameters, parameters, mockedContext);

            assertEquals(Map.of(), result);

            Http.Body body = bodyArgumentCaptor.getValue();

            assertEquals(map, body.getContent());

            ContextFunction<Http, Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

            assertNotNull(capturedFunction);

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

            Http.Configuration configuration = configurationBuilder.build();

            Http.ResponseType responseType = configuration.getResponseType();

            assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
            assertEquals("/users/" + mockedCurrentUserId + "/playlists", stringArgumentCaptor.getValue());
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
        }
    }
}
