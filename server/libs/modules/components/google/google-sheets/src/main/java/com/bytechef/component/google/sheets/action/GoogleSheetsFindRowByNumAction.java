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
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.FIND_ROW_BY_NUM;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.FIND_ROW_BY_NUM_DESCRIPTION;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.FIND_ROW_BY_NUM_TITLE;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.IS_THE_FIRST_ROW_HEADER_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.ROW_NUMBER;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID_PROPERTY;
import static com.bytechef.component.google.sheets.util.GoogleSheetsRowUtils.getRowValues;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.getMapOfValuesForRow;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.sheets.v4.Sheets;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class GoogleSheetsFindRowByNumAction {

    @SuppressFBWarnings("MS")
    public static final Property[] PROPERTIES = {
        SPREADSHEET_ID_PROPERTY,
        SHEET_NAME_PROPERTY,
        IS_THE_FIRST_ROW_HEADER_PROPERTY,
        integer(ROW_NUMBER)
            .label("Row Number")
            .required(true)
            .description("The row number to get from the sheet.")
    };

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(FIND_ROW_BY_NUM)
        .title(FIND_ROW_BY_NUM_TITLE)
        .description(FIND_ROW_BY_NUM_DESCRIPTION)
        .properties(PROPERTIES)
        .output()
        .perform(GoogleSheetsFindRowByNumAction::perform);

    private GoogleSheetsFindRowByNumAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) throws Exception {

        Sheets sheets = GoogleServices.getSheets(connectionParameters);

        List<Object> row = getRowValues(
            sheets, inputParameters.getRequiredString(SPREADSHEET_ID), inputParameters.getRequiredString(SHEET_NAME),
            inputParameters.getRequiredInteger(ROW_NUMBER));

        return getMapOfValuesForRow(inputParameters, sheets, row);
    }

}
