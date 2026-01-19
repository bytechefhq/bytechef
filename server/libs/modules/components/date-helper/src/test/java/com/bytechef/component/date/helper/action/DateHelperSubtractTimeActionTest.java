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
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DAY;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.HOUR;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.INPUT_DATE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MINUTE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MONTH;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.SECOND;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.YEAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.date.helper.util.DateHelperUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class DateHelperSubtractTimeActionTest {

    private final ArgumentCaptor<LocalDateTime> localDateTimeArgumentCaptor = forClass(LocalDateTime.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(INPUT_DATE, LocalDateTime.of(2000, 1, 1, 1, 1, 1), YEAR, 1, MONTH, 1, DAY, 1, HOUR, 1, MINUTE, 1,
            SECOND, 1, DATE_FORMAT, "dd/MM/yy"));

    @Test
    void testPerform() {
        try (MockedStatic<DateHelperUtils> dateHelperUtilsMockedStatic = mockStatic(DateHelperUtils.class)) {
            dateHelperUtilsMockedStatic
                .when(() -> DateHelperUtils.getFormattedDate(stringArgumentCaptor.capture(),
                    localDateTimeArgumentCaptor.capture()))
                .thenReturn("date");

            Object result =
                DateHelperSubtractTimeAction.perform(mockedParameters, mockedParameters, mock(ActionContext.class));

            assertEquals("date", result);
            assertEquals("dd/MM/yy", stringArgumentCaptor.getValue());
            assertEquals(LocalDateTime.of(1998, 11, 30, 0, 0, 0), localDateTimeArgumentCaptor.getValue());
        }
    }
}
