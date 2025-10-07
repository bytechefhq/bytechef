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
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.IS_THE_FIRST_ROW_HEADER;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.IS_THE_FIRST_ROW_HEADER_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.ROW;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.ROW_NUMBER;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.UPDATE_WHOLE_ROW;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.createRange;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.getMapOfValuesForRow;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.getUpdatedRowValues;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.sheets.util.GoogleSheetsUtils;
import com.bytechef.google.commons.GoogleServices;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class GoogleSheetsUpdateRowAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateRow")
        .title("Update Row")
        .description("Overwrite values in an existing row.")
        .properties(
            SPREADSHEET_ID_PROPERTY,
            SHEET_NAME_PROPERTY,
            integer(ROW_NUMBER)
                .label("Row Number")
                .description("The row number to update.")
                .required(true),
            IS_THE_FIRST_ROW_HEADER_PROPERTY,
            bool(UPDATE_WHOLE_ROW)
                .label("Update Whole Row")
                .description("Whether to update the whole row or just specific columns.")
                .defaultValue(true)
                .required(true),
            dynamicProperties(ROW)
                .propertiesLookupDependsOn(SPREADSHEET_ID, SHEET_NAME, IS_THE_FIRST_ROW_HEADER, UPDATE_WHOLE_ROW)
                .properties(GoogleSheetsUtils::createPropertiesToUpdateRow)
                .required(true))
        .output()
        .perform(GoogleSheetsUpdateRowAction::perform);

    private GoogleSheetsUpdateRowAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        Sheets sheets = GoogleServices.getSheets(connectionParameters);
        String range = createRange(
            inputParameters.getRequiredString(SHEET_NAME), inputParameters.getRequiredInteger(ROW_NUMBER));
        List<Object> row = getUpdatedRowValues(inputParameters, connectionParameters);

        ValueRange valueRange = new ValueRange()
            .setValues(List.of(row))
            .setMajorDimension("ROWS");

        try {
            sheets.spreadsheets()
                .values()
                .update(inputParameters.getRequiredString(SPREADSHEET_ID), range, valueRange)
                .setValueInputOption("USER_ENTERED")
                .execute();
        } catch (IOException e) {
            throw GoogleUtils.translateGoogleIOException(e);
        }

        return getMapOfValuesForRow(inputParameters, sheets, row);
    }
}
