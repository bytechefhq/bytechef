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
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.IS_THE_FIRST_ROW_HEADER_PROPERTY;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.ROW_NUMBER;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKBOOK_ID_PROPERTY;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKSHEET_NAME_PROPERTY;
import static com.bytechef.component.microsoft.excel.util.MicrosoftExcelRowUtils.getRowFromWorksheet;
import static com.bytechef.component.microsoft.excel.util.MicrosoftExcelUtils.getMapOfValuesForRow;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.microsoft.commons.MicrosoftUtils;
import java.util.List;

/**
 * @author Monika Kušter
 */
public class MicrosoftExcelFindRowByNumAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("findRowByNum")
        .title("Find Row by Number")
        .description("Get row values from the worksheet by the row number.")
        .properties(
            WORKBOOK_ID_PROPERTY,
            WORKSHEET_NAME_PROPERTY,
            IS_THE_FIRST_ROW_HEADER_PROPERTY,
            integer(ROW_NUMBER)
                .label("Row Number")
                .description("The row number to get the values from.")
                .required(true))
        .output()
        .perform(MicrosoftExcelFindRowByNumAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftExcelFindRowByNumAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        List<Object> row = getRowFromWorksheet(
            inputParameters, context, inputParameters.getRequiredInteger(ROW_NUMBER));

        return getMapOfValuesForRow(inputParameters, context, row);
    }
}
