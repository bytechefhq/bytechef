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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Nikolina Spehar
 */
class AgileCrmNewTaskTriggerTest {
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final long mockedCreatedTime = LocalDateTime.of(
        2025, 12, 8, 0, 0, 0)
        .atZone(ZoneId.systemDefault())
        .toEpochSecond();
    private final ArgumentCaptor<ZoneId> zoneIdArgumentCaptor = ArgumentCaptor.forClass(ZoneId.class);
    private final List<Map<String, Object>> responseList = List.of(Map.of(CREATED_TIME, (int) mockedCreatedTime));

    @Test
    void poll() {
        LocalDateTime mockedLastTimeChecked = LocalDateTime.of(
            2025, 12, 7, 0, 0, 0);

        LocalDateTime mockedNow = LocalDateTime.of(2025, 12, 9, 0, 0, 0);

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(LAST_TIME_CHECKED, mockedLastTimeChecked.toString()));

        try (MockedStatic<LocalDateTime> localDateTimeMockedStatic = mockStatic(
            LocalDateTime.class, Mockito.CALLS_REAL_METHODS)) {

            localDateTimeMockedStatic.when(() -> LocalDateTime.now(zoneIdArgumentCaptor.capture()))
                .thenReturn(mockedNow);

            when(mockedTriggerContext.http(any()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.configuration(any()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.execute())
                .thenReturn(mockedResponse);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(responseList);

            PollOutput pollOutput = AgileCrmNewTaskTrigger.poll(
                mockedParameters, mockedParameters, mockedParameters, mockedTriggerContext);

            PollOutput expectedPollOutput = new PollOutput(
                List.of(Map.of(CREATED_TIME, (int) mockedCreatedTime)),
                Map.of(LAST_TIME_CHECKED, mockedNow),
                false);

            assertEquals(expectedPollOutput, pollOutput);
            assertEquals(ZoneId.systemDefault(), zoneIdArgumentCaptor.getValue());
        }
    }
}
