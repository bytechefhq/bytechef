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
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_ID_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.DeleteSheetRequest;
import com.google.api.services.sheets.v4.model.Request;
import java.util.List;

/**
 * @author Nikolina Spehar
 */
public class GoogleSheetsDeleteSheetAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteSheet")
        .title("Delete Sheet")
        .description("Delete a specified sheet from a spreadsheet.")
        .properties(
            SPREADSHEET_ID_PROPERTY,
            SHEET_ID_PROPERTY)
        .perform(GoogleSheetsDeleteSheetAction::perform);

    private GoogleSheetsDeleteSheetAction() {

    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws Exception {

        Sheets sheets = GoogleServices.getSheets(connectionParameters);

        Request request = new Request()
            .setDeleteSheet(
                new DeleteSheetRequest()
                    .setSheetId(inputParameters.getInteger(SHEET_ID)));

        BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest()
            .setRequests(List.of(request));

        sheets.spreadsheets()
            .batchUpdate(inputParameters.getRequiredString(SPREADSHEET_ID), batchUpdateSpreadsheetRequest)
            .execute();

        return null;
    }
}
