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
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.COLUMN_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.CREATE_COLUMN;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.CREATE_COLUMN_DESCRIPTION;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.CREATE_COLUMN_TITLE;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_RECORD_OUTPUT_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID_PROPERTY;
import static com.bytechef.component.google.sheets.util.GoogleSheetsColumnConverterUtils.columnToLabel;
import static com.bytechef.component.google.sheets.util.GoogleSheetsRowUtils.getRowValues;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.SheetRecord;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.appendValues;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.Property.ObjectProperty;
import com.bytechef.definition.BaseOutputDefinition.OutputSchema;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * @author Monika Kušter
 */
public class GoogleSheetsCreateColumnAction {

    @SuppressFBWarnings("MS")
    public static final Property[] PROPERTIES = {
        SPREADSHEET_ID_PROPERTY,
        SHEET_NAME_PROPERTY,
        string(COLUMN_NAME)
            .label("Column Name")
            .description("Name of the new column.")
            .required(true)
    };

    public static final OutputSchema<ObjectProperty> OUTPUT_SCHEMA = outputSchema(SHEET_RECORD_OUTPUT_PROPERTY);

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_COLUMN)
        .title(CREATE_COLUMN_TITLE)
        .description(CREATE_COLUMN_DESCRIPTION)
        .properties(PROPERTIES)
        .output(OUTPUT_SCHEMA)
        .perform(GoogleSheetsCreateColumnAction::perform);

    private GoogleSheetsCreateColumnAction() {
    }

    public static SheetRecord perform(Parameters inputParameters, Parameters connectionParameters, Context context)
        throws Exception {

        Sheets sheets = GoogleServices.getSheets(connectionParameters);
        String spreadSheetId = inputParameters.getRequiredString(SPREADSHEET_ID);
        String sheetName = inputParameters.getRequiredString(SHEET_NAME);

        List<Object> firstRow = getRowValues(sheets, spreadSheetId, sheetName, 1);

        String range = sheetName + "!" + columnToLabel(firstRow.size() + 1) + "1";
        ValueRange valueRange = new ValueRange()
            .setValues(List.of(List.of(inputParameters.getRequiredString(COLUMN_NAME))))
            .setMajorDimension("COLUMNS");

        appendValues(sheets, spreadSheetId, range, valueRange, "USER_ENTERED");

        List<Object> headers = getRowValues(sheets, spreadSheetId, sheetName, 1);

        return new SheetRecord(spreadSheetId, null, sheetName, headers);
    }
}
