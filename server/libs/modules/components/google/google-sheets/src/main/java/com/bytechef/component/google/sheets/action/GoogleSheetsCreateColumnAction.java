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

package com.bytechef.component.google.sheets.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.HEADERS;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.INCLUDE_ITEMS_FROM_ALL_DRIVES_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID_PROPERTY;
import static com.bytechef.component.google.sheets.util.GoogleSheetsRowUtils.getRowValues;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.appendValues;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.columnToLabel;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class GoogleSheetsCreateColumnAction {

    protected static final String COLUMN_NAME = "columnName";
    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createColumn")
        .title("Create column")
        .description("Append a new column to the end of the sheet.")
        .properties(
            SPREADSHEET_ID_PROPERTY,
            INCLUDE_ITEMS_FROM_ALL_DRIVES_PROPERTY,
            SHEET_NAME_PROPERTY,
            string(COLUMN_NAME)
                .label("Column name")
                .description("Name of the new column.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(SPREADSHEET_ID),
                        string(SHEET_NAME),
                        array(HEADERS)
                            .items(bool(), number(), string()))))
        .perform(GoogleSheetsCreateColumnAction::perform);

    private GoogleSheetsCreateColumnAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws Exception {

        Sheets sheets = GoogleServices.getSheets(connectionParameters);
        String spreadSheetId = inputParameters.getRequiredString(SPREADSHEET_ID);
        String sheetName = inputParameters.getRequiredString(SHEET_NAME);

        List<Object> firstRow = getRowValues(sheets, spreadSheetId, sheetName, 1);

        String range = sheetName + "!" + columnToLabel(firstRow.size() + 1) + "1";
        ValueRange valueRange = new ValueRange()
            .setValues(List.of(List.of(inputParameters.getRequiredString(COLUMN_NAME))))
            .setMajorDimension("COLUMNS");

        appendValues(sheets, spreadSheetId, range, valueRange, "USER_ENTERED");

        return getResponseMap(spreadSheetId, sheetName, sheets);
    }

    private static Map<String, Object> getResponseMap(String spreadSheetId, String sheetName, Sheets sheets)
        throws IOException {
        Map<String, Object> responseMap = new HashMap<>();

        responseMap.put(SPREADSHEET_ID, spreadSheetId);
        responseMap.put(SHEET_NAME, sheetName);
        responseMap.put(HEADERS, getRowValues(sheets, spreadSheetId, sheetName, 1));

        return responseMap;
    }
}
