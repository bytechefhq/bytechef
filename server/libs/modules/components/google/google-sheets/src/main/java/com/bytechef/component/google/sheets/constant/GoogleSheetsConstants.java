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

package com.bytechef.component.google.sheets.constant;

import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableBooleanProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.google.sheets.util.GoogleSheetsUtils;
import com.bytechef.google.commons.GoogleUtils;

/**
 * @author Monika Kušter
 */
public class GoogleSheetsConstants {

    public static final String APPLICATION_VND_GOOGLE_APPS_SPREADSHEET = "application/vnd.google-apps.spreadsheet";
    public static final String COLUMN = "column";
    public static final String HEADERS = "headers";
    public static final String INCLUDE_ITEMS_FROM_ALL_DRIVES = "includeItemsFromAllDrives";
    public static final String IS_THE_FIRST_ROW_HEADER = "isTheFirstRowHeader";
    public static final String LABEL = "label";
    public static final String ROW_NUMBER = "rowNumber";
    public static final String ROW = "row";
    public static final String ROWS = "rows";
    public static final String SHEET_ID = "sheetId";
    public static final String SHEET_NAME = "sheetName";
    public static final String SPREADSHEET_ID = "spreadsheetId";
    public static final String UPDATE_WHOLE_ROW = "updateWholeRow";
    public static final String VALUE_INPUT_OPTION = "valueInputOption";
    public static final String VALUE = "value";
    public static final String VALUES = "values";

    public static final ModifiableBooleanProperty INCLUDE_ITEMS_FROM_ALL_DRIVES_PROPERTY =
        bool(INCLUDE_ITEMS_FROM_ALL_DRIVES)
            .label("Include Sheets from All Drives")
            .description("Whether both My Drive and shared drive sheets should be included in results.")
            .defaultValue(false)
            .required(false);

    public static final ModifiableBooleanProperty IS_THE_FIRST_ROW_HEADER_PROPERTY = bool(IS_THE_FIRST_ROW_HEADER)
        .label("Is the First Row Headers?")
        .description("If the first row is header.")
        .defaultValue(false)
        .required(true);

    public static final ModifiableStringProperty SPREADSHEET_ID_PROPERTY = string(SPREADSHEET_ID)
        .label("Spreadsheet ID")
        .description("The ID of the spreadsheet to apply the updates to.")
        .options(GoogleUtils.getFileOptionsByMimeType(APPLICATION_VND_GOOGLE_APPS_SPREADSHEET, true))
        .required(true);

    public static final ModifiableIntegerProperty SHEET_ID_PROPERTY = integer(SHEET_ID)
        .label("Sheet ID")
        .description("The ID of the sheet.")
        .options((ActionOptionsFunction<String>) GoogleSheetsUtils::getSheetIdOptions)
        .optionsLookupDependsOn(SPREADSHEET_ID)
        .required(true);

    public static final ModifiableStringProperty SHEET_NAME_PROPERTY = string(SHEET_NAME)
        .label("Sheet Name")
        .description("The name of the sheet.")
        .options((ActionOptionsFunction<String>) GoogleSheetsUtils::getSheetNameOptions)
        .optionsLookupDependsOn(SPREADSHEET_ID)
        .required(true);

    public static final ModifiableStringProperty VALUE_INPUT_PROPERTY = string(VALUE_INPUT_OPTION)
        .label("Value Input Option")
        .description("How the input data should be interpreted.")
        .options(
            option("Raw", "RAW",
                "The values the user has entered will not be parsed and will be stored as-is."),
            option("User entered", "USER_ENTERED",
                "The values will be parsed as if the user typed them into the UI. Numbers will stay as numbers, but strings may be converted to numbers, dates, etc. following the same rules that are applied when entering text into a cell via the Google Sheets UI."))
        .required(true);

    private GoogleSheetsConstants() {
    }
}
