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
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_A;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_B;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DAY;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.HOUR;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.INCLUSIVE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MINUTE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MONTH;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.RESOLUTION;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.SECOND;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.YEAR;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class DateHelperIsBetweenDatesActionTest {

    private boolean run(
        LocalDateTime date, LocalDateTime startDate, LocalDateTime endDate,
        boolean inclusive, String resolution) {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(
                DATE, date,
                DATE_A, startDate,
                DATE_B, endDate,
                INCLUSIVE, inclusive,
                RESOLUTION, resolution));

        return DateHelperIsBetweenDatesAction.perform(mockedParameters, null, null);
    }

    @Test
    void testPerformDateInMiddleOfRangeExclusiveDay() {
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        LocalDateTime date = LocalDateTime.of(2024, 1, 15, 12, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 31, 23, 59, 59);

        assertTrue(run(date, startDate, endDate, false, DAY));
    }

    @Test
    void testPerformDateInMiddleOfRangeInclusiveDay() {
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        LocalDateTime date = LocalDateTime.of(2024, 1, 15, 12, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 31, 23, 59, 59);

        assertTrue(run(date, startDate, endDate, true, DAY));
    }

    @Test
    void testPerformDateBeforeRange() {
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 10, 0, 0, 0);
        LocalDateTime date = LocalDateTime.of(2024, 1, 5, 12, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 20, 0, 0, 0);

        assertFalse(run(date, startDate, endDate, false, DAY));
        assertFalse(run(date, startDate, endDate, true, DAY));
    }

    @Test
    void testPerformDateAfterRange() {
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        LocalDateTime date = LocalDateTime.of(2024, 1, 25, 12, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 20, 0, 0, 0);

        assertFalse(run(date, startDate, endDate, false, DAY));
        assertFalse(run(date, startDate, endDate, true, DAY));
    }

    @Test
    void testPerformDateEqualsStartDateExclusive() {
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 10, 12, 0, 0);
        LocalDateTime date = LocalDateTime.of(2024, 1, 10, 12, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 20, 12, 0, 0);

        assertFalse(run(date, startDate, endDate, false, SECOND));
    }

    @Test
    void testPerformDateEqualsStartDateInclusive() {
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 10, 12, 0, 0);
        LocalDateTime date = LocalDateTime.of(2024, 1, 10, 12, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 20, 12, 0, 0);

        assertTrue(run(date, startDate, endDate, true, SECOND));
    }

    @Test
    void testPerformDateEqualsEndDateExclusive() {
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        LocalDateTime date = LocalDateTime.of(2024, 1, 20, 12, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 20, 12, 0, 0);

        assertFalse(run(date, startDate, endDate, false, SECOND));
    }

    @Test
    void testPerformDateEqualsEndDateInclusive() {
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        LocalDateTime date = LocalDateTime.of(2024, 1, 20, 12, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 20, 12, 0, 0);

        assertTrue(run(date, startDate, endDate, true, SECOND));
    }

    @Test
    void testPerformResolutionYear() {
        LocalDateTime startDate = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
        LocalDateTime date = LocalDateTime.of(2022, 6, 15, 12, 30, 0);
        LocalDateTime endDate = LocalDateTime.of(2025, 12, 31, 23, 59, 59);

        assertTrue(run(date, startDate, endDate, false, YEAR));
    }

    @Test
    void testPerformResolutionYearSameYear() {
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        LocalDateTime date = LocalDateTime.of(2024, 6, 15, 12, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 12, 31, 23, 59, 59);

        assertTrue(run(date, startDate, endDate, true, YEAR));
    }

    @Test
    void testPerformResolutionMonth() {
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        LocalDateTime date = LocalDateTime.of(2024, 3, 15, 12, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 5, 31, 23, 59, 59);

        assertTrue(run(date, startDate, endDate, false, MONTH));
    }

    @Test
    void testPerformResolutionMonthSameMonth() {
        LocalDateTime startDate = LocalDateTime.of(2024, 5, 1, 0, 0, 0);
        LocalDateTime date = LocalDateTime.of(2024, 5, 15, 12, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 5, 31, 23, 59, 59);

        assertTrue(run(date, startDate, endDate, true, MONTH));
    }

    @Test
    void testPerformResolutionDay() {
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 10, 0, 0, 0);
        LocalDateTime date = LocalDateTime.of(2024, 1, 15, 12, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 20, 0, 0, 0);

        assertTrue(run(date, startDate, endDate, false, DAY));
    }

    @Test
    void testPerformResolutionDaySameDay() {
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 15, 0, 0, 0);
        LocalDateTime date = LocalDateTime.of(2024, 1, 15, 12, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 15, 23, 59, 59);

        assertTrue(run(date, startDate, endDate, true, DAY));
    }

    @Test
    void testPerformResolutionHour() {
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 15, 10, 0, 0);
        LocalDateTime date = LocalDateTime.of(2024, 1, 15, 12, 30, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 15, 15, 0, 0);

        assertTrue(run(date, startDate, endDate, false, HOUR));
    }

    @Test
    void testPerformResolutionHourSameHour() {
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 15, 12, 0, 0);
        LocalDateTime date = LocalDateTime.of(2024, 1, 15, 12, 30, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 15, 12, 59, 59);

        assertTrue(run(date, startDate, endDate, true, HOUR));
    }

    @Test
    void testPerformResolutionMinute() {
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 15, 12, 10, 0);
        LocalDateTime date = LocalDateTime.of(2024, 1, 15, 12, 15, 30);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 15, 12, 20, 0);

        assertTrue(run(date, startDate, endDate, false, MINUTE));
    }

    @Test
    void testPerformResolutionMinuteSameMinute() {
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 15, 12, 15, 0);
        LocalDateTime date = LocalDateTime.of(2024, 1, 15, 12, 15, 30);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 15, 12, 15, 59);

        assertTrue(run(date, startDate, endDate, true, MINUTE));
    }

    @Test
    void testPerformResolutionSecond() {
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 15, 12, 15, 10);
        LocalDateTime date = LocalDateTime.of(2024, 1, 15, 12, 15, 15);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 15, 12, 15, 20);

        assertTrue(run(date, startDate, endDate, false, SECOND));
    }

    @Test
    void testPerformResolutionSecondSameSecond() {
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 15, 12, 15, 15);
        LocalDateTime date = LocalDateTime.of(2024, 1, 15, 12, 15, 15, 500000000);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 15, 12, 15, 15, 999999999);

        assertTrue(run(date, startDate, endDate, true, SECOND));
    }

    @Test
    void testPerformResolutionDayDifferentTimes() {
        // Same day but different times - should be true with day resolution
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 15, 8, 0, 0);
        LocalDateTime date = LocalDateTime.of(2024, 1, 15, 23, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 15, 10, 0, 0);

        assertTrue(run(date, startDate, endDate, true, DAY));
    }

    @Test
    void testPerformDateOnStartBoundaryDayResolution() {
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 10, 14, 30, 0);
        LocalDateTime date = LocalDateTime.of(2024, 1, 10, 8, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 20, 18, 0, 0);

        assertFalse(run(date, startDate, endDate, false, DAY));
        assertTrue(run(date, startDate, endDate, true, DAY));
    }

    @Test
    void testPerformDateOnEndBoundaryDayResolution() {
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 8, 0, 0);
        LocalDateTime date = LocalDateTime.of(2024, 1, 20, 22, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 20, 18, 0, 0);

        assertFalse(run(date, startDate, endDate, false, DAY));
        assertTrue(run(date, startDate, endDate, true, DAY));
    }

    @Test
    void testPerformInvalidRangeStartAfterEnd() {
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 20, 0, 0, 0);
        LocalDateTime date = LocalDateTime.of(2024, 1, 15, 12, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 10, 0, 0, 0);

        assertFalse(run(date, startDate, endDate, false, DAY));
        assertFalse(run(date, startDate, endDate, true, DAY));
    }

    @Test
    void testPerformCrossingYearBoundaryWithYearResolution() {
        LocalDateTime startDate = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
        LocalDateTime date = LocalDateTime.of(2024, 6, 15, 12, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2025, 12, 31, 23, 59, 59);

        assertTrue(run(date, startDate, endDate, false, YEAR));
    }

    @Test
    void testPerformCrossingMonthBoundaryWithMonthResolution() {
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 15, 0, 0, 0);
        LocalDateTime date = LocalDateTime.of(2024, 2, 10, 12, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 3, 20, 23, 59, 59);

        assertTrue(run(date, startDate, endDate, false, MONTH));
    }
}
