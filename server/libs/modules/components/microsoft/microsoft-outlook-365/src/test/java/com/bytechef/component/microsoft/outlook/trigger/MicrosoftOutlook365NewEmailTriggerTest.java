/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.microsoft.outlook.trigger;

import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ID;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ODATA_NEXT_LINK;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.VALUE;
import static com.bytechef.component.microsoft.outlook.trigger.MicrosoftOutlook365NewEmailTrigger.LAST_TIME_CHECKED;
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
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Monika Kušter
 */
class MicrosoftOutlook365NewEmailTriggerTest {

    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final Parameters parameters = mock(Parameters.class);
    private final ArgumentCaptor<Object[]> queryArgumentCaptor = ArgumentCaptor.forClass(Object[].class);

    @Test
    void testPoll() {
        Map<String, String> firstMail = Map.of(ID, "abc", "receivedDateTime", "2024-01-01T14:28:23Z");

        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 2, 0, 0, 0);

        try (MockedStatic<LocalDateTime> localDateTimeMockedStatic =
            mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS);

            MockedStatic<MicrosoftOutlook365Utils> microsoftOutlook365UtilsMockedStatic =
                mockStatic(MicrosoftOutlook365Utils.class)) {

            localDateTimeMockedStatic.when(LocalDateTime::now)
                .thenReturn(endDate);

            Map<String, String> secondMail = Map.of(ID, "cdf", "receivedDateTime", "2024-01-01T18:23:44Z");

            microsoftOutlook365UtilsMockedStatic
                .when(() -> MicrosoftOutlook365Utils.getItemsFromNextPage("link", mockedTriggerContext))
                .thenReturn(List.of(secondMail));

            when(parameters.getLocalDateTime(LAST_TIME_CHECKED, LocalDateTime.now()))
                .thenReturn(startDate);
            when(mockedTriggerContext.http(any()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.configuration(any()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.execute())
                .thenReturn(mockedResponse);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(Map.of(VALUE, List.of(firstMail), ODATA_NEXT_LINK, "link"));

            PollOutput pollOutput =
                MicrosoftOutlook365NewEmailTrigger.poll(parameters, parameters, parameters, mockedTriggerContext);

            assertEquals(List.of(firstMail, secondMail), pollOutput.records());
            assertFalse(pollOutput.pollImmediately());

            Object[] query = queryArgumentCaptor.getValue();

            assertEquals(List.of("$filter", "isRead eq false", "$orderby", "receivedDateTime desc"),
                Arrays.asList(query));
        }
    }
}
