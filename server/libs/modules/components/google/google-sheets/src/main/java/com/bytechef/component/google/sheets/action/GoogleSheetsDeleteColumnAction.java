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
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.DELETE_COLUMN;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.DELETE_COLUMN_DESCRIPTION;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.DELETE_COLUMN_TITLE;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.LABEL;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_ID_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID_PROPERTY;
import static com.bytechef.component.google.sheets.util.GoogleSheetsColumnConverterUtils.labelToColumn;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.deleteDimension;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Marija Horvat
 */
public class GoogleSheetsDeleteColumnAction {

    @SuppressFBWarnings("MS")
    public static final Property[] PROPERTIES = {
        SPREADSHEET_ID_PROPERTY,
        SHEET_ID_PROPERTY,
        string(LABEL)
            .label("Column Label")
            .description("The label of the column to be deleted.")
            .exampleValue("A")
            .required(true)
    };

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(DELETE_COLUMN)
        .title(DELETE_COLUMN_TITLE)
        .description(DELETE_COLUMN_DESCRIPTION)
        .properties(PROPERTIES)
        .perform(GoogleSheetsDeleteColumnAction::perform);

    private GoogleSheetsDeleteColumnAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context)
        throws Exception {

        Integer columnNumber = labelToColumn(inputParameters.getRequiredString(LABEL));

        deleteDimension(inputParameters, connectionParameters, columnNumber, "COLUMNS");

        return null;
    }
}
