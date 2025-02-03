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

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Sheet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class GoogleSheetsListSheetsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listSheets")
        .title("List Sheets")
        .description("Get all sheets from the spreadsheet.")
        .properties(
            SPREADSHEET_ID_PROPERTY)
        .output()
        .perform(GoogleSheetsListSheetsAction::perform);

    private GoogleSheetsListSheetsAction() {
    }

    public static List<Map<String, Object>> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws Exception {

        Sheets sheets = GoogleServices.getSheets(connectionParameters);

        return getSheetsListResponse(sheets, inputParameters.getRequiredString(SPREADSHEET_ID));
    }

    private static List<Map<String, Object>> getSheetsListResponse(
        Sheets sheets, String spreadsheetId) throws IOException {

        List<Map<String, Object>> sheetsList = new ArrayList<>();

        Collection<Sheet> spreadsheetData = sheets.spreadsheets()
            .get(spreadsheetId)
            .execute()
            .getSheets();

        for (Sheet sheet : spreadsheetData) {
            Integer sheetId = sheet.getProperties()
                .getSheetId();
            String sheetName = sheet.getProperties()
                .getTitle();

            Map<String, Object> sheetProperties = new LinkedHashMap<>();

            sheetProperties.put(SPREADSHEET_ID, spreadsheetId);
            sheetProperties.put(SHEET_ID, sheetId);
            sheetProperties.put(SHEET_NAME, sheetName);

            sheetsList.add(sheetProperties);
        }

        return sheetsList;
    }
}
