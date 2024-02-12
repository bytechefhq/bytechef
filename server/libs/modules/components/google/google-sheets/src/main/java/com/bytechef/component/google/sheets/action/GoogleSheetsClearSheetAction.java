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
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.CLEAR_SHEET;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.INCLUDE_ITEMS_FROM_ALL_DRIVES_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.IS_THE_FIRST_ROW_HEADER;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.IS_THE_FIRST_ROW_HEADER_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_ID_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.UpdateCellsRequest;
import java.io.IOException;
import java.util.List;

/**
 * @author Monika Domiter
 */
public class GoogleSheetsClearSheetAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CLEAR_SHEET)
        .title("Clear sheet")
        .description("Clear a sheet of all values while preserving formats.")
        .properties(
            SPREADSHEET_ID_PROPERTY,
            INCLUDE_ITEMS_FROM_ALL_DRIVES_PROPERTY,
            SHEET_ID_PROPERTY,
            IS_THE_FIRST_ROW_HEADER_PROPERTY)
        .perform(GoogleSheetsClearSheetAction::perform);

    private GoogleSheetsClearSheetAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

        Sheets sheets = GoogleServices.getSheets(connectionParameters);

        Integer startRowIndex = inputParameters.getRequiredBoolean(IS_THE_FIRST_ROW_HEADER) ? 1 : 0;

        UpdateCellsRequest updateCellsRequest = new UpdateCellsRequest()
            .setFields("userEnteredValue")
            .setRange(
                new GridRange()
                    .setSheetId(inputParameters.getRequiredInteger(SHEET_ID))
                    .setStartRowIndex(startRowIndex));

        Request request = new Request().setUpdateCells(updateCellsRequest);

        BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest()
            .setRequests(List.of(request));

        sheets
            .spreadsheets()
            .batchUpdate(
                inputParameters.getRequiredString(SPREADSHEET_ID),
                batchUpdateSpreadsheetRequest)
            .execute();

        return null;
    }
}
