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

import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.IS_THE_FIRST_ROW_HEADER_PROPERTY;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKBOOK_ID;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKSHEET_NAME;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.OptionsFunction;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.microsoft.excel.util.MicrosoftExcelUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Anshul Goel
 */
public class MicrosoftExcelNewRowTriggerV2 {

    private static final String KNOWN_ROW_IDS = "knownRowIds";

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newRow")
        .title("New Row")
        .description(
            "Triggers when a new row is added. Tracks rows by 1-based worksheet row index so editing an existing row " +
                "does not fire the trigger again.")
        .help("", "https://docs.bytechef.io/reference/components/microsoft-excel_v2#new-row")
        .type(TriggerType.POLLING)
        .properties(
            string(WORKBOOK_ID)
                .label("Workbook ID")
                .description("The ID of the workbook.")
                .options((OptionsFunction<String>) MicrosoftExcelUtils::getWorkbookIdOptions)
                .required(true),
            string(WORKSHEET_NAME)
                .label("Worksheet")
                .description("The name of the worksheet.")
                .options((OptionsFunction<String>) MicrosoftExcelUtils::getWorksheetNameOptions)
                .optionsLookupDependsOn(WORKBOOK_ID)
                .required(true),
            IS_THE_FIRST_ROW_HEADER_PROPERTY)
        .output()
        .poll(MicrosoftExcelNewRowTriggerV2::poll)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftExcelNewRowTriggerV2() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext context) {

        List<List<Object>> rows = MicrosoftExcelUtils.getUsedRangeValues(inputParameters, context);

        Set<Integer> knownRowIds = new HashSet<>(
            closureParameters.getList(KNOWN_ROW_IDS, Integer.class, List.of()));

        List<Integer> currentRowIds = new ArrayList<>();
        List<Map<String, Object>> newRows = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            List<Object> row = rows.get(i);

            if (row.isEmpty()) {
                continue;
            }

            int rowId = i + 1;

            currentRowIds.add(rowId);

            if (!knownRowIds.contains(rowId)) {
                newRows.add(MicrosoftExcelUtils.getMapOfValuesForRow(inputParameters, context, row));
            }
        }

        return new PollOutput(newRows, Map.of(KNOWN_ROW_IDS, currentRowIds), false);
    }
}
