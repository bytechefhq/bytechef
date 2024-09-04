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

package com.bytechef.component.google.sheets;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.google.sheets.connection.GoogleSheetsConnection.CONNECTION_DEFINITION;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.google.sheets.action.GoogleSheetsClearSheetAction;
import com.bytechef.component.google.sheets.action.GoogleSheetsCreateColumnAction;
import com.bytechef.component.google.sheets.action.GoogleSheetsCreateSheetAction;
import com.bytechef.component.google.sheets.action.GoogleSheetsDeleteRowAction;
import com.bytechef.component.google.sheets.action.GoogleSheetsFindRowByNumAction;
import com.bytechef.component.google.sheets.action.GoogleSheetsInsertMultipleRowsAction;
import com.bytechef.component.google.sheets.action.GoogleSheetsInsertRowAction;
import com.bytechef.component.google.sheets.action.GoogleSheetsUpdateRowAction;
import com.bytechef.component.google.sheets.trigger.GoogleSheetsOnRowAddedTrigger;
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
        .icon("path:assets/google-sheets.svg")
        .categories(ComponentCategory.PRODUCTIVITY_AND_COLLABORATION)
        .connection(CONNECTION_DEFINITION)
        .actions(
            GoogleSheetsClearSheetAction.ACTION_DEFINITION,
            GoogleSheetsCreateColumnAction.ACTION_DEFINITION,
            GoogleSheetsCreateSheetAction.ACTION_DEFINITION,
            GoogleSheetsDeleteRowAction.ACTION_DEFINITION,
            GoogleSheetsFindRowByNumAction.ACTION_DEFINITION,
            GoogleSheetsInsertMultipleRowsAction.ACTION_DEFINITION,
            GoogleSheetsInsertRowAction.ACTION_DEFINITION,
            GoogleSheetsUpdateRowAction.ACTION_DEFINITION)
        .triggers(GoogleSheetsOnRowAddedTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
