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

import static com.bytechef.component.date.helper.action.DateHelperConvertToDateAction.DATE_TIME_TYPE;
import static com.bytechef.component.date.helper.action.DateHelperConvertToDateAction.DATE_TYPE;
import static com.bytechef.component.date.helper.action.DateHelperConvertToDateAction.TYPE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_FORMAT;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_STRING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Marko Krišković
 */
class DateHelperConvertToDateActionTest {

    private final Context context = mock(Context.class);

    @Test
    void testPerformDateTypeWithIsoFormat() {
        Parameters parameters = MockParametersFactory.create(
            Map.of(
                DATE_STRING, "2023-09-17",
                TYPE, DATE_TYPE));

        Object result = DateHelperConvertToDateAction.perform(parameters, parameters, context);

        assertEquals(LocalDate.of(2023, 9, 17), result);
    }

    @Test
    void testPerformDateTypeWithCustomFormat() {
        Parameters parameters = MockParametersFactory.create(
            Map.of(
                DATE_STRING, "17/09/2023",
                DATE_FORMAT, "dd/MM/yyyy",
                TYPE, DATE_TYPE));

        Object result = DateHelperConvertToDateAction.perform(parameters, parameters, context);

        assertEquals(LocalDate.of(2023, 9, 17), result);
    }

    @Test
    void testPerformDateTimeTypeWithIsoFormat() {
        Parameters parameters = MockParametersFactory.create(
            Map.of(
                DATE_STRING, "2023-09-17T11:23:58",
                TYPE, DATE_TIME_TYPE));

        Object result = DateHelperConvertToDateAction.perform(parameters, parameters, context);

        assertEquals(LocalDateTime.of(2023, 9, 17, 11, 23, 58), result);
    }

    @Test
    void testPerformDateTimeTypeWithCustomFormat() {
        Parameters parameters = MockParametersFactory.create(
            Map.of(
                DATE_STRING, "17-09-2023 11:23:58",
                DATE_FORMAT, "dd-MM-yyyy HH:mm:ss",
                TYPE, DATE_TIME_TYPE));

        Object result = DateHelperConvertToDateAction.perform(parameters, parameters, context);

        assertEquals(LocalDateTime.of(2023, 9, 17, 11, 23, 58), result);
    }

    @Test
    void testPerformInvalidDateStringThrowsException() {
        Parameters parameters = MockParametersFactory.create(
            Map.of(
                DATE_STRING, "not-a-date",
                TYPE, DATE_TYPE));

        org.junit.jupiter.api.Assertions.assertThrows(
            Exception.class,
            () -> DateHelperConvertToDateAction.perform(parameters, parameters, context));
    }
}
