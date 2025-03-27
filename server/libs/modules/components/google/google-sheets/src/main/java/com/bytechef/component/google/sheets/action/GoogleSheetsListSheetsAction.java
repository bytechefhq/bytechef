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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.LIST_SHEETS;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.LIST_SHEETS_DESCRIPTION;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.LIST_SHEETS_TITLE;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_RECORD_OUTPUT_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID_PROPERTY;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.SheetRecord;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.Property.ArrayProperty;
import com.bytechef.definition.BaseOutputDefinition.OutputSchema;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * @author Nikolina Spehar
 */
public class GoogleSheetsListSheetsAction {

    @SuppressFBWarnings("MS")
    public static final Property[] PROPERTIES = {
        SPREADSHEET_ID_PROPERTY
    };

    public static final OutputSchema<ArrayProperty> OUTPUT_SCHEMA = outputSchema(
        array()
            .description("List of sheets in the spreadsheet.")
            .items(SHEET_RECORD_OUTPUT_PROPERTY));

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(LIST_SHEETS)
        .title(LIST_SHEETS_TITLE)
        .description(LIST_SHEETS_DESCRIPTION)
        .properties(PROPERTIES)
        .output(OUTPUT_SCHEMA)
        .perform(GoogleSheetsListSheetsAction::perform);

    private GoogleSheetsListSheetsAction() {
    }

    public static List<SheetRecord> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) throws Exception {

        Sheets sheets = GoogleServices.getSheets(connectionParameters);

        return getSheetsListResponse(sheets, inputParameters.getRequiredString(SPREADSHEET_ID));
    }

    private static List<SheetRecord> getSheetsListResponse(Sheets sheets, String spreadsheetId) throws IOException {
        Collection<Sheet> spreadsheetData = sheets.spreadsheets()
            .get(spreadsheetId)
            .execute()
            .getSheets();

        return spreadsheetData.stream()
            .map(sheet -> {
                SheetProperties sheetProperties = sheet.getProperties();

                return new SheetRecord(spreadsheetId, sheetProperties.getSheetId(), sheetProperties.getTitle(), null);
            })
            .toList();
    }
}
