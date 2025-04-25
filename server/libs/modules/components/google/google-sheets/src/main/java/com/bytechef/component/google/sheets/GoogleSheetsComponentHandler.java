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

package com.bytechef.component.google.sheets;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;
import static com.bytechef.component.google.sheets.connection.GoogleSheetsConnection.CONNECTION_DEFINITION;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.google.sheets.action.GoogleSheetsClearSheetAction;
import com.bytechef.component.google.sheets.action.GoogleSheetsCreateColumnAction;
import com.bytechef.component.google.sheets.action.GoogleSheetsCreateSheetAction;
import com.bytechef.component.google.sheets.action.GoogleSheetsCreateSpreadsheetAction;
import com.bytechef.component.google.sheets.action.GoogleSheetsDeleteColumnAction;
import com.bytechef.component.google.sheets.action.GoogleSheetsDeleteRowAction;
import com.bytechef.component.google.sheets.action.GoogleSheetsDeleteSheetAction;
import com.bytechef.component.google.sheets.action.GoogleSheetsFindRowByNumAction;
import com.bytechef.component.google.sheets.action.GoogleSheetsGetRowsAction;
import com.bytechef.component.google.sheets.action.GoogleSheetsInsertMultipleRowsAction;
import com.bytechef.component.google.sheets.action.GoogleSheetsInsertRowAction;
import com.bytechef.component.google.sheets.action.GoogleSheetsListSheetsAction;
import com.bytechef.component.google.sheets.action.GoogleSheetsUpdateRowAction;
import com.bytechef.component.google.sheets.trigger.GoogleSheetsNewRowTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(ComponentHandler.class)
public class GoogleSheetsComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("googleSheets")
        .title("Google Sheets")
        .description(
            "Google Sheets is a cloud-based spreadsheet software that allows users to create, edit, and collaborate " +
                "on spreadsheets in real-time.")
        .customAction(true)
        .icon("path:assets/google-sheets.svg")
        .categories(ComponentCategory.PRODUCTIVITY_AND_COLLABORATION)
        .connection(CONNECTION_DEFINITION)
        .actions(
            GoogleSheetsClearSheetAction.ACTION_DEFINITION,
            GoogleSheetsCreateColumnAction.ACTION_DEFINITION,
            GoogleSheetsCreateSheetAction.ACTION_DEFINITION,
            GoogleSheetsCreateSpreadsheetAction.ACTION_DEFINITION,
            GoogleSheetsDeleteColumnAction.ACTION_DEFINITION,
            GoogleSheetsDeleteRowAction.ACTION_DEFINITION,
            GoogleSheetsDeleteSheetAction.ACTION_DEFINITION,
            GoogleSheetsFindRowByNumAction.ACTION_DEFINITION,
            GoogleSheetsGetRowsAction.ACTION_DEFINITION,
            GoogleSheetsInsertMultipleRowsAction.ACTION_DEFINITION,
            GoogleSheetsInsertRowAction.ACTION_DEFINITION,
            GoogleSheetsListSheetsAction.ACTION_DEFINITION,
            GoogleSheetsUpdateRowAction.ACTION_DEFINITION)
        .clusterElements(
            tool(GoogleSheetsClearSheetAction.ACTION_DEFINITION),
            tool(GoogleSheetsCreateColumnAction.ACTION_DEFINITION),
            tool(GoogleSheetsCreateSheetAction.ACTION_DEFINITION),
            tool(GoogleSheetsCreateSpreadsheetAction.ACTION_DEFINITION),
            tool(GoogleSheetsDeleteColumnAction.ACTION_DEFINITION),
            tool(GoogleSheetsDeleteRowAction.ACTION_DEFINITION),
            tool(GoogleSheetsDeleteSheetAction.ACTION_DEFINITION),
            tool(GoogleSheetsFindRowByNumAction.ACTION_DEFINITION),
            tool(GoogleSheetsGetRowsAction.ACTION_DEFINITION),
            tool(GoogleSheetsInsertMultipleRowsAction.ACTION_DEFINITION),
            tool(GoogleSheetsInsertRowAction.ACTION_DEFINITION),
            tool(GoogleSheetsListSheetsAction.ACTION_DEFINITION),
            tool(GoogleSheetsUpdateRowAction.ACTION_DEFINITION))
        .triggers(GoogleSheetsNewRowTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
