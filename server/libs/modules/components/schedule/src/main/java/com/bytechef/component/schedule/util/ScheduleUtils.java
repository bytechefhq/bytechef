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

import com.bytechef.component.definition.Option;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
public class ScheduleUtils {

    public static List<Option<Long>> getDayOfWeekOptions() {
        return Arrays.stream(DayOfWeek.values())
            .map(dayOfWeek -> option(
                dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                dayOfWeek.getValue() == 7 ? 1 : dayOfWeek.getValue() + 1))
            .collect(Collectors.toList());
    }

    public static List<Option<String>> getTimeZoneOptions() {
        List<Option<String>> options = new ArrayList<>();
        Instant now = Instant.now();
        Set<String> zoneIds = ZoneId.getAvailableZoneIds();

        for (String zoneId : zoneIds) {
            ZoneId zone = ZoneId.of(zoneId);

            ZoneRules zoneRules = zone.getRules();

            ZoneOffset zoneOffset = zoneRules.getStandardOffset(now);

            String zoneOffsetId = zoneOffset.getId();

            options.add(option(zoneId + " (GMT" + zoneOffsetId.replace("Z", "+00:00") + ")", zoneId));
        }

        options.sort((o1, o2) -> {
            String name = o1.getLabel();

            return name.compareTo(o2.getLabel());
        });

        return options;
    }
}
