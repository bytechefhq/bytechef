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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.microsoft.excel.util.MicrosoftExcelRowDiffUtils;
import com.bytechef.component.microsoft.excel.util.MicrosoftExcelUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Anshul Goel
 */
class MicrosoftExcelNewRowTriggerV2Test {

    private static final List<Object> HEADER_ROW = List.of("h1", "h2");
    private static final List<Object> ROW_1 = List.of("a1", "a2");
    private static final List<Object> ROW_2 = List.of("b1", "b2");
    private static final List<Object> ROW_3 = List.of("c1", "c2");
    private static final List<Object> INSERTED_ROW = List.of("x1", "x2");

    private final Parameters mockedInputParameters = MockParametersFactory.create(
        Map.of(WORKBOOK_ID, 1, WORKSHEET_NAME, "test", IS_THE_FIRST_ROW_HEADER, false));
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);

    @Test
    void testPollOnFirstRunReturnsAllRowsAsNew() {
        PollOutput result = executePoll(List.of(ROW_1, ROW_2), List.of(), List.of(ROW_1, ROW_2));

        assertEquals(List.of(mapOf(ROW_1), mapOf(ROW_2)), result.records());
        assertEquals(Map.of("knownRowHashes", hashesOf(ROW_1, ROW_2)), result.closureParameters());
    }

    @Test
    void testPollDetectsRowAppendedAtBottom() {
        PollOutput result = executePoll(
            List.of(ROW_1, ROW_2, ROW_3), hashesOf(ROW_1, ROW_2), List.of(ROW_3));

        assertEquals(List.of(mapOf(ROW_3)), result.records());
        assertEquals(Map.of("knownRowHashes", hashesOf(ROW_1, ROW_2, ROW_3)), result.closureParameters());
    }

    @Test
    void testPollDetectsRowInsertedInTheMiddle() {
        PollOutput result = executePoll(
            List.of(ROW_1, INSERTED_ROW, ROW_2, ROW_3), hashesOf(ROW_1, ROW_2, ROW_3), List.of(INSERTED_ROW));

        assertEquals(List.of(mapOf(INSERTED_ROW)), result.records());
        assertEquals(
            Map.of("knownRowHashes", hashesOf(ROW_1, INSERTED_ROW, ROW_2, ROW_3)), result.closureParameters());
    }

    @Test
    void testPollDoesNotTriggerWhenExistingRowIsEdited() {
        List<Object> editedRow = List.of("a1-edited", "a2-edited");

        PollOutput result = executePoll(List.of(editedRow, ROW_2), hashesOf(ROW_1, ROW_2), List.of());

        assertEquals(List.of(), result.records());
        assertEquals(Map.of("knownRowHashes", hashesOf(editedRow, ROW_2)), result.closureParameters());
    }

    @Test
    void testPollSkipsRowsPaddedWithBlankCells() {
        List<Object> blankRow = Arrays.asList("", null);

        PollOutput result = executePoll(List.of(ROW_1, blankRow, ROW_2), List.of(), List.of(ROW_1, ROW_2));

        assertEquals(List.of(mapOf(ROW_1), mapOf(ROW_2)), result.records());
        assertEquals(Map.of("knownRowHashes", hashesOf(ROW_1, ROW_2)), result.closureParameters());
    }

    @Test
    void testPollOnEmptyWorksheetReturnsNoRows() {
        PollOutput result = executePoll(List.of(List.of("")), List.of(), List.of());

        assertEquals(List.of(), result.records());
        assertEquals(Map.of("knownRowHashes", List.of()), result.closureParameters());
    }

    @Test
    void testPollSkipsHeaderRow() {
        Parameters inputParameters = MockParametersFactory.create(
            Map.of(WORKBOOK_ID, 1, WORKSHEET_NAME, "test", IS_THE_FIRST_ROW_HEADER, true));

        PollOutput result = executePoll(
            inputParameters, List.of(HEADER_ROW, ROW_1), List.of(), List.of(ROW_1), HEADER_ROW);

        assertEquals(List.of(mapOf(ROW_1)), result.records());
        assertEquals(Map.of("knownRowHashes", hashesOf(ROW_1)), result.closureParameters());
    }

    private PollOutput executePoll(
        List<List<Object>> rows, List<String> knownRowHashes, List<List<Object>> mappedRows) {

        return executePoll(mockedInputParameters, rows, knownRowHashes, mappedRows, List.of());
    }

    private PollOutput executePoll(
        Parameters inputParameters, List<List<Object>> rows, List<String> knownRowHashes,
        List<List<Object>> mappedRows, List<Object> headerRow) {

        Parameters closureParameters = knownRowHashes.isEmpty()
            ? MockParametersFactory.create(Map.of())
            : MockParametersFactory.create(Map.of("knownRowHashes", knownRowHashes));

        try (MockedStatic<MicrosoftExcelUtils> microsoftExcelUtilsMockedStatic =
            mockStatic(MicrosoftExcelUtils.class)) {

            microsoftExcelUtilsMockedStatic
                .when(() -> MicrosoftExcelUtils.getUsedRangeValues(inputParameters, mockedTriggerContext))
                .thenReturn(rows);

            for (List<Object> row : mappedRows) {
                microsoftExcelUtilsMockedStatic
                    .when(() -> MicrosoftExcelUtils.getMapOfValuesForRow(inputParameters, headerRow, row))
                    .thenReturn(mapOf(row));
            }

            return MicrosoftExcelNewRowTriggerV2.poll(
                inputParameters, null, closureParameters, mockedTriggerContext);
        }
    }

    private static Map<String, Object> mapOf(List<Object> row) {
        return Map.of("row", row);
    }

    @SafeVarargs
    private static List<String> hashesOf(List<Object>... rows) {
        return Arrays.stream(rows)
            .map(MicrosoftExcelRowDiffUtils::getRowHash)
            .toList();
    }
}
