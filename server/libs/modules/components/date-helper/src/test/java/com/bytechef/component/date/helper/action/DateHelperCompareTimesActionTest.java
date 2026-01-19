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

import static com.bytechef.component.date.helper.constants.DateHelperComparisonEnum.IS_AFTER;
import static com.bytechef.component.date.helper.constants.DateHelperComparisonEnum.IS_AFTER_OR_EQUAL;
import static com.bytechef.component.date.helper.constants.DateHelperComparisonEnum.IS_BEFORE;
import static com.bytechef.component.date.helper.constants.DateHelperComparisonEnum.IS_BEFORE_OR_EQUAL;
import static com.bytechef.component.date.helper.constants.DateHelperComparisonEnum.IS_EQUAL;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.COMPARISON;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_A;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_B;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.date.helper.constants.DateHelperComparisonEnum;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class DateHelperCompareTimesActionTest {

    private boolean run(DateHelperComparisonEnum comparison, LocalDateTime dateA, LocalDateTime dateB) {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(COMPARISON, comparison.name(), DATE_A, dateA, DATE_B, dateB));

        return DateHelperCompareTimesAction.perform(mockedParameters, null, null);
    }

    @Test
    void testPerformIsEqualSameTimesDifferentDates() {
        LocalDateTime dateA = LocalDateTime.of(2024, 1, 15, 14, 30, 0);
        LocalDateTime dateB = LocalDateTime.of(2024, 12, 25, 14, 30, 0);

        assertTrue(run(IS_EQUAL, dateA, dateB));
    }

    @Test
    void testPerformIsEqualDifferentTimes() {
        LocalDateTime dateA = LocalDateTime.of(2024, 1, 15, 14, 30, 0);
        LocalDateTime dateB = LocalDateTime.of(2024, 1, 15, 15, 30, 0);

        assertFalse(run(IS_EQUAL, dateA, dateB));
    }

    @Test
    void testPerformIsBeforeEarlierTime() {
        LocalDateTime dateA = LocalDateTime.of(2024, 12, 25, 10, 0, 0);
        LocalDateTime dateB = LocalDateTime.of(2024, 1, 1, 15, 0, 0);

        assertTrue(run(IS_BEFORE, dateA, dateB));
    }

    @Test
    void testPerformIsBeforeLaterTime() {
        LocalDateTime dateA = LocalDateTime.of(2024, 1, 1, 15, 0, 0);
        LocalDateTime dateB = LocalDateTime.of(2024, 12, 25, 10, 0, 0);

        assertFalse(run(IS_BEFORE, dateA, dateB));
    }

    @Test
    void testPerformIsAfterLaterTime() {
        LocalDateTime dateA = LocalDateTime.of(2024, 1, 1, 18, 30, 0);
        LocalDateTime dateB = LocalDateTime.of(2024, 12, 25, 12, 15, 0);

        assertTrue(run(IS_AFTER, dateA, dateB));
    }

    @Test
    void testPerformIsAfterEarlierTime() {
        LocalDateTime dateA = LocalDateTime.of(2024, 12, 25, 8, 0, 0);
        LocalDateTime dateB = LocalDateTime.of(2024, 1, 1, 20, 0, 0);

        assertFalse(run(IS_AFTER, dateA, dateB));
    }

    @Test
    void testPerformIsBeforeOrEqualEqualTimes() {
        LocalDateTime dateA = LocalDateTime.of(2024, 5, 10, 13, 45, 30);
        LocalDateTime dateB = LocalDateTime.of(2024, 8, 20, 13, 45, 30);

        assertTrue(run(IS_BEFORE_OR_EQUAL, dateA, dateB));
    }

    @Test
    void testPerformIsBeforeOrEqualBeforeTime() {
        LocalDateTime dateA = LocalDateTime.of(2024, 8, 20, 9, 0, 0);
        LocalDateTime dateB = LocalDateTime.of(2024, 5, 10, 17, 0, 0);

        assertTrue(run(IS_BEFORE_OR_EQUAL, dateA, dateB));
    }

    @Test
    void testPerformIsBeforeOrEqualAfterTime() {
        LocalDateTime dateA = LocalDateTime.of(2024, 1, 1, 22, 0, 0);
        LocalDateTime dateB = LocalDateTime.of(2024, 12, 31, 11, 0, 0);

        assertFalse(run(IS_BEFORE_OR_EQUAL, dateA, dateB));
    }

    @Test
    void testPerformIsAfterOrEqualEqualTimes() {
        LocalDateTime dateA = LocalDateTime.of(2024, 3, 15, 16, 20, 45);
        LocalDateTime dateB = LocalDateTime.of(2024, 9, 5, 16, 20, 45);

        assertTrue(run(IS_AFTER_OR_EQUAL, dateA, dateB));
    }

    @Test
    void testPerformIsAfterOrEqualAfterTime() {
        LocalDateTime dateA = LocalDateTime.of(2024, 11, 30, 23, 59, 59);
        LocalDateTime dateB = LocalDateTime.of(2024, 2, 14, 12, 30, 0);

        assertTrue(run(IS_AFTER_OR_EQUAL, dateA, dateB));
    }

    @Test
    void testPerformIsAfterOrEqualBeforeTime() {
        LocalDateTime dateA = LocalDateTime.of(2024, 6, 1, 6, 0, 0);
        LocalDateTime dateB = LocalDateTime.of(2024, 6, 1, 18, 0, 0);

        assertFalse(run(IS_AFTER_OR_EQUAL, dateA, dateB));
    }
}
