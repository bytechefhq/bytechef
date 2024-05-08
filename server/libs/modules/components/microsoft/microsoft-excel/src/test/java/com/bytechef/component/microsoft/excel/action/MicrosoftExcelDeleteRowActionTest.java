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

package com.bytechef.component.microsoft.excel.action;

import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.ROW_NUMBER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.microsoft.excel.util.MicrosoftExcelUtils;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Domiter
 */
class MicrosoftExcelDeleteRowActionTest extends AbstractMicrosoftExcelActionTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);

    @Test
    void testPerform() {
        when(mockedParameters.getRequiredInteger(ROW_NUMBER))
            .thenReturn(2);
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);

        microsoftExcelUtilsMockedStatic
            .when(() -> MicrosoftExcelUtils.getLastUsedColumnLabel(mockedParameters, mockedContext))
            .thenReturn("C");

        Object result = MicrosoftExcelDeleteRowAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertNull(result);

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(Map.of("shift", "Up"), body.getContent());
    }

}
