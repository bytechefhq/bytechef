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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.HEADERS;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_RECORD_OUTPUT_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID_PROPERTY;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.SheetRecord;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.appendValues;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.createRange;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AddSheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.io.IOException;
import java.util.List;

/**
 * @author Monika Kušter
 */
public class GoogleSheetsCreateSheetAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createSheet")
        .title("Create Sheet")
        .description("Create a blank sheet with title. Optionally, provide headers.")
        .properties(
            SPREADSHEET_ID_PROPERTY,
            string(SHEET_NAME)
                .label("Sheet Name")
                .description("The name of the new sheet.")
                .required(true),
            array(HEADERS)
                .label("Headers")
                .description("The headers of the new sheet.")
                .items(string())
                .required(false))
        .output(outputSchema(SHEET_RECORD_OUTPUT_PROPERTY))
        .perform(GoogleSheetsCreateSheetAction::perform);

    private GoogleSheetsCreateSheetAction() {
    }

    public static SheetRecord perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
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

        BatchUpdateSpreadsheetResponse batchUpdateSpreadsheetResponse;
        try {
            batchUpdateSpreadsheetResponse = sheets
                .spreadsheets()
                .batchUpdate(spreadsheetId, request)
                .execute();
        } catch (IOException e) {
            throw GoogleUtils.translateGoogleIOException(e);
        }

        List<Object> headers = inputParameters.getList(HEADERS, Object.class);

        if (headers != null) {
            ValueRange valueRange = new ValueRange()
                .setValues(List.of(headers))
                .setMajorDimension("ROWS");

            appendValues(sheets, spreadsheetId, createRange(sheetName, 1), valueRange, "USER_ENTERED");
        }

        SheetProperties sheetProperties = batchUpdateSpreadsheetResponse.getReplies()
            .getFirst()
            .getAddSheet()
            .getProperties();

        return new SheetRecord(spreadsheetId, sheetProperties.getSheetId(), sheetProperties.getTitle(), headers);
    }
}
