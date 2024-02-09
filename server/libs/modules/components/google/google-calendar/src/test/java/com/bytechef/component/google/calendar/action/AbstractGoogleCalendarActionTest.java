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

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.CALENDAR_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
public abstract class AbstractGoogleCalendarActionTest {

    protected ArgumentCaptor<String> calendarIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    protected Calendar mockedCalendar = mock(Calendar.class);
    protected ActionContext mockedContext = mock(ActionContext.class);
    protected Event mockedEvent = mock(Event.class);
    protected MockedStatic<GoogleServices> mockedGoogleServices;
    protected Parameters mockedParameters = mock(Parameters.class);

    @BeforeEach
    public void beforeEach() {
        mockedGoogleServices = mockStatic(GoogleServices.class);

        when(mockedParameters.getRequiredString(ACCESS_TOKEN))
            .thenReturn("accessToken");
        when(mockedParameters.getRequiredString(CALENDAR_ID))
            .thenReturn("calendarId");

        mockedGoogleServices.when(() -> GoogleServices.getCalendar(mockedParameters))
            .thenReturn(mockedCalendar);
    }

    @AfterEach
    public void afterEach() {
        assertEquals("calendarId", calendarIdArgumentCaptor.getValue());

        mockedGoogleServices.close();
    }
}
