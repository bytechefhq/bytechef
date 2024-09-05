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
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.outputSchema;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.HEADERS;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID_PROPERTY;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.appendValues;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.createRange;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AddSheetRequest;
import com.google.api.services.sheets.v4.model.AddSheetResponse;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Response;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class GoogleSheetsCreateSheetAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createSheet")
        .title("Create sheet")
        .description("Create a blank sheet with title. Optionally, provide headers.")
        .properties(
            SPREADSHEET_ID_PROPERTY,
            string(SHEET_NAME)
                .label("Sheet name")
                .description("The name of the new sheet.")
                .required(true),
            array(HEADERS)
                .label("Headers")
                .description("The headers of the new sheet.")
                .items(bool(), number(), string())
                .required(false))
        .output(
            outputSchema(
                object()
                    .additionalProperties(string(), integer(), array().items(bool(), number(), string()))))
        .perform(GoogleSheetsCreateSheetAction::perform);

    private GoogleSheetsCreateSheetAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws Exception {

        Sheets sheets = GoogleServices.getSheets(connectionParameters);

        String sheetName = inputParameters.getRequiredString(SHEET_NAME);

        BatchUpdateSpreadsheetRequest request = new BatchUpdateSpreadsheetRequest()
            .setRequests(
                List.of(
                    new Request()
                        .setAddSheet(
                            new AddSheetRequest()
                                .setProperties(
                                    new SheetProperties()
                                        .setTitle(sheetName)))));

        String spreadsheetId = inputParameters.getRequiredString(SPREADSHEET_ID);

        BatchUpdateSpreadsheetResponse batchUpdateSpreadsheetResponse = sheets
            .spreadsheets()
            .batchUpdate(spreadsheetId, request)
            .execute();

        List<Object> headers = inputParameters.getList(HEADERS, Object.class);

        if (headers != null) {
            ValueRange valueRange = new ValueRange()
                .setValues(List.of(headers))
                .setMajorDimension("ROWS");

            appendValues(sheets, spreadsheetId, createRange(sheetName, 1), valueRange, "USER_ENTERED");
        }

        return getNewSheetResponse(spreadsheetId, batchUpdateSpreadsheetResponse, headers);
    }

    private static Map<String, Object> getNewSheetResponse(
        String spreadsheetId, BatchUpdateSpreadsheetResponse batchUpdateSpreadsheetResponse, List<Object> headers) {

        List<Response> responses = batchUpdateSpreadsheetResponse.getReplies();
        Response response = responses.getFirst();
        AddSheetResponse addSheet = response.getAddSheet();
        SheetProperties sheetProperties = addSheet.getProperties();

        Map<String, Object> newsSheet = new HashMap<>();

        newsSheet.put(SPREADSHEET_ID, spreadsheetId);
        newsSheet.put(SHEET_ID, sheetProperties.getSheetId());
        newsSheet.put(SHEET_NAME, sheetProperties.getTitle());

        if (headers != null) {
            newsSheet.put(HEADERS, headers);
        }

        return newsSheet;
    }
}
