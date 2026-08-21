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
 * @author Marija Horvat
 */
public class MicrosoftExcelNewRowTrigger {

    private static final String KNOWN_ROW_HASHES = "knownRowHashes";

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newRow")
        .title("New Row")
        .description("Triggers when a new row is added.")
        .help("", "https://docs.bytechef.io/reference/components/microsoft-excel_v1#new-row")
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
        .poll(MicrosoftExcelNewRowTrigger::poll)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftExcelNewRowTrigger() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext context) {

        List<List<Object>> rows = MicrosoftExcelUtils.getUsedRangeValues(inputParameters, context);

        Set<Integer> knownRowHashes = new HashSet<>(
            closureParameters.getList(KNOWN_ROW_HASHES, Integer.class, List.of()));

        List<Integer> currentRowHashes = new ArrayList<>();
        List<Map<String, Object>> newRows = new ArrayList<>();

        // Rows are matched by content hash rather than position so that a row inserted in the
        // middle of the worksheet is still detected as new, instead of being confused with
        // whichever row happens to shift into the last position (see #4362).
        for (List<Object> row : rows) {
            if (row.isEmpty()) {
                continue;
            }

            int rowHash = row.hashCode();

            currentRowHashes.add(rowHash);

            if (!knownRowHashes.contains(rowHash)) {
                newRows.add(MicrosoftExcelUtils.getMapOfValuesForRow(inputParameters, context, row));
            }
        }

        return new PollOutput(newRows, Map.of(KNOWN_ROW_HASHES, currentRowHashes), false);
    }
}
