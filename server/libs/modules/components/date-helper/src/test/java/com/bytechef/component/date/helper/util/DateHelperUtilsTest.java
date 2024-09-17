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
import static com.bytechef.component.date.helper.util.DateHelperUtils.getFormattedDate;
import static com.bytechef.component.date.helper.util.DateHelperUtils.getZoneOptions;
import static com.bytechef.component.definition.ComponentDsl.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

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
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class DateHelperUtilsTest {

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
}
