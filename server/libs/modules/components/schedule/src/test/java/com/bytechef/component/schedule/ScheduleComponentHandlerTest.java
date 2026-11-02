/*
 * Copyright 2016-2020 the original author or authors.
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
 *
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.component.schedule;

import static com.bytechef.component.definition.ComponentDsl.option;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Option;
import com.bytechef.component.schedule.util.ScheduleUtils;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
class ScheduleComponentHandlerTest {

    private static final List<Option<String>> TIME_ZONE_OPTIONS = List.of(
        option("America/New_York (GMT-05:00)", "America/New_York"),
        option("Europe/Zagreb (GMT+01:00)", "Europe/Zagreb"),
        option("UTC (GMT+00:00)", "UTC"));

    @Test
    void testGetComponentDefinition() {
        try (MockedStatic<ScheduleUtils> scheduleUtilsMockedStatic = mockStatic(
            ScheduleUtils.class, CALLS_REAL_METHODS)) {

            scheduleUtilsMockedStatic.when(ScheduleUtils::getTimeZoneOptions)
                .thenReturn(TIME_ZONE_OPTIONS);

            JsonFileAssert.assertEquals(
                "definition/schedule_v1.json", new ScheduleComponentHandler(null).getDefinition());
        }
    }

    @Test
    @Disabled
    void testCronTrigger() {
        // TODO
    }

    @Test
    @Disabled
    void testEveryDayTrigger() {
        // TODO
    }

    @Test
    @Disabled
    void testEveryMonthTrigger() {
        // TODO
    }

    @Test
    @Disabled
    void testEveryWeekTrigger() {
        // TODO
    }

    @Test
    @Disabled
    void testIntervalTrigger() {
        // TODO
    }
}
