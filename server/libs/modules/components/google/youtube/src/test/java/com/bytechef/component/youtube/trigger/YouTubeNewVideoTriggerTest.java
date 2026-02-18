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

import static com.bytechef.component.youtube.constant.YouTubeConstants.IDENTIFIER;
import static com.bytechef.component.youtube.constant.YouTubeConstants.LAST_TIME_CHECKED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.youtube.util.YouTubeUtils;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
class YouTubeNewVideoTriggerTest {

    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final LocalDateTime mockLocalDate = LocalDateTime.of(2025, 6, 16, 15, 5);
    private final ArgumentCaptor<Object[]> queryArgumentCaptor = ArgumentCaptor.forClass(Object[].class);
    private final Map<String, Object> responseMap = Map.of("items", List.of(
        Map.of("id", Map.of("videoId", "1"), "snippet", Map.of())));
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<TriggerContext> triggerContextArgumentCaptor =
        ArgumentCaptor.forClass(TriggerContext.class);
    private final Parameters mockedParameters =
        MockParametersFactory.create(Map.of(IDENTIFIER, "testIdentifier", LAST_TIME_CHECKED, mockLocalDate));

    @Test
    void testPoll() {
        try (MockedStatic<YouTubeUtils> youtubeUtilsMockedStatic = mockStatic(YouTubeUtils.class)) {
            youtubeUtilsMockedStatic.when(() -> YouTubeUtils.getChannelId(
                stringArgumentCaptor.capture(), triggerContextArgumentCaptor.capture()))
                .thenReturn("channelId");

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

            PollOutput pollOutput = YouTubeNewVideoTrigger.poll(
                mockedParameters, mockedParameters, mockedParameters, mockedTriggerContext);

            assertEquals(List.of(Map.of()), pollOutput.records());
            assertFalse(pollOutput.pollImmediately());

            assertEquals("testIdentifier", stringArgumentCaptor.getValue());
            assertEquals(mockedTriggerContext, triggerContextArgumentCaptor.getValue());

            ZoneId zoneId = ZoneId.systemDefault();
            ZonedDateTime startZonedDate = mockLocalDate.atZone(zoneId);

            Object[] queryArguments = queryArgumentCaptor.getValue();
            Object[] expectedQueryArguments = {
                "part", "snippet",
                "channelId", "channelId",
                "type", "video",
                "order", "date",
                "publishedAfter", startZonedDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            };

            assertArrayEquals(expectedQueryArguments, queryArguments);
        }
    }
}
