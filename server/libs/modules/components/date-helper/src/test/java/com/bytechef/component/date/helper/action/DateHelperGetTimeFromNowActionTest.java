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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
class DateHelperGetTimeFromNowActionTest {

    private String run(LocalDateTime inputDate) {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(DATE, inputDate));

        return DateHelperGetTimeFromNowAction.perform(mockedParameters, null, null);
    }

    @Test
    void testPerformInTwoMonths() {
        LocalDateTime mockedNow = LocalDateTime.of(2026, 1, 12, 15, 0, 0);
        LocalDateTime futureDate = mockedNow.plusMonths(2);

        try (MockedStatic<LocalDateTime> mockedLocalDateTime = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            mockedLocalDateTime.when(LocalDateTime::now)
                .thenReturn(mockedNow);

            assertEquals("in 1 month, 29 days", run(futureDate));
        }
    }

    @Test
    void testPerformFourDaysAgo() {
        LocalDateTime now = LocalDateTime.now()
            .plusSeconds(1);
        LocalDateTime pastDate = now.minusDays(4);

        assertEquals("4 days ago", run(pastDate));
    }

    @Test
    void testPerformOneMonthAgo() {
        LocalDateTime mockedNow = LocalDateTime.of(2026, 1, 12, 15, 0, 0);
        LocalDateTime pastDate = mockedNow.minusMonths(1);

        try (MockedStatic<LocalDateTime> mockedLocalDateTime = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            mockedLocalDateTime.when(LocalDateTime::now)
                .thenReturn(mockedNow);

            assertEquals("1 month, 1 day ago", run(pastDate));
        }
    }

    @Test
    void testPerformInOneYear() {
        LocalDateTime now = LocalDateTime.now()
            .plusSeconds(1);
        LocalDateTime futureDate = now.plusYears(1);

        assertEquals("in 1 year", run(futureDate));
    }

    @Test
    void testPerformTwoYearsAgo() {
        LocalDateTime now = LocalDateTime.now()
            .plusSeconds(1);
        LocalDateTime pastDate = now.minusYears(2);

        assertEquals("2 years, 1 day ago", run(pastDate));
    }

    @Test
    void testPerformInOneDay() {
        LocalDateTime now = LocalDateTime.now()
            .plusSeconds(1);
        LocalDateTime futureDate = now.plusDays(1);

        assertEquals("in 1 day", run(futureDate));
    }

    @Test
    void testPerformInFewHours() {
        LocalDateTime now = LocalDateTime.now()
            .plusSeconds(1);
        LocalDateTime futureDate = now.plusHours(5);

        assertEquals("in 5 hours", run(futureDate));
    }

    @Test
    void testPerformOneHourAgo() {
        LocalDateTime now = LocalDateTime.now()
            .plusSeconds(1);
        LocalDateTime pastDate = now.minusHours(1)
            .minusMinutes(30);

        assertEquals("1 hour, 30 minutes ago", run(pastDate));
    }

    @Test
    void testPerformInFewMinutes() {
        LocalDateTime now = LocalDateTime.now()
            .plusSeconds(1);
        LocalDateTime futureDate = now.plusMinutes(30);

        assertEquals("in 30 minutes", run(futureDate));
    }

    @Test
    void testPerformFewMinutesAgo() {
        LocalDateTime now = LocalDateTime.now()
            .plusSeconds(1);
        LocalDateTime pastDate = now.minusMinutes(15);

        assertEquals("15 minutes ago", run(pastDate));
    }

    @Test
    void testPerformJustNow() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime pastDate = now.minusSeconds(30);

        assertEquals("31 seconds ago", run(pastDate));
    }

    @Test
    void testPerformInFewSeconds() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureDate = now.plusSeconds(45);

        assertEquals("in 44 seconds", run(futureDate));
    }
}
