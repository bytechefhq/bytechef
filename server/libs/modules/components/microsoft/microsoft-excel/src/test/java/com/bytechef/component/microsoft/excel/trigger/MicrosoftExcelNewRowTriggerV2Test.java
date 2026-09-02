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
import com.bytechef.component.microsoft.excel.util.MicrosoftExcelUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Anshul Goel
 */
class MicrosoftExcelNewRowTriggerV2Test {

    private final Parameters mockedInputParameters = MockParametersFactory.create(
        Map.of(WORKBOOK_ID, 1, WORKSHEET_NAME, "test", IS_THE_FIRST_ROW_HEADER, true));
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = forClass(Parameters.class);
    private final ArgumentCaptor<TriggerContext> triggerContextArgumentCaptor = forClass(TriggerContext.class);

    @Test
    void testPollOnFirstRunReturnsAllRowsAsNew() {
        List<Object> row1 = List.of("abc", "sheetName", false);
        List<Object> row2 = List.of("def", "sheetName2", true);
        Map<String, Object> map1 = Map.of("key", "value1");
        Map<String, Object> map2 = Map.of("key", "value2");
        Parameters mockedClosureParameters = MockParametersFactory.create(Map.of());

        try (MockedStatic<MicrosoftExcelUtils> microsoftExcelUtilsMockedStatic = mockStatic(MicrosoftExcelUtils.class)) {
            microsoftExcelUtilsMockedStatic
                .when(() -> MicrosoftExcelUtils.getUsedRangeValues(
                    parametersArgumentCaptor.capture(), triggerContextArgumentCaptor.capture()))
                .thenReturn(List.of(row1, row2));
            microsoftExcelUtilsMockedStatic
                .when(() -> MicrosoftExcelUtils.getMapOfValuesForRow(mockedInputParameters, mockedTriggerContext, row1))
                .thenReturn(map1);
            microsoftExcelUtilsMockedStatic
                .when(() -> MicrosoftExcelUtils.getMapOfValuesForRow(mockedInputParameters, mockedTriggerContext, row2))
                .thenReturn(map2);

            PollOutput result = MicrosoftExcelNewRowTriggerV2.poll(
                mockedInputParameters, null, mockedClosureParameters, mockedTriggerContext);

            assertEquals(
                new PollOutput(List.of(map1, map2), Map.of("knownRowIds", List.of(1, 2)), false),
                result);
        }
    }

    @Test
    void testPollDetectsRowAppendedAtBottom() {
        List<Object> row1 = List.of("a1", "a2");
        List<Object> row2 = List.of("b1", "b2");
        List<Object> row3 = List.of("c1", "c2");
        Map<String, Object> map3 = Map.of("key", "c");
        Parameters mockedClosureParameters = MockParametersFactory.create(Map.of("knownRowIds", List.of(1, 2)));

        try (MockedStatic<MicrosoftExcelUtils> microsoftExcelUtilsMockedStatic = mockStatic(MicrosoftExcelUtils.class)) {
            microsoftExcelUtilsMockedStatic
                .when(() -> MicrosoftExcelUtils.getUsedRangeValues(mockedInputParameters, mockedTriggerContext))
                .thenReturn(List.of(row1, row2, row3));
            microsoftExcelUtilsMockedStatic
                .when(() -> MicrosoftExcelUtils.getMapOfValuesForRow(mockedInputParameters, mockedTriggerContext, row3))
                .thenReturn(map3);

            PollOutput result = MicrosoftExcelNewRowTriggerV2.poll(
                mockedInputParameters, null, mockedClosureParameters, mockedTriggerContext);

            assertEquals(
                new PollOutput(List.of(map3), Map.of("knownRowIds", List.of(1, 2, 3)), false),
                result);
        }
    }

    @Test
    void testPollDoesNotTriggerWhenExistingRowIsEdited() {
        List<Object> editedRow = List.of("a1-edited", "a2-edited");
        Parameters mockedClosureParameters = MockParametersFactory.create(Map.of("knownRowIds", List.of(1)));

        try (MockedStatic<MicrosoftExcelUtils> microsoftExcelUtilsMockedStatic = mockStatic(MicrosoftExcelUtils.class)) {
            microsoftExcelUtilsMockedStatic
                .when(() -> MicrosoftExcelUtils.getUsedRangeValues(mockedInputParameters, mockedTriggerContext))
                .thenReturn(List.of(editedRow));

            PollOutput result = MicrosoftExcelNewRowTriggerV2.poll(
                mockedInputParameters, null, mockedClosureParameters, mockedTriggerContext);

            assertEquals(new PollOutput(List.of(), Map.of("knownRowIds", List.of(1)), false), result);
        }
    }

    @Test
    void testPollSkipsEmptyRows() {
        List<Object> emptyRow = List.of();
        Parameters mockedClosureParameters = MockParametersFactory.create(Map.of());

        try (MockedStatic<MicrosoftExcelUtils> microsoftExcelUtilsMockedStatic = mockStatic(MicrosoftExcelUtils.class)) {
            microsoftExcelUtilsMockedStatic
                .when(() -> MicrosoftExcelUtils.getUsedRangeValues(mockedInputParameters, mockedTriggerContext))
                .thenReturn(List.of(emptyRow));

            PollOutput result = MicrosoftExcelNewRowTriggerV2.poll(
                mockedInputParameters, null, mockedClosureParameters, mockedTriggerContext);

            assertEquals(new PollOutput(List.of(), Map.of("knownRowIds", List.of()), false), result);
        }
    }
}
