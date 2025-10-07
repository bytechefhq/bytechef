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

package com.bytechef.component.google.sheets.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.IS_THE_FIRST_ROW_HEADER;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.IS_THE_FIRST_ROW_HEADER_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.ROW;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.VALUE_INPUT_OPTION;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.VALUE_INPUT_PROPERTY;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.appendValues;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.createRange;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.getMapOfValuesForRow;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.getRowValues;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.sheets.util.GoogleSheetsUtils;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class GoogleSheetsInsertRowAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("insertRow")
        .title("Insert Row")
        .description("Append a row of values to an existing sheet.")
        .properties(
            SPREADSHEET_ID_PROPERTY,
            SHEET_NAME_PROPERTY,
            VALUE_INPUT_PROPERTY,
            IS_THE_FIRST_ROW_HEADER_PROPERTY,
            dynamicProperties(ROW)
                .propertiesLookupDependsOn(SPREADSHEET_ID, SHEET_NAME, IS_THE_FIRST_ROW_HEADER)
                .properties(GoogleSheetsUtils.createPropertiesForNewRows(true))
                .required(true))
        .output()
        .perform(GoogleSheetsInsertRowAction::perform);

    private GoogleSheetsInsertRowAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        Sheets sheets = GoogleServices.getSheets(connectionParameters);
        List<Object> row = getRowValues(inputParameters);
        ValueRange valueRange = new ValueRange()
            .setValues(List.of(row))
            .setMajorDimension("ROWS");
        String spreadsheetId = inputParameters.getRequiredString(SPREADSHEET_ID);
        String range = createRange(inputParameters.getRequiredString(SHEET_NAME), null);

        appendValues(sheets, spreadsheetId, range, valueRange, inputParameters.getRequiredString(VALUE_INPUT_OPTION));

        return getMapOfValuesForRow(inputParameters, sheets, row);
    }
}
