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
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class DateHelperGetDaysBetweenActionTest {

    private int run(LocalDateTime dateA, LocalDateTime dateB) {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(DATE_A, dateA, DATE_B, dateB));

        return DateHelperGetDaysBetweenAction.perform(mockedParameters, null, null);
    }

    @Test
    void testPerformExactlyOneDayApart() {
        LocalDateTime dateA = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        LocalDateTime dateB = LocalDateTime.of(2024, 1, 2, 12, 0, 0);

        assertEquals(1, run(dateA, dateB));
    }

    @Test
    void testPerformExactlySevenDaysApart() {
        LocalDateTime dateA = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        LocalDateTime dateB = LocalDateTime.of(2024, 1, 8, 0, 0, 0);

        assertEquals(7, run(dateA, dateB));
    }

    @Test
    void testPerformSameDates() {
        LocalDateTime dateA = LocalDateTime.of(2024, 6, 15, 10, 30, 0);
        LocalDateTime dateB = LocalDateTime.of(2024, 6, 15, 10, 30, 0);

        assertEquals(0, run(dateA, dateB));
    }

    @Test
    void testPerformNegativeResult() {
        LocalDateTime dateA = LocalDateTime.of(2024, 1, 10, 12, 0, 0);
        LocalDateTime dateB = LocalDateTime.of(2024, 1, 5, 12, 0, 0);

        assertEquals(-5, run(dateA, dateB));
    }
}
