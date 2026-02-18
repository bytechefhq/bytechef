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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import com.bytechef.component.youtube.util.YouTubeUtils;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
@ExtendWith(MockContextSetupExtension.class)
class YouTubeNewVideoTriggerTest {

    private final LocalDateTime mockLocalDate = LocalDateTime.of(2025, 6, 16, 15, 5);
    private final Parameters mockedParameters =
        MockParametersFactory.create(Map.of(IDENTIFIER, "testIdentifier", LAST_TIME_CHECKED, mockLocalDate));
    private final ArgumentCaptor<Object[]> queryArgumentCaptor = forClass(Object[].class);
    private final Map<String, Object> responseMap = Map.of("items", List.of(
        Map.of("id", Map.of("videoId", "1"), "snippet", Map.of())));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<TriggerContext> triggerContextArgumentCaptor = forClass(TriggerContext.class);

    @Test
    void testPoll(
        TriggerContext mockedContext, Http mockedHttp, Http.Executor mockedExecutor, Http.Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        try (MockedStatic<YouTubeUtils> youtubeUtilsMockedStatic = mockStatic(YouTubeUtils.class)) {
            youtubeUtilsMockedStatic.when(() -> YouTubeUtils.getChannelId(
                stringArgumentCaptor.capture(), triggerContextArgumentCaptor.capture()))
                .thenReturn("channelId");

            when(mockedHttp.get(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(responseMap);

            PollOutput pollOutput = YouTubeNewVideoTrigger.poll(
                mockedParameters, mockedParameters, mockedParameters, mockedContext);

            assertEquals(List.of(Map.of()), pollOutput.records());
            assertFalse(pollOutput.pollImmediately());

            assertEquals(List.of("testIdentifier", "/search"), stringArgumentCaptor.getAllValues());
            assertEquals(mockedContext, triggerContextArgumentCaptor.getValue());

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

            ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

            assertNotNull(capturedFunction);

            Http.Configuration.ConfigurationBuilder configurationBuilder =
                configurationBuilderArgumentCaptor.getValue();
            Http.Configuration configuration = configurationBuilder.build();
            Http.ResponseType responseType = configuration.getResponseType();

            assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        }
    }
}
