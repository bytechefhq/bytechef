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

package com.bytechef.component.schedule.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Option;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class ScheduleUtilsTest {

    @Test
    void testGetDayOfWeekOptions() {
        List<Option<Long>> expected = List.of(
            option(DayOfWeek.MONDAY.getDisplayName(TextStyle.FULL, Locale.getDefault()), 2),
            option(DayOfWeek.TUESDAY.getDisplayName(TextStyle.FULL, Locale.getDefault()), 3),
            option(DayOfWeek.WEDNESDAY.getDisplayName(TextStyle.FULL, Locale.getDefault()), 4),
            option(DayOfWeek.THURSDAY.getDisplayName(TextStyle.FULL, Locale.getDefault()), 5),
            option(DayOfWeek.FRIDAY.getDisplayName(TextStyle.FULL, Locale.getDefault()), 6),
            option(DayOfWeek.SATURDAY.getDisplayName(TextStyle.FULL, Locale.getDefault()), 7),
            option(DayOfWeek.SUNDAY.getDisplayName(TextStyle.FULL, Locale.getDefault()), 1));

        List<Option<Long>> result = ScheduleUtils.getDayOfWeekOptions();

        assertEquals(expected, result);
    }
}
