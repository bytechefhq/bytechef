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

package com.bytechef.component.date.helper.action;

import static com.bytechef.component.date.helper.action.DateHelperDateDifferenceAction.END_DATE;
import static com.bytechef.component.date.helper.action.DateHelperDateDifferenceAction.START_DATE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DAY;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.HOUR;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MINUTE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MONTH;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.SECOND;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.UNIT;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.YEAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class DateHelperDateDifferenceActionTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private Parameters mockedParameters;

    @Test
    void testPerformYears() {
        mockedParameters = MockParametersFactory.create(
            Map.of(START_DATE, LocalDateTime.of(2020, 1, 1, 0, 0, 0), END_DATE, LocalDateTime.of(2023, 1, 10, 0, 0, 0),
                UNIT, YEAR));

        Long result =
            DateHelperDateDifferenceAction.perform(mockedParameters, mockedParameters, mockedActionContext);

        assertEquals(3, result);
    }

    @Test
    void testPerformMonths() {
        mockedParameters = MockParametersFactory.create(
            Map.of(START_DATE, LocalDateTime.of(2023, 1, 1, 0, 0, 0), END_DATE, LocalDateTime.of(2023, 4, 10, 0, 0, 0),
                UNIT, MONTH));

        Long result =
            DateHelperDateDifferenceAction.perform(mockedParameters, mockedParameters, mockedActionContext);

        assertEquals(3, result);
    }

    @Test
    void testPerformDays() {
        mockedParameters = MockParametersFactory.create(
            Map.of(START_DATE, LocalDateTime.of(2023, 1, 1, 0, 0, 0), END_DATE, LocalDateTime.of(2023, 1, 10, 0, 0, 0),
                UNIT, DAY));

        Long result =
            DateHelperDateDifferenceAction.perform(mockedParameters, mockedParameters, mockedActionContext);

        assertEquals(9, result);
    }

    @Test
    void testPerformHours() {
        mockedParameters = MockParametersFactory.create(
            Map.of(START_DATE, LocalDateTime.of(2023, 1, 1, 0, 0, 0), END_DATE, LocalDateTime.of(2023, 1, 1, 12, 0, 0),
                UNIT, HOUR));

        Long result =
            DateHelperDateDifferenceAction.perform(mockedParameters, mockedParameters, mockedActionContext);

        assertEquals(12, result);
    }

    @Test
    void testPerformMinutes() {
        mockedParameters = MockParametersFactory.create(
            Map.of(START_DATE, LocalDateTime.of(2023, 1, 1, 0, 0, 0), END_DATE, LocalDateTime.of(2023, 1, 1, 1, 30, 0),
                UNIT, MINUTE));

        Long result =
            DateHelperDateDifferenceAction.perform(mockedParameters, mockedParameters, mockedActionContext);

        assertEquals(90, result);
    }

    @Test
    void testPerformSeconds() {
        mockedParameters = MockParametersFactory.create(
            Map.of(START_DATE, LocalDateTime.of(2023, 1, 1, 0, 0, 0), END_DATE, LocalDateTime.of(2023, 1, 1, 0, 0, 45),
                UNIT, SECOND));

        Long result =
            DateHelperDateDifferenceAction.perform(mockedParameters, mockedParameters, mockedActionContext);

        assertEquals(45, result);
    }

    @Test
    void testPerformUnsupportedUnit() {
        mockedParameters = MockParametersFactory.create(Map.of(UNIT, "UNSUPPORTED"));

        IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () -> DateHelperDateDifferenceAction.perform(mockedParameters,
                mockedParameters, mockedActionContext));

        assertEquals("Unsupported unit: UNSUPPORTED", exception.getMessage());
    }
}
