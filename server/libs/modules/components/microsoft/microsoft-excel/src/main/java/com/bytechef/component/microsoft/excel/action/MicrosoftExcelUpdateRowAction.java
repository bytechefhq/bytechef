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

package com.bytechef.component.microsoft.excel.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.IS_THE_FIRST_ROW_HEADER_PROPERTY;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.ROW_DYNAMIC_PROPERTY;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.ROW_NUMBER;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.UPDATE_ROW;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKBOOK_ID_PROPERTY;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKSHEET_NAME_PROPERTY;
import static com.bytechef.component.microsoft.excel.util.MicrosoftExcelUpdateWorksheetUtils.updateRange;
import static com.bytechef.component.microsoft.excel.util.MicrosoftExcelUtils.getRowInputValues;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Domiter
 */
public class MicrosoftExcelUpdateRowAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(UPDATE_ROW)
        .title("Update row")
        .description("Update a row in a worksheet")
        .properties(
            WORKBOOK_ID_PROPERTY,
            WORKSHEET_NAME_PROPERTY,
            integer(ROW_NUMBER)
                .label("Row number")
                .description("The row number to update")
                .required(true),
            IS_THE_FIRST_ROW_HEADER_PROPERTY,
            ROW_DYNAMIC_PROPERTY)
        .outputSchema(
            object()
                .additionalProperties(bool(), number(), string()))
        .perform(MicrosoftExcelUpdateRowAction::perform);

    private MicrosoftExcelUpdateRowAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return updateRange(
            inputParameters, context, inputParameters.getRequiredInteger(ROW_NUMBER),
            getRowInputValues(inputParameters));
    }

}
