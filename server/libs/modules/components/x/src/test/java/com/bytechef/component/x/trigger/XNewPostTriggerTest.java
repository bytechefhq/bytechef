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

package com.bytechef.component.x.trigger;

import static com.bytechef.component.x.constant.XConstants.DATA;
import static com.bytechef.component.x.constant.XConstants.USERNAME;
import static com.bytechef.component.x.trigger.XNewPostTrigger.LAST_TIME_CHECKED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.x.util.XUtils;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class XNewPostTriggerTest {

    private final ArgumentCaptor<Http.Configuration.ConfigurationBuilder> configurationBuilderArgumentCaptor =
        forClass(Http.Configuration.ConfigurationBuilder.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final ArgumentCaptor<Integer> integerArgumentCaptor = forClass(Integer.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http mockedHttp = mock(Http.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(USERNAME, "test"));
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<ZoneId> zoneIdArgumentCaptor = forClass(ZoneId.class);

    @Test
    void testPoll() {
        LocalDateTime startDate = LocalDateTime.of(2000, 1, 1, 1, 1, 1);
        LocalDateTime endDate = LocalDateTime.of(2000, 1, 1, 2, 2, 2);
        Parameters closureParameters = MockParametersFactory.create(Map.of(LAST_TIME_CHECKED, startDate));

        try (MockedStatic<LocalDateTime> localDateTimeMockedStatic = mockStatic(LocalDateTime.class);
            MockedStatic<XUtils> xUtilsMockedStatic = mockStatic(XUtils.class)) {

            localDateTimeMockedStatic.when(() -> LocalDateTime.now(zoneIdArgumentCaptor.capture()))
                .thenReturn(endDate);
            xUtilsMockedStatic.when(() -> XUtils.getUserIdByUsername(
                contextArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn("123");

            when(closureParameters.getLocalDateTime(LAST_TIME_CHECKED))
                .thenReturn(startDate);

            when(mockedTriggerContext.http(httpFunctionArgumentCaptor.capture()))
                .thenAnswer(inv -> {
                    ContextFunction<Http, Http.Executor> value = httpFunctionArgumentCaptor.getValue();

                    return value.apply(mockedHttp);
                });
            when(mockedHttp.get(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.queryParameters(
                stringArgumentCaptor.capture(), stringArgumentCaptor.capture(),
                stringArgumentCaptor.capture(), stringArgumentCaptor.capture(),
                stringArgumentCaptor.capture(), integerArgumentCaptor.capture()))
                    .thenReturn(mockedExecutor);
            when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.execute())
                .thenReturn(mockedResponse);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(Map.of(DATA, List.of(Map.of("id", "post1"), Map.of("id", "post2"))));

            PollOutput pollOutput = XNewPostTrigger.poll(
                mockedParameters, null, closureParameters, mockedTriggerContext);

            assertEquals(List.of(Map.of("id", "post1"), Map.of("id", "post2")), pollOutput.records());
            assertFalse(pollOutput.pollImmediately());
            assertEquals(mockedTriggerContext, contextArgumentCaptor.getValue());
            assertEquals(ZoneId.of("UTC"), zoneIdArgumentCaptor.getValue());
            assertEquals(100, integerArgumentCaptor.getValue());
            assertEquals(
                List.of(
                    "test", "/users/123/tweets", "start_time", "2000-01-01T01:01:01Z", "end_time",
                    "2000-01-01T02:02:02Z", "max_results"),
                stringArgumentCaptor.getAllValues());
        }
    }
}
