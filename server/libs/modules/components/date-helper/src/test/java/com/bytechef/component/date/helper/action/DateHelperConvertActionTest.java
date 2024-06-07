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

package com.bytechef.component.date.helper.action;

import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_FORMAT;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_FORMAT_OPTION_ISO8601_DATE_TIME_VALUE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_FORMAT_OPTION_ISO8601_DATE_VALUE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_TIMESTAMP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Igor Beslic
 */
class DateHelperConvertActionTest {

    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Parameters mockedParameters = mock(Parameters.class);

    @Disabled
    @Test
    void testActionPerform() {
        mockParameters(1716572102L, DATE_FORMAT_OPTION_ISO8601_DATE_VALUE);

        assertEquals(
            "2024-05-24",
            DateHelperConvertAction.perform(mockedParameters, mockedParameters, mockedContext));

        mockParameters(1716572102000L, DATE_FORMAT_OPTION_ISO8601_DATE_VALUE);

        assertEquals(
            "2024-05-24",
            DateHelperConvertAction.perform(mockedParameters, mockedParameters, mockedContext));

        mockParameters(1716572102L, DATE_FORMAT_OPTION_ISO8601_DATE_TIME_VALUE);

        assertEquals(
            "2024-05-24T19:35:02.000+0200",
            DateHelperConvertAction.perform(mockedParameters, mockedParameters, mockedContext));

        mockParameters(1716572102977L, DATE_FORMAT_OPTION_ISO8601_DATE_TIME_VALUE);

        assertEquals(
            "2024-05-24T19:35:02.977+0200",
            DateHelperConvertAction.perform(mockedParameters, mockedParameters, mockedContext));
    }

    private void mockParameters(Long unixTimestamp, String dateFormat) {
        when(mockedParameters.getRequiredString(DATE_FORMAT))
            .thenReturn(dateFormat);
        when(mockedParameters.getRequiredLong(DATE_TIMESTAMP))
            .thenReturn(unixTimestamp);
    }
}
