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

package com.bytechef.component.youtube.trigger;

import static com.bytechef.component.youtube.constant.YoutubeConstants.IDENTIFIER;
import static com.bytechef.component.youtube.constant.YoutubeConstants.VIDEO;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.youtube.util.YoutubeUtils;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
class YoutubeNewVideoTriggerTest {
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final ArgumentCaptor<Object[]> queryArgumentCaptor = ArgumentCaptor.forClass(Object[].class);
    private final Map<String, Object> responseMap = Map.of("items", List.of(
        Map.of("id", Map.of("videoId", "1"), "snippet", Map.of())));
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<TriggerContext> triggerContextArgumentCaptor =
        ArgumentCaptor.forClass(TriggerContext.class);

    @Test
    void poll() {
        try (MockedStatic<YoutubeUtils> youtubeUtilsMockedStatic = mockStatic(YoutubeUtils.class)) {

            youtubeUtilsMockedStatic.when(() -> YoutubeUtils.getChannelId(
                stringArgumentCaptor.capture(), triggerContextArgumentCaptor.capture()))
                .thenReturn("channelId");

            when(mockedParameters.getRequiredString(IDENTIFIER))
                .thenReturn("testIdentifier");

            when(mockedTriggerContext.http(any()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.configuration(any()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.execute())
                .thenReturn(mockedResponse);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(responseMap);

            PollOutput pollOutput = YoutubeNewVideoTrigger.poll(
                mockedParameters, mockedParameters, mockedParameters, mockedTriggerContext);

            PollOutput expectedPollOutput = new PollOutput(
                List.of(Map.of()), Map.of(VIDEO, List.of("1")), false);

            assertEquals(expectedPollOutput, pollOutput);

            assertEquals("testIdentifier", stringArgumentCaptor.getValue());
            assertEquals(mockedTriggerContext, triggerContextArgumentCaptor.getValue());

            Object[] queryArguments = queryArgumentCaptor.getValue();
            Object[] expectedQueryArguments = {
                "part", "snippet",
                "channelId", "channelId",
                "type", "video",
                "order", "date"
            };

            assertArrayEquals(expectedQueryArguments, queryArguments);
        }
    }
}
