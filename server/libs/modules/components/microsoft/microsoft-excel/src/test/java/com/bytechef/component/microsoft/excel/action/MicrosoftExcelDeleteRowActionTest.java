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

package com.bytechef.component.microsoft.excel.action;

import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.ROW_NUMBER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.microsoft.excel.util.MicrosoftExcelUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class MicrosoftExcelDeleteRowActionTest extends AbstractMicrosoftExcelActionTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(ROW_NUMBER, 2));

    @Test
    void testPerform() {
        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);

        microsoftExcelUtilsMockedStatic
            .when(() -> MicrosoftExcelUtils.getLastUsedColumnLabel(
                parametersArgumentCaptor.capture(), actionContextArgumentCaptor.capture()))
            .thenReturn("C");

        Object result = MicrosoftExcelDeleteRowAction.perform(mockedParameters, mockedParameters, mockedActionContext);

        assertNull(result);

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(Map.of("shift", "Up"), body.getContent());
        assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
        assertEquals(mockedActionContext, actionContextArgumentCaptor.getValue());
    }

}
