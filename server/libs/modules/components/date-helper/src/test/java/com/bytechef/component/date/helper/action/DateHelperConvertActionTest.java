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

import com.bytechef.component.date.helper.constants.DateHelperConstants;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Igor Beslic
 */
public class DateHelperConvertActionTest {

    @Test
    public void testActionPerform() {
        Parameters parameters = getParameters(1716572102L, DateHelperConstants.DATE_FORMAT_OPTION_ISO8601_DATE_VALUE);

        Assertions.assertEquals("2024-05-24",
            DateHelperConvertAction.perform(parameters, parameters, Mockito.mock(ActionContext.class)));

        parameters = getParameters(1716572102000L, DateHelperConstants.DATE_FORMAT_OPTION_ISO8601_DATE_VALUE);

        Assertions.assertEquals("2024-05-24",
            DateHelperConvertAction.perform(parameters, parameters, Mockito.mock(ActionContext.class)));

        parameters = getParameters(1716572102L, DateHelperConstants.DATE_FORMAT_OPTION_ISO8601_DATE_TIME_VALUE);

        Assertions.assertEquals("2024-05-24T19:35:02.000+0200",
            DateHelperConvertAction.perform(parameters, parameters, Mockito.mock(ActionContext.class)));

        parameters = getParameters(1716572102977L, DateHelperConstants.DATE_FORMAT_OPTION_ISO8601_DATE_TIME_VALUE);

        Assertions.assertEquals("2024-05-24T19:35:02.977+0200",
            DateHelperConvertAction.perform(parameters, parameters, Mockito.mock(ActionContext.class)));
    }

    private Parameters getParameters(Long unixTimestamp, String dateFormat) {
        Parameters parameters = Mockito.mock(Parameters.class);

        Mockito.when(parameters.getRequiredString(DateHelperConstants.DATE_FORMAT))
            .thenReturn(dateFormat);
        Mockito.when(parameters.getRequiredLong(DateHelperConstants.DATE_TIMESTAMP))
            .thenReturn(unixTimestamp);

        return parameters;
    }
}
