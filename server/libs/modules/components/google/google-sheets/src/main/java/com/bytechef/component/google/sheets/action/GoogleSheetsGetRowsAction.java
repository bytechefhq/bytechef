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
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.IS_THE_FIRST_ROW_HEADER;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.IS_THE_FIRST_ROW_HEADER_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID_PROPERTY;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.getMapOfValuesForRow;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.sheets.constant.GoogleSheetsConstants;
import com.bytechef.google.commons.GoogleServices;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.sheets.v4.Sheets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class GoogleSheetsGetRowsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getRows")
        .title("Get Rows")
        .description("Get all rows from a Google Sheet.")
        .properties(
            SPREADSHEET_ID_PROPERTY,
            SHEET_NAME_PROPERTY,
            IS_THE_FIRST_ROW_HEADER_PROPERTY)
        .output()
        .perform(GoogleSheetsGetRowsAction::perform);

    private GoogleSheetsGetRowsAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Sheets sheets = GoogleServices.getSheets(connectionParameters);

        List<List<Object>> values;

        try {
            values = sheets
                .spreadsheets()
                .values()
                .get(inputParameters.getRequiredString(GoogleSheetsConstants.SPREADSHEET_ID),
                    inputParameters.getRequiredString(GoogleSheetsConstants.SHEET_NAME))
                .execute()
                .getValues();
        } catch (IOException e) {
            throw GoogleUtils.translateGoogleIOException(e);
        }

        if (inputParameters.getRequiredBoolean(IS_THE_FIRST_ROW_HEADER)) {
            values = values.subList(1, values.size());
        }

        List<Map<String, Object>> rows = new ArrayList<>();

        for (List<Object> row : values) {
            rows.add(getMapOfValuesForRow(inputParameters, sheets, row));
        }

        return rows;
    }
}
