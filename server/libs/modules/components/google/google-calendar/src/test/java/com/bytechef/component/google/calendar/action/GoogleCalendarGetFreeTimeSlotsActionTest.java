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

import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.DATE_RANGE;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.FROM;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.LOCAL_TIME_MIN;
import static com.bytechef.component.google.calendar.constant.GoogleCalendarConstants.TO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.calendar.action.GoogleCalendarGetFreeTimeSlotsAction.Interval;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils.CustomEvent;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleCalendarGetFreeTimeSlotsActionTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(DATE_RANGE,
            Map.of(FROM, LocalDateTime.of(2000, 1, 14, 8, 0, 0), TO, LocalDateTime.of(2000, 1, 20, 8, 0, 0))));
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);

    @Test
    void testPerform() {
        List<CustomEvent> customEvents = createCustomEvents();

        try (MockedStatic<GoogleCalendarUtils> googleCalendarUtilsMockedStatic =
            mockStatic(GoogleCalendarUtils.class)) {

            googleCalendarUtilsMockedStatic
                .when(() -> GoogleCalendarUtils.getCustomEvents(
                    parametersArgumentCaptor.capture(), parametersArgumentCaptor.capture()))
                .thenReturn(customEvents);

            List<Interval> result = GoogleCalendarGetFreeTimeSlotsAction.perform(
                mockedParameters, mockedParameters, mockedActionContext);

            List<Interval> expectedIntervals = new ArrayList<>();

            expectedIntervals
                .add(new Interval(LocalDateTime.of(2000, 1, 16, 9, 30, 0), LocalDateTime.of(2000, 1, 16, 9, 45, 0)));
            expectedIntervals
                .add(new Interval(LocalDateTime.of(2000, 1, 16, 10, 45, 0),
                    LocalDateTime.of(LocalDate.of(2000, 1, 18), LOCAL_TIME_MIN)));
            expectedIntervals
                .add(new Interval(LocalDateTime.of(LocalDate.of(2000, 1, 19), LOCAL_TIME_MIN),
                    LocalDateTime.of(2000, 1, 19, 8, 0, 0)));
            expectedIntervals
                .add(new Interval(LocalDateTime.of(2000, 1, 20, 7, 0, 0), LocalDateTime.of(2000, 1, 20, 8, 0, 0)));

            assertEquals(expectedIntervals, result);
            assertEquals(List.of(mockedParameters, mockedParameters), parametersArgumentCaptor.getAllValues());
        }
    }

    private static List<CustomEvent> createCustomEvents() {
        List<CustomEvent> customEvents = new ArrayList<>();

        customEvents
            .add(createCustomEvent(LocalDateTime.of(2000, 1, 13, 2, 2, 2), LocalDateTime.of(2000, 1, 16, 9, 0, 0)));
        customEvents
            .add(createCustomEvent(LocalDateTime.of(2000, 1, 16, 8, 30, 0), LocalDateTime.of(2000, 1, 16, 9, 30, 0)));
        customEvents
            .add(createCustomEvent(LocalDateTime.of(2000, 1, 16, 9, 45, 0), LocalDateTime.of(2000, 1, 16, 10, 45, 0)));
        customEvents
            .add(createCustomEvent(LocalDate.of(2000, 1, 18), LocalDate.of(2000, 1, 19)));
        customEvents
            .add(createCustomEvent(LocalDateTime.of(2000, 1, 19, 8, 0, 0), LocalDateTime.of(2000, 1, 20, 7, 0, 0)));

        return customEvents;
    }

    private static CustomEvent createCustomEvent(Temporal startTime, Temporal endTime) {
        return new CustomEvent(null, null, null, null, startTime, endTime, null, null, null, null, null, null, null,
            null, null);
    }

    @Test
    void testPerformWhenThereIsNoFreeTime() {
        try (MockedStatic<GoogleCalendarUtils> googleCalendarUtilsMockedStatic =
            mockStatic(GoogleCalendarUtils.class)) {

            googleCalendarUtilsMockedStatic
                .when(() -> GoogleCalendarUtils.getCustomEvents(
                    parametersArgumentCaptor.capture(), parametersArgumentCaptor.capture()))
                .thenReturn(List.of(
                    createCustomEvent(LocalDateTime.of(2000, 1, 13, 2, 2, 2), LocalDateTime.of(2000, 1, 21, 9, 0, 0))));

            List<Interval> result = GoogleCalendarGetFreeTimeSlotsAction.perform(
                mockedParameters, mockedParameters, mockedActionContext);

            assertEquals(List.of(), result);
            assertEquals(List.of(mockedParameters, mockedParameters), parametersArgumentCaptor.getAllValues());
        }
    }
}
