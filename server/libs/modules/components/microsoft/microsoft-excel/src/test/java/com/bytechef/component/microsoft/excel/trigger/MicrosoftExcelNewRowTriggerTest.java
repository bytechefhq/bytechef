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

package com.bytechef.component.microsoft.excel.trigger;

import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.IS_THE_FIRST_ROW_HEADER;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKBOOK_ID;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKSHEET_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.microsoft.excel.util.MicrosoftExcelRowUtils;
import com.bytechef.component.microsoft.excel.util.MicrosoftExcelUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Marija Horvat
 */
class MicrosoftExcelNewRowTriggerTest {

    private final ArgumentCaptor<Integer> integerArgumentCaptor = forClass(Integer.class);
    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> listArgumentCaptor = forClass(List.class);
    private final Parameters mockedClosureParameters = MockParametersFactory.create(Map.of("lastRowIndex", 1));
    private final Parameters mockedInputParameters = MockParametersFactory.create(
        Map.of(WORKBOOK_ID, 1, WORKSHEET_NAME, "test", IS_THE_FIRST_ROW_HEADER, true));
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = forClass(Parameters.class);
    private final ArgumentCaptor<TriggerContext> triggerContextArgumentCaptor = forClass(TriggerContext.class);

    @Test
    void testPoll() {
        List<Object> row = List.of("abc", "sheetName", false);
        Map<String, Object> map = Map.of("key", "value");

        try (MockedStatic<MicrosoftExcelUtils> microsoftExcelUtilsMockedStatic = mockStatic(MicrosoftExcelUtils.class);
            MockedStatic<MicrosoftExcelRowUtils> microsoftExcelRowUtilsMockedStatic =
                mockStatic(MicrosoftExcelRowUtils.class)) {

            microsoftExcelUtilsMockedStatic
                .when(() -> MicrosoftExcelUtils.getLastUsedRowIndex(parametersArgumentCaptor.capture(),
                    triggerContextArgumentCaptor.capture()))
                .thenReturn(2);
            microsoftExcelRowUtilsMockedStatic
                .when(() -> MicrosoftExcelRowUtils.getRowFromWorksheet(
                    parametersArgumentCaptor.capture(), triggerContextArgumentCaptor.capture(),
                    integerArgumentCaptor.capture()))
                .thenReturn(row);
            microsoftExcelUtilsMockedStatic
                .when(() -> MicrosoftExcelUtils.getMapOfValuesForRow(
                    parametersArgumentCaptor.capture(), triggerContextArgumentCaptor.capture(),
                    listArgumentCaptor.capture()))
                .thenReturn(map);

            PollOutput result = MicrosoftExcelNewRowTrigger.poll(
                mockedInputParameters, null, mockedClosureParameters, mockedTriggerContext);

            assertEquals(new PollOutput(List.of(map), Map.of("lastRowIndex", 2), false), result);
            assertEquals(
                List.of(mockedInputParameters, mockedInputParameters, mockedInputParameters),
                parametersArgumentCaptor.getAllValues());
            assertEquals(
                List.of(mockedTriggerContext, mockedTriggerContext, mockedTriggerContext),
                triggerContextArgumentCaptor.getAllValues());
            assertEquals(2, integerArgumentCaptor.getValue());
            assertEquals(row, listArgumentCaptor.getValue());
        }
    }
}
