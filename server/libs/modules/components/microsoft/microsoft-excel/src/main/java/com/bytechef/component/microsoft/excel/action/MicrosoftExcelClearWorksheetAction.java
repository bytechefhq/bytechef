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
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.IS_THE_FIRST_ROW_HEADER;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.IS_THE_FIRST_ROW_HEADER_PROPERTY;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKBOOK_ID;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKBOOK_ID_PROPERTY;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKSHEET_NAME;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKSHEET_NAME_PROPERTY;
import static com.bytechef.component.microsoft.excel.util.MicrosoftExcelUtils.getLastUsedColumnLabel;
import static com.bytechef.component.microsoft.excel.util.MicrosoftExcelUtils.getLastUsedRowIndex;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.microsoft.commons.MicrosoftUtils;
import java.util.List;

/**
 * @author Monika Kušter
 */
public class MicrosoftExcelClearWorksheetAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("clearWorksheet")
        .title("Clear Worksheet")
        .description("Clear a worksheet of all values.")
        .properties(
            WORKBOOK_ID_PROPERTY,
            WORKSHEET_NAME_PROPERTY,
            IS_THE_FIRST_ROW_HEADER_PROPERTY)
        .perform(MicrosoftExcelClearWorksheetAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftExcelClearWorksheetAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String range = inputParameters.getRequiredBoolean(IS_THE_FIRST_ROW_HEADER)
            ? "range(address='A2:" + getLastUsedColumnLabel(inputParameters, context)
                + getLastUsedRowIndex(inputParameters, context) + "')"
            : "usedRange(valuesOnly=true)";

        context.http(http -> http
            .post(
                "/me/drive/items/%s/workbook/worksheets/%s/%s/clear"
                    .formatted(
                        inputParameters.getRequiredString(WORKBOOK_ID),
                        inputParameters.getRequiredString(WORKSHEET_NAME), range)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(Http.Body.of(
                List.of("applyTo", "Contents")
                    .toArray()))
            .execute();

        return null;
    }
}
