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

package com.bytechef.component.agile.crm.trigger;

import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.CREATED_TIME;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.LAST_TIME_CHECKED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
class AgileCrmNewTaskTriggerTest {

    private final ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor =
        forClass(ConfigurationBuilder.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http mockedHttp = mock(Http.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPoll() {
        when(mockedTriggerContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Http, Http.Executor> value = httpFunctionArgumentCaptor.getValue();

                return value.apply(mockedHttp);
            });
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        long older = Instant.parse("2025-01-01T03:30:00Z")
            .getEpochSecond();
        long equalToThreshold = Instant.parse("2025-01-01T04:00:00Z")
            .getEpochSecond();
        long newer1 = Instant.parse("2025-01-01T06:00:00Z")
            .getEpochSecond();
        long newer2 = Instant.parse("2025-01-01T05:00:00Z")
            .getEpochSecond();
        long newest = Instant.parse("2025-01-01T07:00:00Z")
            .getEpochSecond();

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                List.of(
                    Map.of(CREATED_TIME, (int) newer1),
                    Map.of(CREATED_TIME, (int) older),
                    Map.of(CREATED_TIME, (int) newest),
                    Map.of(CREATED_TIME, (int) newer2),
                    Map.of(CREATED_TIME, (int) equalToThreshold)));

        Instant mockedLastTimeChecked = Instant.parse("2025-01-01T04:00:00Z");

        Instant mockedNow = Instant.parse("2025-01-01T08:00:00Z");

        try (MockedStatic<Instant> instantMockedStatic = mockStatic(Instant.class)) {
            instantMockedStatic.when(Instant::now)
                .thenReturn(mockedNow);

            Parameters mockedParameters = MockParametersFactory.create(
                Map.of(LAST_TIME_CHECKED, mockedLastTimeChecked));

            PollOutput pollOutput = AgileCrmNewTaskTrigger.poll(null, null, mockedParameters, mockedTriggerContext);

            PollOutput expectedPollOutput = new PollOutput(
                List.of(
                    Map.of(CREATED_TIME, (int) newest),
                    Map.of(CREATED_TIME, (int) newer1),
                    Map.of(CREATED_TIME, (int) newer2),
                    Map.of(CREATED_TIME, (int) equalToThreshold)),
                Map.of(LAST_TIME_CHECKED, mockedNow),
                false);

            assertEquals(expectedPollOutput, pollOutput);

            ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

            assertNotNull(capturedFunction);

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Configuration configuration = configurationBuilder.build();
            ResponseType responseType = configuration.getResponseType();

            assertEquals(ResponseType.Type.JSON, responseType.getType());
            assertEquals("/tasks/based", stringArgumentCaptor.getValue());
        }
    }

    @Test
    void testPollNoNewTasks() {
        when(mockedTriggerContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Http, Http.Executor> value = httpFunctionArgumentCaptor.getValue();

                return value.apply(mockedHttp);
            });
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                List.of(
                    Map.of(CREATED_TIME, (int) Instant.parse("2025-01-01T00:00:00Z")
                        .getEpochSecond()),
                    Map.of(CREATED_TIME, (int) Instant.parse("2025-01-01T01:00:00Z")
                        .getEpochSecond())));

        Instant mockedLastTimeChecked = Instant.parse("2025-01-01T04:00:00Z");

        Instant mockedNow = Instant.parse("2025-01-01T05:00:00Z");

        try (MockedStatic<Instant> instantMockedStatic = mockStatic(Instant.class)) {
            instantMockedStatic.when(Instant::now)
                .thenReturn(mockedNow);

            Parameters mockedParameters = MockParametersFactory.create(
                Map.of(LAST_TIME_CHECKED, mockedLastTimeChecked));

            PollOutput pollOutput = AgileCrmNewTaskTrigger.poll(null, null, mockedParameters, mockedTriggerContext);

            PollOutput expectedPollOutput = new PollOutput(List.of(), Map.of(LAST_TIME_CHECKED, mockedNow), false);

            assertEquals(expectedPollOutput, pollOutput);

            ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

            assertNotNull(capturedFunction);

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Configuration configuration = configurationBuilder.build();
            ResponseType responseType = configuration.getResponseType();

            assertEquals(ResponseType.Type.JSON, responseType.getType());
            assertEquals("/tasks/based", stringArgumentCaptor.getValue());
        }
    }
}
