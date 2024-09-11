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

import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.EVENT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.calendar.Calendar;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleCalendarDeleteEventActionTest {

    private final ArgumentCaptor<String> calendarIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> eventIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final Calendar mockedCalendar = mock(Calendar.class);
    private final Calendar.Events mockedEvents = mock(Calendar.Events.class);
    private final Calendar.Events.Delete mockedDelete = mock(Calendar.Events.Delete.class);
    private final Parameters parameters = MockParametersFactory.create(
        Map.of(CALENDAR_ID, "calendarId", EVENT_ID, "id"));

    @Test
    void testPerform() throws IOException {
        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {

            googleServicesMockedStatic.when(() -> GoogleServices.getCalendar(parameters))
                .thenReturn(mockedCalendar);

            when(mockedCalendar.events())
                .thenReturn(mockedEvents);
            when(mockedEvents.delete(calendarIdArgumentCaptor.capture(), eventIdArgumentCaptor.capture()))
                .thenReturn(mockedDelete);

            Object result = GoogleCalendarDeleteEventAction.perform(parameters, parameters, mock(ActionContext.class));

            assertNull(result);

            assertEquals("calendarId", calendarIdArgumentCaptor.getValue());
            assertEquals("id", eventIdArgumentCaptor.getValue());

            verify(mockedDelete, times(1)).execute();
        }
    }
}
