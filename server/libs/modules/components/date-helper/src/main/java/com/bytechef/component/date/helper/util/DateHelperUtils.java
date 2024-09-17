/*
 * Copyright 2023-present ByteChef Inc.
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

import static com.bytechef.component.date.helper.constants.DateHelperConstants.UNIX_TIMESTAMP;
import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Monika Kušter
 */
public class DateHelperUtils {

    private DateHelperUtils() {
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
            .map(s -> option(s, s))
            .collect(Collectors.toList());
    }
}
