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

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.microsoft.excel.util.MicrosoftExcelUpdateWorksheetUtils;
import com.bytechef.component.microsoft.excel.util.MicrosoftExcelUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class MicrosoftExcelUpdateRowActionTest extends AbstractMicrosoftExcelActionTest {

    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(ROW_NUMBER, 2));

    @SuppressWarnings("unchecked")
    @Test
    void testPerform() {
        List<Object> row = List.of("abc", "sheetName", false);
        Map<String, Object> map = Map.of("key", "value");

        microsoftExcelUtilsMockedStatic
            .when(() -> MicrosoftExcelUtils.getUpdatedRowValues(
                parametersArgumentCaptor.capture(), actionContextArgumentCaptor.capture()))
            .thenReturn(row);
        updateWorksheetUtilsMockedStatic
            .when(() -> MicrosoftExcelUpdateWorksheetUtils.updateRange(
                parametersArgumentCaptor.capture(), actionContextArgumentCaptor.capture(),
                integerArgumentCaptor.capture(), listArgumentCaptor.capture()))
            .thenReturn(map);

        Object result = MicrosoftExcelUpdateRowAction.perform(mockedParameters, mockedParameters, mockedActionContext);

        assertEquals(map, result);
        assertEquals(List.of(mockedParameters, mockedParameters), parametersArgumentCaptor.getAllValues());
        assertEquals(List.of(mockedActionContext, mockedActionContext), actionContextArgumentCaptor.getAllValues());
        assertEquals(2, integerArgumentCaptor.getValue());
        assertEquals(row, listArgumentCaptor.getValue());
    }
}
