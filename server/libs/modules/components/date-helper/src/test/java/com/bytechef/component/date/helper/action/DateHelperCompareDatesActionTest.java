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

import static com.bytechef.component.date.helper.constants.DateHelperConstants.COMPARISON;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_A;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_B;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DAY;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.HOUR;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.IS_AFTER;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.IS_AFTER_OR_EQUAL;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.IS_BEFORE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.IS_BEFORE_OR_EQUAL;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.IS_EQUAL;
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
class DateHelperCompareDatesActionTest {

    private boolean run(String resolution, String comparison, LocalDateTime dateA, LocalDateTime dateB) {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(
                RESOLUTION, resolution,
                COMPARISON, comparison,
                DATE_A, dateA,
                DATE_B, dateB));

        return DateHelperCompareDatesAction.perform(mockedParameters, null, null);
    }

    @Test
    void testResolutionSecond() {
        LocalDateTime a = LocalDateTime.of(2024, 1, 1, 10, 0, 5, 999999999);
        LocalDateTime b = LocalDateTime.of(2024, 1, 1, 10, 0, 5, 0);

        assertTrue(run(SECOND, IS_EQUAL, a, b));
    }

    @Test
    void testResolutionMinute() {
        LocalDateTime a = LocalDateTime.of(2024, 1, 1, 10, 5, 59);
        LocalDateTime b = LocalDateTime.of(2024, 1, 1, 10, 5, 0);

        assertTrue(run(MINUTE, IS_EQUAL, a, b));
    }

    @Test
    void testResolutionHour() {
        LocalDateTime a = LocalDateTime.of(2024, 1, 1, 10, 59);
        LocalDateTime b = LocalDateTime.of(2024, 1, 1, 10, 0);

        assertTrue(run(HOUR, IS_EQUAL, a, b));
    }

    @Test
    void testResolutionDay() {
        LocalDateTime a = LocalDateTime.of(2024, 1, 1, 23, 59);
        LocalDateTime b = LocalDateTime.of(2024, 1, 1, 0, 0);

        assertTrue(run(DAY, IS_EQUAL, a, b));
    }

    @Test
    void testResolutionMonth() {
        LocalDateTime a = LocalDateTime.of(2024, 1, 31, 23, 59);
        LocalDateTime b = LocalDateTime.of(2024, 1, 1, 0, 0);

        assertTrue(run(MONTH, IS_EQUAL, a, b));
    }

    @Test
    void testResolutionYear() {
        LocalDateTime a = LocalDateTime.of(2024, 12, 31, 23, 59);
        LocalDateTime b = LocalDateTime.of(2024, 1, 1, 0, 0);

        assertTrue(run(YEAR, IS_EQUAL, a, b));
    }

    @Test
    void testIsBefore() {
        assertTrue(run(SECOND, IS_BEFORE,
            LocalDateTime.of(2024, 1, 1, 0, 0, 0),
            LocalDateTime.of(2024, 1, 1, 0, 0, 1)));
    }

    @Test
    void testIsAfter() {
        assertTrue(run(SECOND, IS_AFTER,
            LocalDateTime.of(2024, 1, 1, 0, 0, 2),
            LocalDateTime.of(2024, 1, 1, 0, 0, 1)));
    }

    @Test
    void testIsEqual() {
        assertTrue(run(SECOND, IS_EQUAL,
            LocalDateTime.of(2024, 1, 1, 0, 0, 5),
            LocalDateTime.of(2024, 1, 1, 0, 0, 5)));
    }

    @Test
    void testIsBeforeOrEqualEqualCase() {
        assertTrue(run(SECOND, IS_BEFORE_OR_EQUAL,
            LocalDateTime.of(2024, 1, 1, 0, 0, 5),
            LocalDateTime.of(2024, 1, 1, 0, 0, 5)));
    }

    @Test
    void testIsBeforeOrEqualBeforeCase() {
        assertTrue(run(SECOND, IS_BEFORE_OR_EQUAL,
            LocalDateTime.of(2024, 1, 1, 0, 0, 4),
            LocalDateTime.of(2024, 1, 1, 0, 0, 5)));
    }

    @Test
    void testIsAfterOrEqualAfterCase() {
        assertTrue(run(SECOND, IS_AFTER_OR_EQUAL,
            LocalDateTime.of(2024, 1, 1, 0, 0, 6),
            LocalDateTime.of(2024, 1, 1, 0, 0, 5)));
    }

    @Test
    void testIsAfterOrEqualEqualCase() {
        assertTrue(run(SECOND, IS_AFTER_OR_EQUAL,
            LocalDateTime.of(2024, 1, 1, 0, 0, 5),
            LocalDateTime.of(2024, 1, 1, 0, 0, 5)));
    }

    @Test
    void testIsBeforeNegative() {
        assertFalse(run(SECOND, IS_BEFORE,
            LocalDateTime.of(2024, 1, 1, 0, 0, 5),
            LocalDateTime.of(2024, 1, 1, 0, 0, 5)));
    }

    @Test
    void testIsAfterNegative() {
        assertFalse(run(SECOND, IS_AFTER,
            LocalDateTime.of(2024, 1, 1, 0, 0, 5),
            LocalDateTime.of(2024, 1, 1, 0, 0, 5)));
    }

    @Test
    void testIsEqualNegative() {
        assertFalse(run(SECOND, IS_EQUAL,
            LocalDateTime.of(2024, 1, 1, 0, 0, 5),
            LocalDateTime.of(2024, 1, 1, 0, 0, 6)));
    }

    @Test
    void testTruncationMakesThemEqual() {
        LocalDateTime a = LocalDateTime.of(2024, 1, 1, 10, 30, 59);
        LocalDateTime b = LocalDateTime.of(2024, 1, 1, 10, 30, 0);

        assertTrue(run(MINUTE, IS_EQUAL, a, b));
        assertFalse(run(SECOND, IS_EQUAL, a, b));
    }
}
