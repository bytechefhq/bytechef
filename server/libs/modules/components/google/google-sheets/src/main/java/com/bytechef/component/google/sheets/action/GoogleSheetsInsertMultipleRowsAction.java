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
import static com.bytechef.component.definition.ComponentDSL.dynamicProperties;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.INCLUDE_ITEMS_FROM_ALL_DRIVES_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.IS_THE_FIRST_ROW_HEADER;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.IS_THE_FIRST_ROW_HEADER_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.ROWS;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.VALUES;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.VALUE_INPUT_OPTION;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.VALUE_INPUT_PROPERTY;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.createRange;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.getMapOfValuesForRow;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.sheets.util.GoogleSheetsUtils;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Monika Kušter
 */
public class GoogleSheetsInsertMultipleRowsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("insertMultipleRows")
        .title("Insert multiple rows")
        .description("Append rows to the end of the spreadsheet.")
        .properties(
            SPREADSHEET_ID_PROPERTY,
            INCLUDE_ITEMS_FROM_ALL_DRIVES_PROPERTY,
            SHEET_NAME_PROPERTY,
            VALUE_INPUT_PROPERTY,
            IS_THE_FIRST_ROW_HEADER_PROPERTY,
            dynamicProperties(ROWS)
                .propertiesLookupDependsOn(SPREADSHEET_ID, SHEET_NAME, IS_THE_FIRST_ROW_HEADER)
                .properties(GoogleSheetsUtils.createPropertiesForNewRows(false))
                .required(true))
        .outputSchema(
            array()
                .items(
                    object()
                        .additionalProperties(bool(), number(), string())))
        .perform(GoogleSheetsInsertMultipleRowsAction::perform);

    private GoogleSheetsInsertMultipleRowsAction() {
    }

    public static List<Map<String, Object>> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws Exception {

        Sheets sheets = GoogleServices.getSheets(connectionParameters);

        List<List<Object>> rows = getNewRowsValues(inputParameters);

        ValueRange valueRange = new ValueRange()
            .setValues(rows)
            .setMajorDimension("ROWS");

        sheets.spreadsheets()
            .values()
            .append(
                inputParameters.getRequiredString(SPREADSHEET_ID),
                createRange(inputParameters.getRequiredString(SHEET_NAME), null), valueRange)
            .setValueInputOption(inputParameters.getRequiredString(VALUE_INPUT_OPTION))
            .execute();

        List<Map<String, Object>> newRows = new ArrayList<>();

        for (List<Object> row : rows) {
            newRows.add(getMapOfValuesForRow(inputParameters, sheets, row));
        }

        return newRows;
    }

    private static List<List<Object>> getNewRowsValues(Parameters inputParameters) {
        List<List<Object>> rows = new ArrayList<>();

        Map<String, Object> rowMap = inputParameters.getRequiredMap(ROWS, Object.class);

        Object values = rowMap.get(VALUES);

        if (values instanceof List<?> list) {
            for (Object object : list) {
                if (object instanceof List<?> row) {
                    List<Object> list2 = row.stream()
                        .map(item -> (Object) item)
                        .toList();

                    rows.add(list2);
                } else if (object instanceof Map<?, ?> map) {
                    List<Object> valuesList = map.values()
                        .stream()
                        .map(value -> (Object) value)
                        .collect(Collectors.toList());

                    rows.add(valuesList);
                }
            }
        }
        return rows;
    }
}
