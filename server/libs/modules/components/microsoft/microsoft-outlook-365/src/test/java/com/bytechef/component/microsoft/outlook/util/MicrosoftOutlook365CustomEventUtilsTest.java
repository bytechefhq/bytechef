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

package com.bytechef.component.microsoft.outlook.util;

import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ADDRESS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ATTENDEES;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.DATE_RANGE;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.DATE_TIME;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.EMAIL_ADDRESS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.END;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.FROM;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ID;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.IS_ONLINE_MEETING;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.I_CAL_UID;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.REMINDER_MINUTES_BEFORE_START;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.START;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SUBJECT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.TO;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.VALUE;
import static com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365CustomEventUtils.createCustomEvent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class MicrosoftOutlook365CustomEventUtilsTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final ArgumentCaptor<String> nameArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> valueArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testCreateCustomEvent() {
        Map<?, ?> eventmap = Map.of(
            I_CAL_UID, "icaluid", ID, "id", SUBJECT, "subj", IS_ONLINE_MEETING, true, REMINDER_MINUTES_BEFORE_START, 23,
            START, Map.of(DATE_TIME, "2024-11-23T07:45:22.0000000"),
            END, Map.of(DATE_TIME, "2024-11-23T10:33:23.0000000"),
            ATTENDEES, List.of(Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "attendee1"))), "onlineMeeting",
            Map.of("joinUrl", "url"));

        MicrosoftOutlook365CustomEventUtils.CustomEvent customEvent = createCustomEvent(eventmap);

        assertEquals(eventmap.get(I_CAL_UID), customEvent.iCalUId());
        assertEquals(eventmap.get(ID), customEvent.id());
        assertEquals(eventmap.get(SUBJECT), customEvent.subject());
        assertEquals(LocalDateTime.of(2024, 11, 23, 7, 45, 22), customEvent.startTime());
        assertEquals(LocalDateTime.of(2024, 11, 23, 10, 33, 23), customEvent.endTime());
        assertEquals(List.of("attendee1"), customEvent.attendees());
        assertTrue(customEvent.isOnlineMeeting());
        assertEquals("url", customEvent.onlineMeetingUrl());
        assertEquals(eventmap.get(REMINDER_MINUTES_BEFORE_START), customEvent.reminderMinutesBeforeStart());
    }

    @Test
    void testRetrieveCustomEvents() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(
                DATE_RANGE,
                Map.of(
                    FROM, LocalDateTime.of(2024, Month.SEPTEMBER, 3, 12, 0, 0),
                    TO, LocalDateTime.of(2024, Month.SEPTEMBER, 5, 10, 0, 0))));

        Map<?, ?> e1 = Map.of(
            IS_ONLINE_MEETING, false, ATTENDEES, List.of(), I_CAL_UID, "ical", ID, "id", SUBJECT, "sub",
            REMINDER_MINUTES_BEFORE_START, 1,
            START, Map.of(DATE_TIME, "2024-09-03T11:30:00.0000000"),
            END, Map.of(DATE_TIME, "2024-09-03T12:30:00.0000000"));

        Map<?, ?> e2 = Map.of(
            IS_ONLINE_MEETING, false, ATTENDEES, List.of(), I_CAL_UID, "ical", ID, "id", SUBJECT, "sub",
            REMINDER_MINUTES_BEFORE_START, 1,
            START, Map.of(DATE_TIME, "2024-09-03T07:00:00.0000000"),
            END, Map.of(DATE_TIME, "2024-09-05T09:00:00.0000000"));

        Map<?, ?> e3 = Map.of(
            IS_ONLINE_MEETING, false, ATTENDEES, List.of(), I_CAL_UID, "ical", ID, "id", SUBJECT, "sub",
            REMINDER_MINUTES_BEFORE_START, 1,
            START, Map.of(DATE_TIME, "2024-09-05T08:00:00.0000000"),
            END, Map.of(DATE_TIME, "2024-09-05T09:00:00.0000000"));

        Map<?, ?> e4 = Map.of(
            IS_ONLINE_MEETING, false, ATTENDEES, List.of(), I_CAL_UID, "ical", ID, "id", SUBJECT, "sub",
            REMINDER_MINUTES_BEFORE_START, 1,
            START, Map.of(DATE_TIME, "2024-09-05T09:30:00.0000000"),
            END, Map.of(DATE_TIME, "2024-09-05T10:30:00.0000000"));

        Map<?, ?> e5 = Map.of(
            IS_ONLINE_MEETING, false, ATTENDEES, List.of(), I_CAL_UID, "ical", ID, "id", SUBJECT, "sub",
            REMINDER_MINUTES_BEFORE_START, 1,
            START, Map.of(DATE_TIME, "2024-07-23T06:30:00.0000000"),
            END, Map.of(DATE_TIME, "2024-07-23T07:30:00.0000000"));

        Map<?, ?> e6 = Map.of(
            IS_ONLINE_MEETING, false, ATTENDEES, List.of(), I_CAL_UID, "ical", ID, "id", SUBJECT, "sub",
            REMINDER_MINUTES_BEFORE_START, 1,
            START, Map.of(DATE_TIME, "2024-09-01T09:00:00.0000000"),
            END, Map.of(DATE_TIME, "2024-09-01T10:00:00.0000000"));

        Map<?, ?> e7 = Map.of(
            IS_ONLINE_MEETING, false, ATTENDEES, List.of(), I_CAL_UID, "ical", ID, "id", SUBJECT, "sub",
            REMINDER_MINUTES_BEFORE_START, 1,
            START, Map.of(DATE_TIME, "2024-09-02T04:00:00.0000000"),
            END, Map.of(DATE_TIME, "2024-09-02T05:00:00.0000000"));

        Map<?, ?> e8 = Map.of(
            IS_ONLINE_MEETING, false, ATTENDEES, List.of(), I_CAL_UID, "ical", ID, "id", SUBJECT, "sub",
            REMINDER_MINUTES_BEFORE_START, 1,
            START, Map.of(DATE_TIME, "2024-09-05T11:00:00.0000000"),
            END, Map.of(DATE_TIME, "2024-09-05T12:00:00.0000000"));

        Map<String, Object> body = Map.of(VALUE, List.of(e1, e2, e3, e4, e5, e6, e7, e8));

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.header(nameArgumentCaptor.capture(), valueArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(body);

        try (MockedStatic<MicrosoftOutlook365Utils> microsoftOutlook365UtilsMockedStatic =
            mockStatic(MicrosoftOutlook365Utils.class)) {

            microsoftOutlook365UtilsMockedStatic
                .when(() -> MicrosoftOutlook365Utils.getMailboxTimeZone(mockedActionContext))
                .thenReturn("zone");

            microsoftOutlook365UtilsMockedStatic
                .when(() -> MicrosoftOutlook365Utils.getItemsFromNextPage("link", mockedActionContext))
                .thenReturn(List.of());

            List<MicrosoftOutlook365CustomEventUtils.CustomEvent> customEvents =
                MicrosoftOutlook365CustomEventUtils.retrieveCustomEvents(mockedParameters, mockedActionContext);

            assertEquals(
                List.of(createCustomEvent(e1), createCustomEvent(e2), createCustomEvent(e3), createCustomEvent(e4)),
                customEvents);

            assertEquals("Prefer", nameArgumentCaptor.getValue());
            assertEquals("outlook.timezone=\"zone\"", valueArgumentCaptor.getValue());
        }
    }
}
