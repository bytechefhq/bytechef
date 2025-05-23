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

import com.bytechef.test.jsonasssert.JsonFileAssert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
class ScheduleComponentHandlerTest {

    @Test
    void testGetComponentDefinition() {
        JsonFileAssert.assertEquals("definition/schedule_v1.json",
            new ScheduleComponentHandler(null).getDefinition());
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
