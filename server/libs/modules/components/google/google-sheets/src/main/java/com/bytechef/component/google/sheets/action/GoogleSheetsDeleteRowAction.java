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
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.ROW_NUMBER;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_ID_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID_PROPERTY;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.deleteDimension;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Kušter
 */
public class GoogleSheetsDeleteRowAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteRow")
        .title("Delete Row")
        .description("Delete row on an existing sheet.")
        .properties(
            SPREADSHEET_ID_PROPERTY,
            SHEET_ID_PROPERTY,
            integer(ROW_NUMBER)
                .label("Row Number")
                .description("The row number to delete.")
                .required(true))
        .perform(GoogleSheetsDeleteRowAction::perform);

    private GoogleSheetsDeleteRowAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        deleteDimension(inputParameters, connectionParameters, inputParameters.getRequiredInteger(ROW_NUMBER), "ROWS");

        return null;
    }
}
