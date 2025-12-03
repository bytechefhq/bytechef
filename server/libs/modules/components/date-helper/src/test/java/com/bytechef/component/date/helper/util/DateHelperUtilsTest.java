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

package com.bytechef.component.date.helper.util;

import static com.bytechef.component.date.helper.constants.DateHelperConstants.DAY;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.HOUR;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.IS_AFTER;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.IS_AFTER_OR_EQUAL;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.IS_BEFORE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.IS_BEFORE_OR_EQUAL;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.IS_EQUAL;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MINUTE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MONTH;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.SECOND;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.UNIX_TIMESTAMP;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.YEAR;
import static com.bytechef.component.date.helper.util.DateHelperUtils.applyResolution;
import static com.bytechef.component.date.helper.util.DateHelperUtils.formatDuration;
import static com.bytechef.component.date.helper.util.DateHelperUtils.getChronoUnit;
import static com.bytechef.component.date.helper.util.DateHelperUtils.getComparisonOptions;
import static com.bytechef.component.date.helper.util.DateHelperUtils.getFormattedDate;
import static com.bytechef.component.date.helper.util.DateHelperUtils.getZoneOptions;
import static com.bytechef.component.date.helper.util.DateHelperUtils.normalizeToTimeOnly;
import static com.bytechef.component.definition.ComponentDsl.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 * @author Nikolina Spehar
 */
class DateHelperUtilsTest {

    @Test
    void testApplyResolutionSeconds() {
        LocalDateTime inputDate = LocalDateTime.of(2025, 12, 7, 11, 2, 24);

        LocalDateTime result = applyResolution(SECOND, inputDate);

        LocalDateTime expected = LocalDateTime.of(2025, 12, 7, 11, 2, 24);

        assertEquals(expected, result);
    }

    @Test
    void testApplyResolutionMinutes() {
        LocalDateTime inputDate = LocalDateTime.of(2025, 12, 7, 11, 2, 24);

        LocalDateTime result = applyResolution(MINUTE, inputDate);

        LocalDateTime expected = LocalDateTime.of(2025, 12, 7, 11, 2, 0);

        assertEquals(expected, result);
    }

    @Test
    void testApplyResolutionHours() {
        LocalDateTime inputDate = LocalDateTime.of(2025, 12, 7, 11, 2, 24);

        LocalDateTime result = applyResolution(HOUR, inputDate);

        LocalDateTime expected = LocalDateTime.of(2025, 12, 7, 11, 0, 0);

        assertEquals(expected, result);
    }

    @Test
    void testApplyResolutionDays() {
        LocalDateTime inputDate = LocalDateTime.of(2025, 12, 7, 11, 2, 24);

        LocalDateTime result = applyResolution(DAY, inputDate);

        LocalDateTime expected = LocalDateTime.of(2025, 12, 7, 0, 0, 0);

        assertEquals(expected, result);
    }

    @Test
    void testApplyResolutionMonths() {
        LocalDateTime inputDate = LocalDateTime.of(2025, 12, 7, 11, 2, 24);

        LocalDateTime result = applyResolution(MONTH, inputDate);

        LocalDateTime expected = LocalDateTime.of(2025, 12, 1, 0, 0, 0);

        assertEquals(expected, result);
    }

    @Test
    void testApplyResolutionYears() {
        LocalDateTime inputDate = LocalDateTime.of(2025, 12, 7, 11, 2, 24);

        LocalDateTime result = applyResolution(YEAR, inputDate);

        LocalDateTime expected = LocalDateTime.of(2025, 1, 1, 0, 0, 0);

        assertEquals(expected, result);
    }

    @Test
    void testGetComparisonOptions() {
        List<Option<String>> result = getComparisonOptions();

        List<Option<String>> expected = List.of(
            option(IS_AFTER, IS_AFTER),
            option(IS_AFTER_OR_EQUAL, IS_AFTER_OR_EQUAL),
            option(IS_BEFORE, IS_BEFORE),
            option(IS_BEFORE_OR_EQUAL, IS_BEFORE_OR_EQUAL),
            option(IS_EQUAL, IS_EQUAL));

        assertEquals(expected, result);
    }

    @Test
    void testGetFormattedDateWithUnixTimestamp() {
        LocalDateTime inputDate = LocalDateTime.of(2023, 10, 1, 12, 0);

        Object result = getFormattedDate(UNIX_TIMESTAMP, inputDate);

        ZonedDateTime zonedDateTime = inputDate.atZone(ZoneId.systemDefault());
        long expected = zonedDateTime.toEpochSecond();

        assertEquals(expected, result);
    }

    @Test
    void testGetFormattedDateWithCustomDateFormat() {
        LocalDateTime inputDate = LocalDateTime.of(2023, 10, 1, 12, 0);
        String dateFormat = "yyyy-MM-dd HH:mm:ss";

        Object result = getFormattedDate(dateFormat, inputDate);

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormat);
        String expected = dateTimeFormatter.format(inputDate);

        assertEquals(expected, result);
    }

    @Test
    void testGetZoneOptions() {
        Parameters inputParameters = mock(Parameters.class);

        List<Option<String>> result =
            getZoneOptions(inputParameters, inputParameters, Map.of(), "", mock(ActionContext.class));

        List<Option<String>> expected = ZoneId.getAvailableZoneIds()
            .stream()
            .map(s -> option(s, s))
            .collect(Collectors.toList());

        assertEquals(expected, result);
    }

    @Test
    void testNormalizeToTimeOnly() {
        LocalDateTime inputDate = LocalDateTime.of(2023, 10, 1, 12, 0);
        LocalDateTime result = normalizeToTimeOnly(inputDate);
        LocalDateTime expected = LocalDateTime.of(2025, 1, 1, 12, 0);

        assertEquals(expected, result);
    }

    @Test
    void testFormatDurationZeroSeconds() {
        assertEquals("0 seconds", formatDuration(0));
    }

    @Test
    void testFormatDurationOnlySeconds() {
        assertEquals("45 seconds", formatDuration(45));
    }

    @Test
    void testFormatDurationOnlyMinutes() {
        assertEquals("2 minutes", formatDuration(120));
    }

    @Test
    void testFormatDurationMinutesAndSeconds() {
        assertEquals("1 minute, 5 seconds", formatDuration(65));
    }

    @Test
    void testFormatDurationHoursMinutesSeconds() {
        assertEquals("1 hour, 1 minute, 1 second", formatDuration(3661));
    }

    @Test
    void testFormatDurationDaysHoursMinutesSeconds() {
        assertEquals("1 day, 2 hours, 3 minutes, 4 seconds",
            formatDuration(86400 + 2 * 3600 + 3 * 60 + 4));
    }

    @Test
    void testFormatDurationOneMonth() {
        assertEquals("1 month", formatDuration(30L * 24 * 3600));
    }

    @Test
    void testFormatDurationOneYear() {
        assertEquals("1 year", formatDuration(365L * 24 * 3600));
    }

    @Test
    void testFormatDurationYearMonthDayCombination() {
        long seconds =
            365L * 24 * 3600 +
                2L * 30 * 24 * 3600 +
                3L * 24 * 3600;

        assertEquals("1 year, 2 months, 3 days", formatDuration(seconds));
    }

    @Test
    void testFormatDurationPluralization() {
        assertEquals("2 hours, 2 minutes, 2 seconds",
            formatDuration(2 * 3600 + 2 * 60 + 2));
    }

    @Test
    void testFormatDurationNegativeInput() {
        assertEquals("1 minute", formatDuration(-60));
    }

    @Test
    void testFormatDurationLargeNumber() {
        assertEquals("11 days, 13 hours, 46 minutes, 40 seconds",
            formatDuration(1_000_000L));
    }

    @Test
    void testGetChronUnitSecondMapping() {
        assertEquals(ChronoUnit.SECONDS, getChronoUnit(SECOND));
    }

    @Test
    void testGetChronUnitMinuteMapping() {
        assertEquals(ChronoUnit.MINUTES, getChronoUnit(MINUTE));
    }

    @Test
    void testGetChronUnitHourMapping() {
        assertEquals(ChronoUnit.HOURS, getChronoUnit(HOUR));
    }

    @Test
    void testGetChronUnitDayMapping() {
        assertEquals(ChronoUnit.DAYS, getChronoUnit(DAY));
    }

    @Test
    void testGetChronUnitMonthMapping() {
        assertEquals(ChronoUnit.MONTHS, getChronoUnit(MONTH));
    }

    @Test
    void testGetChronUnitYearMapping() {
        assertEquals(ChronoUnit.YEARS, getChronoUnit(YEAR));
    }

    @Test
    void testGetChronUnitUnsupportedUnitThrows() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> getChronoUnit("WEEK"));
        assertEquals("Unsupported unit: WEEK", ex.getMessage());
    }
}
