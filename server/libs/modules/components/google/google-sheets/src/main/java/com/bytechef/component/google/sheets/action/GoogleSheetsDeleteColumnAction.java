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
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.LABEL;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_ID_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID_PROPERTY;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.labelToColum;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.Request;
import java.util.List;

/**
 * @author Marija Horvat
 */
public class GoogleSheetsDeleteColumnAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteColumn")
        .title("Delete Column")
        .description("Delete column on an existing sheet.")
        .properties(
            SPREADSHEET_ID_PROPERTY,
            SHEET_ID_PROPERTY,
            string(LABEL)
                .label("Column Label")
                .description("The label of the column to be deleted.")
                .exampleValue("A")
                .required(true))
        .perform(GoogleSheetsDeleteColumnAction::perform);

    private GoogleSheetsDeleteColumnAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws Exception {

        String label = inputParameters.getRequiredString(LABEL);

        Integer columnNumber = labelToColum(label);

        DimensionRange dimensionRange = new DimensionRange()
            .setSheetId(inputParameters.getRequiredInteger(SHEET_ID))
            .setDimension("COLUMNS")
            .setStartIndex(columnNumber - 1)
            .setEndIndex(columnNumber);

        Request request = new Request()
            .setDeleteDimension(
                new DeleteDimensionRequest()
                    .setRange(dimensionRange));

        BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest()
            .setRequests(List.of(request));

        Sheets sheets = GoogleServices.getSheets(connectionParameters);

        sheets.spreadsheets()
            .batchUpdate(inputParameters.getRequiredString(SPREADSHEET_ID), batchUpdateSpreadsheetRequest)
            .execute();

        return null;
    }
}
