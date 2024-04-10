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

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.INCLUDE_ITEMS_FROM_ALL_DRIVES_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.INSERT_ROW;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.IS_THE_FIRST_ROW_HEADER_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.ROW_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.VALUE_INPUT_OPTION;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.createRange;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.getMapOfValuesForRow;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.getRowValues;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class GoogleSheetsInsertRowAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(INSERT_ROW)
        .title("Insert row")
        .description("Append a row of values to an existing sheet")
        .properties(
            SPREADSHEET_ID_PROPERTY,
            INCLUDE_ITEMS_FROM_ALL_DRIVES_PROPERTY,
            SHEET_NAME_PROPERTY,
            string(VALUE_INPUT_OPTION)
                .label("Value input option")
                .description("How the input data should be interpreted.")
                .options(
                    option("Raw", "RAW",
                        "The values the user has entered will not be parsed and will be stored as-is."),
                    option("User entered", "USER_ENTERED",
                        "The values will be parsed as if the user typed them into the UI. Numbers will stay as numbers, but strings may be converted to numbers, dates, etc. following the same rules that are applied when entering text into a cell via the Google Sheets UI."))
                .required(true),
            IS_THE_FIRST_ROW_HEADER_PROPERTY,
            ROW_PROPERTY)
        .outputSchema(
            object()
                .additionalProperties(bool(), number(), string()))
        .perform(GoogleSheetsInsertRowAction::perform);

    private GoogleSheetsInsertRowAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

        Sheets sheets = GoogleServices.getSheets(connectionParameters);

        List<Object> row = getRowValues(inputParameters);

        ValueRange valueRange = new ValueRange()
            .setValues(List.of(row))
            .setMajorDimension("ROWS");

        String spreadsheetId = inputParameters.getRequiredString(SPREADSHEET_ID);

        String range = createRange(inputParameters.getRequiredString(SHEET_NAME), null);

        sheets
            .spreadsheets()
            .values()
            .append(spreadsheetId, range, valueRange)
            .setValueInputOption(inputParameters.getRequiredString(VALUE_INPUT_OPTION))
            .execute();

        return getMapOfValuesForRow(inputParameters, sheets, row);
    }

}
