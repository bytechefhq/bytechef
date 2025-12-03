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

import static com.bytechef.component.date.helper.constants.DateHelperConstants.DAY;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DURATION;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.HOUR;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MINUTE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.SECOND;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.UNIT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class DateHelperGetDurationActionTest {

    private String run(long duration, String unitType) {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(DURATION, duration, UNIT, unitType));

        return DateHelperGetDurationAction.perform(mockedParameters, null, null);
    }

    @Test
    void testGetDurationTwoHours() {
        String result = run(7200, SECOND);
        assertEquals("2 hours", result);
    }

    @Test
    void testGetDurationOneHour() {
        String result = run(3600, SECOND);
        assertEquals("1 hour", result);
    }

    @Test
    void testGetDurationMinutesFromSeconds() {
        String result = run(180, SECOND);
        assertEquals("3 minutes", result);
    }

    @Test
    void testGetDurationOneMinute() {
        String result = run(60, SECOND);
        assertEquals("1 minute", result);
    }

    @Test
    void testGetDurationLessThanMinute() {
        String result = run(30, SECOND);
        assertEquals("30 seconds", result);
    }

    @Test
    void testGetDurationHoursFromMinutes() {
        String result = run(150, MINUTE);
        assertEquals("2 hours, 30 minutes", result);
    }

    @Test
    void testGetDurationOneDayFromHours() {
        String result = run(24, HOUR);
        assertEquals("1 day", result);
    }

    @Test
    void testGetDurationMultipleDaysFromHours() {
        String result = run(49, HOUR);
        assertEquals("2 days, 1 hour", result);
    }

    @Test
    void testGetDurationWeeksFromDays() {
        String result = run(14, DAY);
        assertEquals("14 days", result);
    }

    @Test
    void testGetDurationMonthsFromDaysApprox() {
        String result = run(60, DAY);
        assertEquals("2 months", result);
    }

    @Test
    void testGetDurationYearsFromDaysApprox() {
        String result = run(365, DAY);
        assertEquals("1 year", result);
    }

    @Test
    void testGetDurationZero() {
        String result = run(0, SECOND);
        assertEquals("0 seconds", result);
    }
}
