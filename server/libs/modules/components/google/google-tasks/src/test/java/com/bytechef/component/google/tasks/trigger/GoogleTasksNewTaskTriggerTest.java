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

import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.LIST_ID;
import static com.bytechef.component.google.tasks.trigger.GoogleTasksNewTaskTrigger.LAST_TIME_CHECKED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.google.tasks.util.GoogleTasksUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Marija Horvat
 */
class GoogleTasksNewTaskTriggerTest {

    private final ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPoll() {
        LocalDateTime startDate = LocalDateTime.of(2000, 1, 1, 1, 1, 1);

        Parameters mockedParameters =
            MockParametersFactory.create(Map.of(LAST_TIME_CHECKED, startDate, LIST_ID, "listId"));

        try (MockedStatic<GoogleTasksUtils> googleTasksUtilsMockedStatic = mockStatic(GoogleTasksUtils.class)) {
            List<Map<String, String>> responseList = List.of(Map.of("id", "abc"));

            googleTasksUtilsMockedStatic.when(() -> GoogleTasksUtils.getTasks(
                contextArgumentCaptor.capture(), stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(responseList);

            TriggerDefinition.PollOutput pollOutput = GoogleTasksNewTaskTrigger.poll(
                mockedParameters, mockedParameters, mockedParameters, mockedTriggerContext);

            assertEquals(responseList, pollOutput.records());
            assertFalse(pollOutput.pollImmediately());

            assertEquals(mockedTriggerContext, contextArgumentCaptor.getValue());
            assertEquals(List.of("listId", "2000-01-01T01:01:01.000Z"), stringArgumentCaptor.getAllValues());
        }
    }
}
