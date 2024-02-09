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

package com.bytechef.component.google.calendar.action;

import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ALWAYS_INCLUDE_EMAIL;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_TYPE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ICAL_UID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.MAX_ATTENDEES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.MAX_RESULTS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ORDER_BY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.PAGE_TOKEN;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.PRIVATE_EXTENDED_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.Q;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SHARED_EXTENDED_PROPERTY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SHOW_HIDDEN_INVITATIONS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SINGLE_EVENTS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SYNC_TOKEN;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TIME_MAX;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TIME_MIN;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TIME_ZONE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.UPDATE_MIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.google.calendar.util.GoogleCalendarUtils;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Events;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
class GoogleCalendarGetEventsActionTest extends AbstractGoogleCalendarActionTest {

    private final ArgumentCaptor<Boolean> alwaysIncludeEmailArgumentCapture = ArgumentCaptor.forClass(Boolean.class);
    private final ArgumentCaptor<String> calendarIdArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> eventTypesArgumentCaptor = ArgumentCaptor.forClass(List.class);
    private final ArgumentCaptor<String> iCalUIDArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<Integer> maxAttendesArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    private final ArgumentCaptor<Integer> maxResultsArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    private final Events mockedEvent = mock(Events.class);
    private final Calendar.Events mockedEvents = mock(Calendar.Events.class);
    private final Calendar.Events.List mockedList = mock(Calendar.Events.List.class);
    private final ArgumentCaptor<String> orderByArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> pageTokenArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> privateExtendedPropertyArgumentCaptor = ArgumentCaptor.forClass(List.class);
    private final ArgumentCaptor<String> qArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> sharedExtendedPropertyArgumentCaptor = ArgumentCaptor.forClass(List.class);
    private final ArgumentCaptor<Boolean> showHiddenInvitationsArgumentCapture = ArgumentCaptor.forClass(Boolean.class);
    private final ArgumentCaptor<Boolean> singleEventsArgumentCapture = ArgumentCaptor.forClass(Boolean.class);
    private final ArgumentCaptor<String> syncTokenArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> timeZoneArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<DateTime> timeMaxArgumentCaptor = ArgumentCaptor.forClass(DateTime.class);
    private final ArgumentCaptor<DateTime> timeMinArgumentCaptor = ArgumentCaptor.forClass(DateTime.class);
    private final ArgumentCaptor<DateTime> updateMinArgumentCaptor = ArgumentCaptor.forClass(DateTime.class);

    @Test
    @SuppressWarnings("unchecked")
    void testPerform() throws IOException {
        List<String> eventTypes = List.of("default", "focusTime");
        List<String> privateExtendedProperties = List.of("privateExtendedProperty");
        List<String> sharedExtendedProperties = List.of("sharedExtendedProperty");

        LocalDateTime localDateTime = LocalDateTime.of(2015, Month.AUGUST, 15, 10, 10, 10);
        Date date = GoogleCalendarUtils.convertToDateViaSqlTimestamp(localDateTime);

        when(mockedParameters.getRequiredString(CALENDAR_ID))
            .thenReturn("calendarId");
        when(mockedParameters.getBoolean(ALWAYS_INCLUDE_EMAIL))
            .thenReturn(true);
        when(mockedParameters.getList(EVENT_TYPE, String.class, List.of()))
            .thenReturn(eventTypes);
        when(mockedParameters.getString(ICAL_UID))
            .thenReturn("iCalUID");
        when(mockedParameters.getInteger(MAX_ATTENDEES))
            .thenReturn(5);
        when(mockedParameters.getInteger(MAX_RESULTS))
            .thenReturn(10);
        when(mockedParameters.getString(ORDER_BY))
            .thenReturn("updated");
        when(mockedParameters.getString(PAGE_TOKEN))
            .thenReturn("pageToken");
        when(mockedParameters.getList(PRIVATE_EXTENDED_PROPERTY, String.class, List.of()))
            .thenReturn(privateExtendedProperties);
        when(mockedParameters.getString(Q))
            .thenReturn("q");
        when(mockedParameters.getList(SHARED_EXTENDED_PROPERTY, String.class, List.of()))
            .thenReturn(sharedExtendedProperties);
        when(mockedParameters.getBoolean(SHOW_HIDDEN_INVITATIONS))
            .thenReturn(true);
        when(mockedParameters.getBoolean(SINGLE_EVENTS))
            .thenReturn(true);
        when(mockedParameters.getString(SYNC_TOKEN))
            .thenReturn("syncToken");
        when(mockedParameters.getLocalDateTime(TIME_MAX))
            .thenReturn(localDateTime);
        when(mockedParameters.getLocalDateTime(TIME_MIN))
            .thenReturn(localDateTime);
        when(mockedParameters.getString(TIME_ZONE))
            .thenReturn("timeZone");
        when(mockedParameters.getLocalDateTime(UPDATE_MIN))
            .thenReturn(localDateTime);

        when(mockedCalendar.events())
            .thenReturn(mockedEvents);
        when(mockedEvents.list(calendarIdArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setAlwaysIncludeEmail(alwaysIncludeEmailArgumentCapture.capture()))
            .thenReturn(mockedList);
        when(mockedList.setEventTypes(eventTypesArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setICalUID(iCalUIDArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setMaxAttendees(maxAttendesArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setMaxResults(maxResultsArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setOrderBy(orderByArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setPageToken(pageTokenArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setPrivateExtendedProperty(privateExtendedPropertyArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setQ(qArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setSharedExtendedProperty(sharedExtendedPropertyArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setShowHiddenInvitations(showHiddenInvitationsArgumentCapture.capture()))
            .thenReturn(mockedList);
        when(mockedList.setSingleEvents(singleEventsArgumentCapture.capture()))
            .thenReturn(mockedList);
        when(mockedList.setSyncToken(syncTokenArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setTimeMax(timeMaxArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setTimeMin(timeMinArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setTimeZone(timeZoneArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setUpdatedMin(updateMinArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.execute()).thenReturn(mockedEvent);

        try (MockedStatic<GoogleCalendarUtils> googleCalendarUtilsMockedStatic =
            mockStatic(GoogleCalendarUtils.class)) {
            googleCalendarUtilsMockedStatic
                .when(() -> GoogleCalendarUtils.convertToDateViaSqlTimestamp(any()))
                .thenReturn(date);

            Events result = GoogleCalendarGetEventsAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(mockedEvent, result);

            assertEquals("calendarId", calendarIdArgumentCaptor.getValue());
            assertEquals(true, alwaysIncludeEmailArgumentCapture.getValue());
            assertEquals("iCalUID", iCalUIDArgumentCaptor.getValue());
            assertEquals(5, maxAttendesArgumentCaptor.getValue());
            assertEquals(10, maxResultsArgumentCaptor.getValue());
            assertEquals(eventTypes, eventTypesArgumentCaptor.getValue());
            assertEquals("updated", orderByArgumentCaptor.getValue());
            assertEquals("pageToken", pageTokenArgumentCaptor.getValue());
            assertEquals(privateExtendedProperties, privateExtendedPropertyArgumentCaptor.getValue());
            assertEquals("q", qArgumentCaptor.getValue());
            assertEquals(sharedExtendedProperties, sharedExtendedPropertyArgumentCaptor.getValue());
            assertEquals(true, showHiddenInvitationsArgumentCapture.getValue());
            assertEquals(true, singleEventsArgumentCapture.getValue());
            assertEquals("syncToken", syncTokenArgumentCaptor.getValue());
            assertEquals(new DateTime(date), timeMaxArgumentCaptor.getValue());
            assertEquals(new DateTime(date), timeMinArgumentCaptor.getValue());
            assertEquals("timeZone", timeZoneArgumentCaptor.getValue());
            assertEquals(new DateTime(date), updateMinArgumentCaptor.getValue());
        }
    }
}
