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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.microsoft.excel.util.MicrosoftExcelUpdateWorksheetUtils;
import com.bytechef.component.microsoft.excel.util.MicrosoftExcelUtils;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
class MicrosoftExcelAppendRowActionTest extends AbstractMicrosoftExcelActionTest {

    @Test
    void testPerform() {
        List<Object> row = List.of("abc", "sheetName", false);
        Map<String, Object> map = Map.of("key", "value");

        microsoftExcelUtilsMockedStatic
            .when(() -> MicrosoftExcelUtils.getRowInputValues(mockedParameters))
            .thenReturn(row);
        microsoftExcelUtilsMockedStatic
            .when(() -> MicrosoftExcelUtils.getLastUsedRowIndex(mockedParameters, mockedContext))
            .thenReturn(2);
        updateWorksheetUtilsMockedStatic
            .when(() -> MicrosoftExcelUpdateWorksheetUtils.updateRange(mockedParameters, mockedContext, 3, row))
            .thenReturn(map);

        Object result = MicrosoftExcelAppendRowAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(map, result);
    }
}
