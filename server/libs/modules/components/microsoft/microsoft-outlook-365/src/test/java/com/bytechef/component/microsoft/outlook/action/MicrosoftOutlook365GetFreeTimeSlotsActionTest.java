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

package com.bytechef.component.microsoft.outlook.action;

import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.DATE_RANGE;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.FROM;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.TO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.microsoft.outlook.action.MicrosoftOutlook365GetFreeTimeSlotsAction.Interval;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365CustomEventUtils;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365CustomEventUtils.CustomEvent;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class MicrosoftOutlook365GetFreeTimeSlotsActionTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(DATE_RANGE,
            Map.of(FROM, LocalDateTime.of(2000, 1, 14, 8, 0, 0), TO, LocalDateTime.of(2000, 1, 20, 8, 0, 0))));

    @Test
    void testPerform() {
        List<CustomEvent> customEvents = createCustomEvents();

        try (MockedStatic<MicrosoftOutlook365CustomEventUtils> microsoftOutlook365CustomEventUtilsMockedStatic =
            mockStatic(MicrosoftOutlook365CustomEventUtils.class)) {

            microsoftOutlook365CustomEventUtilsMockedStatic
                .when(() -> MicrosoftOutlook365CustomEventUtils.retrieveCustomEvents(mockedParameters,
                    mockedActionContext))
                .thenReturn(customEvents);

            List<Interval> result =
                MicrosoftOutlook365GetFreeTimeSlotsAction.perform(mockedParameters, mockedParameters,
                    mockedActionContext);

            List<Interval> expectedIntervals = new ArrayList<>();

            expectedIntervals
                .add(new Interval(LocalDateTime.of(2000, 1, 16, 9, 30, 0), LocalDateTime.of(2000, 1, 16, 9, 45, 0)));
            expectedIntervals
                .add(new Interval(LocalDateTime.of(2000, 1, 16, 10, 45, 0), LocalDateTime.of(2000, 1, 19, 8, 0, 0)));

            assertEquals(expectedIntervals, result);
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
            .add(createCustomEvent(LocalDateTime.of(2000, 1, 19, 8, 0, 0), LocalDateTime.of(2000, 1, 21, 0, 0, 0)));

        return customEvents;
    }

    private static CustomEvent createCustomEvent(LocalDateTime startTime, LocalDateTime endTime) {
        return new CustomEvent(null, null, null, startTime, endTime, null, false, null, 2);
    }
}
