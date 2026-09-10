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

package com.bytechef.component.google.sheets.util;

import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.TriggerContext.Data.Scope.WORKFLOW;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.APPLICATION_VND_GOOGLE_APPS_SPREADSHEET;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.IS_THE_FIRST_ROW_HEADER;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.IS_THE_FIRST_ROW_HEADER_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.getMapOfValuesForRow;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.OptionsFunction;
import com.bytechef.google.commons.GoogleServices;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.sheets.v4.Sheets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Shared logic for the row-hash-tracking Google Sheets triggers (new row, modified row) that poll a sheet and report
 * rows whose position and hash changed between polls.
 *
 * @author Marko Kriskovic
 */
public class GoogleSheetsRowTriggerUtils {

    private static final String KNOWN_ROW_HASHES = "knownRowHashes";

    private GoogleSheetsRowTriggerUtils() {
    }

    public static Property[] getSpreadsheetAndSheetProperties() {
        return new Property[] {
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
                .required(true)
        };
    }

    /**
     * Polls the configured sheet, diffs the current row hashes against the ones known from the previous poll using
     * {@code changedRowIndexesFunction}, and returns the rows at the resulting indexes.
     */
    public static List<Map<String, Object>> getChangedRows(
        Parameters inputParameters, Parameters connectionParameters, TriggerContext context,
        BiFunction<List<String>, List<String>, List<Integer>> changedRowIndexesFunction) {

        Sheets sheets = GoogleServices.getSheets(connectionParameters);

        List<List<Object>> values = GoogleSheetsUtils.getSpreadsheetValues(
            sheets, inputParameters.getRequiredString(SPREADSHEET_ID), inputParameters.getRequiredString(SHEET_NAME));

        if (values == null) {
            return Collections.emptyList();
        }

        int firstDataRowIndex = inputParameters.getRequiredBoolean(IS_THE_FIRST_ROW_HEADER) ? 1 : 0;

        List<List<Object>> dataRows = new ArrayList<>();
        List<String> currentRowHashes = new ArrayList<>();

        for (int index = firstDataRowIndex; index < values.size(); index++) {
            List<Object> row = values.get(index);

            if (GoogleSheetsRowDiffUtils.isBlankRow(row)) {
                continue;
            }

            dataRows.add(row);
            currentRowHashes.add(GoogleSheetsRowDiffUtils.getRowHash(row));
        }

        Optional<Object> knownRowHashesOptional = context.data(data -> data.fetch(WORKFLOW, KNOWN_ROW_HASHES));

        List<String> knownRowHashes = knownRowHashesOptional
            .map(GoogleSheetsRowTriggerUtils::toRowHashList)
            .orElseGet(List::of);

        List<Map<String, Object>> changedRows = new ArrayList<>();

        for (int index : changedRowIndexesFunction.apply(knownRowHashes, currentRowHashes)) {
            changedRows.add(getMapOfValuesForRow(inputParameters, sheets, dataRows.get(index)));
        }

        context.data(data -> data.put(WORKFLOW, KNOWN_ROW_HASHES, currentRowHashes));

        return changedRows;
    }

    private static List<String> toRowHashList(Object storedRowHashes) {
        List<String> rowHashes = new ArrayList<>();

        if (storedRowHashes instanceof List<?> list) {
            for (Object rowHash : list) {
                if (rowHash != null) {
                    rowHashes.add(String.valueOf(rowHash));
                }
            }
        }

        return rowHashes;
    }
}
