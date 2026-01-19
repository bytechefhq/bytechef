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
import static com.bytechef.component.date.helper.constants.DateHelperConstants.INCLUSIVE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.INCLUSIVE_SECONDS;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.TIME_A;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.TIME_B;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class DateHelperIsBetweenTimesActionTest {

    private boolean run(
        LocalDateTime datetime, LocalTime startTime, LocalTime endTime, boolean inclusiveSeconds, boolean inclusive) {

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(
                DATE, datetime, TIME_A, startTime, TIME_B, endTime, INCLUSIVE_SECONDS, inclusiveSeconds, INCLUSIVE,
                inclusive));

        return DateHelperIsBetweenTimesAction.perform(mockedParameters, null, null);
    }

    @Test
    void testPerformTimeInMiddleOfRange() {
        LocalDateTime datetime = LocalDateTime.of(2023, 5, 15, 14, 30, 0);
        LocalTime startTime = LocalTime.of(9, 0, 0);
        LocalTime endTime = LocalTime.of(18, 0, 0);

        assertTrue(run(datetime, startTime, endTime, true, true));
    }

    @Test
    void testPerformTimeBeforeRange() {
        LocalDateTime datetime = LocalDateTime.of(2023, 5, 15, 8, 30, 0);
        LocalTime startTime = LocalTime.of(9, 0, 0);
        LocalTime endTime = LocalTime.of(18, 0, 0);

        assertFalse(run(datetime, startTime, endTime, true, true));
    }

    @Test
    void testPerformTimeAfterRange() {
        LocalDateTime datetime = LocalDateTime.of(2023, 5, 15, 19, 30, 0);
        LocalTime startTime = LocalTime.of(9, 0, 0);
        LocalTime endTime = LocalTime.of(18, 0, 0);

        assertFalse(run(datetime, startTime, endTime, true, true));
    }

    @Test
    void testPerformTimeEqualsStartInclusive() {
        LocalDateTime datetime = LocalDateTime.of(2023, 5, 15, 9, 0, 0);
        LocalTime startTime = LocalTime.of(9, 0, 0);
        LocalTime endTime = LocalTime.of(18, 0, 0);

        assertTrue(run(datetime, startTime, endTime, true, true));
    }

    @Test
    void testPerformTimeEqualsStartExclusive() {
        LocalDateTime datetime = LocalDateTime.of(2023, 5, 15, 9, 0, 0);
        LocalTime startTime = LocalTime.of(9, 0, 0);
        LocalTime endTime = LocalTime.of(18, 0, 0);

        assertFalse(run(datetime, startTime, endTime, true, false));
    }

    @Test
    void testPerformTimeEqualsEndInclusive() {
        LocalDateTime datetime = LocalDateTime.of(2023, 5, 15, 18, 0, 0);
        LocalTime startTime = LocalTime.of(9, 0, 0);
        LocalTime endTime = LocalTime.of(18, 0, 0);

        assertTrue(run(datetime, startTime, endTime, true, true));
    }

    @Test
    void testPerformTimeEqualsEndExclusive() {
        LocalDateTime datetime = LocalDateTime.of(2023, 5, 15, 18, 0, 0);
        LocalTime startTime = LocalTime.of(9, 0, 0);
        LocalTime endTime = LocalTime.of(18, 0, 0);

        assertFalse(run(datetime, startTime, endTime, true, false));
    }

    @Test
    void testPerformWithSecondsIncluded() {
        LocalDateTime datetime = LocalDateTime.of(2023, 5, 15, 14, 30, 45);
        LocalTime startTime = LocalTime.of(14, 30, 0);
        LocalTime endTime = LocalTime.of(14, 30, 0);

        assertFalse(run(datetime, startTime, endTime, true, true));
    }

    @Test
    void testPerformWithSecondsIgnored() {
        LocalDateTime datetime = LocalDateTime.of(2023, 5, 15, 14, 30, 45);
        LocalTime startTime = LocalTime.of(14, 30, 0);
        LocalTime endTime = LocalTime.of(14, 31, 0);

        assertTrue(run(datetime, startTime, endTime, false, true));
    }

    @Test
    void testPerformWithSecondsIgnoredSameMinute() {
        LocalDateTime datetime = LocalDateTime.of(2023, 5, 15, 14, 30, 59);
        LocalTime startTime = LocalTime.of(14, 30, 0);
        LocalTime endTime = LocalTime.of(14, 30, 0);

        assertTrue(run(datetime, startTime, endTime, false, true));
    }

    @Test
    void testPerformRangeCrossingMidnight() {
        LocalDateTime datetime = LocalDateTime.of(2023, 5, 15, 2, 30, 0);
        LocalTime startTime = LocalTime.of(22, 0, 0);
        LocalTime endTime = LocalTime.of(6, 0, 0);

        assertTrue(run(datetime, startTime, endTime, true, true));
    }

    @Test
    void testPerformRangeCrossingMidnightBeforeStart() {
        LocalDateTime datetime = LocalDateTime.of(2023, 5, 15, 21, 30, 0);
        LocalTime startTime = LocalTime.of(22, 0, 0);
        LocalTime endTime = LocalTime.of(6, 0, 0);

        assertFalse(run(datetime, startTime, endTime, true, true));
    }

    @Test
    void testPerformRangeCrossingMidnightAfterEnd() {
        LocalDateTime datetime = LocalDateTime.of(2023, 5, 15, 7, 30, 0);
        LocalTime startTime = LocalTime.of(22, 0, 0);
        LocalTime endTime = LocalTime.of(6, 0, 0);

        assertFalse(run(datetime, startTime, endTime, true, true));
    }

    @Test
    void testPerformDifferentTimezoneUTC() {
        LocalDateTime datetime = LocalDateTime.of(2023, 5, 15, 14, 30, 0);
        LocalTime startTime = LocalTime.of(9, 0, 0);
        LocalTime endTime = LocalTime.of(18, 0, 0);

        assertTrue(run(datetime, startTime, endTime, true, true));
    }

    @Test
    void testPerformDifferentTimezoneEurope() {
        LocalDateTime datetime = LocalDateTime.of(2023, 5, 15, 14, 30, 0);
        LocalTime startTime = LocalTime.of(9, 0, 0);
        LocalTime endTime = LocalTime.of(18, 0, 0);

        assertTrue(run(datetime, startTime, endTime, true, true));
    }

    @Test
    void testPerformIgnoresDatePart() {
        LocalDateTime datetime1 = LocalDateTime.of(2020, 1, 1, 14, 30, 0);
        LocalDateTime datetime2 = LocalDateTime.of(2025, 12, 31, 14, 30, 0);
        LocalTime startTime = LocalTime.of(9, 0, 0);
        LocalTime endTime = LocalTime.of(18, 0, 0);

        assertTrue(run(datetime1, startTime, endTime, true, true));
        assertTrue(run(datetime2, startTime, endTime, true, true));
    }

    @Test
    void testPerformOneSecondBeforeEndInclusive() {
        LocalDateTime datetime = LocalDateTime.of(2023, 5, 15, 17, 59, 59);
        LocalTime startTime = LocalTime.of(9, 0, 0);
        LocalTime endTime = LocalTime.of(18, 0, 0);

        assertTrue(run(datetime, startTime, endTime, true, true));
    }

    @Test
    void testPerformOneSecondAfterStart() {
        LocalDateTime datetime = LocalDateTime.of(2023, 5, 15, 9, 0, 1);
        LocalTime startTime = LocalTime.of(9, 0, 0);
        LocalTime endTime = LocalTime.of(18, 0, 0);

        assertTrue(run(datetime, startTime, endTime, true, false));
    }

    @Test
    void testPerformMidnightTime() {
        LocalDateTime datetime = LocalDateTime.of(2023, 5, 15, 0, 0, 0);
        LocalTime startTime = LocalTime.of(0, 0, 0);
        LocalTime endTime = LocalTime.of(23, 59, 59);

        assertTrue(run(datetime, startTime, endTime, true, true));
    }

    @Test
    void testPerformVeryShortTimeRange() {
        LocalDateTime datetime = LocalDateTime.of(2023, 5, 15, 12, 0, 30);
        LocalTime startTime = LocalTime.of(12, 0, 0);
        LocalTime endTime = LocalTime.of(12, 1, 0);

        assertTrue(run(datetime, startTime, endTime, true, true));
    }

    @Test
    void testPerformExactMidnightCrossing() {
        LocalDateTime datetime = LocalDateTime.of(2023, 5, 15, 0, 0, 0);

        LocalTime startTime = LocalTime.of(22, 0, 0);
        LocalTime endTime = LocalTime.of(6, 0, 0);

        assertTrue(run(datetime, startTime, endTime, true, true));
    }
}
