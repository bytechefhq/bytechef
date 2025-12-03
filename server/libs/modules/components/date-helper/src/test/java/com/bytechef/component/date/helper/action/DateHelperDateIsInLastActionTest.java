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
import static com.bytechef.component.date.helper.constants.DateHelperConstants.HOUR;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.IN_LAST;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MINUTE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MONTH;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.SECOND;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.TIME_ZONE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.UNIT;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.YEAR;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class DateHelperDateIsInLastActionTest {

    private static final String UTC = "UTC";

    private boolean run(LocalDateTime date, int inLast, String inLastUnit, String timeZone) {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(
                DATE, date,
                IN_LAST, inLast,
                UNIT, inLastUnit,
                TIME_ZONE, timeZone));

        return DateHelperDateIsInLastAction.perform(mockedParameters, null, null);
    }

    @Test
    void testPerformDateInLastSecondsWithinRange() {
        LocalDateTime date = LocalDateTime.now(ZoneId.of(UTC))
            .minusSeconds(30);

        assertTrue(run(date, 60, SECOND, UTC));
    }

    @Test
    void testPerformDateInLastSecondsOutsideRange() {
        LocalDateTime date = LocalDateTime.now(ZoneId.of(UTC))
            .minusSeconds(120);

        assertFalse(run(date, 60, SECOND, UTC));
    }

    @Test
    void testPerformDateInLastMinutesWithinRange() {
        LocalDateTime date = LocalDateTime.now(ZoneId.of(UTC))
            .minusMinutes(10);

        assertTrue(run(date, 15, MINUTE, UTC));
    }

    @Test
    void testPerformDateInLastMinutesOutsideRange() {
        LocalDateTime date = LocalDateTime.now(ZoneId.of(UTC))
            .minusMinutes(30);

        assertFalse(run(date, 15, MINUTE, UTC));
    }

    @Test
    void testPerformDateInLastHoursWithinRange() {
        LocalDateTime date = LocalDateTime.now(ZoneId.of(UTC))
            .minusHours(5);

        assertTrue(run(date, 10, HOUR, UTC));
    }

    @Test
    void testPerformDateInLastHoursOutsideRange() {
        LocalDateTime date = LocalDateTime.now(ZoneId.of(UTC))
            .minusHours(25);

        assertFalse(run(date, 24, HOUR, UTC));
    }

    @Test
    void testPerformDateInLastDaysWithinRange() {
        LocalDateTime date = LocalDateTime.now(ZoneId.of(UTC))
            .minusDays(3);

        assertTrue(run(date, 7, DAY, UTC));
    }

    @Test
    void testPerformDateInLastDaysOutsideRange() {
        LocalDateTime date = LocalDateTime.now(ZoneId.of(UTC))
            .minusDays(10);

        assertFalse(run(date, 7, DAY, UTC));
    }

    @Test
    void testPerformDateInLastMonthsWithinRange() {
        LocalDateTime date = LocalDateTime.now(ZoneId.of(UTC))
            .minusMonths(2);

        assertTrue(run(date, 6, MONTH, UTC));
    }

    @Test
    void testPerformDateInLastMonthsOutsideRange() {
        LocalDateTime date = LocalDateTime.now(ZoneId.of(UTC))
            .minusMonths(8);

        assertFalse(run(date, 6, MONTH, UTC));
    }

    @Test
    void testPerformDateInLastYearsWithinRange() {
        LocalDateTime date = LocalDateTime.now(ZoneId.of(UTC))
            .minusYears(1);

        assertTrue(run(date, 3, YEAR, UTC));
    }

    @Test
    void testPerformDateInLastYearsOutsideRange() {
        LocalDateTime date = LocalDateTime.now(ZoneId.of(UTC))
            .minusYears(5);

        assertFalse(run(date, 3, YEAR, UTC));
    }

    @Test
    void testPerformDateExactlyAtThreshold() {
        LocalDateTime date = LocalDateTime.now(ZoneId.of(UTC))
            .minusDays(2);

        assertTrue(run(date, 2, DAY, UTC));
    }

    @Test
    void testPerformDateIsNow() {
        LocalDateTime date = LocalDateTime.now(ZoneId.of(UTC));

        assertTrue(run(date, 1, DAY, UTC));
    }

    @Test
    void testPerformFutureDate() {
        LocalDateTime date = LocalDateTime.now(ZoneId.of(UTC))
            .plusDays(1);

        assertFalse(run(date, 7, DAY, UTC));
    }

    @Test
    void testPerformZeroValue() {
        LocalDateTime date = LocalDateTime.now(ZoneId.of(UTC));

        assertTrue(run(date, 0, DAY, UTC));
    }

    @Test
    void testPerformDifferentTimeZone() {
        String pacificTimeZone = "America/Los_Angeles";
        LocalDateTime date = LocalDateTime.now(ZoneId.of(pacificTimeZone))
            .minusHours(5);

        assertTrue(run(date, 10, HOUR, pacificTimeZone));
    }

    @Test
    void testPerformDateExactlyAtUpperBound() {
        LocalDateTime date = LocalDateTime.now(ZoneId.of(UTC));

        assertTrue(run(date, 1, MINUTE, UTC));
    }
}
