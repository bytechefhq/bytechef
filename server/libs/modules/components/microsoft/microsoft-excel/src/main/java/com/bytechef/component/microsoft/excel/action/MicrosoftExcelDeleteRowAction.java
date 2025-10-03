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
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.ROW_NUMBER;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKBOOK_ID;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKBOOK_ID_PROPERTY;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKSHEET_NAME;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKSHEET_NAME_PROPERTY;
import static com.bytechef.component.microsoft.excel.util.MicrosoftExcelUtils.getLastUsedColumnLabel;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.microsoft.commons.MicrosoftUtils;
import java.util.List;

/**
 * @author Monika Domiter
 */
public class MicrosoftExcelDeleteRowAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteRow")
        .title("Delete Row")
        .description("Delete row on an existing sheet.")
        .properties(
            WORKBOOK_ID_PROPERTY,
            WORKSHEET_NAME_PROPERTY,
            integer(ROW_NUMBER)
                .label("Row Number")
                .description("The row number to delete.")
                .required(true))
        .perform(MicrosoftExcelDeleteRowAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftExcelDeleteRowAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        int rowNumber = inputParameters.getRequiredInteger(ROW_NUMBER);

        String range = "A" + rowNumber + ":" + getLastUsedColumnLabel(inputParameters, context) + rowNumber;

        context.http(http -> http
            .post(
                "/me/drive/items/%s/workbook/worksheets/%s/range(address='%s')/delete"
                    .formatted(inputParameters.getRequiredString(WORKBOOK_ID),
                        inputParameters.getRequiredString(WORKSHEET_NAME), range)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(Http.Body.of(List.of("shift", "Up")
                .toArray()))
            .execute();

        return null;
    }
}
