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
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.INCLUDE_ITEMS_FROM_ALL_DRIVES_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.IS_THE_FIRST_ROW_HEADER_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.ROW_NUMBER;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.ROW_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.UPDATE_ROW;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.VALUES;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.createRange;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.getMapOfValuesForRow;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class GoogleSheetsUpdateRowAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(UPDATE_ROW)
        .title("Update row")
        .description("Overwrite values in an existing row")
        .properties(
            SPREADSHEET_ID_PROPERTY,
            INCLUDE_ITEMS_FROM_ALL_DRIVES_PROPERTY,
            SHEET_NAME_PROPERTY,
            integer(ROW_NUMBER)
                .label("Row number")
                .description("The row number to update")
                .required(true),
            IS_THE_FIRST_ROW_HEADER_PROPERTY,
            ROW_PROPERTY)
        .outputSchema(
            object()
                .additionalProperties(bool(), number(), string()))
        .perform(GoogleSheetsUpdateRowAction::perform);

    private GoogleSheetsUpdateRowAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

        Sheets sheets = GoogleServices.getSheets(connectionParameters);

        String range = createRange(
            inputParameters.getRequiredString(SHEET_NAME), inputParameters.getRequiredInteger(ROW_NUMBER));

        List<Object> row;

        Object rowMap = inputParameters.getRequired(VALUES);
        Class<?> valuesClass = rowMap.getClass();

        if (valuesClass.equals(LinkedHashMap.class)) {
            row = new ArrayList<>(((LinkedHashMap<String, Object>) rowMap).values());
        } else {
            row = inputParameters.getRequiredList(VALUES, Object.class);
        }

        ValueRange valueRange = new ValueRange()
            .setValues(List.of(row))
            .setMajorDimension("ROWS");

        sheets
            .spreadsheets()
            .values()
            .update(inputParameters.getRequiredString(SPREADSHEET_ID), range, valueRange)
            .setValueInputOption("USER_ENTERED")
            .execute();

        return getMapOfValuesForRow(inputParameters, sheets, row);
    }
}
