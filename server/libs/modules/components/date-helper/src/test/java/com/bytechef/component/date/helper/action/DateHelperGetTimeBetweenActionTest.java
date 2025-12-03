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

import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_A;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_B;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DAY;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.HOUR;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MINUTE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MONTH;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.SECOND;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.YEAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class DateHelperGetTimeBetweenActionTest {

    private Map<String, Object> run(LocalDateTime dateA, LocalDateTime dateB) {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(DATE_A, dateA, DATE_B, dateB));

        return DateHelperGetTimeBetweenAction.perform(mockedParameters, null, null);
    }

    @Test
    void testBasicTimeDifference() {
        LocalDateTime dateA = LocalDateTime.of(2024, 1, 1, 10, 30, 15);
        LocalDateTime dateB = LocalDateTime.of(2024, 1, 3, 17, 0, 0);

        Map<String, Object> result = run(dateA, dateB);

        assertEquals(0, result.get(YEAR));
        assertEquals(0, result.get(MONTH));
        assertEquals(2, result.get(DAY));
        assertEquals(6, result.get(HOUR));
        assertEquals(29, result.get(MINUTE));
        assertEquals(45, result.get(SECOND));
    }

    @Test
    void testSameDayDifference() {
        LocalDateTime dateA = LocalDateTime.of(2024, 1, 1, 10, 30, 15);
        LocalDateTime dateB = LocalDateTime.of(2024, 1, 1, 17, 0, 0);

        Map<String, Object> result = run(dateA, dateB);

        assertEquals(0, result.get(YEAR));
        assertEquals(0, result.get(MONTH));
        assertEquals(0, result.get(DAY));
        assertEquals(6, result.get(HOUR));
        assertEquals(29, result.get(MINUTE));
        assertEquals(45, result.get(SECOND));
    }

    @Test
    void testMultipleMonthsDifference() {
        LocalDateTime dateA = LocalDateTime.of(2024, 1, 15, 10, 0, 0);
        LocalDateTime dateB = LocalDateTime.of(2024, 4, 20, 14, 30, 45);

        Map<String, Object> result = run(dateA, dateB);

        assertEquals(0, result.get(YEAR));
        assertEquals(3, result.get(MONTH));
        assertEquals(5, result.get(DAY));
        assertEquals(4, result.get(HOUR));
        assertEquals(30, result.get(MINUTE));
        assertEquals(45, result.get(SECOND));
    }

    @Test
    void testMultipleYearsDifference() {
        LocalDateTime dateA = LocalDateTime.of(2022, 5, 10, 8, 15, 30);
        LocalDateTime dateB = LocalDateTime.of(2024, 8, 15, 12, 45, 15);

        Map<String, Object> result = run(dateA, dateB);

        assertEquals(2, result.get(YEAR));
        assertEquals(3, result.get(MONTH));
        assertEquals(5, result.get(DAY));
        assertEquals(4, result.get(HOUR));
        assertEquals(29, result.get(MINUTE));
        assertEquals(45, result.get(SECOND));
    }

    @Test
    void testIdenticalDates() {
        LocalDateTime date = LocalDateTime.of(2024, 1, 1, 12, 0, 0);

        Map<String, Object> result = run(date, date);

        assertEquals(0, result.get(YEAR));
        assertEquals(0, result.get(MONTH));
        assertEquals(0, result.get(DAY));
        assertEquals(0, result.get(HOUR));
        assertEquals(0, result.get(MINUTE));
        assertEquals(0, result.get(SECOND));
    }

    @Test
    void testNegativeDifference() {
        LocalDateTime dateA = LocalDateTime.of(2024, 1, 5, 10, 0, 0);
        LocalDateTime dateB = LocalDateTime.of(2024, 1, 3, 8, 0, 0);

        Map<String, Object> result = run(dateA, dateB);

        assertEquals(0, result.get(YEAR));
        assertEquals(0, result.get(MONTH));
        assertEquals(-2, result.get(DAY));
        assertTrue((int) result.get(HOUR) <= 0);
    }

    @Test
    void testOneSecondDifference() {
        LocalDateTime dateA = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        LocalDateTime dateB = LocalDateTime.of(2024, 1, 1, 12, 0, 1);

        Map<String, Object> result = run(dateA, dateB);

        assertEquals(0, result.get(HOUR));
        assertEquals(0, result.get(MINUTE));
        assertEquals(1, result.get(SECOND));
    }

    @Test
    void testLeapYear() {
        LocalDateTime dateA = LocalDateTime.of(2024, 2, 28, 0, 0, 0);
        LocalDateTime dateB = LocalDateTime.of(2024, 3, 1, 0, 0, 0);

        Map<String, Object> result = run(dateA, dateB);

        assertEquals(0, result.get(MONTH));
        assertEquals(2, result.get(DAY));
    }

    @Test
    void testYearBoundary() {
        LocalDateTime dateA = LocalDateTime.of(2023, 12, 30, 23, 59, 59);
        LocalDateTime dateB = LocalDateTime.of(2024, 1, 2, 0, 0, 1);

        Map<String, Object> result = run(dateA, dateB);

        assertEquals(0, result.get(YEAR));
        assertEquals(0, result.get(MONTH));
        assertEquals(3, result.get(DAY));
        assertEquals(0, result.get(HOUR));
        assertEquals(0, result.get(MINUTE));
        assertEquals(2, result.get(SECOND));
    }

    @Test
    void testExactly24Hours() {
        LocalDateTime dateA = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        LocalDateTime dateB = LocalDateTime.of(2024, 1, 2, 12, 0, 0);

        Map<String, Object> result = run(dateA, dateB);

        assertEquals(1, result.get(DAY));
        assertEquals(0, result.get(HOUR));
        assertEquals(0, result.get(MINUTE));
        assertEquals(0, result.get(SECOND));
    }

    @Test
    void testDifferentMonthLengths() {
        LocalDateTime dateA = LocalDateTime.of(2024, 1, 31, 0, 0, 0);
        LocalDateTime dateB = LocalDateTime.of(2024, 2, 29, 0, 0, 0);

        Map<String, Object> result = run(dateA, dateB);

        assertEquals(0, result.get(YEAR));
        assertEquals(0, result.get(MONTH));
        assertEquals(29, result.get(DAY));
    }
}
