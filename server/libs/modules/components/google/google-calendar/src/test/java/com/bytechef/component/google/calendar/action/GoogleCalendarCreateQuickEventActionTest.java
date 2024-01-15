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
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SEND_UPDATES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.google.calendar.util.GoogleCalendarUtils;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
class GoogleCalendarCreateQuickEventActionTest extends AbstractGoogleCalendarActionTest {

    private final ArgumentCaptor<String> calendarIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> eventTextArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final Event mockedEvent = mock(Event.class);
    private final Calendar.Events mockedEvents = mock(Calendar.Events.class);
    private final Calendar.Events.QuickAdd mockedQuickAdd = mock(Calendar.Events.QuickAdd.class);
    private final ArgumentCaptor<String> sendUpdatesArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform() throws IOException {
        when(mockedParameters.getBoolean(ANYONE_CAN_ADD_SELF))
            .thenReturn(true);
        when(mockedParameters.getRequiredString(TEXT))
            .thenReturn("text");
        when(mockedParameters.getString(SEND_UPDATES))
            .thenReturn("sendUpdates");

        when(mockedCalendar.events())
            .thenReturn(mockedEvents);
        when(mockedEvents.quickAdd(calendarIdArgumentCaptor.capture(), eventTextArgumentCaptor.capture()))
            .thenReturn(mockedQuickAdd);
        when(mockedQuickAdd.setSendUpdates(sendUpdatesArgumentCaptor.capture()))
            .thenReturn(mockedQuickAdd);
        when(mockedQuickAdd.execute())
            .thenReturn(mockedEvent);

        try (MockedStatic<GoogleCalendarUtils> googleCalendarUtilsMockedStatic = mockStatic(GoogleCalendarUtils.class)) {
            googleCalendarUtilsMockedStatic.when(
                    () -> GoogleCalendarUtils.getCalendar(mockedParameters))
                .thenReturn(mockedCalendar);

            Event result = GoogleCalendarCreateQuickEventAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(mockedEvent, result);
            assertEquals("primary", calendarIdArgumentCaptor.getValue());
            assertEquals("sendUpdates", sendUpdatesArgumentCaptor.getValue());
            assertEquals("text", eventTextArgumentCaptor.getValue());
        }
    }
}
