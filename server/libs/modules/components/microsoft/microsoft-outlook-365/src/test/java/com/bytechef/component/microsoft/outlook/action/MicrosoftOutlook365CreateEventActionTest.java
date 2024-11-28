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

package com.bytechef.component.microsoft.outlook.action;

import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ADDRESS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ALL_DAY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ATTENDEES;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.DATE_TIME;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.EMAIL_ADDRESS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.END;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.IS_ONLINE_MEETING;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.REMINDER_MINUTES_BEFORE_START;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.START;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SUBJECT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.TIME_ZONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365CustomEventUtils;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365CustomEventUtils.CustomEvent;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class MicrosoftOutlook365CreateEventActionTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final Map<String, Object> bodyMap = Map.of("subject", "birthday");
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final CustomEvent mockedCustomEvent = mock(CustomEvent.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private Parameters mockedParameters;
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private MockedStatic<MicrosoftOutlook365Utils> microsoftOutlook365UtilsMockedStatic;
    private MockedStatic<MicrosoftOutlook365CustomEventUtils> microsoftOutlook365CustomEventUtilsMockedStatic;

    @BeforeEach
    void beforeEach() {
        microsoftOutlook365UtilsMockedStatic = mockStatic(MicrosoftOutlook365Utils.class);
        microsoftOutlook365CustomEventUtilsMockedStatic = mockStatic(MicrosoftOutlook365CustomEventUtils.class);

        microsoftOutlook365CustomEventUtilsMockedStatic
            .when(() -> MicrosoftOutlook365CustomEventUtils.createCustomEvent(bodyMap))
            .thenReturn(mockedCustomEvent);

        microsoftOutlook365UtilsMockedStatic
            .when(() -> MicrosoftOutlook365Utils.getMailboxTimeZone(mockedActionContext))
            .thenReturn("zone");

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(bodyMap);
    }

    @AfterEach
    void afterEach() {
        microsoftOutlook365UtilsMockedStatic.close();
        microsoftOutlook365CustomEventUtilsMockedStatic.close();
    }

    @Test
    void testPerform() {
        LocalDateTime startLocalDateTime = LocalDateTime.of(2000, 1, 1, 12, 5, 3);
        LocalDateTime endLocalDateTime = LocalDateTime.of(20000, 1, 4, 5, 6, 0);

        mockedParameters = MockParametersFactory.create(
            Map.of(
                SUBJECT, "birthday", ALL_DAY, false, START, startLocalDateTime, END, endLocalDateTime,
                ATTENDEES, List.of("attende1@mail.com"), IS_ONLINE_MEETING, false, REMINDER_MINUTES_BEFORE_START, 23));

        CustomEvent result =
            MicrosoftOutlook365CreateEventAction.perform(mockedParameters, mockedParameters, mockedActionContext);

        assertEquals(mockedCustomEvent, result);

        Http.Body body = bodyArgumentCaptor.getValue();

        Map<String, Object> expectedBody = Map.of(
            SUBJECT, "birthday", IS_ONLINE_MEETING, false, REMINDER_MINUTES_BEFORE_START, 23,
            START, Map.of(DATE_TIME, startLocalDateTime, TIME_ZONE, "zone"),
            END, Map.of(DATE_TIME, endLocalDateTime, TIME_ZONE, "zone"),
            ATTENDEES, List.of(Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "attende1@mail.com"))));

        assertEquals(expectedBody, body.getContent());
    }

    @Test
    void testPerformForAllDayEvent() {
        LocalDate startLocalDate = LocalDate.of(2000, 1, 1);
        LocalDate endLocalDate = LocalDate.of(20000, 1, 4);

        mockedParameters = MockParametersFactory.create(
            Map.of(
                SUBJECT, "birthday", ALL_DAY, true, START, startLocalDate, END, endLocalDate,
                ATTENDEES, List.of("attende1@mail.com"), IS_ONLINE_MEETING, false, REMINDER_MINUTES_BEFORE_START, 23));

        CustomEvent result =
            MicrosoftOutlook365CreateEventAction.perform(mockedParameters, mockedParameters, mockedActionContext);

        assertEquals(mockedCustomEvent, result);

        Http.Body body = bodyArgumentCaptor.getValue();

        Map<String, Object> expectedBody = Map.of(
            SUBJECT, "birthday", IS_ONLINE_MEETING, false, REMINDER_MINUTES_BEFORE_START, 23,
            START, Map.of(DATE_TIME, LocalDateTime.of(startLocalDate, LocalTime.MIN), TIME_ZONE, "zone"),
            END, Map.of(DATE_TIME, LocalDateTime.of(endLocalDate.plusDays(1), LocalTime.MIN), TIME_ZONE, "zone"),
            ATTENDEES, List.of(Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "attende1@mail.com"))));

        assertEquals(expectedBody, body.getContent());
    }
}
