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
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.BASE_URL;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.DELETE_ROW;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.ROW_NUMBER;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKBOOK_ID;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKBOOK_ID_PROPERTY;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKBOOK_WORKSHEETS_PATH;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKSHEET_NAME;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKSHEET_NAME_PROPERTY;
import static com.bytechef.component.microsoft.excel.util.MicrosoftExcelUtils.getLastUsedColumnLabel;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import java.util.List;

/**
 * @author Monika Domiter
 */
public class MicrosoftExcelDeleteRowAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(DELETE_ROW)
        .title("Delete row")
        .description("Delete row on an existing sheet")
        .properties(
            WORKBOOK_ID_PROPERTY,
            WORKSHEET_NAME_PROPERTY,
            integer(ROW_NUMBER)
                .label("Row number")
                .description("The row number to delete")
                .required(true))
        .perform(MicrosoftExcelDeleteRowAction::perform);

    private MicrosoftExcelDeleteRowAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        int rowNumber = inputParameters.getRequiredInteger(ROW_NUMBER);

        String range = "A" + rowNumber + ":" + getLastUsedColumnLabel(inputParameters, context) + rowNumber;

        context.http(http -> http
            .post(
                BASE_URL + "/" + inputParameters.getRequiredString(WORKBOOK_ID) + WORKBOOK_WORKSHEETS_PATH +
                    inputParameters.getRequiredString(WORKSHEET_NAME) + "/range(address='" + range + "')/delete"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(Http.Body.of(List.of("shift", "Up")
                .toArray()))
            .execute();

        return null;
    }
}
