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

package com.bytechef.component.google.sheets.util;

import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.INCLUDE_ITEMS_FROM_ALL_DRIVES;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.IS_THE_FIRST_ROW_HEADER;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.VALUES;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableStringProperty;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Monika Domiter
 */
public class GoogleSheetsUtils {

    static final HashMap<Integer, String> sheetIdMap = new HashMap<>();

    private GoogleSheetsUtils() {
    }

    public static String createRange(Integer sheetId, Integer rowNumber) {
        String sheetName = sheetIdMap.get(sheetId);

        if (rowNumber == null) {
            return sheetName;
        }

        return sheetName + "!" + rowNumber + ":" + rowNumber;
    }

    public static List<Property.ArrayProperty> createArrayPropertyForRow(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) throws IOException {

        boolean isFirstRowHeader = inputParameters.getRequiredBoolean(IS_THE_FIRST_ROW_HEADER);

        ModifiableArrayProperty updatedRow = array(VALUES)
            .label("Values")
            .required(true);

        if (isFirstRowHeader) {
            List<Object> firstRow = GoogleSheetsRowUtils.getRow(
                GoogleServices.getSheets(connectionParameters),
                inputParameters.getRequiredString(SPREADSHEET_ID),
                inputParameters.getRequiredInteger(SHEET_ID),
                1);

            List<ModifiableStringProperty> list = new ArrayList<>();

            for (Object value : firstRow) {
                list.add(string(value.toString()));
            }
            updatedRow.items(list);
        } else {
            updatedRow.items(bool(), number(), string());
        }

        return List.of(updatedRow);
    }

    public static Map<String, Object> getMapOfValuesForRow(Parameters inputParameters, Sheets sheets, List<Object> row)
        throws IOException {
        Map<String, Object> valuesMap;

        if (inputParameters.getRequiredBoolean(IS_THE_FIRST_ROW_HEADER)) {
            List<Object> firstRow = GoogleSheetsRowUtils.getRow(
                sheets,
                inputParameters.getRequiredString(SPREADSHEET_ID),
                inputParameters.getRequiredInteger(SHEET_ID),
                1);

            valuesMap = IntStream.range(0, row.size())
                .boxed()
                .collect(Collectors.toMap(i -> firstRow.get(i)
                    .toString(), row::get, (a, b) -> b, LinkedHashMap::new));
        } else {
            valuesMap = IntStream.range(0, row.size())
                .boxed()
                .collect(Collectors.toMap(i -> columnToLabel(i + 1), row::get, (a, b) -> b,
                    LinkedHashMap::new));
        }
        return valuesMap;
    }

    public static List<Option<String>> getSheetIdOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context)
        throws IOException {

        List<Option<String>> options = new ArrayList<>();

        Sheets sheets = GoogleServices.getSheets(connectionParameters);

        List<Sheet> sheetsList = sheets.spreadsheets()
            .get(inputParameters.getRequiredString(SPREADSHEET_ID))
            .execute()
            .getSheets();

        for (Sheet sheet : sheetsList) {
            SheetProperties sheetProperties = sheet.getProperties();

            sheetIdMap.put(sheetProperties.getSheetId(), sheetProperties.getTitle());

            options.add(
                option(sheetProperties
                    .getTitle(),
                    sheetProperties
                        .getSheetId()
                        .toString()));
        }

        return options;
    }

    public static List<Option<String>> getSpreadsheetIdOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context)
        throws IOException {

        Drive drive = GoogleServices.getDrive(connectionParameters);

        List<File> files = drive.files()
            .list()
            .setQ("mimeType='application/vnd.google-apps.spreadsheet'")
            .setIncludeItemsFromAllDrives(inputParameters.getBoolean(INCLUDE_ITEMS_FROM_ALL_DRIVES))
            .setSupportsAllDrives(true)
            .execute()
            .getFiles();

        List<Option<String>> options = new ArrayList<>();

        for (File file : files) {
            options.add(
                option(file.getName(), file.getId()));
        }

        return options;
    }

    private static String columnToLabel(int columnNumber) {
        StringBuilder columnName = new StringBuilder();

        while (columnNumber > 0) {
            int modulo = (columnNumber - 1) % 26;
            columnName.insert(0, (char) (65 + modulo));
            columnNumber = (columnNumber - modulo) / 26;
        }

        return "column " + columnName;
    }
}
