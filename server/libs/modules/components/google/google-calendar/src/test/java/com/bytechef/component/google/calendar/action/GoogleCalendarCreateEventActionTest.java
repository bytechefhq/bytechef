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

import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ANYONE_CAN_ADD_SELF;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ATTENDEES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.COLOR_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CONFERENCE_DATA;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CONFERENCE_DATA_VERSION;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.DESCRIPTION;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.END;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_TYPE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EXTENDED_PROPERTIES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.FOCUS_TIME_PROPERTIES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.GADGET;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.GUEST_CAN_INVITE_OTHERS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.GUEST_CAN_MODIFY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.GUEST_CAN_SEE_OTHER_GUESTS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.LOCATION;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.MAX_ATTENDEES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.OUT_OF_OFFICE_PROPERTIES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.RECURRENCE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.REMINDERS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SEND_UPDATES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SEQUENCE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SOURCE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.START;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.STATUS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SUMMARY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SUPPORTS_ATTACHMENTS;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TRANSPARENCY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.VISIBILITY;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.WORKING_LOCATION_PROPERTIES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.google.calendar.util.GoogleCalendarUtils;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.ConferenceData;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventFocusTimeProperties;
import com.google.api.services.calendar.model.EventOutOfOfficeProperties;
import com.google.api.services.calendar.model.EventWorkingLocationProperties;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
class GoogleCalendarCreateEventActionTest extends AbstractGoogleCalendarActionTest {

    private final ArgumentCaptor<String> calendarIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<Integer> conferenceDataVersionArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    private final ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
    private final ArgumentCaptor<Integer> maxAttendesArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    private final Event mockedEvent = mock(Event.class);
    private final Calendar.Events mockedEvents = mock(Calendar.Events.class);
    private final Calendar.Events.Insert mockedInsert = mock(Calendar.Events.Insert.class);
    private final ArgumentCaptor<String> sendUpdatesArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<Boolean> supporstAttachmentsArgumentCaptor = ArgumentCaptor.forClass(Boolean.class);
    private final List<EventAttendee> eventAttendees = List.of(new EventAttendee());
    private final ConferenceData conferenceData = new ConferenceData();
    private final Event.ExtendedProperties extendedProperties = new Event.ExtendedProperties();
    private final GoogleCalendarUtils.EventDateTimeCustom eventDateTimeCustom =
        new GoogleCalendarUtils.EventDateTimeCustom(
            LocalDate.of(2010, 11, 10), LocalDateTime.of(2010, 11, 10, 8, 20), "timeZone");
    private final EventFocusTimeProperties eventFocusTimeProperties = new EventFocusTimeProperties();
    private final Event.Gadget gadget = new Event.Gadget();
    private final EventOutOfOfficeProperties eventOutOfOfficeProperties = new EventOutOfOfficeProperties();
    private final List<String> recurrence = List.of("recurrence");
    private final Event.Reminders reminders = new Event.Reminders();
    private final Event.Source source = new Event.Source();
    private final EventWorkingLocationProperties eventWorkingLocationProperties = new EventWorkingLocationProperties();
    private final EventDateTime eventDateTime = new EventDateTime();

    @Test
    void testPerform() throws IOException {

        when(mockedParameters.getInteger(CONFERENCE_DATA_VERSION))
            .thenReturn(1);
        when(mockedParameters.getInteger(MAX_ATTENDEES))
            .thenReturn(2);
        when(mockedParameters.getString(SEND_UPDATES))
            .thenReturn("sendUpdates");
        when(mockedParameters.getBoolean(SUPPORTS_ATTACHMENTS))
            .thenReturn(true);

        when(mockedParameters.get(START, GoogleCalendarUtils.EventDateTimeCustom.class))
            .thenReturn(eventDateTimeCustom);
        when(mockedParameters.get(END, GoogleCalendarUtils.EventDateTimeCustom.class))
            .thenReturn(eventDateTimeCustom);

        when(mockedParameters.getBoolean(ANYONE_CAN_ADD_SELF))
            .thenReturn(true);
        when(mockedParameters.getList(ATTENDEES, EventAttendee.class, List.of()))
            .thenReturn(eventAttendees);
        when(mockedParameters.getString(COLOR_ID))
            .thenReturn("2");
        when(mockedParameters.get(CONFERENCE_DATA, ConferenceData.class))
            .thenReturn(conferenceData);
        when(mockedParameters.getString(DESCRIPTION))
            .thenReturn("description");
        when(mockedParameters.getString(EVENT_TYPE))
            .thenReturn("default");
        when(mockedParameters.get(EXTENDED_PROPERTIES, Event.ExtendedProperties.class))
            .thenReturn(extendedProperties);
        when(mockedParameters.get(FOCUS_TIME_PROPERTIES, EventFocusTimeProperties.class))
            .thenReturn(eventFocusTimeProperties);
        when(mockedParameters.get(GADGET, Event.Gadget.class))
            .thenReturn(gadget);
        when(mockedParameters.getBoolean(GUEST_CAN_INVITE_OTHERS))
            .thenReturn(true);
        when(mockedParameters.getBoolean(GUEST_CAN_MODIFY))
            .thenReturn(true);
        when(mockedParameters.getBoolean(GUEST_CAN_SEE_OTHER_GUESTS))
            .thenReturn(true);
        when(mockedParameters.getString(ID))
            .thenReturn("id");
        when(mockedParameters.getString(LOCATION))
            .thenReturn("location");
        when(mockedParameters.get(OUT_OF_OFFICE_PROPERTIES, EventOutOfOfficeProperties.class))
            .thenReturn(eventOutOfOfficeProperties);
        when(mockedParameters.getList(RECURRENCE, String.class, List.of()))
            .thenReturn(recurrence);
        when(mockedParameters.get(REMINDERS, Event.Reminders.class))
            .thenReturn(reminders);
        when(mockedParameters.getInteger(SEQUENCE))
            .thenReturn(1);
        when(mockedParameters.get(SOURCE, Event.Source.class))
            .thenReturn(source);
        when(mockedParameters.getString(STATUS))
            .thenReturn("status");
        when(mockedParameters.getString(SUMMARY))
            .thenReturn("summary");
        when(mockedParameters.getString(TRANSPARENCY))
            .thenReturn("transparency");
        when(mockedParameters.getString(VISIBILITY))
            .thenReturn("visibility");
        when(mockedParameters.get(WORKING_LOCATION_PROPERTIES, EventWorkingLocationProperties.class))
            .thenReturn(eventWorkingLocationProperties);

        when(mockedCalendar.events())
            .thenReturn(mockedEvents);
        when(mockedEvents.insert(calendarIdArgumentCaptor.capture(), eventArgumentCaptor.capture()))
            .thenReturn(mockedInsert);
        when(mockedInsert.setConferenceDataVersion(conferenceDataVersionArgumentCaptor.capture()))
            .thenReturn(mockedInsert);
        when(mockedInsert.setMaxAttendees(maxAttendesArgumentCaptor.capture()))
            .thenReturn(mockedInsert);
        when(mockedInsert.setSendUpdates(sendUpdatesArgumentCaptor.capture()))
            .thenReturn(mockedInsert);
        when(mockedInsert.setSupportsAttachments(supporstAttachmentsArgumentCaptor.capture()))
            .thenReturn(mockedInsert);
        when(mockedInsert.execute())
            .thenReturn(mockedEvent);

        try (MockedStatic<GoogleCalendarUtils> googleCalendarUtilsMockedStatic = mockStatic(GoogleCalendarUtils.class)) {
            googleCalendarUtilsMockedStatic.when(() -> GoogleCalendarUtils.getCalendar(mockedParameters))
                .thenReturn(mockedCalendar);

            googleCalendarUtilsMockedStatic.when(() -> GoogleCalendarUtils.createEventDateTime(any()))
                .thenReturn(eventDateTime);

            Event result = GoogleCalendarCreateEventAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(mockedEvent, result);
            assertEquals("primary", calendarIdArgumentCaptor.getValue());
            assertEquals(1, conferenceDataVersionArgumentCaptor.getValue());
            assertEquals(2, maxAttendesArgumentCaptor.getValue());
            assertEquals("sendUpdates", sendUpdatesArgumentCaptor.getValue());
            assertEquals(true, supporstAttachmentsArgumentCaptor.getValue());

            Event event = eventArgumentCaptor.getValue();

            testEvent(event);
        }
    }

    private void testEvent(Event event) {
        assertEquals(true, event.getAnyoneCanAddSelf());
        assertEquals(eventAttendees, event.getAttendees());
        assertEquals(conferenceData, event.getConferenceData());
        assertEquals("2", event.getColorId());
        assertEquals("description", event.getDescription());
        assertEquals(eventDateTime, event.getEnd());
        assertEquals("default", event.getEventType());
        assertEquals(extendedProperties, event.getExtendedProperties());
        assertEquals(eventFocusTimeProperties, event.getFocusTimeProperties());
        assertEquals(gadget, event.getGadget());
        assertEquals(true, event.getGuestsCanInviteOthers());
        assertEquals(true, event.getGuestsCanModify());
        assertEquals(true, event.getGuestsCanSeeOtherGuests());
        assertEquals("id", event.getId());
        assertEquals("location", event.getLocation());
        assertEquals(eventOutOfOfficeProperties, event.getOutOfOfficeProperties());
        assertEquals(eventDateTime, event.getOriginalStartTime());
        assertEquals(recurrence, event.getRecurrence());
        assertEquals(reminders, event.getReminders());
        assertEquals(1, event.getSequence());
        assertEquals(source, event.getSource());
        assertEquals(eventDateTime, event.getStart());
        assertEquals("status", event.getStatus());
        assertEquals("summary", event.getSummary());
        assertEquals("transparency", event.getTransparency());
        assertEquals("visibility", event.getVisibility());
        assertEquals(eventWorkingLocationProperties, event.getWorkingLocationProperties());
    }
}
