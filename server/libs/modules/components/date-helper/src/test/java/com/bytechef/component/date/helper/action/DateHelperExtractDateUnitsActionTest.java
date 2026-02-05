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

import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DAY;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DAY_OF_WEEK;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.HOUR;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MINUTE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MONTH;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MONTH_NAME;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.SECOND;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.TIME;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.UNIT;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.YEAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class DateHelperExtractDateUnitsActionTest {

    private Parameters mockedParameters;
    private final ActionContext mockedActionContext = mock(ActionContext.class);

    @Test
    void testPerformYear() {
        mockedParameters = MockParametersFactory.create(
            Map.of(DATE, LocalDateTime.of(2023, 1, 1, 0, 0), UNIT, YEAR));

        Object result = DateHelperExtractDateUnitsAction.perform(mockedParameters, null, mockedActionContext);

        assertEquals(2023, result);
    }

    @Test
    void testPerformMonth() {
        mockedParameters = MockParametersFactory.create(
            Map.of(DATE, LocalDateTime.of(2023, 1, 1, 0, 0), UNIT, MONTH));

        Object result = DateHelperExtractDateUnitsAction.perform(mockedParameters, null, mockedActionContext);

        assertEquals(1, result);
    }

    @Test
    void testPerformDay() {
        mockedParameters = MockParametersFactory.create(
            Map.of(DATE, LocalDateTime.of(2023, 1, 1, 0, 0), UNIT, DAY));

        Object result = DateHelperExtractDateUnitsAction.perform(mockedParameters, null, mockedActionContext);

        assertEquals(1, result);
    }

    @Test
    void testPerformHour() {
        mockedParameters = MockParametersFactory.create(
            Map.of(DATE, LocalDateTime.of(2023, 1, 1, 0, 0), UNIT, HOUR));

        Object result = DateHelperExtractDateUnitsAction.perform(mockedParameters, null, mockedActionContext);

        assertEquals(0, result);
    }

    @Test
    void testPerformMinute() {
        mockedParameters = MockParametersFactory.create(
            Map.of(DATE, LocalDateTime.of(2023, 1, 1, 0, 2), UNIT, MINUTE));

        Object result = DateHelperExtractDateUnitsAction.perform(mockedParameters, null, mockedActionContext);

        assertEquals(2, result);
    }

    @Test
    void testPerformSecond() {
        mockedParameters = MockParametersFactory.create(
            Map.of(DATE, LocalDateTime.of(2023, 1, 1, 0, 0, 3), UNIT, SECOND));

        Object result = DateHelperExtractDateUnitsAction.perform(mockedParameters, null, mockedActionContext);

        assertEquals(3, result);
    }

    @Test
    void testPerformDayOfWeek() {
        mockedParameters = MockParametersFactory.create(
            Map.of(DATE, LocalDateTime.of(2023, 1, 1, 0, 0), UNIT, DAY_OF_WEEK));

        Object result = DateHelperExtractDateUnitsAction.perform(mockedParameters, null, mockedActionContext);

        assertEquals(DayOfWeek.SUNDAY.getDisplayName(TextStyle.FULL, Locale.getDefault()), result);
    }

    @Test
    void testPerformMonthName() {
        mockedParameters = MockParametersFactory.create(
            Map.of(DATE, LocalDateTime.of(2023, 1, 1, 0, 0), UNIT, MONTH_NAME));

        Object result = DateHelperExtractDateUnitsAction.perform(mockedParameters, null, mockedActionContext);

        assertEquals(Month.JANUARY.getDisplayName(TextStyle.FULL, Locale.getDefault()), result);
    }

    @Test
    void testPerformDate() {
        mockedParameters = MockParametersFactory.create(
            Map.of(DATE, LocalDateTime.of(2023, 1, 1, 0, 0), UNIT, DATE));

        Object result = DateHelperExtractDateUnitsAction.perform(mockedParameters, null, mockedActionContext);

        assertEquals(LocalDate.of(2023, 1, 1), result);
    }

    @Test
    void testPerformTime() {
        mockedParameters = MockParametersFactory.create(
            Map.of(DATE, LocalDateTime.of(2023, 1, 1, 0, 0), UNIT, TIME));

        Object result = DateHelperExtractDateUnitsAction.perform(mockedParameters, null, mockedActionContext);

        assertEquals(LocalTime.of(0, 0, 0), result);
    }

    @Test
    void testPerformUnsupportedUnit() {
        mockedParameters = MockParametersFactory.create(
            Map.of(DATE, LocalDateTime.of(2023, 1, 1, 0, 0), UNIT, "UNSUPPORTED"));

        IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class,
                () -> DateHelperExtractDateUnitsAction.perform(mockedParameters,
                    null, mock(ActionContext.class)));

        assertEquals("Unsupported unit UNSUPPORTED", exception.getMessage());
    }
}
