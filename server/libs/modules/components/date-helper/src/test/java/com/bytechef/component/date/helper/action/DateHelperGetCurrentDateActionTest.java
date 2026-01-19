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

import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_FORMAT;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.TIME_ZONE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.UNIX_TIMESTAMP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Monika Kušter
 */
class DateHelperGetCurrentDateActionTest {
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private Parameters mockedParameters;

    @Test
    void testPerformWithUnixTimestamp() {
        mockedParameters = MockParametersFactory.create(
            Map.of(DATE_FORMAT, UNIX_TIMESTAMP, TIME_ZONE, "America/New_York"));

        LocalDateTime localDateTime = LocalDateTime.of(2023, 1, 1, 1, 1, 1);

        try (MockedStatic<LocalDateTime> localDateTimeMockedStatic = mockStatic(
            LocalDateTime.class, Mockito.CALLS_REAL_METHODS)) {

            localDateTimeMockedStatic.when(LocalDateTime::now)
                .thenReturn(localDateTime);

            Object result = DateHelperGetCurrentDateAction.perform(mockedParameters, null, mockedActionContext);

            ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of("America/New_York"));

            long epochSecond = zonedDateTime.toEpochSecond();

            assertEquals(epochSecond, result);
        }
    }

    @Test
    void testPerformWithCustomDateFormat() {
        String format = "yyyy-MM-dd HH:mm:ss";
        mockedParameters = MockParametersFactory.create(Map.of(DATE_FORMAT, format, TIME_ZONE, "America/New_York"));

        LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of("America/New_York"));

        try (MockedStatic<LocalDateTime> localDateTimeMockedStatic = mockStatic(
            LocalDateTime.class, Mockito.CALLS_REAL_METHODS)) {

            localDateTimeMockedStatic.when(() -> LocalDateTime.now(ZoneId.of("America/New_York")))
                .thenReturn(localDateTime);

            Object result = DateHelperGetCurrentDateAction.perform(mockedParameters, null, mockedActionContext);

            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(format);
            String expected = dateTimeFormatter.format(localDateTime);

            assertEquals(expected, result);
        }
    }
}
