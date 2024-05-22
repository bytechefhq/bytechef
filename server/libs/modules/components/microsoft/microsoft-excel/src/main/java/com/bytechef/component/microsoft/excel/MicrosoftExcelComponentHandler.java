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

package com.bytechef.component.microsoft.excel;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.MICROSOFT_EXCEL;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.microsoft.excel.action.MicrosoftExcelAppendRowAction;
import com.bytechef.component.microsoft.excel.action.MicrosoftExcelClearWorksheetAction;
import com.bytechef.component.microsoft.excel.action.MicrosoftExcelDeleteRowAction;
import com.bytechef.component.microsoft.excel.action.MicrosoftExcelFindRowByNumAction;
import com.bytechef.component.microsoft.excel.action.MicrosoftExcelUpdateRowAction;
import com.bytechef.component.microsoft.excel.connection.MicrosoftExcelConnection;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(ComponentHandler.class)
public class MicrosoftExcelComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(MICROSOFT_EXCEL)
        .title("Microsoft Excel")
        .description(
            "Microsoft Excel is a spreadsheet program used for organizing, analyzing, and visualizing data in " +
                "tabular form.")
        .icon("path:assets/microsoft-excel.svg")
        .categories(ComponentCategory.PRODUCTIVITY_AND_COLLABORATION)
        .connection(MicrosoftExcelConnection.CONNECTION_DEFINITION)
        .actions(
            MicrosoftExcelAppendRowAction.ACTION_DEFINITION,
            MicrosoftExcelClearWorksheetAction.ACTION_DEFINITION,
            MicrosoftExcelDeleteRowAction.ACTION_DEFINITION,
            MicrosoftExcelFindRowByNumAction.ACTION_DEFINITION,
            MicrosoftExcelUpdateRowAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
