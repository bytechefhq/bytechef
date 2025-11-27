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

package com.bytechef.component.google.tasks.trigger;

import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.ALL_TASKS;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.LIST_ID;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.MAX_RESULTS;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.PAGE_TOKEN;
import static com.bytechef.component.google.tasks.trigger.GoogleTasksNewTaskTrigger.LAST_TIME_CHECKED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
import org.mockito.Mockito;

/**
 * @author Marija Horvat
 * @author Monika Kušter
 */
class GoogleTasksNewTaskTriggerTest {

    private final ArgumentCaptor<Configuration.ConfigurationBuilder> configurationBuilderArgumentCaptor =
        forClass(Configuration.ConfigurationBuilder.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http mockedHttp = mock(Http.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final ArgumentCaptor<Object[]> objectArgumentCapture = forClass(Object[].class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    @SuppressWarnings("unchecked")
    void testPollForEditorEnvironment() {
        Instant startInstant = Instant.parse("2000-01-01T01:01:01Z");
        Instant endDate = Instant.parse("2000-01-02T00:00:00Z");

        Parameters inputParameters = MockParametersFactory.create(Map.of(LIST_ID, "list-1"));
        Parameters closureParameters = MockParametersFactory.create(Map.of(LAST_TIME_CHECKED, startInstant));

        List<Map<String, String>> responseList = List.of(Map.of("id", "abc"));

        try (MockedStatic<Instant> instantMockedStatic = mockStatic(Instant.class, Mockito.CALLS_REAL_METHODS)) {
            instantMockedStatic.when(Instant::now)
                .thenReturn(endDate);

            when(mockedTriggerContext.isEditorEnvironment())
                .thenReturn(true);
            when(mockedTriggerContext.http(httpFunctionArgumentCaptor.capture()))
                .thenAnswer(inv -> {
                    ContextFunction<Http, Http.Executor> value = httpFunctionArgumentCaptor.getValue();

                    return value.apply(mockedHttp);
                });
            when(mockedHttp.get(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.queryParameters(objectArgumentCapture.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.execute())
                .thenReturn(mockedResponse);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(Map.of("items", responseList));

            PollOutput pollOutput = GoogleTasksNewTaskTrigger.poll(
                inputParameters, null, closureParameters, mockedTriggerContext);

            assertEquals(
                new PollOutput(
                    responseList,
                    Map.of(ALL_TASKS, List.of("abc"), LAST_TIME_CHECKED, endDate), false),
                pollOutput);

            ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

            assertNotNull(capturedFunction);

            Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Configuration configuration = configurationBuilder.build();
            Http.ResponseType responseType = configuration.getResponseType();

            assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
            assertEquals("https://tasks.googleapis.com/tasks/v1/lists/list-1/tasks", stringArgumentCaptor.getValue());

            Object[] objects = new Object[] {
                PAGE_TOKEN, null,
                MAX_RESULTS, 1,
                "updatedMin", "2000-01-01T01:01:01Z"
            };

            assertArrayEquals(objects, objectArgumentCapture.getValue());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPollForNonEditorInitialLoadWithPagination() {
        Instant now = Instant.parse("2025-01-01T00:00:00Z");

        Parameters inputParameters = MockParametersFactory.create(Map.of(LIST_ID, "list-1"));
        Parameters closureParameters = MockParametersFactory.create(Map.of());

        try (MockedStatic<Instant> instantMockedStatic = mockStatic(Instant.class, Mockito.CALLS_REAL_METHODS)) {
            instantMockedStatic.when(Instant::now)
                .thenReturn(now);

            when(mockedTriggerContext.isEditorEnvironment())
                .thenReturn(false);

            when(mockedTriggerContext.http(httpFunctionArgumentCaptor.capture()))
                .thenAnswer(inv -> {
                    ContextFunction<Http, Http.Executor> value = httpFunctionArgumentCaptor.getValue();
                    return value.apply(mockedHttp);
                });
            when(mockedHttp.get(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.queryParameters(objectArgumentCapture.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.execute())
                .thenReturn(mockedResponse);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(
                    Map.of("items", List.of(Map.of("id", "abc")), "nextPageToken", "next-1"),
                    Map.of("items", List.of(Map.of("id", "xyz"))));

            PollOutput pollOutput = GoogleTasksNewTaskTrigger.poll(
                inputParameters, null, closureParameters, mockedTriggerContext);

            assertEquals(
                new PollOutput(
                    List.of(), Map.of(ALL_TASKS, List.of("abc", "xyz"), LAST_TIME_CHECKED, now), false),
                pollOutput);

            ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

            assertNotNull(capturedFunction);

            Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

            Configuration configuration = configurationBuilder.build();

            Http.ResponseType responseType = configuration.getResponseType();

            assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
            assertEquals(
                List.of("https://tasks.googleapis.com/tasks/v1/lists/list-1/tasks",
                    "https://tasks.googleapis.com/tasks/v1/lists/list-1/tasks"),
                stringArgumentCaptor.getAllValues());

            List<Object[]> allQueryParams = objectArgumentCapture.getAllValues();
            assertEquals(2, allQueryParams.size());

            Object[] firstCall = allQueryParams.get(0);
            Object[] secondCall = allQueryParams.get(1);

            assertArrayEquals(new Object[] {
                PAGE_TOKEN, null,
                MAX_RESULTS, 100,
                "updatedMin", null
            }, firstCall);

            assertArrayEquals(new Object[] {
                PAGE_TOKEN, "next-1",
                MAX_RESULTS, 100,
                "updatedMin", null
            }, secondCall);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPollForNonEditorSubsequentPollWithUpdatedMin() {
        Instant previous = Instant.parse("2024-12-31T23:59:00Z");
        Instant now = Instant.parse("2025-01-01T00:00:00Z");

        Parameters inputParameters = MockParametersFactory.create(Map.of(LIST_ID, "list-1"));
        Parameters closureParameters = MockParametersFactory.create(
            Map.of(ALL_TASKS, List.of("abc"), LAST_TIME_CHECKED, previous));

        try (MockedStatic<Instant> instantMockedStatic = mockStatic(Instant.class, Mockito.CALLS_REAL_METHODS)) {
            instantMockedStatic.when(Instant::now)
                .thenReturn(now);

            when(mockedTriggerContext.isEditorEnvironment())
                .thenReturn(false);

            when(mockedTriggerContext.http(httpFunctionArgumentCaptor.capture()))
                .thenAnswer(inv -> {
                    ContextFunction<Http, Http.Executor> value = httpFunctionArgumentCaptor.getValue();
                    return value.apply(mockedHttp);
                });
            when(mockedHttp.get(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.queryParameters(objectArgumentCapture.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.execute())
                .thenReturn(mockedResponse);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(Map.of("items", List.of(Map.of("id", "abc"), Map.of("id", "xyz"))));

            PollOutput pollOutput = GoogleTasksNewTaskTrigger.poll(
                inputParameters, null, closureParameters, mockedTriggerContext);

            assertEquals(
                new PollOutput(
                    List.of(Map.of("id", "xyz")), Map.of(ALL_TASKS, List.of("abc", "xyz"), LAST_TIME_CHECKED, now),
                    false),
                pollOutput);

            Configuration.ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

            Configuration configuration = configurationBuilder.build();

            Http.ResponseType responseType = configuration.getResponseType();

            assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
            assertEquals("https://tasks.googleapis.com/tasks/v1/lists/list-1/tasks", stringArgumentCaptor.getValue());

            Object[] expectedQuery = new Object[] {
                PAGE_TOKEN, null,
                MAX_RESULTS, 100,
                "updatedMin", previous.toString()
            };

            assertArrayEquals(expectedQuery, objectArgumentCapture.getValue());
        }
    }
}
