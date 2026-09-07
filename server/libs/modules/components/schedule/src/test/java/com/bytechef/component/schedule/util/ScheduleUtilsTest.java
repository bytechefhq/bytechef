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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.definition.Option;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.time.zone.ZoneRules;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class ScheduleUtilsTest {

    @Test
    void testGetTimeZoneOptionsLabelsStandardOffsetNotTheCurrentOne() {
        Instant now = Instant.now();

        List<Option<String>> options = ScheduleUtils.getTimeZoneOptions();

        Optional<String> zoneInDaylightSaving = options.stream()
            .map(Option::getValue)
            .filter(zoneId -> {
                ZoneId zone = ZoneId.of(zoneId);

                ZoneRules zoneRules = zone.getRules();

                return zoneRules.isDaylightSavings(now);
            })
            .findFirst();

        assertTrue(zoneInDaylightSaving.isPresent(), "no zone is observing daylight saving, cannot assert on one");

        String zoneId = zoneInDaylightSaving.get();

        ZoneId zone = ZoneId.of(zoneId);

        ZoneRules zoneRules = zone.getRules();

        ZoneOffset standardOffset = zoneRules.getStandardOffset(now);

        String expectedLabel = zoneId + " (GMT" + standardOffset.getId()
            .replace("Z", "+00:00") + ")";

        Option<String> option = options.stream()
            .filter(candidate -> zoneId.equals(candidate.getValue()))
            .findFirst()
            .orElseThrow();

        assertEquals(expectedLabel, option.getLabel());
    }

    @Test
    void testGetTimeZoneOptionsCoversEveryAvailableZone() {
        List<Option<String>> options = ScheduleUtils.getTimeZoneOptions();

        assertEquals(ZoneId.getAvailableZoneIds()
            .size(), options.size());
    }

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
