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

package com.bytechef.component.google.calendar.action;

import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.SEND_UPDATES;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils.CustomEvent;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleCalendarCreateQuickEventActionTest {

    private final ArgumentCaptor<Calendar> calendarArgumentCaptor = ArgumentCaptor.forClass(Calendar.class);
    private final ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
    private final Calendar mockedCalendar = mock(Calendar.class);
    private final CustomEvent mockedCustomEvent = mock(CustomEvent.class);
    private final Event mockedEvent = mock(Event.class);
    private final Calendar.Events mockedEvents = mock(Calendar.Events.class);
    private final Calendar.Events.QuickAdd mockedQuickAdd = mock(Calendar.Events.QuickAdd.class);
    private final Parameters parameters = MockParametersFactory.create(
        Map.of(CALENDAR_ID, "calendarId", TEXT, "text", SEND_UPDATES, "sendUpdates"));
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform() throws IOException {
        String calendarTimezone = "Europe/Zagreb";

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class);
            MockedStatic<GoogleCalendarUtils> googleCalendarUtilsMockedStatic = mockStatic(GoogleCalendarUtils.class);
            MockedStatic<GoogleUtils> googleUtilsMockedStatic = mockStatic(GoogleUtils.class)) {

            googleServicesMockedStatic.when(() -> GoogleServices.getCalendar(parametersArgumentCaptor.capture()))
                .thenReturn(mockedCalendar);
            googleUtilsMockedStatic
                .when(() -> GoogleUtils.getCalendarTimezone(calendarArgumentCaptor.capture()))
                .thenReturn(calendarTimezone);
            googleCalendarUtilsMockedStatic
                .when(() -> GoogleCalendarUtils.createCustomEvent(
                    eventArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(mockedCustomEvent);

            when(mockedCalendar.events())
                .thenReturn(mockedEvents);
            when(mockedEvents.quickAdd(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(mockedQuickAdd);
            when(mockedQuickAdd.setSendUpdates(stringArgumentCaptor.capture()))
                .thenReturn(mockedQuickAdd);
            when(mockedQuickAdd.execute())
                .thenReturn(mockedEvent);

            CustomEvent result = GoogleCalendarCreateQuickEventAction.perform(
                parameters, parameters, mock(ActionContext.class));

            assertEquals(mockedCustomEvent, result);

            assertEquals(parameters, parametersArgumentCaptor.getValue());
            assertEquals(mockedCalendar, calendarArgumentCaptor.getValue());
            assertEquals(mockedEvent, eventArgumentCaptor.getValue());
            assertEquals(
                List.of("calendarId", "text", "sendUpdates", calendarTimezone), stringArgumentCaptor.getAllValues());
        }
    }
}
