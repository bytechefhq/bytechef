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

import static com.bytechef.component.date.helper.constants.DateHelperConstants.BUSINESS_HOURS_END;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.BUSINESS_HOURS_START;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.BUSINESS_WEEK_END;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.BUSINESS_WEEK_START;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.INPUT_DATE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.TIME_ZONE;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class DateHelperIsBusinessHoursActionTest {

    private final Context mockedContext = mock(Context.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            INPUT_DATE, LocalDateTime.of(2025, 1, 1, 13, 1, 1),
            BUSINESS_WEEK_START, 1, BUSINESS_WEEK_END, 5, BUSINESS_HOURS_START, LocalTime.of(8, 0, 0),
            BUSINESS_HOURS_END, LocalTime.of(16, 0, 0), TIME_ZONE, "UTC"));

    @Test
    void testPerform() {
        boolean result = DateHelperIsBusinessHoursAction.perform(mockedParameters, null, mockedContext);

        assertTrue(result);
    }
}
