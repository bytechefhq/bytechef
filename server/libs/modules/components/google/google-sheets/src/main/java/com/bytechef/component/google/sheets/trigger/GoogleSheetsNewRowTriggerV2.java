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

package com.bytechef.component.google.sheets.trigger;

import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.definition.TriggerContext.Data.Scope.WORKFLOW;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.APPLICATION_VND_GOOGLE_APPS_SPREADSHEET;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.IS_THE_FIRST_ROW_HEADER_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.getMapOfValuesForRow;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.OptionsFunction;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.google.sheets.util.GoogleSheetsUtils;
import com.bytechef.google.commons.GoogleServices;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.sheets.v4.Sheets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Anshul Goel
 */
public class GoogleSheetsNewRowTriggerV2 {

    private static final String KNOWN_ROW_IDS = "knownRowIds";

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newRow")
        .title("New Row")
        .description(
            "Triggers when a new row is added. Tracks rows by 1-based sheet row index so editing an existing row " +
                "does not fire the trigger again.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(SPREADSHEET_ID)
                .label("Spreadsheet")
                .description("The spreadsheet to apply the updates to.")
                .options(GoogleUtils.getFileOptionsByMimeTypeForTriggers(APPLICATION_VND_GOOGLE_APPS_SPREADSHEET, true))
                .required(true),
            IS_THE_FIRST_ROW_HEADER_PROPERTY,
            string(SHEET_NAME)
                .label("Sheet")
                .description("The name of the sheet")
                .options((OptionsFunction<String>) GoogleSheetsUtils::getSheetNameOptions)
                .optionsLookupDependsOn(SPREADSHEET_ID)
                .required(true))
        .output()
        .webhookEnable(GoogleSheetsNewRowTriggerV2::webhookEnable)
        .webhookDisable(GoogleSheetsNewRowTriggerV2::webhookDisable)
        .webhookRequest(GoogleSheetsNewRowTriggerV2::webhookRequest)
        .help("", "https://docs.bytechef.io/reference/components/google-sheets_v2#new-row");

    private GoogleSheetsNewRowTriggerV2() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        return GoogleSheetsNewRowTrigger.webhookEnable(
            inputParameters, connectionParameters, webhookUrl, workflowExecutionId, context);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        GoogleSheetsNewRowTrigger.webhookDisable(
            inputParameters, connectionParameters, outputParameters, workflowExecutionId, context);
    }

    protected static List<Map<String, Object>> webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers,
        HttpParameters parameters, WebhookBody body, WebhookMethod method, Parameters output,
        TriggerContext context) {

        Sheets sheets = GoogleServices.getSheets(connectionParameters);

        List<List<Object>> values = GoogleSheetsUtils.getSpreadsheetValues(
            sheets, inputParameters.getRequiredString(SPREADSHEET_ID), inputParameters.getRequiredString(SHEET_NAME));

        if (values == null) {
            return Collections.emptyList();
        }

        Optional<Object> knownRowIdsOptional = context.data(data -> data.fetch(WORKFLOW, KNOWN_ROW_IDS));

        Set<Integer> knownRowIds = knownRowIdsOptional
            .map(GoogleSheetsNewRowTriggerV2::toRowIdSet)
            .orElseGet(Set::of);

        List<Integer> currentRowIds = new ArrayList<>();
        List<Map<String, Object>> newRows = new ArrayList<>();

        for (int i = 0; i < values.size(); i++) {
            int rowId = i + 1;

            currentRowIds.add(rowId);

            if (!knownRowIds.contains(rowId)) {
                newRows.add(getMapOfValuesForRow(inputParameters, sheets, values.get(i)));
            }
        }

        context.data(data -> data.put(WORKFLOW, KNOWN_ROW_IDS, currentRowIds));

        return newRows;
    }

    private static Set<Integer> toRowIdSet(Object storedRowIds) {
        Set<Integer> rowIds = new HashSet<>();

        if (storedRowIds instanceof List<?> list) {
            for (Object rowId : list) {
                if (rowId instanceof Number number) {
                    rowIds.add(number.intValue());
                }
            }
        }

        return rowIds;
    }
}
