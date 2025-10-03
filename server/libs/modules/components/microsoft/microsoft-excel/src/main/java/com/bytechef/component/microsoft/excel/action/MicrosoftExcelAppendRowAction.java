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

package com.bytechef.component.microsoft.excel.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.IS_THE_FIRST_ROW_HEADER;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.IS_THE_FIRST_ROW_HEADER_PROPERTY;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.ROW;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKBOOK_ID;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKBOOK_ID_PROPERTY;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKSHEET_NAME;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKSHEET_NAME_PROPERTY;
import static com.bytechef.component.microsoft.excel.util.MicrosoftExcelUpdateWorksheetUtils.updateRange;
import static com.bytechef.component.microsoft.excel.util.MicrosoftExcelUtils.getLastUsedRowIndex;
import static com.bytechef.component.microsoft.excel.util.MicrosoftExcelUtils.getRowValues;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.microsoft.excel.util.MicrosoftExcelUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;

/**
 * @author Monika Kušter
 */
public class MicrosoftExcelAppendRowAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("appendRow")
        .title("Append Row")
        .description("Append a row of values to an existing worksheet.")
        .properties(
            WORKBOOK_ID_PROPERTY,
            WORKSHEET_NAME_PROPERTY,
            IS_THE_FIRST_ROW_HEADER_PROPERTY,
            dynamicProperties(ROW)
                .propertiesLookupDependsOn(IS_THE_FIRST_ROW_HEADER, WORKSHEET_NAME, WORKBOOK_ID)
                .properties(MicrosoftExcelUtils::createPropertiesForNewRow)
                .required(true))
        .output()
        .perform(MicrosoftExcelAppendRowAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftExcelAppendRowAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return updateRange(
            inputParameters, context, getLastUsedRowIndex(inputParameters, context) + 1,
            getRowValues(inputParameters));
    }
}
