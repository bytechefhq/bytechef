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

package com.bytechef.commons.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Normalizes temporal values so that every value fixing a point on the timeline is represented as a
 * {@link ZonedDateTime} at UTC, which is the type {@code parseDate} returns and therefore the one workflow expressions
 * can compare against.
 *
 * @author Ivica Cardic
 */
public final class TemporalValueUtils {

    private TemporalValueUtils() {
    }

    public static @Nullable Object normalize(@Nullable Object value) {
        return switch (value) {
            case null -> null;
            case java.sql.Date sqlDate -> sqlDate.toLocalDate();
            case java.sql.Time sqlTime -> sqlTime.toLocalTime();
            case Date date -> date.toInstant()
                .atZone(ZoneOffset.UTC);
            case Instant instant -> instant.atZone(ZoneOffset.UTC);
            case OffsetDateTime offsetDateTime -> offsetDateTime.toInstant()
                .atZone(ZoneOffset.UTC);
            case ZonedDateTime zonedDateTime -> zonedDateTime.withZoneSameInstant(ZoneOffset.UTC);
            case LocalDate localDate -> localDate;
            case LocalDateTime localDateTime -> localDateTime;
            case LocalTime localTime -> localTime;
            case OffsetTime offsetTime -> offsetTime;
            case Map<?, ?> map -> normalizeMap(map);
            case List<?> list -> normalizeList(list);
            default -> value;
        };
    }

    private static Map<String, @Nullable Object> normalizeMap(Map<?, ?> map) {
        Map<String, @Nullable Object> normalizedMap = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            normalizedMap.put(String.valueOf(entry.getKey()), normalize(entry.getValue()));
        }

        return normalizedMap;
    }

    private static List<@Nullable Object> normalizeList(List<?> list) {
        List<@Nullable Object> normalizedList = new ArrayList<>(list.size());

        for (Object item : list) {
            normalizedList.add(normalize(item));
        }

        return normalizedList;
    }
}
