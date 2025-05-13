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
import static com.bytechef.component.google.tasks.trigger.GoogleTasksNewTaskTrigger.LAST_TIME_CHECKED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
class GoogleTasksNewTaskTriggerTest {

    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPoll() {
        LocalDateTime startDate = LocalDateTime.of(2000, 1, 1, 1, 1, 1);
        Parameters mockedParameters = MockParametersFactory.create(Map.of(LAST_TIME_CHECKED, startDate));

        List<Map<String, String>> responseList = List.of(Map.of("id", "abc"));

        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("items", responseList));

        PollOutput pollOutput = GoogleTasksNewTaskTrigger.poll(
            mockedParameters, mockedParameters, mockedParameters, mockedTriggerContext);

        assertEquals(responseList, pollOutput.records());
        assertEquals(List.of("abc"), pollOutput.closureParameters()
            .get(ALL_TASKS));
        assertFalse(pollOutput.pollImmediately());

        assertEquals(List.of("updatedMin", "2000-01-01T01%3A01%3A01.000Z"), stringArgumentCaptor.getAllValues());
    }
}
