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

package com.bytechef.component.microsoft.outlook.action;

import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ADDRESS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ALL_DAY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ATTENDEES;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CALENDAR;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.DATE_TIME;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.EMAIL_ADDRESS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.END;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.IS_ONLINE_MEETING;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.REMINDER_MINUTES_BEFORE_START;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.START;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SUBJECT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.TIME_ZONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365CustomEventUtils;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365CustomEventUtils.CustomEvent;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class MicrosoftOutlook365CreateEventActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    private final Map<String, Object> bodyMap = Map.of("subject", "birthday");
    private final CustomEvent mockedCustomEvent = mock(CustomEvent.class);
    private Parameters mockedParameters;
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<Map> mapArgumentCaptor = forClass(Map.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        LocalDateTime startLocalDateTime = LocalDateTime.of(2000, 1, 1, 12, 5, 3);
        LocalDateTime endLocalDateTime = LocalDateTime.of(20000, 1, 4, 5, 6, 0);

        mockedParameters = MockParametersFactory.create(
            Map.of(
                CALENDAR, "xy", SUBJECT, "birthday", ALL_DAY, false, START, startLocalDateTime,
                END, endLocalDateTime, ATTENDEES, List.of("attende1@mail.com"), IS_ONLINE_MEETING, false,
                REMINDER_MINUTES_BEFORE_START, 23));

        try (MockedStatic<MicrosoftOutlook365Utils> microsoftOutlook365UtilsMockedStatic =
            mockStatic(MicrosoftOutlook365Utils.class);
            MockedStatic<MicrosoftOutlook365CustomEventUtils> microsoftOutlook365CustomEventUtilsMockedStatic =
                mockStatic(MicrosoftOutlook365CustomEventUtils.class)) {

            microsoftOutlook365CustomEventUtilsMockedStatic
                .when(() -> MicrosoftOutlook365CustomEventUtils.createCustomEvent(mapArgumentCaptor.capture()))
                .thenReturn(mockedCustomEvent);
            microsoftOutlook365UtilsMockedStatic
                .when(() -> MicrosoftOutlook365Utils.getMailboxTimeZone(contextArgumentCaptor.capture()))
                .thenReturn("zone");

            when(mockedHttp.post(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(bodyMap);

            CustomEvent result = MicrosoftOutlook365CreateEventAction.perform(
                mockedParameters, null, mockedContext);

            assertEquals(mockedCustomEvent, result);
            assertNotNull(httpFunctionArgumentCaptor.getValue());

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Configuration configuration = configurationBuilder.build();

            assertEquals(Http.ResponseType.JSON, configuration.getResponseType());
            assertEquals("/me/calendars/xy/events", stringArgumentCaptor.getValue());

            Map<String, Object> expectedBody = Map.of(
                SUBJECT, "birthday", IS_ONLINE_MEETING, false, REMINDER_MINUTES_BEFORE_START, 23,
                START, Map.of(DATE_TIME, startLocalDateTime, TIME_ZONE, "zone"),
                END, Map.of(DATE_TIME, endLocalDateTime, TIME_ZONE, "zone"),
                ATTENDEES, List.of(Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "attende1@mail.com"))));

            assertEquals(Body.of(expectedBody, BodyContentType.JSON), bodyArgumentCaptor.getValue());
            assertEquals(bodyMap, mapArgumentCaptor.getValue());
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
        }
    }

    @Test
    void testPerformForAllDayEvent(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        LocalDate startLocalDate = LocalDate.of(2000, 1, 1);
        LocalDate endLocalDate = LocalDate.of(20000, 1, 4);

        mockedParameters = MockParametersFactory.create(
            Map.of(
                CALENDAR, "xy", SUBJECT, "birthday", ALL_DAY, true, START, startLocalDate, END, endLocalDate,
                ATTENDEES, List.of("attende1@mail.com"), IS_ONLINE_MEETING, false, REMINDER_MINUTES_BEFORE_START, 23));

        try (MockedStatic<MicrosoftOutlook365Utils> microsoftOutlook365UtilsMockedStatic =
            mockStatic(MicrosoftOutlook365Utils.class);
            MockedStatic<MicrosoftOutlook365CustomEventUtils> microsoftOutlook365CustomEventUtilsMockedStatic =
                mockStatic(MicrosoftOutlook365CustomEventUtils.class)) {

            microsoftOutlook365CustomEventUtilsMockedStatic
                .when(() -> MicrosoftOutlook365CustomEventUtils.createCustomEvent(mapArgumentCaptor.capture()))
                .thenReturn(mockedCustomEvent);
            microsoftOutlook365UtilsMockedStatic
                .when(() -> MicrosoftOutlook365Utils.getMailboxTimeZone(contextArgumentCaptor.capture()))
                .thenReturn("zone");

            when(mockedHttp.post(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(bodyMap);

            CustomEvent result = MicrosoftOutlook365CreateEventAction.perform(
                mockedParameters, null, mockedContext);

            assertEquals(mockedCustomEvent, result);
            assertNotNull(httpFunctionArgumentCaptor.getValue());

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Configuration configuration = configurationBuilder.build();

            assertEquals(Http.ResponseType.JSON, configuration.getResponseType());
            assertEquals("/me/calendars/xy/events", stringArgumentCaptor.getValue());

            Map<String, Object> expectedBody = Map.of(
                SUBJECT, "birthday", IS_ONLINE_MEETING, false, REMINDER_MINUTES_BEFORE_START, 23,
                START, Map.of(DATE_TIME, LocalDateTime.of(startLocalDate, LocalTime.MIN), TIME_ZONE, "zone"),
                END, Map.of(DATE_TIME, LocalDateTime.of(endLocalDate.plusDays(1), LocalTime.MIN), TIME_ZONE, "zone"),
                ATTENDEES, List.of(Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "attende1@mail.com"))));

            assertEquals(Body.of(expectedBody, BodyContentType.JSON), bodyArgumentCaptor.getValue());
            assertEquals(bodyMap, mapArgumentCaptor.getValue());
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
        }
    }
}
