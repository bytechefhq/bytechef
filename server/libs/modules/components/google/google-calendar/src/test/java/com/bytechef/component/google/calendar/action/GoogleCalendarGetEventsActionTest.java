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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils;
import com.bytechef.component.google.calendar.util.GoogleCalendarUtils.CustomEvent;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleCalendarGetEventsActionTest {

    private final Parameters mockedParameters = mock(Parameters.class);
    private final CustomEvent mockedCustomEvent = mock(CustomEvent.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);

    @Test
    void testPerform() {
        try (MockedStatic<GoogleCalendarUtils> googleCalendarUtilsMockedStatic =
            mockStatic(GoogleCalendarUtils.class)) {

            googleCalendarUtilsMockedStatic
                .when(() -> GoogleCalendarUtils.getCustomEvents(
                    parametersArgumentCaptor.capture(), parametersArgumentCaptor.capture()))
                .thenReturn(List.of(mockedCustomEvent));

            List<CustomEvent> result = GoogleCalendarGetEventsAction.perform(
                mockedParameters, mockedParameters, mock(ActionContext.class));

            assertEquals(List.of(mockedCustomEvent), result);
            assertEquals(List.of(mockedParameters, mockedParameters), parametersArgumentCaptor.getAllValues());
        }
    }
}
