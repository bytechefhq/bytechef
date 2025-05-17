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

package com.bytechef.component.beamer.trigger;

import static com.bytechef.component.beamer.constant.BeamerConstants.DATE_FROM;
import static com.bytechef.component.beamer.constant.BeamerConstants.LAST_TIME_CHECKED;
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
class BeamerNewPostTriggerTest {

    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<ZoneId> zoneIdArgumentCaptor = ArgumentCaptor.forClass(ZoneId.class);
    private final List<Map<String, Object>> responseList = List.of(Map.of());

    @Test
    void poll() {
        LocalDateTime startDate = LocalDateTime.of(2025, 4, 3, 0, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2025, 4, 4, 0, 0, 0);

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(LAST_TIME_CHECKED, startDate.toString()));

        try (MockedStatic<LocalDateTime> localDateTimeMockedStatic = mockStatic(
            LocalDateTime.class, Mockito.CALLS_REAL_METHODS)) {

            localDateTimeMockedStatic.when(() -> LocalDateTime.now(zoneIdArgumentCaptor.capture()))
                .thenReturn(endDate);

            when(mockedTriggerContext.http(any()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.queryParameter(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.configuration(any()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.execute())
                .thenReturn(mockedResponse);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(responseList);

            PollOutput pollOutput = BeamerNewPostTrigger.poll(
                mockedParameters, mockedParameters, mockedParameters, mockedTriggerContext);

            PollOutput expectedPollOutput = new PollOutput(
                List.of(Map.of()), Map.of(LAST_TIME_CHECKED, endDate), false);

            assertEquals(expectedPollOutput, pollOutput);
            assertEquals(ZoneId.systemDefault(), zoneIdArgumentCaptor.getValue());
            assertEquals(List.of(DATE_FROM, startDate.toString()), stringArgumentCaptor.getAllValues());
        }
    }
}
