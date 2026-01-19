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
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MINUTE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MONTH;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.SECOND;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.UNIX_TIMESTAMP;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.YEAR;
import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.date.helper.constants.DateHelperComparisonEnum;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;

/**
 * @author Monika Kušter
 * @author Nikolina Spehar
 */
public class DateHelperUtils {

    private DateHelperUtils() {
    }

    public static LocalDateTime applyResolution(String resolution, LocalDateTime localDateTime) {
        return switch (resolution) {
            case SECOND -> localDateTime.truncatedTo(ChronoUnit.SECONDS);
            case MINUTE -> localDateTime.truncatedTo(ChronoUnit.MINUTES);
            case HOUR -> localDateTime.truncatedTo(ChronoUnit.HOURS);
            case DAY -> localDateTime.truncatedTo(ChronoUnit.DAYS);
            case MONTH -> localDateTime.withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
            case YEAR -> localDateTime.withMonth(1)
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
            default -> throw new IllegalArgumentException("Unknown resolution type: " + resolution);
        };
    }

    public static @NonNull String formatDuration(long totalSeconds) {
        if (totalSeconds == 0) {
            return "0 seconds";
        }

        totalSeconds = Math.abs(totalSeconds);

        StringBuilder stringBuilder = new StringBuilder();

        for (Unit unit : UNITS) {
            long quantity = totalSeconds / unit.seconds;
            if (quantity > 0) {
                if (!stringBuilder.isEmpty()) {
                    stringBuilder.append(", ");
                }

                stringBuilder.append(quantity)
                    .append(" ")
                    .append(unit.plural(quantity));
                totalSeconds %= unit.seconds;
            }
        }

        return stringBuilder.toString();
    }

    public static ChronoUnit getChronoUnit(String unit) {
        return switch (unit) {
            case SECOND -> ChronoUnit.SECONDS;
            case MINUTE -> ChronoUnit.MINUTES;
            case HOUR -> ChronoUnit.HOURS;
            case DAY -> ChronoUnit.DAYS;
            case MONTH -> ChronoUnit.MONTHS;
            case YEAR -> ChronoUnit.YEARS;
            default -> throw new IllegalArgumentException("Unsupported unit: " + unit);
        };
    }

    public static List<Option<String>> getComparisonOptions() {
        return Arrays.stream(DateHelperComparisonEnum.values())
            .map(dateHelperComparisonEnum -> {
                String name = dateHelperComparisonEnum.getName();

                return (Option<String>) option(name, name);
            })
            .toList();
    }

    public static List<Option<Long>> getDayOfWeekOptions() {
        return Arrays.stream(DayOfWeek.values())
            .map(dayOfWeek -> (Option<Long>) option(
                dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()), dayOfWeek.getValue()))
            .toList();
    }

    public static Object getFormattedDate(String dateFormat, LocalDateTime inputDate) {
        if (dateFormat.equals(UNIX_TIMESTAMP)) {
            ZonedDateTime zonedDateTime = inputDate.atZone(ZoneId.systemDefault());

            return zonedDateTime.toEpochSecond();
        } else {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormat);

            return dateTimeFormatter.format(inputDate);
        }
    }

    public static List<Option<String>> getZoneOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        return ZoneId.getAvailableZoneIds()
            .stream()
            .map(s -> (Option<String>) option(s, s))
            .toList();
    }

    public static LocalDateTime normalizeToTimeOnly(LocalDateTime localDateTime) {
        return localDateTime.withYear(2025)
            .withMonth(1)
            .withDayOfMonth(1);
    }

    private static final List<Unit> UNITS = List.of(
        new Unit(YEAR, 31536000),
        new Unit(MONTH, 2592000),
        new Unit(DAY, 86400),
        new Unit(HOUR, 3600),
        new Unit(MINUTE, 60),
        new Unit(SECOND, 1));

    private record Unit(String name, long seconds) {
        String plural(long quantity) {
            return quantity == 1 ? name : name + "s";
        }
    }
}
